package org.dtk.analysis.script.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.dtk.analysis.script.ScriptParserErrorReporter;
import org.dtk.analysis.script.exceptions.InvalidLiteralNode;
import org.dtk.analysis.script.node.ArrayLiteral;
import org.dtk.analysis.script.node.ObjectLiteral;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.Token;

public class ObjectLiteralTest {
	
	private static ObjectLiteral getLiteralNode(String literalStr) throws InvalidLiteralNode {
		String scriptSource = "var lit = " + literalStr;
		
		CompilerEnvirons ce = new CompilerEnvirons(); 
		ScriptParserErrorReporter errorReporter = new ScriptParserErrorReporter();
		
		ce.setGenerateDebugInfo(true);		
		ce.initFromContext(ContextFactory.getGlobal().enterContext());
		ce.setErrorReporter(errorReporter);
		
		Parser p = new Parser(ce, errorReporter); 
		ScriptOrFnNode ast = p.parse(scriptSource, "script", 0);
		
		return new ObjectLiteral(ast.getFirstChild().getFirstChild().getFirstChild());
	}
	
	private Map<String, Object> getPrepopulatedMap(Object... keyValues) {
		Map<String, Object> prepopulatedMap = new HashMap<String, Object>();
		
		for(int i = 1; i < keyValues.length; i += 2) {
			prepopulatedMap.put((String) keyValues[i-1], keyValues[i]);
		}
		
		return prepopulatedMap;
	}	
	
	
	private static void assertValues(ObjectLiteral lit, Map<String, Object> valueMap) {
		for(String key: lit.getKeys()) {
			assertEquals(valueMap.get(key), lit.getValue(key));
		}
	}
	
	@Test
	public void canParseEmptyLiteralNode() throws InvalidLiteralNode {		
		ObjectLiteral lit = getLiteralNode("{}");
		assertEquals(Collections.EMPTY_LIST, lit.getKeys());
	}	

	@Test
	public void detectsStringValuesInObjectLiteral() throws InvalidLiteralNode  {	
		ObjectLiteral lit = getLiteralNode("{ key: 'value', an: 'other'}");		
		assertEquals(Arrays.asList("key", "an"), lit.getKeys());
		assertValues(lit, getPrepopulatedMap("key", "value", "an", "other"));		
	}
	
	@Test
	public void detectsBooleanValuesInObjectLiteral() throws InvalidLiteralNode  {	
		ObjectLiteral lit = getLiteralNode("{ key: true, an: false}");		
		assertEquals(Arrays.asList("key", "an"), lit.getKeys());
		assertValues(lit, getPrepopulatedMap("key", Boolean.TRUE, "an", Boolean.FALSE));		
	}
	
	@Test
	public void detectsNumberValuesInObjectLiteral() throws InvalidLiteralNode {					
		ObjectLiteral lit = getLiteralNode("{ key: 0, an: 1}");		
		assertEquals(Arrays.asList("key", "an"), lit.getKeys());
		assertValues(lit, getPrepopulatedMap("key", 0.0, "an", 1.0));
	}
	
	@Test
	public void detectsArrayValuesInObjectLiteral() throws InvalidLiteralNode {	
		ObjectLiteral lit = getLiteralNode("{ key: []}");						
		assertEquals(Arrays.asList("key"), lit.getKeys());
		
		Object arrayLit = lit.getValue("key");	
		assertTrue(arrayLit instanceof ArrayLiteral);
		
		assertEquals(Collections.EMPTY_LIST, ((ArrayLiteral) arrayLit).getValueList());
	}	
	
	@Test
	public void detectsInnerObjLiteralValuesInObjectLiteral() throws InvalidLiteralNode {	
		ObjectLiteral lit = getLiteralNode("{ key: { num: 1, str: 'str', b: true }}");						
		assertEquals(Arrays.asList("key"), lit.getKeys());		
		Object innerObjLit = lit.getValue("key");	
		assertTrue(innerObjLit instanceof ObjectLiteral);
		
		assertValues((ObjectLiteral) innerObjLit, getPrepopulatedMap("num", 1.0, "str", "str", "b", true));
	}			
	
	@Test(expected=InvalidLiteralNode.class)
	public void throwsExceptionWhenPassingNonLiteralNode() throws InvalidLiteralNode {					
		Node node = new Node(Token.SCRIPT);
		ObjectLiteral lit = new ObjectLiteral(node);		
	}
}
