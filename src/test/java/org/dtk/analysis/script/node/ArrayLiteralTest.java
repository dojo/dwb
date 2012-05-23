package org.dtk.analysis.script.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

public class ArrayLiteralTest {
	
	private static ArrayLiteral getLiteralNode(String literalStr) throws InvalidLiteralNode {
		String scriptSource = "var lit = " + literalStr;
		
		CompilerEnvirons ce = new CompilerEnvirons(); 
		ScriptParserErrorReporter errorReporter = new ScriptParserErrorReporter();
		
		ce.setGenerateDebugInfo(true);		
		ce.initFromContext(ContextFactory.getGlobal().enterContext());
		ce.setErrorReporter(errorReporter);
		
		Parser p = new Parser(ce, errorReporter); 
		ScriptOrFnNode ast = p.parse(scriptSource, "script", 0);
		
		return new ArrayLiteral(ast.getFirstChild().getFirstChild().getFirstChild());
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
		ArrayLiteral lit = getLiteralNode("[]");
		assertEquals(Collections.EMPTY_LIST, lit.getValueList());
	}	

	@Test
	public void detectsStringValuesInArrayLiteral() throws InvalidLiteralNode  {	
		ArrayLiteral lit = getLiteralNode("[ 'value', 'other' ]");		
		assertEquals(Arrays.asList("value","other"), lit.getValueList());				
	}
	
	@Test
	public void detectsBooleanValuesInArrayLiteral() throws InvalidLiteralNode  {	
		ArrayLiteral lit = getLiteralNode("[ true, false]");		
		assertEquals(Arrays.asList(Boolean.TRUE, Boolean.FALSE), lit.getValueList());				
	}
	
	@Test
	public void detectsNumberValuesInArrayLiteral() throws InvalidLiteralNode {					
		ArrayLiteral lit = getLiteralNode("[0, 1]");		
		assertEquals(Arrays.asList(0.0, 1.0), lit.getValueList());				
	}
	
	@Test
	public void detectsArrayValuesInArrayLiteral() throws InvalidLiteralNode {	
		ArrayLiteral lit = getLiteralNode("[[]]");		
		List<Object> values = lit.getValueList();		
		assertTrue(1 == values.size());		
		Object arrayLit = values.get(0);	
		assertTrue(arrayLit instanceof ArrayLiteral);		
		assertEquals(Collections.EMPTY_LIST, ((ArrayLiteral) arrayLit).getValueList());
	}	
	
	@Test
	public void detectsInnerObjLiteralValuesInArrayLiteral() throws InvalidLiteralNode {	
		ArrayLiteral lit = getLiteralNode("[{ num: 1, str: 'str', b: true }]");						
		
		List<Object> values = lit.getValueList();		
		assertTrue(1 == values.size());		
		Object innerLit = values.get(0);	
		assertTrue(innerLit instanceof ObjectLiteral);		
		
		assertValues((ObjectLiteral) innerLit, getPrepopulatedMap("num", 1.0, "str", "str", "b", true));
	}			
	
	@Test(expected=InvalidLiteralNode.class)
	public void throwsExceptionWhenPassingNonLiteralNode() throws InvalidLiteralNode {					
		Node node = new Node(Token.SCRIPT);
		ObjectLiteral lit = new ObjectLiteral(node);		
	}
}
