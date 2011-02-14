package org.dtk.resources.dependencies;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dtk.resources.dependencies.WebPage;
import org.junit.* ;
import static org.junit.Assert.* ;

public class WebPageTest {
	protected URL samplePageAddress;
	
	@Test
	public void test_parseSimplePage() throws IOException {
		//samplePageAddress = "http://o.sitepen.com/labs/code/dnd/intro/dnd/5-dnd.html";
		
		//samplePageAddress = new URL("http://jthomas.vm.bytemark.co.uk/sample2.html");
		
		//samplePageAddress = new URL("http://www.lightstreamer.com/demo/DojoDemo/");
		
		//samplePageAddress = "http://localhost:8080/dwb/";
		
		samplePageAddress = new URL("http://garethj.bluehost.ibm.com/boots/");
		
		//samplePageAddress = "http://9.145.187.101/em3/js/uk/co/bbc/fabric/em3/bijit/tests/FilterSelectTest.html";
		
		System.out.println("Test parsing of a very simple page") ;
		WebPage webPage = new WebPage(this.samplePageAddress);
		assertTrue(webPage.parse());
		
		System.out.println("Modules discovered...");
		for(String module : webPage.getModules()) {
			System.out.println(module);
		}
		
		System.out.println("Custom modules discovered...");
		
		Map<String, String> moduleContent = webPage.getCustomModuleContent();
		
		for(String key : moduleContent.keySet()) {
			System.out.println(key);
			System.out.println(moduleContent.get(key));
		}
 	}
	
	/*
	@Test public void test_parseDjConfigModulePaths() {
		WebPage webPage = new WebPage(null);
		webPage.parseDjConfigModulePaths("parseOnLoad:true, baseUrl:'./', modulePaths:{'web_builder': './js/web_builder'}");
		assertTrue(true);
	}
	
	/*
	@Test
	public void test_findAllModulesSimple() {
		System.out.println("Test finding all modules in a very simple page");
		WebPage webPage = new WebPage(this.samplePageAddress);
		webPage.parse();
		
		List<String> modules = webPage.getModules();
		
		assertTrue(modules.size() == 15);
	}*//*
	
	@Test
	public void test_isDojoScript() {
		WebPage webPage = new WebPage(null);
		String[] scripts = {
			"http://ajax.googleapis.com/ajax/libs/dojo/1.5/dojo/dojo.xd.js.uncompressed.js",
			"http://ajax.googleapis.com/ajax/libs/dojo/1.5/dojo/dojo.xd.js",
			"/some/file/locatio/dojo.js",
			"dojo.js",
			"http://example.com/dojo.js",
		};
		
		String[] incorrectScripts = {
			"dijit.js",
			"dojojs",
			"http://google.com/dojo.js/script.js"
		};
		
		for (String script: scripts ) {
			System.out.println("Testing (true) " + script);
			assertTrue(webPage.isDojoScript(script));
		}
		
		for (String script: incorrectScripts ) {
			System.out.println("Testing (false) " + script);
			assertFalse(webPage.isDojoScript(script));
		}
	} */
}


