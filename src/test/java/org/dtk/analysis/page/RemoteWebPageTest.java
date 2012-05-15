package org.dtk.analysis.page;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.dtk.util.FileUtil;
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
		
	
	// Start filling out this method with local web page tests.....
	// Must support AMD and non-AMD 
	
	// Utility method
	private String getResourceAsString(String filePath) throws IOException {
		InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);		
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
		
		public Map<String, String> mockHttpResponses = new HashMap<String, String>(); 
		
		@Override
		public HttpResponse execute(HttpUriRequest request) throws IOException,
			ClientProtocolException {
			
			String url = request.getURI().toString();
			
			if (!url.startsWith(hostPrefix)) {
				throw new IOException("Trying to read non-local file");
			}
			
			url = appDir + url.substring(hostPrefix.length());
			String responseText = getResourceAsString(url);
			
			StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), HttpStatus.SC_OK, null);			
			HttpResponse response = new BasicHttpResponse(statusLine);
			response.setEntity(new StringEntity(responseText));
			
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
