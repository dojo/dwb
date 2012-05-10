package org.dtk.analysis.script.node;

import org.dtk.analysis.script.exceptions.InvalidLiteralNode;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

/**
 * Base class for wrapper around JavaScript AST literal declarations, e.g. object or array, that 
 * provides parsing of a child attribute, converting between JS type and similar Java class. Used 
 * to assist accessing values in Java rather than manually converting on each access. 
 * 
 * @author James Thomas
 */

abstract public class BaseLiteralNode {
	
	/**
	 * Internal literal node, used to store and access AST properties.
	 */
	protected Node literalNode; 
	
	/**
	 * Provide default constructor that ensures passed literal node conforms
	 * to implementing class type (Object or Array).
	 * 
	 * @param literalNode - AST node containing JS literal declaration
	 * @throws InvalidLiteralNode - Node is not correct literal type
	 */
	public BaseLiteralNode(Node literalNode) throws InvalidLiteralNode {
		if (literalNode != null && getLiteralType() != literalNode.getType()) {
			throw new InvalidLiteralNode();
		}
		this.literalNode = literalNode;
	}
		
	/**
	 * Abstract method that returns concrete literal implementation 
	 * type, values stored in Token.class
	 * 
	 * @return Token value 
	 */
	abstract protected int getLiteralType();

	/**
	 * Retrieve a converted AST node value as a Java-friendly class. 
	 * Types are converted as follows...
	 * 
	 * String -> String
	 * Boolean -> bool
	 * Number -> Double
	 * Array -> ArrayLiteral
	 * Object -> ObjectLiteral
	 * 
	 * @param valueNode - Literal value
	 * @return Converted property value, null if unknown type encountered.
	 */
	protected Object extractPropertyValue(Node valueNode) {
		Object propertyValue; 
		
		try {
			switch(valueNode.getType()) {
			case Token.STRING: 
				propertyValue = valueNode.getString();	
				break;
			case Token.TRUE:
				propertyValue = true;
				break;
			case Token.FALSE:
				propertyValue = false;
				break;
			case Token.NUMBER:
				propertyValue = valueNode.getDouble();
				break;
			case Token.ARRAYLIT:
				propertyValue = new ArrayLiteral(valueNode);
				break;
			case Token.OBJECTLIT:
				propertyValue = new ObjectLiteral(valueNode);						
				break;
			default:
				propertyValue = null;
				break;
			}
		} catch (InvalidLiteralNode e) {
			propertyValue = null;
		}
		
		return propertyValue;
	}	
}