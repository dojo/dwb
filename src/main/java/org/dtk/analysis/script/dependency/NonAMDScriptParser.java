package org.dtk.analysis.script.dependency;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

/**
 * Implementation of the ScriptDependencyParser interface for the old style
 * Dojo module format, i.e. non-AMD. In this old format, module dependencies
 * are identified using the dojo.require("some.module.identifier") statement.
 * 
 * These calls are parsed from the code and the single module identifiers passed
 * as the first argument retrieved.
 * 
 * @author James Thomas
 */
public class NonAMDScriptParser extends BaseScriptDependencyParser {

	/**
	 * Property name used to access module dependency function in Dojo.
	 */
	protected final static String DOJO_REQUIRE_METHOD = "require";

	/**
	 * Global object reference name used to access Dojo functions.
	 */
	protected final static String DOJO_OBJ_REF = "dojo";
	
	/**
	 * Default constructor. 
	 * 
	 * @param scriptSoure - JavaScript source text.
	 */
	public NonAMDScriptParser(String scriptSoure) {
		super(scriptSoure);
	}

	/**
	 * Check whether the AST node corresponds to the 
	 * following format, "dojo.require()" 
	 * 
	 * @return Node is a dojo.require call 
	 */
	@Override
	protected boolean isModuleDependencyCall(Node node) {		
		return isDojoObjectRef(node) && isRequirePropertyCall(node.getFirstChild()); 		
	}
	
	/**
	 * Check whether the AST node for a property lookup on an object
	 * returns a matching function name for our dependency method.
	 * 
	 * @param propertyResolution - Object property lookup node
	 * @return Reference was a dependency function call
	 */
	protected boolean isRequirePropertyCall(Node propertyResolution) {
		String propertyName = getFunctionPropertyName(propertyResolution);

		if (propertyName != null && DOJO_REQUIRE_METHOD.equals(propertyName)) {
			return true;
		} 
		
		return false;
	}
				
	/**
	 * Return module identifier passed as argument to dependency call. 
	 * Will be a single string identifier. 
	 * 
	 * @param node - Module dependency call node
	 * @return Single module dependency in list, empty list if not found
	 */
	@Override
	protected List<String> retrieveDependencyArguments(Node node) {
		List<String> singleDependencyList = new ArrayList<String>();
		
		Node propertyResolution = node.getFirstChild(); 
		
		if (propertyResolution != null) {			
			String firstArg = getFnCallArgumentStr(propertyResolution, 0);
			if (firstArg != null) {
				singleDependencyList.add(firstArg);
			}
		}
		
		return singleDependencyList;
	}
	
	/**
	 * Function call should be a property lookup from the 
	 * global "dojo" object reference. 
	 * 
	 * @param functionCall - Function call reference
	 * @return Function call from main dojo object reference. 
	 */
	protected boolean isDojoObjectRef (Node functionCall) {
		Node propertyLookup = functionCall.getFirstChild();
		
		if (propertyLookup != null && 
			propertyLookup.getType() == Token.GETPROP 
			&& propertyLookup.getFirstChild().getType() == Token.NAME) {
				String functionName = propertyLookup.getFirstChild().getString();
				if (DOJO_OBJ_REF.equals(functionName)) {
					return true;
			}
		}
		
		return false;
	}
}
