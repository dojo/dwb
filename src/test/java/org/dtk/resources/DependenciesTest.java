package org.dtk.resources;

import static org.junit.Assert.* ;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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
import org.dtk.util.Options;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

/**
 * Test the RESTful Dependencies API. This resource can
 * analyse a variety of input sources (HTML file, web address, existing profile)
 * to discover Dojo module dependencies. The response will contain a list 
 * of discovered module dependencies and may include temporary package references
 * for any custom modules. 
 * 
 * @author James Thomas
 */

public class DependenciesTest {

	/**
	 * Service end point details.
	 */
	final protected String baseResourcePath = "/api/dependencies/";
	
	/** Sample web address with Dojo application */
	final protected String validWebAddress = "http://jamesthom.as/sample.html";
	
	/** Simple invalid web address */
	final protected String invalidWebAddress = "\\";
	
	/** Valid URL which doesn't resolve to a known host */
	final protected String nonExistentWebAddress = "http://some.unknown.url/";
	
	/** Sample web address with no Dojo modules */
	final protected String validWebAddressNoModules = "http://jamesthom.as/no_modules.html";
	
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
	public void test_AnalyseWebPage() throws ClientProtocolException, IOException, URISyntaxException {
		// Simulate multipart html FORM submission. 
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		// Add HTML file and type="web_page" fields.
		StringBody strBody = new StringBody("web_page");
		reqEntity.addPart("type", strBody);
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample.html");		
		InputStreamBody htmlPage = new InputStreamBody(is, "sample.html");
		reqEntity.addPart("value", htmlPage);
		
		// Simulate form submission, check HTTP status and parse JSON from HTML response.
		HttpResponse response = simulateMultiPartFormSubmission(reqEntity);
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		
		// JSON response is encoded within a HTML <textarea> element. Retrieve and 
		// convert to a map collection.
		Map<String, Object> jsonResponse = extractJsonFromHTMLEncodedResponse(response);
		
		// Compare lists of dojo.require() modules that were contained within the test
		// web page we submitted. 
		assertEquals(Arrays.asList("dijit.form.Button", "dijit.form.Form", "dojo.parser",
			"dijit.layout.BorderContainer", "dijit.layout.ContentPane", "dijit.layout.TabContainer", 
			"dijit.form.Select", "dijit.TitlePane", "dijit.form.CheckBox", "dojox.grid.EnhancedGrid", 
			"dojox.grid.enhanced.plugins.IndirectSelection", "dojo.data.ItemFileReadStore", 
			"dojo.data.ItemFileWriteStore", "dojox.data.AndOrWriteStore"),
			jsonResponse.get("requiredDojoModules"));
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
	public void test_AnalyseWebPageNoModules() throws ClientProtocolException, IOException, URISyntaxException {
		// Simulate multipart html FORM submission. 
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		// Add HTML file and type="web_page" fields.
		StringBody strBody = new StringBody("web_page");
		reqEntity.addPart("type", strBody);
		
		InputStreamBody htmlPage = new InputStreamBody(getClass().getClassLoader().getResourceAsStream("sampleNoModules.html"), 
			"sampleNoModules.html");
		reqEntity.addPart("value", htmlPage);
		
		// Simulate form submission, check HTTP status and parse JSON from HTML response.
		HttpResponse response = simulateMultiPartFormSubmission(reqEntity);
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		
		// JSON response is encoded within a HTML <textarea> element. Retrieve and 
		// convert to a map collection.
		Map<String, Object> jsonResponse = extractJsonFromHTMLEncodedResponse(response);
		
		// Compare lists of dojo.require() modules that were contained within the test
		// web page we submitted. Should be empty! 
		List<String> requiredDojoModules = (List<String>) jsonResponse.get("requiredDojoModules");
		assertTrue(requiredDojoModules.isEmpty());
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
		// Simulate multipart html FORM submission. 
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		// Add HTML file and type="web_page" fields with invalid HTML file.
		StringBody strBody = new StringBody("web_page");
		reqEntity.addPart("type", strBody);
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("sampleInvalidHTML.html");
		InputStreamBody htmlPage = new InputStreamBody(is, "sampleInvalidHTML.html");
		reqEntity.addPart("value", htmlPage);
		
		// Simulate form submission, check HTTP status returned is 200. Our service 
		// can handle badly formed HTML without blowing up...
		HttpResponse response = simulateMultiPartFormSubmission(reqEntity);
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		
		// JSON response is encoded within a HTML <textarea> element. Retrieve and 
		// convert to a map collection.
		Map<String, Object> jsonResponse = extractJsonFromHTMLEncodedResponse(response);
		
		// Compare lists of dojo.require() modules that were contained within the test
		// web page we submitted. Should be empty! 
		List<String> requiredDojoModules = (List<String>) jsonResponse.get("requiredDojoModules");
		assertTrue(requiredDojoModules.isEmpty());
	}
	
	/**
	 * Test dependency analysis for a remote web application. Simulate a 
	 * HTML form submission containing the URL. Verify resulting 
	 * response contains expected modules contained within app. 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_AnalyseWebApplication() throws ClientProtocolException, IOException, URISyntaxException {
		// Simulate multipart html FORM submission. 
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		// Add HTML file and type="web_page" fields.
		StringBody strBody = new StringBody("url");
		reqEntity.addPart("type", strBody);
		StringBody webApplicationAddress = new StringBody(validWebAddress);
		reqEntity.addPart("value", webApplicationAddress);
		
		// Simulate form submission, check HTTP status and parse JSON from HTML response.
		HttpResponse response = simulateMultiPartFormSubmission(reqEntity);
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		
		// JSON response is encoded within a HTML <textarea> element. Retrieve and 
		// convert to a map collection.
		Map<String, Object> jsonResponse = extractJsonFromHTMLEncodedResponse(response);
		
		// Compare lists of dojo.require() modules that were contained within the test
		// application we submitted. 
		assertEquals(Arrays.asList("dijit.form.FilteringSelect","web_builder.app","dojo.back",
			"web_builder.child","web_builder.util.util","dijit.form.Button","dijit.form.Form",
			"dojo.parser"),
			jsonResponse.get("requiredDojoModules"));
		
		// Response should also contain a single entry in the temporary packages object for 
		// the "web_builder" modules. This is the temporary package reference. 
		List<Map<String, String>> tempPackages = (List<Map<String, String>>) jsonResponse.get("packages");
		assertEquals(tempPackages.size(), 1);
		Map<String, String> tempPackage = tempPackages.get(0);		
		assertNotNull(tempPackage.get("name"));
		assertNotNull(tempPackage.get("version"));
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
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_AnalyseWebApplicationWithNoDependencies() throws ClientProtocolException, IOException, URISyntaxException {
		// Simulate multipart html FORM submission. 
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		// Add HTML file and type="web_page" fields using non existent web address.
		StringBody strBody = new StringBody("url");
		reqEntity.addPart("type", strBody);
		StringBody webApplicationAddress = new StringBody(validWebAddressNoModules);
		reqEntity.addPart("value", webApplicationAddress);
		
		// Simulate form submission, check HTTP status and parse JSON from HTML response.
		HttpResponse response = simulateMultiPartFormSubmission(reqEntity);
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		
		// JSON response is encoded within a HTML <textarea> element. Retrieve and 
		// convert to a map collection.
		Map<String, Object> jsonResponse = extractJsonFromHTMLEncodedResponse(response);
		
		// Compare lists of dojo.require() modules that were contained within the test
		// web page we submitted. Should be empty! 
		List<String> requiredDojoModules = (List<String>) jsonResponse.get("requiredDojoModules");
		assertTrue(requiredDojoModules.isEmpty());
	}
	
	/**
	 * Test dependency analysis for an existing build profile. Simulate a 
	 * HTML form submission containing the profile file. Verify resulting 
	 * response contains expected modules contained within page. 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_AnalyseBuildProfile() throws ClientProtocolException, IOException, URISyntaxException {
		// Simulate multipart html FORM submission. 
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		// Add HTML file and type="web_page" fields.
		StringBody strBody = new StringBody("profile");
		reqEntity.addPart("type", strBody);
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("baseplus.profile.js");
		InputStreamBody htmlPage = new InputStreamBody(is, "baseplus.profile.js");
		reqEntity.addPart("value", htmlPage);
		// Simulate form submission, check HTTP status and parse JSON from HTML response.
		HttpResponse response = simulateMultiPartFormSubmission(reqEntity);
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		
		// JSON response is encoded within a HTML <textarea> element. Retrieve and 
		// convert to a map collection.
		Map<String, Object> jsonObj = extractJsonFromHTMLEncodedResponse(response);
		
		// Should contain list of object layers from profile
		List<Object> layers = (List<Object>) jsonObj.get("layers");
		
		assertNotNull(layers);
		
		// Construct static instance of expected layers 
		List<Object> expectedLayers = new ArrayList<Object>() {{ 
			add(new HashMap() {{
				put("name", "dojo.js");
				put("dependencies", Arrays.asList("dijit._Widget","dijit._Templated","dojo.fx","dojo.NodeList-fx"));
			}});
			add(new HashMap() {{
				put("name", "custom.js");
				put("dependencies", Arrays.asList("dojo.back","dojo.cache","dojo.colors"));
			}});
			add(new HashMap() {{
				put("name", "another_custom.js");
				put("dependencies", Arrays.asList("dojo.back","dojo.cache","dojo.colors"));
			}});
		}};
		
		// Compare contents of layers
		assertEquals(expectedLayers, layers);
	}
	
	/**
	 * Test dependency analysis for a build profile which is invalid JavaScript. Simulate a 
	 * HTML form submission containing the profile file. Verify service responds with 
	 * a HTTP 400, bad request message. 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_AnalyseBuildProfileWithInvalidProfile() throws ClientProtocolException, IOException, URISyntaxException {
		// Simulate multipart html FORM submission. 
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		// Add HTML file and type="web_page" fields.
		StringBody strBody = new StringBody("profile");
		reqEntity.addPart("type", strBody);

		InputStream is = getClass().getClassLoader().getResourceAsStream("error.profile.js");
		InputStreamBody htmlPage = new InputStreamBody(is, "error.profile.js");
		
		reqEntity.addPart("value", htmlPage);
		
		// Simulate form submission, check HTTP status and parse JSON from HTML response.
		HttpResponse response = simulateMultiPartFormSubmission(reqEntity);
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
	}
	
	/**
	 * Test dependency analysis for an existing build profile, which happens to have
	 * no module dependencies. Simulate a HTML form submission containing the profile 
	 * file. Verify resulting response contains expected modules contained within page. 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_AnalyseBuildProfileWithEmptyProfile() throws ClientProtocolException, IOException, URISyntaxException {
		// Simulate multipart html FORM submission. 
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		// Add HTML file and type="web_page" fields.
		StringBody strBody = new StringBody("profile");
		reqEntity.addPart("type", strBody);
		InputStream is = getClass().getClassLoader().getResourceAsStream("empty.profile.js");
		InputStreamBody htmlPage = new InputStreamBody(is, "empty.profile.js");
		reqEntity.addPart("value", htmlPage);
		
		// Simulate form submission, check HTTP status and parse JSON from HTML response.
		HttpResponse response = simulateMultiPartFormSubmission(reqEntity);
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
	
		// JSON response is encoded within a HTML <textarea> element. Retrieve and 
		// convert to a map collection.
		Map<String, Object> jsonObj = extractJsonFromHTMLEncodedResponse(response);
		
		// Should contain list of object layers from profile
		List<Object> layers = (List<Object>) jsonObj.get("layers");;
		
		assertNotNull(layers);
		
		// The build profile was empty, check response confirms this.
		assertTrue(layers.isEmpty());
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
	protected HttpResponse simulateMultiPartFormSubmission(MultipartEntity multipartEntity) throws ClientProtocolException, IOException, URISyntaxException {
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
	protected Map<String, Object> extractJsonFromHTMLEncodedResponse(HttpResponse response) throws IllegalStateException, IOException {
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
	 
	protected Object extractJsonFromHTMLEncodedResponse(HttpResponse response, Class Klass) throws IllegalStateException, IOException {
		// Check for HTML encompassed JSON response. 
		String httpEncodedJson = IOUtils.toString(response.getEntity().getContent());
		
		// Extract JSON from textarea tag in returned HTML.
		Document doc = Jsoup.parse(httpEncodedJson);
		Elements textareas = doc.getElementsByTag("textarea");
		String jsonContent = textareas.get(0).text();
		
		// Convert JSON objet to generic map collection.
		ObjectMapper om = new ObjectMapper();	
		Object details = om.readValue(jsonContent, Klass);
		
		return details;
	}
}
