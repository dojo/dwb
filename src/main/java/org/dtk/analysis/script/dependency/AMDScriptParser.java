package org.dtk.analysis.script.dependency;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

/**
 * Implementation of the ScriptDependencyParser interface for JavaScript source files  
 * using the AMD module format API, i.e define(); or require(); 
 * 
 * These calls are parsed from the code and the list of module identifiers passed
 * as the function arguments retrieved. Dependencies may be identified in a variety of 
 * formats, 
 * 		require("some/module");
 * 		require(["some/module",...]);  
 * or the define method:  
 * 		define(["some/module"],...);
 *		define("module/id", ["some/module"],...);
 * 
 * @author James Thomas
 */

public class AMDScriptParser extends BaseScriptDependencyParser {

	/**
	 * AMD API property name for requiring modules.
	 */
	protected final static String AMD_REQUIRE_METHOD = "require";
	
	/**
	 * AMD API property name for defining modules.
	 */
	protected final static String AMD_DEFINE_METHOD = "define";
	
	/**
	 * Default constructor. 
	 * 
	 * @param scriptSoure - JavaScript source text.
	 */
	public AMDScriptParser(String scriptSoure) {
		super(scriptSoure);
	}

	/**
	 * AST Node is an AMD API call that may contain module 
	 * dependencies if either the function name matches a global
	 * reference for "define" or "require". 
	 * 
	 * @param node - AST node for function call 
	 * @return Function call matches AMD API name
	 */
	@Override
	protected
	boolean isModuleDependencyCall(Node node) {
		return isAmdRequireCall(node) || isAmdDefineCall(node);
	}
	
	/**
	 * Extract all module identifiers listed in the function call arguments
	 * for the AMD API definitions. Otherwise, return an empty list
	 * 
	 * @param node - AMD API function call
	 * @return All dependency arguments discovered or an empty list
	 */
	@Override
	protected
	List<String> retrieveDependencyArguments(Node node) {
		List<String> dependencyArguments = new ArrayList<String>();
		
		if (isAmdRequireCall(node)) {
			dependencyArguments.addAll(retrieveRequireDependencyArguments(node));
		} else if (isAmdDefineCall(node)) {
			dependencyArguments.addAll(retrieveDefineDependencyArguments(node));
		}
		
		return dependencyArguments;
	}
	
	/**
	 * Extract all module identifiers listed in the function call arguments
	 * for the AMD "require" API definition. Dependencies may be listed as 
	 * a string identifier or an array of identifiers. API call may also have
	 * an optional configuration parameter as the first argument.
	 * 
	 * @param node - AMD API function call
	 * @return All dependency arguments discovered or an empty list
	 */
	protected List<String> retrieveRequireDependencyArguments(Node node) {
		List<String> dependencyArguments = new ArrayList<String>();
		
		Node firstFnArgument = getFnCallArgument(node.getFirstChild(), 0);
		
		if (firstFnArgument != null) {
			switch (firstFnArgument.getType()) {
				case Token.STRING:
					dependencyArguments.add(firstFnArgument.getString());
					break;
				case Token.ARRAYLIT:								
					dependencyArguments.addAll(getNodeStringChildren(firstFnArgument));
					break;
				case Token.OBJECTLIT:
					if (firstFnArgument.getNext() != null) {
						dependencyArguments.addAll(getNodeStringChildren(firstFnArgument.getNext()));
					}
				default:
					break;
			}
		}
		
		return dependencyArguments;
	}

	/**
	 * Extract all module identifiers listed in the function call arguments
	 * for the AMD "define" API definition. Dependencies may be listed as 
	 * a string identifier or an array of identifiers. 
	 * 
	 * @param node - AMD API function call
	 * @return All dependency arguments discovered or an empty list
	 */
	protected List<String> retrieveDefineDependencyArguments (Node node) {
		List<String> dependencyArguments = new ArrayList<String>();
		
		Node firstFnArgument = getFnCallArgument(node.getFirstChild(), 0);
		
		if (firstFnArgument != null) {
			switch (firstFnArgument.getType()) {
				case Token.STRING:
					dependencyArguments.addAll(getNodeStringChildren(firstFnArgument.getNext()));
					break;
				case Token.ARRAYLIT:								
					dependencyArguments.addAll(getNodeStringChildren(firstFnArgument));
					break;
				default:
					break;
			}
		}
		
		return dependencyArguments;
	}

	/**
	 * Does function call name matches AMD "require" API method?
	 *  
	 * @param node - Function call 
	 * @return Function name matches AMD "require"
	 */
	protected boolean isAmdRequireCall(Node node) {		
		return AMD_REQUIRE_METHOD.equals(getFunctionName(node));
	}
	
	/**
	 * Does function call name matches AMD "define" API method?
	 *  
	 * @param node - Function call 
	 * @return Function name matches AMD "define"
	 */
	protected boolean isAmdDefineCall(Node node) {		
		return AMD_DEFINE_METHOD.equals(getFunctionName(node));
	}
}