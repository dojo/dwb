package org.dtk.analysis.script.loader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

public class AbstractModulePathResolverTest {

	protected final String baseUrl = "http://some.host/dojo-lib/dojo/";
	
	@Test 
	public void willResolveRelativePathForModuleIdWithEmptyConfig() {
		MockModulePathResolver resolver = new MockModulePathResolver(null, Collections.EMPTY_MAP);		
		assertEquals("../a/b/c.js", resolver.getRelativePath("a.b.c"));		
	}
	
	@Test 
	public void willResolveRelativePathForModuleIdWithFirstPathMatch() {
		Map<String, String> paths = new HashMap<String, String>();
		paths.put("a", "./some/other/path");
		
		MockModulePathResolver resolver = new MockModulePathResolver(null, paths);		
		assertEquals("./some/other/path/b/c.js", resolver.getRelativePath("a.b.c"));		
	}
	
	@Test 
	public void willResolveRelativePathForModuleIdWithLaterPathMatch() {
		Map<String, String> paths = new HashMap<String, String>();
		paths.put("a.b", "./some/other/path");
		
		MockModulePathResolver resolver = new MockModulePathResolver(null, paths);		
		assertEquals("./some/other/path/c.js", resolver.getRelativePath("a.b.c"));		
	}
	
	@Test 
	public void willResolveMostSpecificModulePathMatch() {
		Map<String, String> paths = new HashMap<String, String>();
		paths.put("a", "./another/path");
		paths.put("a.b", "./some/other/path");
		
		MockModulePathResolver resolver = new MockModulePathResolver(null, paths);		
		assertEquals("./some/other/path/c.js", resolver.getRelativePath("a.b.c"));		
	}
	
	@Test 
	public void willResolveMostLastModulePathWhenOverlappingConfig() {
		Map<String, String> paths = new HashMap<String, String>();
		paths.put("a.b", "./another/path");
		paths.put("a.b", "./some/other/path");
		
		MockModulePathResolver resolver = new MockModulePathResolver(null, paths);		
		assertEquals("./some/other/path/c.js", resolver.getRelativePath("a.b.c"));		
	}
	
	@Test 
	public void willResolveAbsolutePathForModuleIdWithEmptyConfig() throws MalformedURLException {
		MockModulePathResolver resolver = new MockModulePathResolver(new URL(baseUrl), Collections.EMPTY_MAP);		
		assertEquals(new URL(new URL(baseUrl), "../a/b/c.js"), resolver.getAbsolutePath("a.b.c"));		
	}
	
	@Test 
	public void willResolveAbsolutePathForModuleIdWithSimpleConfig() throws MalformedURLException {
		Map<String, String> paths = new HashMap<String, String>();
		paths.put("a", "./some/other/path");
		
		MockModulePathResolver resolver = new MockModulePathResolver(new URL(baseUrl), paths);			
		
		assertEquals(new URL(new URL(baseUrl), "./some/other/path/b/c.js"), resolver.getAbsolutePath("a.b.c"));		
	}
	
	@Test 
	public void willResolveAbsolutePathForModuleIdWithRelativePathConfig() throws MalformedURLException {
		Map<String, String> paths = new HashMap<String, String>();
		paths.put("a", "../../some/other/path");
		
		MockModulePathResolver resolver = new MockModulePathResolver(new URL(baseUrl), paths);			
		
		assertEquals(new URL(new URL(baseUrl), "../../some/other/path/b/c.js"), resolver.getAbsolutePath("a.b.c"));		
	}
	
	
	private class MockModulePathResolver extends AbstractModulePathResolver {

		public MockModulePathResolver(URL baseUri,
				Map<String, String> modulePaths) {
			super(baseUri, modulePaths);
		}

		@Override
		protected char getModulePathSeparator() {
			return '.';
		}		
	}	
}
