package org.dtk.resources.dependencies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.dtk.resources.dependencies.ScriptParser;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.* ;

public class ScriptParserTest {
	protected static HttpClient httpclient = new DefaultHttpClient();
	
	protected static String sampleModuleLocation = "http://jthomas.vm.bytemark.co.uk/js/web_builder/app.js";
	
	//protected static String sampleModuleLocation = "http://localhost:8080/dwb/js/dwb/Main/_base.js";
	
	protected static ScriptParser scriptParser = null;
	
	protected static String moduleContent = null;
	
	public ScriptParserTest() {
		
	}
	
	@BeforeClass 
	public static void downloadSampleModules () {
		moduleContent = retrieveURLContent(sampleModuleLocation);
	}
	
	@Test
	public void test_ParsingSimpleContent() {
		scriptParser = new ScriptParser(moduleContent);	
		scriptParser.parse();
	}
	
	@Test
	public void test_RetrieveModuleRequires() {
		assertEquals(scriptParser.retrieveModuleRequires(), Arrays.asList("dojo.back", "web_builder.child"));
	}
	
	@Test
	public void test_RetrieveModuleProvides() {
		assertEquals(scriptParser.retrieveModuleProvides(), Arrays.asList("web_builder.app"));
	}
	
	@Test
	public void test_RetrieveModuleDeclares() {
		assertEquals(scriptParser.retrieveModuleDeclares(), Arrays.asList("web_builder.app"));
	}
	
	@Test
	public void test_RetrieveModulePaths() {
		Map<String, String> modulePaths = new HashMap<String, String>() {{
			put("some.path", "../../modules");
			put("another.path", "../../more_modules");
			put("foo.bar", "../other_modules");
			put("misc", "/misc/more/modules");
			put("a", "../a");
		}};
		
		assertEquals(scriptParser.retrieveModulePaths(), modulePaths);
	}
	
	private static String retrieveURLContent(String URL) {
		String URLContent = null;
		
        try {
        	HttpGet httpget = new HttpGet(URL);
			HttpResponse response = httpclient.execute(httpget);
			URLContent = EntityUtils.toString(response.getEntity());
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return URLContent;
	}
}
