/**
 * Class to statically analyze the source code and handle the visited nodes.
 * 
 * @author emerson, Chris
 */
package dataTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

import javax.xml.transform.Source;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jface.text.Position;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import edu.pdx.cs.multiview.jdt.util.JDTUtils;

class Visitor extends ASTVisitor {

	private StackMap<Position, DataNode> nodes = new StackMap<Position, DataNode>();
	/**
	 *  Method SimpleName mapped to a list of 2 lists:
	 *  The first list is the parameters in the method declaration
	 *  The second list is the arguments passed in by the invoked method
	 *  
	 *  Ex. methodArgsVSInvokedArgs.get("TestMethodName").get(0) returns the list
	 *  of args in the method declaration.
	 */
	private HashMap<String, List<List<SimpleName>>> methodArgsVSInvokedArgs = 
									new HashMap<String, List<List<SimpleName>>>();
	private Finder finder;

	private static String source;

	public Visitor(String someSource) {
		this.source = someSource;
		parseData();
	}

	/**
	 * Function that returns the source code
	 * 
	 * @return- source code as a str
	 */
	public String getSource() {
		return source;
	}

	/**
	 * Searches through code to find instances of the current variable
	 * 
	 * @param currentData:
	 *            String of current data selected
	 */
	private void addOccurrences(DataNode dn) {
		nodes.put(new Position(dn.getStartPosition(), dn.getLength()), dn);
		finder.add(dn);
	}

	/**
	 * Method that parses the source statically to get the data we want for the
	 * plugin. This function searches for all parameters and declared variables
	 * and adds them to the data list.
	 * 
	 * http://stackoverflow.com/questions/15308080/how-to-get-all-visible-
	 * variables-for-a-certain-method-in-jdt
	 * 
	 * @param str
	 */
	public void parseData() {
		IProject [] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		IJavaProject thisProject = JavaCore.create(projects[0]);
		char[] code = source.toCharArray();
		finder = Finder.getInstance();
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(code);
		parser.setResolveBindings(true);
		parser.setProject(thisProject);
		
		
		parser.setUnitName(thisProject.getElementName());
		
		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		
		cu.accept(new ASTVisitor() {
			DataNode addedNode = null;
			SimpleName method = null;
			public boolean visit( SimpleName sn ) {
				
				if( sn.resolveTypeBinding() != null ) {
					
					String binding = sn.resolveBinding().toString();
//					if( binding.matches("^[a-zA-Z_$][a-zA-Z_$0-9]*$\\[pos: [0-9]*\\]\\[id:[0-9]*\\]\\[pc: [0-9]*-[0-9]*\\]")) {
//					}
					
//					if( binding.contains("[pos:") && binding.contains("][id:") && binding.contains("][pc:" )) {
					
					// Filters out methods and Object type declarations
					if( sn.isDeclaration() && binding.contains("(")) {
						method = sn;
					} else if( !binding.contains("class" ) && !binding.contains("(")) {	
						addedNode = new DataNode( sn, method );
						addedNode.setStartPosition(cu.getExtendedStartPosition(sn) );
						addOccurrences(addedNode);
					}
				}
				return true;
			}

			public boolean visit(MethodDeclaration md) {
				
				method = md.getName();
				
				String methodBinding = method.resolveBinding().toString();
				List<SimpleName> args = new ArrayList<SimpleName>();
				for (Object o : md.parameters()) {
					SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
					args.add(0, svd.getName());
				}
				if( methodArgsVSInvokedArgs.containsKey( methodBinding ) ) {
					List argLists = methodArgsVSInvokedArgs.get(methodBinding);
					argLists.add(0, args);
				} else {
					ArrayList argLists = new ArrayList<List<SimpleName>>();
					argLists.add(0, args);
					methodArgsVSInvokedArgs.put(methodBinding, argLists);
				}
				md.accept(new ASTVisitor() {
					public boolean visit(MethodInvocation mi) {
						String invokedBinding = mi.getName().resolveBinding().toString();
						
						List<SimpleName> args = mi.arguments();
						if( methodArgsVSInvokedArgs.containsKey(invokedBinding ) ) {
							List argLists = methodArgsVSInvokedArgs.get(invokedBinding);
							// When the declaration is added, will push invoked to the right
							argLists.add(args);
						} else {
							ArrayList argLists = new ArrayList<List<SimpleName>>();
							argLists.add(args);
							methodArgsVSInvokedArgs.put(invokedBinding, argLists);
						}
						return true;
					}
				});
				//Set to null so that class variables can have a null method name
				method = null;
				return true;
			}
		});
		finder.setMethodArgsVSInvokedArgs(methodArgsVSInvokedArgs);
	}


	/**
	 * Adds the current node to list of nodes to be highlighted
	 * 
	 * @param node
	 */
	private void add(ASTNode node) {
		int start = node.getStartPosition();
		int length = node.getLength();
	}

	private void parseEquals( String statement ) {
		
	}
	public DataNode statementAt(int index) {
		for (Position p : nodes.keyStack()) {
			boolean isContained = p.offset <= index && index < p.offset + p.length;
			if (isContained) {
				return nodes.get(p);
			}
		}
		return null;
	}

	private class StackMap<K, V> extends HashMap<K, V> {

		private static final long serialVersionUID = -266310554828357936L;
		private Stack<K> stack = new BackwardStack<K>();

		@Override
		public V put(K arg0, V arg1) {
			stack.push(arg0);
			return super.put(arg0, arg1);
		}

		public Stack<K> keyStack() {
			return stack;
		}
	}

	/**
	 * A stack that you can iterate backwards through
	 * 
	 * @author emerson
	 *
	 * @param <X>
	 */
	static class BackwardStack<X> extends Stack<X> {

		private static final long serialVersionUID = -8981676925135756869L;

		@Override
		public Iterator<X> iterator() {

			List<X> list = new ArrayList<X>(this.size());
			for (int i = size() - 1; i >= 0; i--)
				list.add(get(i));

			return list.iterator();
		}
	}
}