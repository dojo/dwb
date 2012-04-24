package org.dtk.analysis.script;

import java.util.HashSet;
import java.util.Set;

import org.dtk.resources.dependencies.ScriptParserErrorReporter;
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
	protected String functionCallArgument(Node functionCallNodes, int argIndex) {
		String fnCallArg = null;
		
		Node argument = functionCallNodes.getNext();
		while (argument != null && argIndex > 0) {
			argument = argument.getNext();
			argIndex--;
		};
		
		// Check we have a simple type value.
		if (argument != null && argument.getType() == Token.STRING) {
			fnCallArg = argument.getString();
		}
		
		return fnCallArg;
	}
	
	/**
	 * Return declared property name for a function reference. 
	 * 
	 * @param propertyLookup - Function reference to search
	 * @return Function property name, null if value not found or not a string.
	 */
	protected String functionPropertyName(Node propertyLookup) {
		String propertyName = null;
		
		Node objectReference = propertyLookup.getFirstChild();
		
		if (objectReference != null) {
			Node propertyNameNode = objectReference.getNext();
		
			if (propertyNameNode != null && propertyNameNode.getType() == Token.STRING) {
				propertyName = propertyNameNode.getString();
			}
		}
		
		return propertyName;
	}
}
