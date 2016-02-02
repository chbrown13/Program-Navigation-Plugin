package dataTool;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import dataTool.annotations.SuggestedSelectionAnnotation;

public class Finder {
	
	final public static String UP = "up";
	final public static String DOWN = "down";
	
	private static String findDirection = UP; //default direction is up
	private static Finder currentFinder;
	private static Color currentColor;
	private String goToName = null;
	private int goToOffset = -1;
	
	public Finder () {
		//Do nothing
	}
	
	public void setGoToIndex(int offset) {
		System.out.println(offset + " off");
		goToOffset = offset;
	}
	
	public void setGoToFunc(String name) {
		goToName = name;
	}
	
	public int getGoToIndex() {
		if(goToOffset > 0) {
			return goToOffset;
		}
		return -1;
	}
		
	public String getGoToFunc() {
		if(goToName != null) {
			return goToName;
		}
		return null;
	}
	
	public ArrayList<DataNode> getOccurrences(String data) {
		if(findDirection.equals(UP)) {
			UpFinder finder = UpFinder.getInstance();
			return finder.getUpOccurrences(data);
		}
		else if(findDirection.equals(DOWN)) {
			DownFinder finder = DownFinder.getInstance();
			return finder.getDownOccurrences(data);
		}
		return null;
	}
	
	/**
	 * Controls which flow the tool will navigate to show data flow
	 * @param s: Direction for flow display, required to be UP or DOWN
	 */
	/*public void setFlowDirection(String s) {
		if(s.equals(UP)) {
			findDirection = s;
			currentFinder = UpFinder.getInstance();
			SuggestedSelectionAnnotation.color = new Color(null, 0, 0, 255);
		}
		else if(s.equals(DOWN)) {
			findDirection = s;
			currentFinder = DownFinder.getInstance();
			SuggestedSelectionAnnotation.color = new Color(null, 255, 0, 0);
		}
		else {
			//something went very wrong...
			findDirection = null;
		}
	}*/
	
	/**
	 * Function to return the current direction of the flow navigation.
	 */
	public String getFlowDirection() {
		return findDirection;
	}
	
	/**
	 * Function that checks if selected text is a variable
	 * @param str: String value of current text
	 * @returns true if current variable is a DataNode, else false
	 */
	public boolean contains(String str) {
		return currentFinder.contains(str);
	}
	
	/**
	 * Function to get the finder instance
	 * @returns the current finder instance searching in the appropriate direction.
	 */
	public static Finder getInstance() {
		if(findDirection.equals(UP)) {
			return UpFinder.getInstance();
		}
		else if(findDirection.equals(DOWN)){
			return DownFinder.getInstance();
		}
		return null;
	}
}
