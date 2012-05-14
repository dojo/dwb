package org.dtk.analysis.page;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.dtk.analysis.ModuleFormat;
import org.dtk.analysis.script.config.LoaderConfigParserTest;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.Test;

public class RecursiveWebPageTest {	
	
	private String getResourceAsString(String filePath) throws IOException {
		InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);		
		return IOUtils.toString(is);
	}
	
	@Test
	public void defaultsToNonAmdScriptModeWithoutConfig() throws IOException {
		MockWebPage webPage = new MockWebPage();
		webPage.isDojoScript = true;
		
		Attributes attrs = new Attributes();		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);

		webPage.parsePreDojoScript(dojoScript);
		assertEquals(Collections.EMPTY_MAP, webPage.modulePaths);
	}
	
	@Test
	public void parsesPathsValueFromConfigAttrOnScriptTag() throws IOException {
		MockWebPage webPage = new MockWebPage();
		webPage.isDojoScript = true;
		
		Attributes attrs = new Attributes();
		attrs.put("data-dojo-config", "paths: {'some': 'path', 'an': 'other'}");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		webPage.parsePreDojoScript(dojoScript);
		assertEquals(LoaderConfigParserTest.getPrepopulatedMap("some", "path", "an", "other"), webPage.modulePaths);
	}
	
	@Test
	public void parsesPathsValueFromOldConfigAttrOnScriptTag() throws IOException {
		MockWebPage webPage = new MockWebPage();
		webPage.isDojoScript = true;
		
		Attributes attrs = new Attributes();
		attrs.put("djConfig", "paths: {'some': 'path', 'an': 'other'}");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		webPage.parsePreDojoScript(dojoScript);
		assertEquals(LoaderConfigParserTest.getPrepopulatedMap("some", "path", "an", "other"), webPage.modulePaths);
	}

	@Test
	public void parsesPathsValueFromConfigDeclarationInScriptTag() throws IOException {
		MockWebPage webPage = new MockWebPage();
		webPage.scriptContents = "var dojoConfig = { paths: {'some': 'path', 'an': 'other'} }";
		
		Attributes attrs = new Attributes();			
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		webPage.parsePreDojoScript(dojoScript);
		assertEquals(LoaderConfigParserTest.getPrepopulatedMap("some", "path", "an", "other"), webPage.modulePaths);
	}	
	
	@Test
	public void parsesPathsValueFromOldConfigDeclarationInScriptTag() throws IOException {
		MockWebPage webPage = new MockWebPage();
		webPage.scriptContents = "var djConfig = { paths: {'some': 'path', 'an': 'other'} }";
		
		Attributes attrs = new Attributes();			
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		webPage.parsePreDojoScript(dojoScript);
		assertEquals(LoaderConfigParserTest.getPrepopulatedMap("some", "path", "an", "other"), webPage.modulePaths);
	}	
	
	@Test
	public void parsesMultiplePathsValuesFromConfigsScripts() throws IOException {
		MockWebPage webPage = new MockWebPage();		
		webPage.scriptContents = "var djConfig = { paths: {'some': 'path', 'an': 'other'} }";
	
		Attributes attrs = new Attributes();			
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		webPage.parsePreDojoScript(dojoScript);
		
		webPage.isDojoScript = true;

		attrs.put("data-dojo-config", "paths: {'more': 'paths'}");	
		
		webPage.parsePreDojoScript(dojoScript);
		
		assertEquals(LoaderConfigParserTest.getPrepopulatedMap("some", "path", "an", "other", "more", "paths"), webPage.modulePaths);
	}
	
	@Test
	public void parsesPathsWithCorrectPrecedence() throws IOException {
		MockWebPage webPage = new MockWebPage();		
		webPage.scriptContents = "var djConfig = { paths: {'some': 'path', 'an': 'other'} }";
	
		Attributes attrs = new Attributes();			
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		webPage.parsePreDojoScript(dojoScript);
		
		webPage.isDojoScript = true;

		attrs.put("data-dojo-config", "paths: {'some': 'override'}");	
		
		webPage.parsePreDojoScript(dojoScript);
		
		assertEquals(LoaderConfigParserTest.getPrepopulatedMap("some", "override", "an", "other"), webPage.modulePaths);
	}	
	
	@Test
	public void canRecursivelyAnalyseNonAMDModules() throws IOException {
		MockWebPage webPage = new MockWebPage();				
		String moduleContents = getResourceAsString("sample_module_libs/non_amd/simple_deps/script.js");		
		
		Set<String> packages = new HashSet<String>() {{
			add("sample");
		}};
		
		webPage.staticModuleSource.put("sample.app", getResourceAsString("sample_module_libs/non_amd/simple_deps/app.js"));
		webPage.staticModuleSource.put("sample.dep_one", getResourceAsString("sample_module_libs/non_amd/simple_deps/dep_one.js"));
		webPage.staticModuleSource.put("sample.dep_two", getResourceAsString("sample_module_libs/non_amd/simple_deps/dep_two.js"));
		webPage.staticModuleSource.put("sample.dep_three", getResourceAsString("sample_module_libs/non_amd/simple_deps/dep_three.js"));		

		webPage.recursivelyAnalyseScriptDependencies(moduleContents);						
		
		assertEquals(packages, webPage.discoveredModules.keySet());
		assertEquals(Arrays.asList("sample.app", "sample.dep_one", "sample.dep_two", "sample.dep_three"), 
			webPage.discoveredModules.get("sample"));
		assertEquals(webPage.staticModuleSource, webPage.moduleSource);
	}
	
	@Test
	public void canRecursivelyAnalyseNonAMDModulesWithCyclicDependencies() throws IOException {
		MockWebPage webPage = new MockWebPage();				
		String moduleContents = getResourceAsString("sample_module_libs/non_amd/cyclic_deps/script.js");		
		
		Set<String> packages = new HashSet<String>() {{
			add("sample");
		}};
		
		webPage.staticModuleSource.put("sample.app", getResourceAsString("sample_module_libs/non_amd/cyclic_deps/app.js"));
		webPage.staticModuleSource.put("sample.dep_one", getResourceAsString("sample_module_libs/non_amd/cyclic_deps/dep_one.js"));
		webPage.staticModuleSource.put("sample.dep_two", getResourceAsString("sample_module_libs/non_amd/cyclic_deps/dep_two.js"));
		webPage.staticModuleSource.put("sample.dep_three", getResourceAsString("sample_module_libs/non_amd/cyclic_deps/dep_three.js"));		

		webPage.recursivelyAnalyseScriptDependencies(moduleContents);						
		
		assertEquals(packages, webPage.discoveredModules.keySet());
		assertEquals(Arrays.asList("sample.app", "sample.dep_one", "sample.dep_two", "sample.dep_three"), 
			webPage.discoveredModules.get("sample"));
		assertEquals(webPage.staticModuleSource, webPage.moduleSource);
	}
	
	@Test
	public void canIgnoreNonAmdModulesFromSpecificPackages() throws IOException {
		MockWebPage webPage = new MockWebPage();				
		String moduleContents = getResourceAsString("sample_module_libs/non_amd/simple_deps/script.js");		
		
		Set<String> packages = new HashSet<String>() {{
			add("sample");
		}};
			
		webPage.ignoredPackages = packages;		
		webPage.recursivelyAnalyseScriptDependencies(moduleContents);						
		
		assertEquals(packages, webPage.discoveredModules.keySet());
		assertEquals(Arrays.asList("sample.app"), webPage.discoveredModules.get("sample"));
		assertEquals(webPage.staticModuleSource, webPage.moduleSource);
	}

	@Test
	public void canRecursivelyAnalyseAMDModules() throws IOException {
		MockWebPage webPage = new MockWebPage();	
		webPage.identifier = "/";
		webPage.moduleFormat = ModuleFormat.AMD;
		
		String moduleContents = getResourceAsString("sample_module_libs/amd/simple_deps/script.js");		
		
		Set<String> packages = new HashSet<String>() {{
			add("sample");
		}};
		
		webPage.staticModuleSource.put("sample/app", getResourceAsString("sample_module_libs/amd/simple_deps/app.js"));
		webPage.staticModuleSource.put("sample/dep_one", getResourceAsString("sample_module_libs/amd/simple_deps/dep_one.js"));
		webPage.staticModuleSource.put("sample/dep_two", getResourceAsString("sample_module_libs/amd/simple_deps/dep_two.js"));
		webPage.staticModuleSource.put("sample/dep_three", getResourceAsString("sample_module_libs/amd/simple_deps/dep_three.js"));		

		webPage.recursivelyAnalyseScriptDependencies(moduleContents);						
		
		assertEquals(packages, webPage.discoveredModules.keySet());
		assertEquals(Arrays.asList("sample/app", "sample/dep_one", "sample/dep_two", "sample/dep_three"), 
			webPage.discoveredModules.get("sample"));
		assertEquals(webPage.staticModuleSource, webPage.moduleSource);
	}
	
	@Test
	public void canRecursivelyAnalyseAMDModulesWithCyclicDependencies() throws IOException {
		MockWebPage webPage = new MockWebPage();	
		webPage.identifier = "/";
		webPage.moduleFormat = ModuleFormat.AMD;
		
		String moduleContents = getResourceAsString("sample_module_libs/amd/cyclic_deps/script.js");		
		
		Set<String> packages = new HashSet<String>() {{
			add("sample");
		}};
		
		webPage.staticModuleSource.put("sample/app", getResourceAsString("sample_module_libs/amd/cyclic_deps/app.js"));
		webPage.staticModuleSource.put("sample/dep_one", getResourceAsString("sample_module_libs/amd/cyclic_deps/dep_one.js"));
		webPage.staticModuleSource.put("sample/dep_two", getResourceAsString("sample_module_libs/amd/cyclic_deps/dep_two.js"));
		webPage.staticModuleSource.put("sample/dep_three", getResourceAsString("sample_module_libs/amd/cyclic_deps/dep_three.js"));		

		webPage.recursivelyAnalyseScriptDependencies(moduleContents);						
		
		assertEquals(packages, webPage.discoveredModules.keySet());
		assertEquals(Arrays.asList("sample/app", "sample/dep_one", "sample/dep_two", "sample/dep_three"), 
			webPage.discoveredModules.get("sample"));
		assertEquals(webPage.staticModuleSource, webPage.moduleSource);
	}
	
	@Test
	public void canIgnoreAmdModulesFromSpecificPackages() throws IOException {
		MockWebPage webPage = new MockWebPage();			
		webPage.identifier = "/";
		webPage.moduleFormat = ModuleFormat.AMD;
		String moduleContents = getResourceAsString("sample_module_libs/amd/simple_deps/script.js");		
		
		Set<String> packages = new HashSet<String>() {{
			add("sample");
		}};
			
		webPage.ignoredPackages = packages;		
		webPage.recursivelyAnalyseScriptDependencies(moduleContents);						
		
		assertEquals(packages, webPage.discoveredModules.keySet());
		assertEquals(Arrays.asList("sample/app"), webPage.discoveredModules.get("sample"));
		assertEquals(webPage.staticModuleSource, webPage.moduleSource);
	}		
	
	private class MockWebPage extends RecursiveWebPage {
		public boolean isDojoScript = false;
		public String scriptContents = "";
		
		public Map<String, String> staticModuleSource = new HashMap<String, String>(); 
				
		public String identifier = "\\.";
		
		protected MockWebPage() throws IOException {
			super(null, null);
		}

		@Override
		protected boolean isDojoScript(Element script) {
			return isDojoScript;
		}

		@Override
		protected String getAbsoluteModuleIdentifier(String moduleIdentifer) {
			return moduleIdentifer;
		}

		@Override
		protected String getPackageIdentifier(String moduleIdentifer) {
			return moduleIdentifer.split(identifier)[0];
		}

		@Override
		protected String retrieveScriptContents(Element script) {
			return scriptContents;
		}	

		@Override
		protected String retrieveModuleSource(String moduleIdentifier) {
			return staticModuleSource.get(moduleIdentifier);
		}

		// Override parsing to stop it being called...
		@Override 
		protected void parse() {			
		}
	}
	
}
