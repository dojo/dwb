package org.dtk.analysis.script.node;

import java.util.ArrayList;
import java.util.List;

import org.dtk.analysis.script.exceptions.InvalidLiteralNode;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

/**
 * Extension of the BaseLiteralNode class to support JavaScript array
 * literals. Provides utility method for returning converted element
 * values as a List class. 
 * 
 * The first time the value list is requested, we walk the declaration
 * node's children from left to right, extracting and converting each property
 * value. Order of the elements is maintained between array and returned list.
 * 
 * @author James Thomas
 */

public class ArrayLiteral extends BaseLiteralNode {

	/**
	 * Generated list containing array property values.
	 */
	protected List<Object> arrayPropertyValues;
	
	/**
	 * Default constructor, delegate to base constructor.
	 * 
	 * @param literalNode - Array literal node
	 * @throws InvalidLiteralNode - Node is not an array literal 
	 */
	public ArrayLiteral(Node literalNode) throws InvalidLiteralNode {
		super(literalNode);
	}

	/**
	 * Extract and return list of array literal members,
	 * converted to Java classes.
	 * 
	 * @return Converted array literal values
	 */
	public List<Object> getValueList() {
		if (!hasParsedArrayPropertyValues()) {
			parseArrayPropertyValues();
		}
		
		return arrayPropertyValues;
	}
	
	/**
	 * Have we already parsed array literal members?
	 * 
	 * @return Literal values have been parsed
	 */
	protected boolean hasParsedArrayPropertyValues() {
		return arrayPropertyValues != null;
	}	
	
	/**
	 * Update internal literal member list with 
	 * current values by traversing child nodes of literal declaration.
	 */
	protected void parseArrayPropertyValues() {
		arrayPropertyValues = new ArrayList<Object>();		
		Node arrayItem = literalNode.getFirstChild();
		
		while(arrayItem != null) {
			arrayPropertyValues.add(extractPropertyValue(arrayItem));			
			arrayItem = arrayItem.getNext();
		}			
	}
	
	/**
	 * Valid literal node types for use in constructor. 
	 * 
	 * @return AST node type
	 */
	@Override
	protected int getLiteralType() {
		return Token.ARRAYLIT;
	}
}
