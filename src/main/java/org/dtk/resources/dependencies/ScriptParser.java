package org.dtk.resources.dependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.FunctionNode;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.Token;

public class ScriptParser {
	// Source code for JavaScript to be parsed
	String scriptSource;
	
	// Store indication that we've already parsed this script. 
	boolean scriptParsed = false;
	
	// Store modules referenced in any dojo.provide/declare/require function calls,
	// keyed on the function name. 
	Map<String, List<String>> moduleLists = new HashMap<String, List<String>>() {{
		put("require", new ArrayList<String>());
		put("provide", new ArrayList<String>());
		put("declare", new ArrayList<String>());
	}};
	
	// Collection of any custom module paths defined, through dojo.registerModulePath
	// or djConfig. 
	Map<String, String> modulePaths = new HashMap<String, String>();
	
	Set<Node> parsedNodes = new HashSet<Node>();
	
	public ScriptParser(String scriptSource) {
		this.scriptSource = scriptSource;
	}
	
	// TODO: Fill this out
	private class CustomErrorReporter implements ErrorReporter {

		@Override
		public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
			System.out.println("ERROR: " + message);
		}

		@Override
		public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
			System.out.println("Evaluator Exception: " + message);
			return new EvaluatorException(message);
		}

		@Override
		public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
			System.out.println("WARNING: " + message);
		}
		
	}
	
	protected void parse() throws EvaluatorException {
		// Parse script source 
		CompilerEnvirons ce = new CompilerEnvirons(); 
		ce.setGenerateDebugInfo(true);
		
		ce.initFromContext(ContextFactory.getGlobal().enterContext());
		
		CustomErrorReporter cer = new CustomErrorReporter();
		
		ce.setErrorReporter(cer);
		
		Parser p = new Parser(ce, cer); 
		ScriptOrFnNode ast = p.parse(this.scriptSource, "script", 0);
		
		recursiveParse(ast);
		
		this.scriptParsed = true;
	}
	
	protected void recursiveParse(Node node) {
		// Ignore null nodes or those we have seen before!
		if (node == null || parsedNodes.contains(node)) {
			return;
		}
		
		// Script level nodes have global list of function definitions. 
		// Parse inner tokens for function definitions here.
		if (node instanceof ScriptOrFnNode) {
			ScriptOrFnNode sofn = (ScriptOrFnNode) node;
			int count = sofn.getFunctionCount() - 1;
			while (count > -1) {
				FunctionNode fn = sofn.getFunctionNode(count);
				recursiveParse(fn);
				count--;
			}
		}
		
		// Parse children from left to right.
		recursiveParse(node.getFirstChild());
		recursiveParse(node.getLastChild());
		
		// Parse this node and then move along to next
		parseNode(node);
		parsedNodes.add(node);
		
		recursiveParse(node.getNext());		
	}
	
	protected void parseNode(Node node) {
		switch(node.getType()) {
		// Function call token
		case Token.CALL: 
			if (isDojoCall(node)) {
				// We are looking for dojo.some_function(...) calls, this means first
				// child of function call is property resolution of function in the 
				// dojo object.
				Node propertyResolution = node.getFirstChild(); 
				
				if (propertyResolution != null) {
					// Find property label accessing object
					String propertyName = functionPropertyName(propertyResolution);
					// Single argument module functions, put out value and store in appropriate list.
					if ("require".equals(propertyName) || "provide".equals(propertyName) || "declare".equals(propertyName)) {
						String firstArg = functionCallArgument(propertyResolution, 0);
						List<String> moduleList = moduleLists.get(propertyName);
						
						if (!moduleList.contains(firstArg)) {
							moduleList.add(firstArg);
						}
					} else if ("registerModulePath".equals(propertyName)) {
						// Extract module identifier and module path, first two arguments to 
						// dojo.registerModulePath();
						String modulePrefix = functionCallArgument(propertyResolution, 0);
						String modulePath = functionCallArgument(propertyResolution, 1);
						
						modulePaths.put(modulePrefix, modulePath);
					} 
				}
				
			}
			break;
		// Global identifier token, check for djConfig assignment, djConfig = {...}
		case Token.SETNAME:
			// LHS is global name binding.
			Node bindName = node.getFirstChild();
			
			// LHS should be djConfig global assignment.
			if (bindName.getType() == Token.BINDNAME && "djConfig".equals(bindName.getString())) {
				// RHS of statement should be literal assignment.
				Node assignmentExpr = node.getLastChild();
				parseModulePathsExpr(assignmentExpr);
			}
			
			break;
		// Local identifier token, check for djConfig assignment, var djConfig = {...}
		case Token.NAME: 
			// LHS is name token
			if ("djConfig".equals(node.getString())) {
				// Check for object literal assignment 
				Node assignmentExpr = node.getFirstChild();
				parseModulePathsExpr(assignmentExpr);
			}
			
			break;
		}
	}
	
	protected boolean isDojoCall (Node functionCall) {
		boolean isDojoCall = false;
		
		Node propertyLookup = functionCall.getFirstChild();
		
		// Check node is a property name lookup from the "dojo" object. 
		// Property Name Lookup -> Source, Property
		if (propertyLookup.getType() == Token.GETPROP && propertyLookup.getFirstChild().getType() == Token.NAME) {
				String functionName = propertyLookup.getFirstChild().getString();
				if ("dojo".equals(functionName)) {
					isDojoCall = true;
			}
		}
		
		return isDojoCall;
	}
	
	// Try to find module paths expression in djConfig object literal. Merge results
	// into existing module paths map.
	protected void parseModulePathsExpr(Node objectLiteral) {
		if (objectLiteral.getType() == Token.OBJECTLIT) {
			Node modulePathsLiteral = findLiteralValue(objectLiteral, "modulePaths");
			if (modulePathsLiteral != null && modulePathsLiteral.getType() == Token.OBJECTLIT) {
				// Put out all String module path values and merge into existing map.
				Map<String, String> modulePaths = findLiteralStringValues(modulePathsLiteral);
				this.modulePaths.putAll(modulePaths);
			}
		}
	}
	
	protected Map<String, String> findLiteralStringValues(Node objLiteral) {
		Map<String, String> literalValues = new HashMap<String, String>();
		
		// Find object literal keys from property list. 
		Object[] propertyList = (Object[]) objLiteral.getProp(Token.EQ);
		
		// Iterate through each literal key, extracting associated value. Ignore
		// any non-string values, we aren't attempting inner-resolution of references. 
		if (propertyList != null) {
			int numArgs = 0;
			// Child nodes are linked list of object values
			Node propertyValue = objLiteral.getFirstChild();
			
			while (numArgs < propertyList.length) {
				// Convert key/value pairs into a handy format
				if (propertyValue.getType() == Token.STRING) {
					literalValues.put((String) propertyList[numArgs], propertyValue.getString());
				}
				
				propertyValue = propertyValue.getNext();
				numArgs++;
			}
		}
		
		return literalValues;
	}
	
	// Find an object literal's value for a given key. If not found,
	// return null.
	protected Node findLiteralValue(Node objLiteral, String key) {
		Node literalValueNode = null;
		
		// Get list of all properties
		Object[] propertyList = (Object[]) objLiteral.getProp(Token.EQ);
		
		// Iterate through key/values looking for a match
		if (propertyList != null) {
			Node propertyValue = objLiteral.getFirstChild();
			for(int index = 0; index < propertyList.length; index++) {
				if (key.equals(propertyList[index])) {
					literalValueNode = propertyValue;
					break;
				}
				propertyValue = propertyValue.getNext();
			}
		}
		
		return literalValueNode;
	}
	
	// Extract property name accessed. Will have two child nodes,
	// object reference and property name. Ignore any further 
	// variable resolution.
	protected String functionPropertyName(Node propertyLookup) {
		String propertyName = null;
		
		// First argument is an object reference that lookup should be 
		// performed against. 
		Node objectReference = propertyLookup.getFirstChild();
		
		if (objectReference != null) {
			// Find property identifier we are retrieving...
			Node propertyNameNode = objectReference.getNext();
			
			// Only handle explicit string properties, we cannot resolve variables.
			if (propertyNameNode != null && propertyNameNode.getType() == Token.STRING) {
				propertyName = propertyNameNode.getString();
			}
		}
		
		return propertyName;
	}
	
	// Given child nodes for a function call, object ref and arguments, return
	// the string value for the first argument. Ignore any complicated resolution.
	protected String functionCallArgument(Node functionCallNodes, int argIndex) {
		String fnCallArg = null;
		
		// Traverse argument nodes until we reach desired one. 
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
	
	public Map<String, String> retrieveModulePaths() throws EvaluatorException {
		if (!scriptParsed) {
			parse();
		}
		
		return this.modulePaths;
	}
	
	public List<String> retrieveModuleRequires() throws EvaluatorException {
		return retrieveModuleList("require");
	}
	
	public List<String> retrieveModuleDeclares() throws EvaluatorException {
		return retrieveModuleList("declare");
	}
	
	public List<String> retrieveModuleProvides() throws EvaluatorException {
		return retrieveModuleList("provide");
	}
	
	protected List<String> retrieveModuleList(String identifier) throws EvaluatorException {
		if (!scriptParsed) {
			parse();
		}
		
		return this.moduleLists.get(identifier);
	}
}
