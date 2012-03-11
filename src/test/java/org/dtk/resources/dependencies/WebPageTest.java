package org.dtk.resources.dependencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.* ;
import static org.junit.Assert.* ;

/**
 * Test the ability of the WebPage parser to handle a variety of 
 * remote and local web pages. 
 * 
 * Local pages are static resources and shouldn't attempt to pull down any 
 * custom modules defined. 
 * 
 * Remote resources represent a more complex task, which should automatically
 * parse and extract all custom modules with source from the remote host. Testing
 * using a variety of remote pages, which different Dojo configurations. 
 * 
 * @author James Thomas
 */

public class WebPageTest {
	/** Parameter keys for remote web page meta-data container. Used to 
	 *  access test values for each parsed page. */
	protected final static String PAGE_ADDRESS = "pageAddress";
	protected final static String PAGE_MODULES = "pageModules";
	protected final static String CUSTOM_MODULES = "customPageModules";
	
	/** Remote web page with large amount of DTK modules along with two 
	 *  custom modules */
	protected Map<String, Object> remotePageWithCustomModules = new HashMap<String, Object>() {{
		put(PAGE_ADDRESS, "http://staging.onlinescoutmanager.co.uk/");
		put(PAGE_MODULES, Arrays.asList(
			"dijit.Calendar", 
			"dijit.layout.ContentPane",
			"dijit.layout.BorderContainer",
			"dijit.layout.TabContainer",
			"jellard.widgets.TabContainerIntelligentClose",
			"jellard.widgets.DateTextBox",
			"dijit.TitlePane",
			"dojox.grid.DataGrid",
			"dojox.charting.Chart2D",
			"dojo.data.ItemFileWriteStore",
	        "dijit.form.Form",
	        "dijit.form.Button",
	        "dijit.form.ValidationTextBox",
	        "dijit.form.Textarea",
	        "dijit.form.DateTextBox",
	        "dijit.form.Select",
	        "dijit.form.FilteringSelect",
	        "dijit.form.CheckBox",
	        "dijit.Dialog",
	        "dijit.Menu",
	        "dijit.form.DropDownButton",
	        "dijit.form.ComboBox",
	        "dojox.charting.Chart2D",
	        "dijit.Editor"));
		put(CUSTOM_MODULES, Arrays.asList(
			"jellard.widgets.TabContainerIntelligentClose",
			"jellard.widgets.DateTextBox"));
	}};
	
	/** Simple Dojo application using small amount of modules with no custom module
	 * definitions. HTML page links to external JS source file containing application
	 * source code. */
	protected Map<String, Object> simplePageWithFewModules = new HashMap<String, Object>() {{
		put(PAGE_ADDRESS, "http://o.sitepen.com/labs/code/dnd/intro/dnd/5-dnd.html");
		put(PAGE_MODULES, Arrays.asList(
			"dojo.dnd.Source",
			"dijit.TitlePane",
			"dijit.form.Button"));
		put(CUSTOM_MODULES, Arrays.asList());
	}};
	
	/** Standard Dojo application with large amount of standard modules, no custom
	 * external modules defined. Also, includes missing referenced JS file, "layer.js".
	 * All modules should be pulled from external dojo source.*/
	protected Map<String, Object> pageWithLotsOfStandardModules = new HashMap<String, Object>() {{
		put(PAGE_ADDRESS, "http://demos.dojotoolkit.org/demos/editor/");
		put(PAGE_MODULES, Arrays.asList(
			"dijit.layout.BorderContainer",
			"dijit.layout.ContentPane",
			"dijit.layout.AccordionContainer",
			"dijit.layout.AccordionPane",
			"dojox.fx.text",
	 		"dijit.Editor",
	 		"dijit._editor.plugins.FullScreen",
			"dijit._editor.plugins.LinkDialog",
			"dijit._editor.plugins.Print",
			"dijit._editor.plugins.ViewSource",
			"dijit._editor.plugins.FontChoice",
			"dijit._editor.plugins.NewPage",
			"dijit._editor.plugins.ToggleDir",
			"dojox.editor.plugins.ShowBlockNodes",
			"dojox.editor.plugins.ToolbarLineBreak",
			"dojox.editor.plugins.Save",
			"dojox.editor.plugins.InsertEntity",
			"dojox.editor.plugins.Preview",
			"dojox.editor.plugins.PageBreak",
			"dojox.editor.plugins.PrettyPrint",
			"dojox.editor.plugins.CollapsibleToolbar",
			"dojox.editor.plugins.Blockquote",
			"dojox.editor.plugins.InsertAnchor",
			"dojox.editor.plugins.NormalizeIndentOutdent",
			"dojox.editor.plugins.FindReplace",
			"dojox.editor.plugins.TablePlugins",
			"dojox.editor.plugins.TextColor",
			"dojox.editor.plugins.Breadcrumb",
			"dojox.editor.plugins.PasteFromWord",
			"dojox.editor.plugins.Smiley",
			"dojox.editor.plugins.NormalizeStyle",
			"dojox.editor.plugins.StatusBar"));
		put(CUSTOM_MODULES, Arrays.asList());
	}};
	
	/** Very basic Dojo application, with custom modules located at relative root
	 *  path from base DTK modules. */
	protected Map<String, Object> tinyPageWithRelativeCustomModules = new HashMap<String, Object>() {{
		put(PAGE_ADDRESS, "http://demos.dojotoolkit.org/demos/fonts/");
		put(PAGE_MODULES, Arrays.asList(
			"demos.fonts.src.news",
			"demos.fonts.src.pie",
			"dojo.date.locale", 
			"dojox.gfx", 
			"dojox.gfx.VectorText",
			"dojox.gfx.utils"));
		put(CUSTOM_MODULES, Arrays.asList(
			"demos.fonts.src.news",
			"demos.fonts.src.pie"));
	}};
	
	/** Large Dojo application with no custom modules, lots of HTML and JS to parse with multiple
	 *  script tags (inline and external). */
	protected Map<String, Object> largePageWithStandardModules = new HashMap<String, Object>() {{
		put(PAGE_ADDRESS, "http://www.lightstreamer.com/demo/DojoDemo/");
		put(PAGE_MODULES, Arrays.asList(
			"dojo.parser",
			"dijit.dijit",
			"dojox.grid.DataGrid",
			"dojo.data.ObjectStore",
			"dojox.store.LightstreamerStore",
			"dijit.layout.BorderContainer",
			"dijit.layout.ContentPane",
			"dijit.form.Button",
			"dijit.Dialog",
			"dojox.charting.Chart",
			"dojox.charting.axis2d.Default",
			"dojox.charting.plot2d.Default",
			"dojox.charting.themes.Claro"));
		put(CUSTOM_MODULES, Arrays.asList());
	}};
	
	
	@Test
	public void test_remotePageWithCustomModules() throws IOException {
		verifyPageMetaData(remotePageWithCustomModules);
 	}
	
	@Test
	public void test_simplePageWithStandardModules() throws IOException {
		verifyPageMetaData(simplePageWithFewModules);
 	}
	
	@Test
	public void test_pageWithLotsOfStandardModules() throws IOException {
		verifyPageMetaData(pageWithLotsOfStandardModules);
 	}
	
	@Test
	public void test_tinyPageWithRelativeCustomModules() throws IOException {
		verifyPageMetaData(tinyPageWithRelativeCustomModules);
 	}
 	
	@Test 
	public void test_largePageWithStandardModules() throws IOException {
		verifyPageMetaData(largePageWithStandardModules);
 	}
	
	/**
	 * 
	 * 
	 * @param pageMetaData
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	protected void verifyPageMetaData(Map<String, Object> pageMetaData) throws MalformedURLException, IOException {
		/** Extract full page meta-data for this location. */
		String pageAddress = (String) pageMetaData.get(PAGE_ADDRESS);
		List<String> pageModules = (List<String>) pageMetaData.get(PAGE_MODULES);
		List<String> customModules = (List<String>) pageMetaData.get(CUSTOM_MODULES);
		
		/** Create new instance for this web address and verify the page parses correctly */
		WebPage testPage = new WebPage(new URL(pageAddress));
		assertTrue(testPage.parse());
		
		List<String> testPageModules = testPage.getModules();
		Map<String, String> customPageModules = testPage.getCustomModuleContent();
		
		/** Ensure lists are in the same order for comparison */
		Collections.sort(testPageModules);
		Collections.sort(pageModules);
		
		/** Verify all application modules parsed as expected */
		assertEquals(testPageModules, pageModules);
		
		/** Should only find (non-empty) custom modules we're expecting */
		assertEquals(new HashSet<String>(customModules), customPageModules.keySet());
		
		for(String content: customPageModules.values()) {
			assertTrue(content != null && content.length() > 0);
		}
	}
}


