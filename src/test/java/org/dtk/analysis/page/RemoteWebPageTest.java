package org.dtk.analysis.page;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.dtk.analysis.exceptions.FatalAnalysisError;
import org.dtk.analysis.exceptions.ModuleSourceNotAvailable;
import org.dtk.analysis.exceptions.UnknownModuleIdentifier;
import org.dtk.util.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.Test;

public class RemoteWebPageTest {
		
	@Test
	public void willRetrieveInlineScriptContents() throws MalformedURLException, IOException {
		Document document = new Document("");

		RemoteWebPage webPage = new RemoteWebPage(document, new URL("http://localhost/"), new MockHttpClient());		
		
		// Create inline script tag...
		String scriptContents = getResourceAsString("sample_module_libs/non_amd/simple_deps/app.js");
		Attributes attrs = new Attributes();
		Element inlineScript = new Element(Tag.valueOf("script"), "", attrs);				
		document.appendChild(inlineScript);		
		inlineScript.html(scriptContents);
		
		String comparison = StringEscapeUtils.unescapeHtml(webPage.retrieveScriptContents(inlineScript));		
		assertEquals(scriptContents.trim(), comparison.trim());		
	}
		
	@Test
	public void willRetrieveExternalScriptContents() throws MalformedURLException, IOException {
		Document document = new Document("");
		MockHttpClient mockHttpClient = new MockHttpClient();

		RemoteWebPage webPage = new RemoteWebPage(document, new URL("http://localhost/"), mockHttpClient);		
		
		// Create inline script tag...
		String scriptContents = getResourceAsString("sample_module_libs/non_amd/simple_deps/app.js");
		Attributes attrs = new Attributes();
		attrs.put("src", "app.js");		
		Element inlineScript = new Element(Tag.valueOf("script"), "http://localhost/", attrs);						
		
		// Set pre-canned response
		mockHttpClient.hostPrefix = "http://localhost/";
		mockHttpClient.appDir = "sample_module_libs/non_amd/simple_deps/";		 
		
		assertEquals(scriptContents, webPage.retrieveScriptContents(inlineScript));		
	}
		
	@Test
	public void willRecursivelyParseAmdModulesDependencies() throws MalformedURLException, IOException, FatalAnalysisError, ModuleSourceNotAvailable, UnknownModuleIdentifier {		
		MockHttpClient mockHttpClient = new MockHttpClient();

		// Set pre-canned response
		mockHttpClient.hostPrefix = "http://localhost/";
		mockHttpClient.appDir = "sample_apps/amd/local_dtk_with_only_dtk_reqs/";		 
		
		Document document = Jsoup.parse(getResourceAsString("sample_apps/amd/local_dtk_with_only_dtk_reqs/index.html"), mockHttpClient.hostPrefix);
		
		RemoteWebPage webPage = new RemoteWebPage(document, new URL(mockHttpClient.hostPrefix), mockHttpClient);										
		
		Map<String, List<String>> packages = new HashMap<String, List<String>>() {{
			put("dojo", Arrays.asList("dojo/parser"));
			put("dijit", Arrays.asList("dijit/form/Button"));
			put("dojox", Arrays.asList("dojox/grid/EnhancedGrid"));
		}};
				
		assertEquals(packages, webPage.getModules());
		assertEquals(getResourceAsString("sample_apps/amd/local_dtk_with_only_dtk_reqs/lib/dojo/parser.js"),
			webPage.getModuleSource("dojo/parser"));
		assertEquals(getResourceAsString("sample_apps/amd/local_dtk_with_only_dtk_reqs/lib/dijit/form/Button.js"),
				webPage.getModuleSource("dijit/form/Button"));
		assertEquals(getResourceAsString("sample_apps/amd/local_dtk_with_only_dtk_reqs/lib/dojox/grid/EnhancedGrid.js"),
				webPage.getModuleSource("dojox/grid/EnhancedGrid"));					
	}
	
	@Test
	public void willRecursivelyParseNonAmdModulesDependencies() throws MalformedURLException, IOException, FatalAnalysisError, ModuleSourceNotAvailable, UnknownModuleIdentifier {		
		MockHttpClient mockHttpClient = new MockHttpClient();

		// Set pre-canned response
		mockHttpClient.hostPrefix = "http://localhost/";
		mockHttpClient.appDir = "sample_apps/non_amd/local_dtk_with_only_dtk_reqs/";		 
		
		Document document = Jsoup.parse(getResourceAsString("sample_apps/non_amd/local_dtk_with_only_dtk_reqs/index.html"), mockHttpClient.hostPrefix);
		
		RemoteWebPage webPage = new RemoteWebPage(document, new URL(mockHttpClient.hostPrefix), mockHttpClient);										
		
		Map<String, List<String>> packages = new HashMap<String, List<String>>() {{
			put("dojo", Arrays.asList("dojo.parser"));
			put("dijit", Arrays.asList("dijit.form.Button"));
			put("dojox", Arrays.asList("dojox.grid.EnhancedGrid"));
		}};
				
		assertEquals(packages, webPage.getModules());
		assertEquals(getResourceAsString("sample_apps/non_amd/local_dtk_with_only_dtk_reqs/lib/dojo/parser.js"),
			webPage.getModuleSource("dojo.parser"));
		assertEquals(getResourceAsString("sample_apps/non_amd/local_dtk_with_only_dtk_reqs/lib/dijit/form/Button.js"),
				webPage.getModuleSource("dijit.form.Button"));
		assertEquals(getResourceAsString("sample_apps/non_amd/local_dtk_with_only_dtk_reqs/lib/dojox/grid/EnhancedGrid.js"),
				webPage.getModuleSource("dojox.grid.EnhancedGrid"));					
	}
	
	@Test
	public void willRecursivelyParseAmdModulesWithPathsConfig() throws MalformedURLException, IOException, FatalAnalysisError, ModuleSourceNotAvailable, UnknownModuleIdentifier {		
		MockHttpClient mockHttpClient = new MockHttpClient();
		
		// Set pre-canned response
		mockHttpClient.hostPrefix = "http://localhost/";
		mockHttpClient.appDir = "sample_apps/amd/local_dtk_with_custom_modules_paths/";		 
		
		Document document = Jsoup.parse(getResourceAsString(mockHttpClient.appDir + "index.html"), mockHttpClient.hostPrefix);
		
		RemoteWebPage webPage = new RemoteWebPage(document, new URL(mockHttpClient.hostPrefix), mockHttpClient);										
		
		Map<String, List<String>> packages = new HashMap<String, List<String>>() {{
			put("dojo", Arrays.asList("dojo/parser"));
			put("dijit", Arrays.asList("dijit/form/Button"));
			put("dojox", Arrays.asList("dojox/grid/EnhancedGrid"));
			put("custom", Arrays.asList("custom/custom"));
			put("sample", Arrays.asList("sample/app", "sample/dep_one", "sample/dep_two", "sample/dep_three"));
		}};
				
		assertEquals(packages, webPage.getModules());
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/lib/dtk/dojo/parser.js"),
			webPage.getModuleSource("dojo/parser"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/lib/dtk/dijit/form/Button.js"),
				webPage.getModuleSource("dijit/form/Button"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/lib/dtk/dojox/grid/EnhancedGrid.js"),
				webPage.getModuleSource("dojox/grid/EnhancedGrid"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/lib/custom/custom.js"),
				webPage.getModuleSource("custom/custom"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/sample/app.js"),
				webPage.getModuleSource("sample/app"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/sample/dep_one.js"),
				webPage.getModuleSource("sample/dep_one"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/sample/dep_two.js"),
				webPage.getModuleSource("sample/dep_two"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/sample/dep_three.js"),
				webPage.getModuleSource("sample/dep_three"));
	}
	
	@Test
	public void willRecursivelyParseNonAmdModulesWithPathsConfig() throws MalformedURLException, IOException, FatalAnalysisError, ModuleSourceNotAvailable, UnknownModuleIdentifier {		
		MockHttpClient mockHttpClient = new MockHttpClient();
		
		// Set pre-canned response
		mockHttpClient.hostPrefix = "http://localhost/";
		mockHttpClient.appDir = "sample_apps/non_amd/local_dtk_with_custom_modules_paths/";		 
		
		Document document = Jsoup.parse(getResourceAsString(mockHttpClient.appDir + "index.html"), mockHttpClient.hostPrefix);
		
		RemoteWebPage webPage = new RemoteWebPage(document, new URL(mockHttpClient.hostPrefix), mockHttpClient);										
		
		Map<String, List<String>> packages = new HashMap<String, List<String>>() {{
			put("dojo", Arrays.asList("dojo.parser"));
			put("dijit", Arrays.asList("dijit.form.Button"));
			put("dojox", Arrays.asList("dojox.grid.EnhancedGrid"));
			put("custom", Arrays.asList("custom.custom"));
			put("sample", Arrays.asList("sample.app", "sample.dep_one", "sample.dep_two", "sample.dep_three"));
		}};
				
		assertEquals(packages, webPage.getModules());
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/lib/dtk/dojo/parser.js"),
			webPage.getModuleSource("dojo.parser"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/lib/dtk/dijit/form/Button.js"),
				webPage.getModuleSource("dijit.form.Button"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/lib/dtk/dojox/grid/EnhancedGrid.js"),
				webPage.getModuleSource("dojox.grid.EnhancedGrid"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/lib/custom/custom.js"),
				webPage.getModuleSource("custom.custom"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/sample/app.js"),
				webPage.getModuleSource("sample.app"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/sample/dep_one.js"),
				webPage.getModuleSource("sample.dep_one"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/sample/dep_two.js"),
				webPage.getModuleSource("sample.dep_two"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/sample/dep_three.js"),
				webPage.getModuleSource("sample.dep_three"));
	}
	

	@Test
	public void willRecursivelyParseAmdModulesDependenciesWithCustomBaseUrl() throws MalformedURLException, IOException, FatalAnalysisError, ModuleSourceNotAvailable, UnknownModuleIdentifier {		
		MockHttpClient mockHttpClient = new MockHttpClient();

		// Set pre-canned response
		mockHttpClient.hostPrefix = "http://localhost/";
		mockHttpClient.appDir = "sample_apps/amd/local_dtk_with_custom_base_url/";		 
		
		Document document = Jsoup.parse(getResourceAsString("sample_apps/amd/local_dtk_with_custom_base_url/index.html"), mockHttpClient.hostPrefix);
		
		RemoteWebPage webPage = new RemoteWebPage(document, new URL(mockHttpClient.hostPrefix), mockHttpClient);										
		
		Map<String, List<String>> packages = new HashMap<String, List<String>>() {{
			put("dojo", Arrays.asList("dojo/parser"));
			put("dijit", Arrays.asList("dijit/form/Button"));
			put("dojox", Arrays.asList("dojox/grid/EnhancedGrid"));
		}};
				
		assertEquals(packages, webPage.getModules());
		assertEquals(getResourceAsString("sample_apps/amd/local_dtk_with_custom_base_url/dojo/parser.js"),
			webPage.getModuleSource("dojo/parser"));
		assertEquals(getResourceAsString("sample_apps/amd/local_dtk_with_custom_base_url/dijit/form/Button.js"),
				webPage.getModuleSource("dijit/form/Button"));
		assertEquals(getResourceAsString("sample_apps/amd/local_dtk_with_custom_base_url/dojox/grid/EnhancedGrid.js"),
				webPage.getModuleSource("dojox/grid/EnhancedGrid"));					
	}
	
	@Test
	public void willRecursivelyParseNonAmdModulesDependenciesWithCustomBaseUrl() throws MalformedURLException, IOException, FatalAnalysisError, ModuleSourceNotAvailable, UnknownModuleIdentifier {		
		MockHttpClient mockHttpClient = new MockHttpClient();

		// Set pre-canned response
		mockHttpClient.hostPrefix = "http://localhost/";
		mockHttpClient.appDir = "sample_apps/non_amd/local_dtk_with_custom_base_url/";		 
		
		Document document = Jsoup.parse(getResourceAsString("sample_apps/non_amd/local_dtk_with_custom_base_url/index.html"), mockHttpClient.hostPrefix);
		
		RemoteWebPage webPage = new RemoteWebPage(document, new URL(mockHttpClient.hostPrefix), mockHttpClient);										
		
		Map<String, List<String>> packages = new HashMap<String, List<String>>() {{
			put("dojo", Arrays.asList("dojo.parser"));
			put("dijit", Arrays.asList("dijit.form.Button"));
			put("dojox", Arrays.asList("dojox.grid.EnhancedGrid"));
		}};
				
		assertEquals(packages, webPage.getModules());
		assertEquals(getResourceAsString("sample_apps/non_amd/local_dtk_with_custom_base_url/dojo/parser.js"),
			webPage.getModuleSource("dojo.parser"));
		assertEquals(getResourceAsString("sample_apps/non_amd/local_dtk_with_custom_base_url/dijit/form/Button.js"),
				webPage.getModuleSource("dijit.form.Button"));
		assertEquals(getResourceAsString("sample_apps/non_amd/local_dtk_with_custom_base_url/dojox/grid/EnhancedGrid.js"),
				webPage.getModuleSource("dojox.grid.EnhancedGrid"));					
	}
	
	@Test
	public void willIgnoreExplicitPackagesForRecursivelyParsing() throws MalformedURLException, IOException, FatalAnalysisError, ModuleSourceNotAvailable, UnknownModuleIdentifier {		
		MockHttpClient mockHttpClient = new MockHttpClient();
		
		// Set pre-canned response
		mockHttpClient.hostPrefix = "http://localhost/";
		mockHttpClient.appDir = "sample_apps/amd/local_dtk_with_custom_modules_paths/";		 
		
		Document document = Jsoup.parse(getResourceAsString(mockHttpClient.appDir + "index.html"), mockHttpClient.hostPrefix);
		
		RemoteWebPage webPage = new RemoteWebPage(document, new URL(mockHttpClient.hostPrefix), 
			mockHttpClient, new HashSet<String>() {{
			add("sample");
		}});										
		
		Map<String, List<String>> packages = new HashMap<String, List<String>>() {{
			put("dojo", Arrays.asList("dojo/parser"));
			put("dijit", Arrays.asList("dijit/form/Button"));
			put("dojox", Arrays.asList("dojox/grid/EnhancedGrid"));
			put("custom", Arrays.asList("custom/custom"));
			put("sample", Arrays.asList("sample/app"));
		}};
				
		assertEquals(packages, webPage.getModules());
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/lib/dtk/dojo/parser.js"),
			webPage.getModuleSource("dojo/parser"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/lib/dtk/dijit/form/Button.js"),
				webPage.getModuleSource("dijit/form/Button"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/lib/dtk/dojox/grid/EnhancedGrid.js"),
				webPage.getModuleSource("dojox/grid/EnhancedGrid"));
		assertEquals(getResourceAsString(mockHttpClient.appDir + "/lib/custom/custom.js"),
				webPage.getModuleSource("custom/custom"));
		
		boolean sourceAvailable = false;
		
		try {
			assertEquals(getResourceAsString(mockHttpClient.appDir + "/sample/app.js"),
				webPage.getModuleSource("sample/app"));
			sourceAvailable = true;
		} catch (ModuleSourceNotAvailable e) {			
		}
		
		assertFalse(sourceAvailable);		
	}
	
	// Utility method
	private String getResourceAsString(String filePath) throws IOException {
		InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);		
		if (is == null) {
			return null;
		}
		return IOUtils.toString(is);
	}
		
	/**
	 * Mock implementation of the HTTP client, will return responses from 
	 * a file system rather than a remote host.
	 * 
	 * @author James Thomas
	 */
	private class MockHttpClient implements HttpClient {
		
		public String hostPrefix;
		public String appDir;
		
		@Override
		public HttpResponse execute(HttpUriRequest request) throws IOException,
			ClientProtocolException {
			
			String url = request.getURI().toString();
			
			if (!url.startsWith(hostPrefix)) {
				throw new IOException("Trying to read non-local file");
			}
			
			url = appDir + url.substring(hostPrefix.length());
			String responseText = getResourceAsString(url);
			
			StatusLine statusLine;			
			HttpResponse response;
			
			if (responseText != null) {
				statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), HttpStatus.SC_OK, null);			
				response = new BasicHttpResponse(statusLine);				
				response.setEntity(new StringEntity(responseText));
			} else {
				statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), HttpStatus.SC_NOT_FOUND, null);			
				response = new BasicHttpResponse(statusLine);
			}
			
			return response;
		}
		
		@Override
		public HttpParams getParams() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ClientConnectionManager getConnectionManager() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public HttpResponse execute(HttpUriRequest request, HttpContext context)
				throws IOException, ClientProtocolException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public HttpResponse execute(HttpHost target, HttpRequest request)
				throws IOException, ClientProtocolException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public HttpResponse execute(HttpHost target, HttpRequest request,
				HttpContext context) throws IOException,
				ClientProtocolException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> T execute(HttpUriRequest request,
				ResponseHandler<? extends T> responseHandler)
				throws IOException, ClientProtocolException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> T execute(HttpUriRequest request,
				ResponseHandler<? extends T> responseHandler,
				HttpContext context) throws IOException,
				ClientProtocolException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> T execute(HttpHost target, HttpRequest request,
				ResponseHandler<? extends T> responseHandler)
				throws IOException, ClientProtocolException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> T execute(HttpHost target, HttpRequest request,
				ResponseHandler<? extends T> responseHandler,
				HttpContext context) throws IOException,
				ClientProtocolException {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
