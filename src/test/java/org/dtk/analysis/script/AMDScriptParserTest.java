package org.dtk.analysis.script;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.dtk.analysis.script.dependency.AMDScriptParser;
import org.junit.Test;

/**
 * Unit tests for the AMDScriptParser class. 
 * 
 * Performs tests to ensure all calls to the AMD APIs that include
 * module dependencies are captured. This may be either using the 
 * require method:  
 * 		require("some/module");
 * 		require(["some/module",...]);  
 * 		require({config: value}, ["some/module",...]);  
 * or the define method:  
 * 		define(["some/module"],...);
 *		define("module/id", ["some/module"],...);  
 * 
 * @author James Thomas
 */

public class AMDScriptParserTest {

	private static List<String> getScriptDeps(String source) {
		AMDScriptParser parser = new AMDScriptParser(source);		
		return parser.getModuleDependencies();		
	}

	@Test 
	public void returnsEmptyListWithEmptySource() {		
		assertEquals(Arrays.asList(), getScriptDeps(""));
	}
	
	@Test 
	public void returnsEmptyListWithInvalidScript() {		
		assertEquals(Arrays.asList(), getScriptDeps("some func("));
	}
	
	@Test 
	public void returnsEmptyListWithRequireAndNoArguments() {		
		assertEquals(Arrays.asList(), getScriptDeps("require();"));
		assertEquals(Arrays.asList(), getScriptDeps("require([], function () {});"));
		assertEquals(Arrays.asList(), getScriptDeps("require(null, function () {});"));
		assertEquals(Arrays.asList(), getScriptDeps("require({}, [], function () {});"));
		assertEquals(Arrays.asList(), getScriptDeps("require({}, null, function () {});"));
	}	
	
	@Test 
	public void returnsEmptyListWithDefineAndNoArguments() {		
		assertEquals(Arrays.asList(), getScriptDeps("define({});"));
		assertEquals(Arrays.asList(), getScriptDeps("define(function () {});"));
		assertEquals(Arrays.asList(), getScriptDeps("define('id', function () {});"));
		assertEquals(Arrays.asList(), getScriptDeps("define('id', [], function () {});"));
		assertEquals(Arrays.asList(), getScriptDeps("define([], function () {});"));		
	}	
	
	@Test 
	public void detectRequireCallWithSingleStringArgument() {
		assertEquals(Arrays.asList("some/module/id"), getScriptDeps("var module = require('some/module/id');"));
	}
	
	@Test 
	public void detectRequireCallWithModuleArrayArgument() {
		assertEquals(Arrays.asList("some/module/id", "another/module/id", "final/module/id"), 
			getScriptDeps("require(['some/module/id', 'another/module/id', 'final/module/id'], function (a, b, c) {});"));
	}	
	
	@Test 
	public void detectRequireCallWithModuleArrayArgumentAndConfig() {
		assertEquals(Arrays.asList("some/module/id", "another/module/id", "final/module/id"), 
			getScriptDeps("require({config_value: 'key'}, ['some/module/id', 'another/module/id', 'final/module/id'], " +
			"function (a, b, c) {});"));
	}
	
	@Test 
	public void detectDefineCallWithSingleDependencyArgument() {
		assertEquals(Arrays.asList("some/module/id"), getScriptDeps("define(['some/module/id'], function () {});"));
	}
	
	@Test 
	public void detectDefineCallWithMultipleDependencyArguments() {
		assertEquals(Arrays.asList("some/module/id", "another/module/id"), 
			getScriptDeps("define(['some/module/id', 'another/module/id'], function () {});"));
	}
	
	@Test 
	public void detectDefineCallWithModuleIdentifierSpecifiedDependencyArguments() {
		assertEquals(Arrays.asList("some/module/id", "another/module/id"), 
			getScriptDeps("define('this/module/id', ['some/module/id', 'another/module/id'], function () {});"));
	}	
	
	@Test 
	public void detectCJSCompatibleRequireCallWrappedWithinDefine() {
		assertEquals(Arrays.asList("some/module/id", "another/module/id"), 
			getScriptDeps("define(function (require, exports, module) " +
				"{ var a = require('some/module/id'), b = require('another/module/id')});"));		
	}
}
