package org.dtk.analysis.script;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for the NonAMDScriptParser class. 
 * 
 * Performs tests to ensure all instances of dojo.require('some.module')
 * present within the code are captured correctly.  
 * 
 * @author James Thomas
 */

public class NonAMDScriptParserTest {

	private static List<String> getScriptDeps(String source) {
		NonAMDScriptParser parser = new NonAMDScriptParser(source);		
		return parser.getModuleDependencies();		
	}

	@Test 
	public void returnsEmptyListWithEmptySource() {		
		assertEquals(getScriptDeps(""), Arrays.asList());
	}
	
	@Test 
	public void returnsEmptyListWithInvalidScript() {		
		assertEquals(getScriptDeps("some func("), Arrays.asList());
	}	
	
	@Test 
	public void singleDojoRequireCallsAreDetected() {		
		assertEquals(getScriptDeps("dojo.require('a.b.c');"), Arrays.asList("a.b.c"));
	}
	
	@Test 
	public void multipleDifferentDojoRequireCallsAreDetected() {		
		assertEquals(getScriptDeps("dojo.require('a.b.c'); dojo.require('d.e.f'); dojo.require('g.h.i');"), 
			Arrays.asList("a.b.c", "d.e.f", "g.h.i"));
	}
	
	@Test 
	public void lazyLoadedDojoRequireDetected() {
		assertEquals(getScriptDeps("dojo.ready(function () { dojo.require('a.b.c') } );"),
			Arrays.asList("a.b.c"));
	}
	
	@Test 
	public void scriptWithMultipleSameModuleIdentifiersOnlyListOnce() {
		assertEquals(getScriptDeps("dojo.require('a.b.c');dojo.require('a.b.c');"), 
			Arrays.asList("a.b.c"));
	}
	
	@Test 
	public void doesNotPickUpDojoDeclares() {		
		assertEquals(getScriptDeps("dojo.declare('a.b.c');"), Arrays.asList());
	}

	@Test 
	public void doesNotPickUpDojoProvides() {		
		assertEquals(getScriptDeps("dojo.provide('a.b.c');"), Arrays.asList());
	}
	
	@Test 
	public void doesNotPickUpAMDRequires() {		
		assertEquals(getScriptDeps("require('a.b.c');"), Arrays.asList());
	}
}
