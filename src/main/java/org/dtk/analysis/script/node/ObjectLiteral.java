package org.dtk.analysis.script.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dtk.analysis.script.exceptions.InvalidLiteralNode;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

/**
 * Extension of the BaseLiteralNode class to support JavaScript object
 * literals. Provides utility method for returning converted element
 * values as a Map class. 
 * 
 * Provides two public methods, getKeys & getValue, to abstract away the process
 * of traversing node properties to find the list of literal members and their 
 * associated values. To ensure we don't have to traverse node for each access, 
 * we maintain an internal map of previously discovered values. 
 * 
 * @author James Thomas
 */

public class ObjectLiteral extends BaseLiteralNode {

	/** 
	 * Internal cache for object literal keys
	 */
	protected List<String> literalKeys; 

	/** 
	 * Internal cache for object literal values discovered
	 */
	protected Map<String, Object> literalValues = new HashMap<String, Object>();

	/**
	 * Default constructor, delegate to base constructor.
	 * 
	 * @param literalNode - Array literal node
	 * @throws InvalidLiteralNode - Node is not an array literal 
	 */
	public ObjectLiteral(Node literalNode) throws InvalidLiteralNode {
		super(literalNode);
	}

	/**
	 * Extract and return list of literal keys.
	 * 
	 * @return Literal keys
	 */
	public List<String> getKeys() {
		if (!hasParsedLiteralKeys()) {
			parseObjLitKeys();
		}

		return literalKeys;
	}

	/**
	 * Return a literal value for a given key. 
	 * 
	 * @param key - Literal member key
	 * @return Literal value if found, otherwise null
	 */
	public Object getValue(String key) {
		if (!hasParsedLiteralValue(key)) {
			parseLiteralValue(key);
		}

		return literalValues.get(key);
	}

	/**
	 * Find the associated literal value for a given
	 * identifier. If we don't recognise that key, 
	 * don't look for value.
	 * 
	 * @param key - Literal key identifier
	 */
	protected void parseLiteralValue(String key) {		
		List<String> keys = getKeys();

		int index = keys.indexOf(key);

		if (index != -1) {
			Node propertyValue = literalNode.getFirstChild();
			while (index > 0) {
				propertyValue = propertyValue.getNext();	
				index--;
			}
			literalValues.put(key, extractPropertyValue(propertyValue));
		}								
	}	

	/**
	 * Check if we have already parsed that literal key value.
	 * 
	 * @param key - Literal key
	 * @return Has parsed this value
	 */
	protected boolean hasParsedLiteralValue(String key) {
		return literalValues.containsKey(key);
	}

	/**
	 * Have we tried to parse the literal keys for this node?
	 * 
	 * @return Keys have been parsed
	 */
	protected boolean hasParsedLiteralKeys() {
		return literalKeys != null;
	}

	/**
	 * Update internal literal key cache with all the current node
	 * properties. 
	 */
	protected void parseObjLitKeys() {				
		Object[] propertyList = (Object[]) literalNode.getProp(Token.EQ);
		literalKeys = new ArrayList<String>();

		if (propertyList != null) {			
			List<Object> properties = Arrays.asList(propertyList);

			for(Object property: properties) {
				literalKeys.add((String) property);				
			}						
		}				
	}

	/**
	 * Valid literal node type, used for checking constructor
	 * parameters.
	 * 
	 * @return JavaScript AST node type
	 */
	@Override
	protected int getLiteralType() {
		return Token.OBJECTLIT;
	}	
}
