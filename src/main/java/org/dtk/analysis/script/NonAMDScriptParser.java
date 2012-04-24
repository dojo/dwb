package org.dtk.analysis.script;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

public class NonAMDScriptParser extends BaseScriptDependencyParser {

	protected final static String DOJO_REQUIRE_METHOD = "require";
	
	protected final static String DOJO_OBJ_REF = "dojo";
	
	public NonAMDScriptParser(String scriptSoure) {
		super(scriptSoure);
	}

	@Override
	protected boolean isModuleDependencyCall(Node node) {		
		return isDojoObjectRef(node) && isRequirePropertyCall(node.getFirstChild()); 
		
	}
	
	protected boolean isRequirePropertyCall(Node propertyResolution) {
			// Find property label accessing object
			String propertyName = functionPropertyName(propertyResolution);
			// Single argument module functions, put out value and store in appropriate list.
			if (propertyName != null && DOJO_REQUIRE_METHOD.equals(propertyName)) {
				return true;
			} 
		
		return false;
	}
	
			// We are looking for dojo.some_function(...) calls, this means first
			// child of function call is property resolution of function in the 
			// dojo object.
			
	
	@Override
	protected List<String> retrieveDependencyArguments(Node node) {
		List<String> singleDependencyList = new ArrayList<String>();
		
		Node propertyResolution = node.getFirstChild(); 
		
		if (propertyResolution != null) {			
			String firstArg = functionCallArgument(propertyResolution, 0);
			if (firstArg != null) {
				singleDependencyList.add(firstArg);
			}
		}
		
		return singleDependencyList;
	}
	
	protected boolean isDojoObjectRef (Node functionCall) {
		Node propertyLookup = functionCall.getFirstChild();
		
		// Check node is a property name lookup from the "dojo" object. 
		// Property Name Lookup -> Source, Property
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
