package org.dtk.analysis.script.config;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.dtk.analysis.script.node.ArrayLiteral;
import org.dtk.analysis.script.node.ObjectLiteral;
import org.junit.Test;

/**
 * Unit tests for the LoaderConfigParserTest class. 
 * 
 * @author James Thomas
 */

public class LoaderConfigParserTest {
	
	private static Map<String, Object> getLoaderConfig(String source) {
		LoaderConfigParser parser = new LoaderConfigParser(source);		
		return parser.getScriptConfig();		
	}
	
	static public Map<String, Object> getPrepopulatedMap(Object... keyValues) {
		Map<String, Object> prepopulatedMap = new HashMap<String, Object>();
		
		for(int i = 1; i < keyValues.length; i += 2) {
			prepopulatedMap.put((String) keyValues[i-1], keyValues[i]);
		}
		
		return prepopulatedMap;
	}	
	
	@Test
	public void detectsConfigurationSetUsingGlobal() {		
		assertEquals(getPrepopulatedMap("key", "value"), getLoaderConfig("dojoConfig = { key: 'value' };"));
		assertEquals(getPrepopulatedMap("key", "value"), getLoaderConfig("djConfig = { key: 'value' };"));
	}
	
	@Test
	public void detectsConfigurationSetUsingLocalDeclaration() {		
		assertEquals(getPrepopulatedMap("key", "value"), getLoaderConfig("var djConfig = { key: 'value' };"));		
		assertEquals(getPrepopulatedMap("key", "value"), getLoaderConfig("var dojoConfig = { key: 'value' };"));		
	}
	
	@Test
	public void detectsCorrectValuesInConfigurationLiteral() {
		Map <String, Object> config = getLoaderConfig("var dojoConfig = { num: 1.0, arr: [], boo: true, str: 'str', lit: {}};");
		
		// String
		assertEquals("str", config.get("str"));
		
		// Boolean
		assertEquals(true, config.get("boo"));
		
		// Number
		assertEquals(1.0, config.get("num"));		
			
		// Array
		assertEquals(Collections.EMPTY_LIST, ((ArrayLiteral) config.get("arr")).getValueList());
			
		// Object
		assertEquals(Collections.EMPTY_LIST, ((ObjectLiteral) config.get("lit")).getKeys());
	}	

	@Test
	public void ignoresNonLiteralConfigurationValues() {				
		assertEquals(Collections.EMPTY_MAP, getLoaderConfig("var dojoConfig = null;"));
		assertEquals(Collections.EMPTY_MAP, getLoaderConfig("var dojoConfig = false;"));
		assertEquals(Collections.EMPTY_MAP, getLoaderConfig("var dojoConfig = [];"));
		assertEquals(Collections.EMPTY_MAP, getLoaderConfig("var dojoConfig = 1;"));
		assertEquals(Collections.EMPTY_MAP, getLoaderConfig("var dojoConfig = other_var;"));
	}
	
	@Test
	public void ignoresInvalidConfigurationSetUsingLocalDeclaration() {		
		assertEquals(Collections.EMPTY_MAP, getLoaderConfig("var dojoconfig = { key: 'value' };"));		
	}
	
	@Test
	public void ignoresInvalidConfigurationSetUsingGlobalDeclaration() {		
		assertEquals(Collections.EMPTY_MAP, getLoaderConfig("dojoconfig = { key: 'value' };"));		
	}
}
