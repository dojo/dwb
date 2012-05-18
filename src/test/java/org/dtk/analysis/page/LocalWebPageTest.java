package org.dtk.analysis.page;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.dtk.analysis.exceptions.FatalAnalysisError;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.Test;

public class LocalWebPageTest {
	@Test
	public void detectDojoScriptFromLocalFilePath() {
		LocalWebPage localWebPage = new LocalWebPage("");
		
		Attributes attrs = new Attributes();
		attrs.put("src", "dojo.js");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		assertTrue(localWebPage.isDojoScript(dojoScript));
	}
	
	@Test
	public void detectDojoScriptFromFullFilePath() {
		LocalWebPage localWebPage = new LocalWebPage("");
		
		Attributes attrs = new Attributes();
		attrs.put("src", "/some/folders/go/here/dojo.js");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		assertTrue(localWebPage.isDojoScript(dojoScript));
	}
	
	@Test
	public void detectDojoScriptWhenPathHasQueryParameters() {
		LocalWebPage localWebPage = new LocalWebPage("");
		
		Attributes attrs = new Attributes();
		attrs.put("src", "dojo.js?cachebust=true");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		assertTrue(localWebPage.isDojoScript(dojoScript));
	}
	
	@Test
	public void detectDojoScriptWithExtraPathChars() {
		LocalWebPage localWebPage = new LocalWebPage("");
		
		Attributes attrs = new Attributes();
		attrs.put("src", "dojo.js.uncompressed.js");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);		
		assertTrue(localWebPage.isDojoScript(dojoScript));
	}
	
	@Test
	public void dontDetectNonDojoScript() {
		LocalWebPage localWebPage = new LocalWebPage("");
		
		Attributes attrs = new Attributes();
		attrs.put("src", "another_script.js");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		assertFalse(localWebPage.isDojoScript(dojoScript));
	}
	
	// Testing pages with old-style module format dependencies....
	
	@Test
	public void detectsNonAmdModuleDependenciesInLocalPage() throws IOException, FatalAnalysisError {
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample_pages/non_amd/local.html");		
		String pageContents = IOUtils.toString(is);
		
		LocalWebPage localWebPage = new LocalWebPage(pageContents);
		
		Map<String, List<String>> expectedModulesAndPackages = new HashMap<String, List<String>>();
		expectedModulesAndPackages.put("dojo", Arrays.asList("dojo.parser", "dojo.data.ItemFileReadStore"));
		expectedModulesAndPackages.put("dijit", Arrays.asList("dijit.form.Button", "dijit.form.Form", 
			"dijit.layout.ContentPane", "dijit.layout.TabContainer", "dijit.form.Select"));		
		expectedModulesAndPackages.put("dojox", Arrays.asList("dojox.grid.EnhancedGrid", 
			"dojox.grid.enhanced.plugins.IndirectSelection", "dojox.data.AndOrWriteStore"));
		
		assertEquals(expectedModulesAndPackages, localWebPage.getModules());		
	}
	
	@Test
	public void detectsNonAmdModuleDependenciesInXDPage() throws IOException, FatalAnalysisError {
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample_pages/non_amd/cross_domain.html");		
		String pageContents = IOUtils.toString(is);
		
		LocalWebPage localWebPage = new LocalWebPage(pageContents);
		
		Map<String, List<String>> expectedModulesAndPackages = new HashMap<String, List<String>>();
		expectedModulesAndPackages.put("dojo", Arrays.asList("dojo.parser", "dojo.data.ItemFileReadStore"));
		expectedModulesAndPackages.put("dijit", Arrays.asList("dijit.form.CheckBox", "dijit.form.Button", 
			"dijit.layout.ContentPane", "dijit.TitlePane"));		
		expectedModulesAndPackages.put("dojox", Arrays.asList("dojox.grid.EnhancedGrid", 
			"dojox.grid.enhanced.plugins.IndirectSelection", "dojox.data.AndOrWriteStore"));
		
		assertEquals(expectedModulesAndPackages, localWebPage.getModules());		
	}
	
	@Test
	public void detectsNonAmdCustomModuleDependencies() throws IOException, FatalAnalysisError {
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample_pages/non_amd/local_with_non_dtk_modules.html");		
		String pageContents = IOUtils.toString(is);
		
		LocalWebPage localWebPage = new LocalWebPage(pageContents);
		
		Map<String, List<String>> expectedModulesAndPackages = new HashMap<String, List<String>>();
		expectedModulesAndPackages.put("web_builder", Arrays.asList("web_builder.app", "web_builder.app.a",
			"web_builder.app.b", "web_builder.app.c"));
		
		assertEquals(expectedModulesAndPackages, localWebPage.getModules());		
	}
	
	// Testing pages with AMD style module dependencies
	
	@Test
	public void detectsAmdModuleDependenciesInRequireAPI() throws IOException, FatalAnalysisError {
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample_pages/amd/local_with_require.html");		
		String pageContents = IOUtils.toString(is);
		
		LocalWebPage localWebPage = new LocalWebPage(pageContents);
		
		Map<String, List<String>> expectedModulesAndPackages = new HashMap<String, List<String>>();
		expectedModulesAndPackages.put("dojo", Arrays.asList("dojo/parser", "dojo/data/ItemFileReadStore"));
		expectedModulesAndPackages.put("dijit", Arrays.asList("dijit/form/Button", "dijit/form/Form", 
			"dijit/layout/ContentPane", "dijit/layout/TabContainer", "dijit/form/Select"));		
		expectedModulesAndPackages.put("dojox", Arrays.asList("dojox/grid/EnhancedGrid", 
			"dojox/grid/enhanced/plugins/IndirectSelection", "dojox/data/AndOrWriteStore"));
		
		assertEquals(expectedModulesAndPackages, localWebPage.getModules());		
	}
	
	@Test
	public void detectsAmdModuleDependenciesInDefineAPI() throws IOException, FatalAnalysisError {
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample_pages/amd/local_with_define.html");		
		String pageContents = IOUtils.toString(is);
		
		LocalWebPage localWebPage = new LocalWebPage(pageContents);
		
		Map<String, List<String>> expectedModulesAndPackages = new HashMap<String, List<String>>();
		expectedModulesAndPackages.put("dojo", Arrays.asList("dojo/parser", "dojo/data/ItemFileReadStore"));
		expectedModulesAndPackages.put("dijit", Arrays.asList("dijit/form/Button", "dijit/form/Form", 
			"dijit/layout/ContentPane", "dijit/layout/TabContainer", "dijit/form/Select"));		
		expectedModulesAndPackages.put("dojox", Arrays.asList("dojox/grid/EnhancedGrid", 
			"dojox/grid/enhanced/plugins/IndirectSelection", "dojox/data/AndOrWriteStore"));
		
		assertEquals(expectedModulesAndPackages, localWebPage.getModules());		
	}
	
	@Test
	public void detectsAmdModuleDependenciesWithNonDTKModules() throws IOException, FatalAnalysisError {
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample_pages/amd/local_with_non_dtk_modules.html");		
		String pageContents = IOUtils.toString(is);
		
		LocalWebPage localWebPage = new LocalWebPage(pageContents);
		
		Map<String, List<String>> expectedModulesAndPackages = new HashMap<String, List<String>>();
		expectedModulesAndPackages.put("web_builder", Arrays.asList("web_builder/app", "web_builder/app/a",
				"web_builder/app/b", "web_builder/app/c"));
		
		assertEquals(expectedModulesAndPackages, localWebPage.getModules());		
	}
}
