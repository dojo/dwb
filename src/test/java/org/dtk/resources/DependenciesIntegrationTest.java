package org.dtk.resources;

import static org.junit.Assert.* ;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.dtk.resources.dependencies.DependenciesResponse;
import org.dtk.util.FileServer;
import org.dtk.util.Options;

/**
 * Test the RESTful Dependencies API. This resource can
 * analyse a variety of input sources (HTML file, web address, existing profile)
 * to discover Dojo module dependencies. The response will contain a list 
 * of discovered module dependencies and may include temporary package references
 * for any custom modules. 
 * 
 * @author James Thomas
 */

public class DependenciesIntegrationTest {

	/**
	 * Service end point details.
	 */
	static final protected String baseResourcePath = "/api/dependencies/";
	
	/** Sample web address with Dojo application */
	final protected String validWebAddress = "http://localhost:9080/index.html";
	
	final protected int testPort = 9080;
	
	/** Simple invalid web address */
	final protected String invalidWebAddress = "\\";
	
	/** Valid URL which doesn't resolve to a known host */
	final protected String nonExistentWebAddress = "http://some.unknown.url/";
	
	/**
	 * Test dependency analysis for a blank form submission. Verify HTTP
	 * error code 400 is returned. 
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_AnalyseBlankSubmission() throws URISyntaxException, ClientProtocolException, IOException {
		// Simulate multipart html FORM submission with empty form.
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		HttpResponse response = simulateMultiPartFormSubmission(reqEntity);
		
		// HTTP 400, invalid client request. 
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
	}
	
	/**
	 * Test dependency analysis for a form submission without value parameter. 
	 * Verify HTTP error code 400 is returned. 
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_AnalyseMissingValueSubmission() throws URISyntaxException, ClientProtocolException, IOException {
		// Simulate multipart html FORM submission. 
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		// Add type parameter but not value.
		StringBody strBody = new StringBody("web_page");
		reqEntity.addPart("type", strBody);
		
		HttpResponse response = simulateMultiPartFormSubmission(reqEntity);
		
		// HTTP 400, invalid client request. 
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
	}
	
	/**
	 * Test dependency analysis for a form submission without type parameter. 
	 * Verify HTTP error code 400 is returned. 
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_AnalyseMissingTypeSubmission() throws URISyntaxException, ClientProtocolException, IOException {
		// Simulate multipart html FORM submission. 
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		// Add value parameter but not type.
		StringBody strBody = new StringBody("Some content goes here!");
		reqEntity.addPart("value", strBody);

		HttpResponse response = simulateMultiPartFormSubmission(reqEntity);
		
		// HTTP 400, invalid client request. 
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
	}

	/**
	 * Test dependency analysis for a static HTML file. Simulate a 
	 * HTML form submission containing the HTML file. Verify resulting 
	 * response contains expected modules contained within page. 
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_AnalyseNonAmdWebPage() throws ClientProtocolException, IOException, URISyntaxException {
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample_pages/non_amd/local.html");				
		HttpResponse response = generateHTMLFormPost("web_page", IOUtils.toString(is));	
		
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());		
		
		Map<String, Object> jsonResponse = extractJsonFromHTMLEncodedResponse(response);		
		compareUnsortedModuleLists(Arrays.asList("dijit.form.Button", "dijit.form.Form", "dojo.parser",
			"dijit.layout.ContentPane", "dijit.layout.TabContainer", "dijit.form.Select", "dojox.grid.EnhancedGrid", 
			"dojox.grid.enhanced.plugins.IndirectSelection", "dojo.data.ItemFileReadStore", "dojox.data.AndOrWriteStore"),
			(List<String>) jsonResponse.get("requiredDojoModules"));		
	}

	/**
	 * Test dependency analysis for a static HTML file. Simulate a 
	 * HTML form submission containing the HTML file. Verify resulting 
	 * response contains expected modules contained within page. 
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_AnalyseAmdWebPage() throws ClientProtocolException, IOException, URISyntaxException {		
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample_pages/amd/local_with_require.html");				
		HttpResponse response = generateHTMLFormPost("web_page", IOUtils.toString(is));	
		
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());		
		
		Map<String, Object> jsonResponse = extractJsonFromHTMLEncodedResponse(response);		
		compareUnsortedModuleLists(Arrays.asList("dijit.form.Button", "dijit.form.Form", "dojo.parser",
			"dijit.layout.ContentPane", "dijit.layout.TabContainer", "dijit.form.Select", "dojox.grid.EnhancedGrid", 
			"dojox.grid.enhanced.plugins.IndirectSelection", "dojo.data.ItemFileReadStore", "dojox.data.AndOrWriteStore"),
			(List<String>) jsonResponse.get("requiredDojoModules"));
	}
	
	/**
	 * Test dependency analysis for a static HTML file. Simulate a 
	 * HTML form submission containing the HTML file. Use HTML file 
	 * with no dojo modules. Verify resulting response contains zero
	 * modules and doesn't return a HTTP error. 
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_AnalyseWebPageWithNoModules() throws ClientProtocolException, IOException, URISyntaxException {
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample_pages/amd/local_with_no_deps/index.html");				
		HttpResponse response = generateHTMLFormPost("web_page", IOUtils.toString(is));	
		
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());		
		
		Map<String, Object> jsonResponse = extractJsonFromHTMLEncodedResponse(response);	

		assertEquals(Collections.EMPTY_LIST, (List<String>) jsonResponse.get("requiredDojoModules"));
		assertEquals(Collections.EMPTY_LIST, (List<String>) jsonResponse.get("availableModules"));		
	}
	
	/**
	 * Test dependency analysis for a static HTML file when using an invalid file.
	 * Simulate a HTML form submission containing a binary file. Verify correct
	 * HTTP error code is returned. 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_AnalyseWebPageInvalidHTML() throws ClientProtocolException, IOException, URISyntaxException {
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample_pages/non_amd/invalid_html/index.html");				
		HttpResponse response = generateHTMLFormPost("web_page", IOUtils.toString(is));	
		
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());		
		
		Map<String, Object> jsonResponse = extractJsonFromHTMLEncodedResponse(response);	

		assertEquals(Collections.EMPTY_LIST, (List<String>) jsonResponse.get("requiredDojoModules"));
		assertEquals(Collections.EMPTY_LIST, (List<String>) jsonResponse.get("availableModules"));	
	}
	
	/**
	 * Test dependency analysis for a remote web application (using old dojo module style). Simulate a 
	 * HTML form submission containing the URL. Verify resulting response contains expected modules 
	 * contained within app.
	 * @throws Exception 
	 */
	@Test
	public void test_AnalyseNonAmdWebApplication() throws Exception {
		testAnalyseWebApplicationFromDirectory("sample_apps/non_amd/local_dtk_with_custom_modules_paths");
	}
	
	/**
	 * Test dependency analysis for a remote web application (using old dojo module style). Simulate a 
	 * HTML form submission containing the URL. Verify resulting response contains expected modules 
	 * contained within app.
	 * @throws Exception 
	 */
	@Test
	public void test_AnalyseAmdWebApplication() throws Exception {
		testAnalyseWebApplicationFromDirectory("sample_apps/amd/local_dtk_with_custom_modules_paths");				
	}
	
	public void testAnalyseWebApplicationFromDirectory(String directoryPath) throws Exception {
		FileServer fs = FileServer.spawn(testPort, directoryPath);		
		
		HttpResponse response = generateHTMLFormPost("url", validWebAddress);		
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());	
		verifyWebApplicationDependencies(response);
		
		fs.stop();
	}	
	
	/**
	 * Test dependency analysis for a remote web application but 
	 * using an invalid URL format. Simulate a HTML form submission 
	 * containing the URL. Verify service responds with a HTTP 400
	 * status message.
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_AnalyseWebApplicationWithInvalidURLFormat() 
		throws ClientProtocolException, IOException, URISyntaxException {	
		// Simulate multipart html FORM submission. 
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		// Add HTML file and type="web_page" fields using invalid web address
		StringBody strBody = new StringBody("url");
		reqEntity.addPart("type", strBody);
		StringBody webApplicationAddress = new StringBody(invalidWebAddress);
		reqEntity.addPart("value", webApplicationAddress);
		
		// Simulate form submission, check HTTP status and parse JSON from HTML response.
		HttpResponse response = simulateMultiPartFormSubmission(reqEntity);
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
	}
	
	/**
	 * Test dependency analysis for a remote web application but 
	 * using a valid URL that doesn't exist. Simulate a HTML form submission 
	 * containing the URL. Verify service responds with a HTTP 400
	 * status message.
	 */
	@Test
	public void test_AnalyseWebApplicationWithNonExistantURL()
	 throws ClientProtocolException, IOException, URISyntaxException {
		// Simulate multipart html FORM submission. 
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		// Add HTML file and type="web_page" fields using non existent web address.
		StringBody strBody = new StringBody("url");
		reqEntity.addPart("type", strBody);
		StringBody webApplicationAddress = new StringBody(nonExistentWebAddress);
		reqEntity.addPart("value", webApplicationAddress);
		
		// Simulate form submission, check HTTP status and parse JSON from HTML response.
		HttpResponse response = simulateMultiPartFormSubmission(reqEntity);
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
	}
	
	/**
	 * Test dependency analysis for a remote web application with 
	 * valid URL that contains no modules. Simulate a HTML form submission 
	 * containing the URL. Verify service responds with a HTTP 200 response
	 * and response body contains an empty module list.
	 * @throws Exception 
	 */
	@Test
	public void test_AnalyseWebApplicationWithNoDependencies() throws Exception {
		FileServer fs = FileServer.spawn(testPort, "sample_pages/amd/local_with_no_deps");		
		
		HttpResponse response = generateHTMLFormPost("url", validWebAddress);		
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());	
		
		Map<String, Object> jsonResponse = extractJsonFromHTMLEncodedResponse(response);
		
		assertEquals(Collections.EMPTY_LIST, (List<String>) jsonResponse.get("requiredDojoModules"));
		assertEquals(Collections.EMPTY_LIST, (List<String>) jsonResponse.get("availableModules"));
		
		fs.stop();
	}
	

	protected static HttpResponse generateHTMLFormPost(String key, String value) 
	throws ClientProtocolException, IOException, URISyntaxException {
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

		reqEntity.addPart("type", new StringBody(key));
		reqEntity.addPart("value", new StringBody(value));
		
		return simulateMultiPartFormSubmission(reqEntity);		
	}
	
	protected static void verifyWebApplicationDependencies(HttpResponse response) throws IllegalStateException, IOException {
		Map<String, Object> jsonResponse = extractJsonFromHTMLEncodedResponse(response);
		
		compareUnsortedModuleLists((List<String>) jsonResponse.get("requiredDojoModules"), 
			Arrays.asList("dojox.grid.EnhancedGrid", "dijit.form.Button", "dojo.parser"));		
		compareUnsortedModuleLists((List<String>) jsonResponse.get("availableModules"), 
			Arrays.asList("sample.app", "custom.custom", "sample.dep_one", "sample.dep_two", "sample.dep_three"));
		assertTemporaryPackagesNotNull((List<Map<String, String>>) jsonResponse.get("packages"));		
	}
	
	protected static void assertTemporaryPackagesNotNull(List<Map<String, String>> tempPackages) {
		assertEquals(tempPackages.size(), 1);
		Map<String, String> tempPackage = tempPackages.get(0);		
		assertNotNull(tempPackage.get("name"));
		assertNotNull(tempPackage.get("version"));		
	}	
	
	protected static void compareUnsortedModuleLists(List<String> expected, List<String> actual) {
		Collections.sort(expected);
		Collections.sort(actual);		
		assertEquals(expected, actual);		
	}
	
	/**
	 * Simulate HTML <form> submission, returning HTTP response.
	 * 
	 * @param multipartEntity - Form submission parts.
	 * @return HTTP form post response
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	static protected HttpResponse simulateMultiPartFormSubmission(MultipartEntity multipartEntity) throws ClientProtocolException, IOException, URISyntaxException {
		// Create HTTP Post request to base package URL
		URL url = new URL(Options.getTestAPILocation(baseResourcePath));
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(url.toURI());
		
		// Set configured form entity on the HTTP POST.
		httpPost.setEntity(multipartEntity);
		
		return httpclient.execute(httpPost);
	}
	
	/**
	 * Given a HTTP response with HTML content, extract first <textarea>
	 * tag, assuming content is JSON. Convert JSON string to generic map
	 * collection
	 * 
	 * @param response - HTTP response for form submission.
	 * @return Converted JSON response.
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	static protected Map<String, Object> extractJsonFromHTMLEncodedResponse(HttpResponse response) throws IllegalStateException, IOException {
		// Check for HTML encompassed JSON response. 
		String httpEncodedJson = IOUtils.toString(response.getEntity().getContent());
		
		// Extract JSON from textarea tag in returned HTML.
		Document doc = Jsoup.parse(httpEncodedJson);
		Elements textareas = doc.getElementsByTag("textarea");
		String jsonContent = textareas.get(0).text();
		
		// Convert JSON objet to generic map collection.
		ObjectMapper om = new ObjectMapper();	
		Map<String, Object> details = om.readValue(jsonContent, Map.class);
		
		return details;
	}	
}
