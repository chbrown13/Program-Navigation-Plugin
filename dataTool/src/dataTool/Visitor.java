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
import java.util.Stack;

import javax.xml.transform.Source;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jface.text.Position;

import edu.pdx.cs.multiview.jdt.util.JDTUtils;

class Visitor extends ASTVisitor{
	
	private StackMap<Position,ASTNode> nodes = new StackMap<Position,ASTNode>();
	private static HashSet<String> data = new HashSet<String>();
	private static ArrayList<SimpleName> seenMethod = new ArrayList<SimpleName>();
	
	private String source;
	
	public Visitor(String someSource) {
		this.source = someSource;
		parseData(source.toCharArray());
		Finder finder = new Finder(Finder.UP);
		finder.initialize(data, source);
	}

	public void preVisit(ASTNode node){
		if(shouldVisit(node)) {
			add(node);
		}
	}
	
	/**
	 *  Function that returns the source code
	 * @return- source code as a str
	 */
	public String getSource() {
		return source;
	}
	
	/**
	 * Method to determine if the current node should be highlighted
	 * @param node
	 * @return boolean: True if selected node should be highlighted, else false
	 */
	private boolean shouldVisit(ASTNode node) {
		if (node instanceof SimpleName) {
			String name = ((SimpleName) node).getIdentifier();
			return data.contains(name);
		}
		return false;
	}
	
	/**
	 * Method that parses the source statically to get the data we want for the 
	 * plugin. This function searches for all parameters and declared variables
	 * and adds them to the data list.
	 * 
	 * http://stackoverflow.com/questions/15308080/how-to-get-all-visible-variables-for-a-certain-method-in-jdt
	 * @param str
	 */
	public static void parseData(char[] str) {	
		UpFinder finder = UpFinder.getInstance();
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(str);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		cu.accept(new ASTVisitor() {

		    public boolean visit(VariableDeclarationFragment vdf) {
		    	String var = vdf.getName().getIdentifier();
		        if (!data.contains(var)) {
		        	data.add(var);
		        	finder.add(var, vdf.getName());
		        }
		        return true;
		    }

		    public boolean visit(MethodDeclaration md) {
		    	if (!seenMethod.contains(md.getName())) {
		    		seenMethod.add(md.getName());
		    		String param = "";
		    		for (Object o: md.parameters()) {
		    			param = ((SingleVariableDeclaration) o).getName().getIdentifier();
		    			data.add(param);
		    			finder.add(param, (SingleVariableDeclaration) o);
		    		}
		    		md.accept(new ASTVisitor() {
			                public boolean visit(VariableDeclarationFragment vdf2) {
			                	String var2 = vdf2.getName().getIdentifier();
			                	//System.out.println("in method: " + vdf);
			                	if (!data.contains(var2)) {
			                		data.add(var2);
			                		finder.add(var2, vdf2.getName());
			                	}
			                    return true;
			                }
			        });
		    	}
			    return true;
		    }
		});
	}
	
	/**
	 * Adds the current node to list of nodes to be highlighted
	 * @param node
	 */
	private void add(ASTNode node) {
		int start = node.getStartPosition();
		int length = node.getLength();
		nodes.put(new Position(start,length),node);
	}
	
	public ASTNode statementAt(int index){
		for(Position p : nodes.keyStack()){			
			boolean isContained = p.offset <= index && index < p.offset + p.length;								
			if(isContained){
				return nodes.get(p);
			}
		}
		return null;
	}
	
	private class StackMap<K,V> extends HashMap<K, V>{
		
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
	static class BackwardStack<X> extends Stack<X>{

		private static final long serialVersionUID = -8981676925135756869L;

		@Override
		public Iterator<X> iterator(){

			List<X> list = new ArrayList<X>(this.size());
			for(int i = size()-1; i>=0; i--)
				list.add(get(i));
			
			return list.iterator();
		}
	}
}