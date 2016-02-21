package dataTool.annotations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.ui.JavaUI;

import dataTool.DataCallHierarchy;
import dataTool.DataNode;
import dataTool.EnableNavigationAction;
import dataTool.Finder;
import dataTool.ui.NavigationDownBox;
import dataTool.ui.NavigationUpBox;
import edu.pdx.cs.multiview.jdt.util.JDTUtils;
import edu.pdx.cs.multiview.jface.annotation.ISelfDrawingAnnotation;

public class LinkAnnotation extends Annotation implements ISelfDrawingAnnotation {
	
	private DataNode linkNode;
	private static ASTNode searchResult;
	public static Set<IMethod> searchResultsUp;
	public static Set<IMethod> searchResultsDown;
	public String searchMethod = "";

	/**
	 * Function to add link annotations to methods of parameters
	 */
	public void draw(GC gc, StyledText textWidget, int offset, int length) {		
		StyleRange style = new StyleRange();
		style.start = offset;
		style.length = length;
		style.underline = true;
		style.underlineStyle = SWT.UNDERLINE_LINK;
		textWidget.setStyleRange(style);
		textWidget.addMouseListener(new MouseListener(){

			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
				mouseDown(arg0);
			}

			@Override
			public void mouseDown(MouseEvent arg0) {
				int click = textWidget.getOffsetAtLocation(new Point(arg0.x,arg0.y));
				if(click >= style.start && click <= style.start+style.length){
					Object[] search;
					IMethod im;
					System.out.println("clicked");
					if(linkNode.getDeclarationMethod() != null) {
						try {
							search = searchResultsUp.toArray();
							im = (IMethod)search[0];
							searchMethod = linkNode.getDeclarationMethod().getName().getIdentifier();
							openLink(im);
						} catch (Exception e) {
							// Auto-generated catch block
							e.printStackTrace();
						}
					}
					else if(linkNode.getInvocationMethod() != null) {
						//TODO Down
						/*try {
							search = searchResultsDown.toArray();
							for(Object o: search) {
								im = (IMethod) o;
								if(im.getElementName().equals(linkNode.getParameterMethod().getName().getIdentifier())) {
									NavigationDownBox.getInstance().openLink(im);
									break;
								}
							}
						} catch (Exception e) {
							// Auto-generated catch block
							e.printStackTrace();
						}*/
					}
					//EnableNavigationAction plugin = new EnableNavigationAction();
	    			//try {
						//plugin.reset(null);
					//} catch (JavaModelException e) {
						// Auto-generated catch block
					//	e.printStackTrace();
					//}
				}
			}

			@Override
			public void mouseUp(MouseEvent arg0) {
				// Do nothing
				
			}
			
		});
	}
	
	/**
	 * Opens invocation of new method in the editor and clears navigation box links
	 * @param i: IMethod to open
	 */
	public void openLink(IMethod i) {
		IEditorPart editor = null;
		try {
			editor = JavaUI.openInEditor(i, true, true);
		} catch (JavaModelException | PartInitException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
		//if(editor != null) {
			//String code = JDTUtils.getCUSource((AbstractTextEditor) editor);
			//lineSearch(code.toCharArray(), i);
			//goToLine(editor);
		//}
	}
	
	private void lineSearch(char[] source, IMethod method) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(source);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		cu.accept(new ASTVisitor(){
			public boolean visit(MethodDeclaration md) {
				String methodName = md.getName().getIdentifier();
				md.accept(new ASTVisitor() {
				public boolean visit(MethodInvocation m) {
					if(method.getElementName().equals(methodName)) {
						if(m.getName().getIdentifier().equals(searchMethod)) {
							searchResult = m;
						}
					}
					return true;
				}
				public boolean visit(ClassInstanceCreation c) {
					//System.out.println("  "+c.getType().toString() +" " + AnnotationManager.currentSearch);
					return true;
				}
			});
				return true;
		}
		});
	}	
	/**
	 * Opens the new class at the specific line
	 * http://stackoverflow.com/questions/2873879/eclipe-pde-jump-to-line-x-and-highlight-it
	 * @param editorPart: current editor
	 * @param lineNumber: line number of method invocation
	 */
	private static void goToLine(IEditorPart editorPart) {
		  if (!(editorPart instanceof ITextEditor)) {
		    return;
		  }
		  ITextEditor editor = (ITextEditor) editorPart;
		  IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		  if (document != null && searchResult != null) {
		    	editor.selectAndReveal(searchResult.getStartPosition(), searchResult.getLength());
		  }
		}
	
	/**
	 * Removes the link annotation from the editor
	 * @param old: StyleRange for a link annotation to a method
	 * @returns StyleRange removing the link from method
	 */
	public static StyleRange removeAnnotation(StyleRange old) {
		StyleRange clear = new StyleRange();
		clear.start = old.start;
		clear.length = old.length;
		clear.underline = false;
		return clear;
	}
	
	public void setDataNode(DataNode node) {
		linkNode = node;
	}

}
