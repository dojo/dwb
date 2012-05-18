package org.dtk.analysis.page;

import static org.junit.Assert.*;

import org.dtk.analysis.ModuleFormat;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.Test;

public class WebPageTest {
	
	@Test
	public void defaultsToNonAmdScriptModeWithoutConfig() {
		MockWebPage webPage = new MockWebPage();
		webPage.isDojoScript = true;
		
		Attributes attrs = new Attributes();		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		assertEquals(ModuleFormat.NON_AMD, webPage.moduleFormat);
		webPage.parsePreDojoScript(dojoScript);
		assertEquals(ModuleFormat.NON_AMD, webPage.moduleFormat);
	}
	
	@Test
	public void disablesAmdScriptParsingWithConfigAttrOnScriptTag() {
		MockWebPage webPage = new MockWebPage();
		webPage.isDojoScript = true;
		
		Attributes attrs = new Attributes();
		attrs.put("data-dojo-config", "async: false");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		webPage.parsePreDojoScript(dojoScript);
		assertEquals(ModuleFormat.NON_AMD, webPage.moduleFormat);
	}
	
	@Test
	public void enablesAmdScriptParsingWithConfigAttrOnScriptTag() {
		MockWebPage webPage = new MockWebPage();
		webPage.isDojoScript = true;
		
		Attributes attrs = new Attributes();
		attrs.put("data-dojo-config", "async: true");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		webPage.parsePreDojoScript(dojoScript);
		assertEquals(ModuleFormat.AMD, webPage.moduleFormat);
	}
	
	@Test
	public void enablesAmdScriptParsingWithOldConfigAttrOnScriptTag() {
		MockWebPage webPage = new MockWebPage();
		webPage.isDojoScript = true;
		
		Attributes attrs = new Attributes();
		attrs.put("djconfig", "async: true");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		webPage.parsePreDojoScript(dojoScript);
		assertEquals(ModuleFormat.AMD, webPage.moduleFormat);
	}
	
	@Test
	public void enablesAmdScriptParsingWithLocalConfigInScriptTag() {
		MockWebPage webPage = new MockWebPage();
		webPage.scriptContents = "var dojoConfig = { async : true }";
		
		Attributes attrs = new Attributes();		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		webPage.parsePreDojoScript(dojoScript);
		assertEquals(ModuleFormat.AMD, webPage.moduleFormat);
		
		webPage.moduleFormat = ModuleFormat.NON_AMD;
		webPage.scriptContents = "var djConfig = { async : true }";
		webPage.parsePreDojoScript(dojoScript);
		assertEquals(ModuleFormat.AMD, webPage.moduleFormat);		
	}
	
	@Test
	public void enablesAmdScriptParsingWithGlobalConfigInScriptTag() {
		MockWebPage webPage = new MockWebPage();
		webPage.scriptContents = "dojoConfig = { async : true }";
		
		Attributes attrs = new Attributes();		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		webPage.parsePreDojoScript(dojoScript);
		assertEquals(ModuleFormat.AMD, webPage.moduleFormat);
		
		webPage.moduleFormat = ModuleFormat.NON_AMD;
		webPage.scriptContents = "djConfig = { async : true }";
		webPage.parsePreDojoScript(dojoScript);
		assertEquals(ModuleFormat.AMD, webPage.moduleFormat);
	}
	
	private class MockWebPage extends WebPage {
		public boolean isDojoScript = false;
		public String scriptContents = "";
		
		protected MockWebPage() {
			super(null);
			parse();
		}

		@Override
		protected boolean isDojoScript(Element script) {
			return isDojoScript;
		}

		@Override
		protected String getAbsoluteModuleIdentifier(String moduleIdentifer) {
			return null;
		}

		@Override
		protected String getPackageIdentifier(String moduleIdentifer) {
			return null;
		}

		@Override
		protected String retrieveScriptContents(Element script) {
			return scriptContents;
		}
		
		// Override parsing to stop if being called...
		@Override 
		protected void parse() {			
		}
	}
	
}
