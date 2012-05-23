package org.dtk.analysis.script;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.FunctionNode;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.Token;

/**
 * Abstract class to set up parsing of JavaScript source
 * files by the Closure compiler. 
 * 
 * Set ups internal state to turn source file into an AST
 * and recursively walk the tree, calling the "parseNode"  
 * method on finding each node. Extending classes will implement
 * the "parseNode" method to perform actions based on the nodes found.
 * 
 * Also provides a series of utility method to assist processing
 * AST nodes for common tasks, e.g accessing function arguments.
 * 
 * @author James Thomas
 */

public abstract class BaseScriptParser {
	
	/**
	 * JavaScript source text to be parsed.
	 */
	protected String scriptSource;
	
	/**
	 * Set of nodes that have already been visiting 
	 * by the AST walker.
	 */
	Set<Node> parsedNodes = new HashSet<Node>();
	
	/**
	 * Default constructor, passing in JavaScript source
	 * as a text string.
	 * 
	 * @param scriptSource - JavaScript text
	 */
	public BaseScriptParser(String scriptSource) {
		this.scriptSource = scriptSource;		
	}
	
	/**
	 * Start the recursively parsing of the JavaScript 
	 * text source. Sets up the new parsing environment and 
	 * begins to walk the AST generate from the parsed source.
	 * 
	 * @throws EvaluatorException - Fatal exception thrown while
	 * parsing JavaScript source
	 */
	protected void parse() throws EvaluatorException {
		// Parse script source 
		CompilerEnvirons ce = new CompilerEnvirons(); 
		ScriptParserErrorReporter errorReporter = new ScriptParserErrorReporter();
		
		ce.setGenerateDebugInfo(true);		
		ce.initFromContext(ContextFactory.getGlobal().enterContext());
		ce.setErrorReporter(errorReporter);
		
		Parser p = new Parser(ce, errorReporter); 
		ScriptOrFnNode ast = p.parse(this.scriptSource, "script", 0);
		
		searchAstForNodes(ast);
	}
	
	/** 
	 * Depth-first search of parsed JavaScript source
	 * to discover all available AST node references.
	 * 
	 * If a node has children or is a top-level script node, 
	 * recursively search those before proceeding.
	 * 
	 * Every discovered node is passed to the abstract "parseNode"
	 * method for processing.    
	 * 
	 * After processing, move on "next" link for current node.
	 * 
	 * @param node - AST Node
	 */
	protected void searchAstForNodes(Node node) {
		// Ignore null nodes or those we have seen before!
		if (node == null || parsedNodes.contains(node)) {
			return;
		}
		
		// Script level nodes have global list of function definitions. 
		// Parse inner tokens for function definitions here.
		if (node instanceof ScriptOrFnNode) {
			searchFunctionDefsForNodes((ScriptOrFnNode) node);			
		}
		
		// Parse children from left to right.
		searchAstForNodes(node.getFirstChild());
		searchAstForNodes(node.getLastChild());
		
		// Parse this node and then move along to next
		parseNode(node);
		parsedNodes.add(node);
		
		searchAstForNodes(node.getNext());		
	}
	
	/**
	 * Abstract method for processing each node discovered
	 * during AST traversal. Guaranteed to only be called once 
	 * per node.
	 * 
	 * @param node - Discovered node.
	 */
	abstract protected void parseNode(Node node);
	
	/**
	 * Search for nodes in each function definition under
	 * the script node.  
	 * 
	 * @param sofn - Script or function node
	 */
	protected void searchFunctionDefsForNodes(ScriptOrFnNode sofn) {
		int count = sofn.getFunctionCount() - 1;
		while (count > -1) {
			FunctionNode fn = sofn.getFunctionNode(count);
			searchAstForNodes(fn);
			count--;
		}
	}
	
	/**
	 * Return function parameter value, at the given index, assuming it is 
	 * a string. Parameter must be a string primitive, we won't perform any 
	 * indirect resolution to access value.
	 * 
	 * @param functionCallNodes - AST Nodes containing function arguments
	 * @param argIndex - Argument index to access
	 * @return String argument value, null if not available or not a string.
	 */
	protected String getFnCallArgumentStr(Node fnCallNode, int argIndex) {
		String fnCallArg = null;
		Node argument = getFnCallArgument(fnCallNode, argIndex);
		
		if (argument != null) {
			fnCallArg = getStringNodeLabel(argument);				
		}
		
		return fnCallArg;
	}
	
	/**
	 * Return raw AST node corresponding to function call parameter at 
	 * given index. If parameter isn't available, null value returned. 
	 * 
	 * @param fnCallNode - AST nodes containing function arguments
	 * @param argIndex - Argument index to retrieve
	 * @return Argument node, null if not found
	 */
	protected Node getFnCallArgument(Node fnCallNode, int argIndex) {
		Node argument = fnCallNode.getNext();
		while (argument != null && argIndex > 0) {
			argument = argument.getNext();
			argIndex--;
		};		
		
		return argument;
	}
	
	/**
	 * Return string label name for a function node.
	 * 
	 * @param fnNode - Function node
	 * @return Label string, null if node found.
	 */
	protected String getFunctionName(Node fnNode) {		
		return getStringNodeLabel(fnNode.getFirstChild());		
	}
	
	/**
	 * Return declared property name for a function reference. 
	 * 
	 * @param propertyLookup - Function reference to search
	 * @return Function property name, null if value not found or not a string.
	 */
	protected String getFunctionPropertyName(Node propertyLookup) {
		String propertyName = null;		
		Node objectReference = propertyLookup.getFirstChild();
		
		if (objectReference != null) {
			propertyName = getStringNodeLabel(objectReference.getNext());
		}
		
		return propertyName;
	}

	/**
	 * Return string labels for all child nodes for the parent 
	 * node that are either a Token.STRING or Token.NAME. 
	 *  
	 * @param node - Parent node 
	 * @return Available string labels for child nodes
	 */
	protected List<String> getNodeStringChildren(Node node) {
		List<String> childStrings = new ArrayList<String>();
		Node child = node.getFirstChild();
		
		while (child != null) {			
			String nodeLabel = getStringNodeLabel(child);
			if (nodeLabel != null) {
				childStrings.add(nodeLabel);
			}
			child = child.getNext();
		};		
		
		return childStrings;
	}
	
	/**
	 * Return label identifier for a string node.
	 * 
	 * @param strNode - Node to retrieve label for. 
	 * @return Label string node
	 */
	protected String getStringNodeLabel(Node strNode) {
		String strNodeLabel = null;
		
		if (strNode != null && (strNode.getType() == Token.STRING 
			|| strNode.getType() == Token.NAME 
			|| strNode.getType() == Token.BINDNAME)) {
			strNodeLabel = strNode.getString();
		}
		
		return strNodeLabel;
	}
}
