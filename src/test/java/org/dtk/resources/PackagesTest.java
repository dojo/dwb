package org.dtk.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.dtk.util.Options;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

/**
 * Test the RESTful Packages API. This API provides access to the packages 
 * available to the build system, allowing a user to retrieve a list of all
 * packages present, retrieve details about specific packages (versions and 
 * modules) and create temporary packages from user applications.
 * 
 * @author James Thomas
 */

public class PackagesTest {

	/**
	 * Service end point path.
	 */	
	final protected String packageAPIPath = "/api/packages/";

	/**
	 * Test normal path through the code. Retrieve all packages
	 * available and for each one, validate retrieval of package
	 * versions and module details.
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_RetrievePackageList() throws URISyntaxException, ClientProtocolException, IOException {
		HttpResponse response = generateDefaultPackageRequest();
		
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		
		// Convert resposne from JSON object to generic map structure. 
		ObjectMapper om = new ObjectMapper();	
		Map<String, List> packageDetails = om.readValue(response.getEntity().getContent(), Map.class);
		
		// Validate required build parameters are returned, each list should
		// be non-empty and conform the given format. 
		validateNonEmptyParameterList(packageDetails.get("optimise"));
		validateNonEmptyParameterList(packageDetails.get("cdn"));
		
		// Retrieve list of available packages. For each package,
		// retrieve package versions and modules. 
		List<Map<String,String>> packagesList = packageDetails.get("packages");
		assertNotNull(packagesList);
		assertTrue(packagesList.size() > 0);
		
		// Each package should contain a name and link to the versions resource
		// for that package.
		Iterator<Map<String,String>> iter = packagesList.iterator();
		while(iter.hasNext()) {
			Map<String, String> packageInfo = iter.next();
			String packageName = packageInfo.get("name"), versionsLink = packageInfo.get("link");
			assertNotNull(packageName);
			assertNotNull(versionsLink);
			validatePackageVersions(versionsLink);
		}
	}
	
	/**
	 * Retrieve version information for given package. Verify response 
	 * corresponds to expected format. For each version, check expected
	 * response is returned. 
	 * 
	 * @param packageVersionsLink - URL corresponding to a package's version resource.
	 * @throws ClientProtocolException
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public void validatePackageVersions(String packageVersionsLink) throws ClientProtocolException, URISyntaxException, IOException {
		HttpResponse response = jsonGetRequest(new URL(packageVersionsLink));
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		
		ObjectMapper om = new ObjectMapper();	
		List<Map<String, String>> packageVersions = om.readValue(response.getEntity().getContent(), List.class);
		
		assertNotNull(packageVersions);
		assertTrue(packageVersions.size() > 0);
		
		Iterator<Map<String, String>> iter = packageVersions.iterator();
		
		while(iter.hasNext()) {
			Map<String, String> packageVersion = iter.next();
			String versionLabel = packageVersion.get("name"), versionLink = packageVersion.get("link");
			assertNotNull(versionLabel);
			assertNotNull(versionLink);
			validatePackageVersionDetails(versionLink);
		}
	}
	
	/**
	 * Retrieve details for a package version, using passed link, and 
	 * verify that response contains name, description and modules values.
	 * The "modules" value should be a collection of lists with two string 
	 * elements, corresponding to module name and description. 
	 * 
	 * @param packageVersionLink - URL to retrieve package version details from.
	 * @throws ClientProtocolException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public void validatePackageVersionDetails(String packageVersionLink) throws ClientProtocolException, MalformedURLException, URISyntaxException, IOException {
		HttpResponse response = jsonGetRequest(new URL(packageVersionLink));
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		
		// Convert JSON objet to generic map collection.
		ObjectMapper om = new ObjectMapper();	
		Map<String, Object> details = om.readValue(response.getEntity().getContent(), Map.class);
		
		// Package details must contain a full name and description.
		assertNotNull(details.get("description"));
		assertNotNull(details.get("name"));
		
		// Retrieve and verify each module is an array with two elements (name and desc).
		List<List<String>> packageModules = (List<List<String>>) details.get("modules");
		assertNotNull(packageModules);
		assertTrue(packageModules.size() > 0);
		
		Iterator<List<String>> iter = packageModules.iterator();
		while(iter.hasNext()) {
			List<String> module = iter.next();
			assertEquals(2, module.size());
		}
	}
	
	/**
	 * Retrieve an invalid package version. API should
	 * respond with a standard 404. Generate random
	 * package name to access. 
	 * 
	 * @throws IOException 
	 * @throws URISyntaxException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_RetrieveVersionsForInvalidPackage() throws ClientProtocolException, URISyntaxException, IOException {
		// Generate random package name to retrieve. 
		String randomPackageName = RandomStringUtils.randomAlphabetic(32);
		URL invalidPackageResource = new URL(Options.getTestAPILocation(packageAPIPath) + randomPackageName);
		HttpResponse response = jsonGetRequest(invalidPackageResource);
		assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());	
	}
	
	/**
	 * Test the retrieval of package details for a valid package but 
	 * with an invalid version. Use the first package returned from 
	 * the global resource list with a pseudo-random version identifier. 
	 * Service should response with a HTTP 404. 
	 * 
	 * @throws ClientProtocolException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	@Test
	public void test_RetrieveDetailsForValidPackageAndInvalidVersion() throws ClientProtocolException, MalformedURLException, URISyntaxException, IOException {
		// Retrieve actual list of packages and pick first one found.
		HttpResponse response = generateDefaultPackageRequest();
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		
		// Convert response from JSON object to generic map structure. 
		ObjectMapper om = new ObjectMapper();	
		Map<String, List> packageDetails = om.readValue(response.getEntity().getContent(), Map.class);
		
		// Retrieve list of available packages and just pick first one.
		String packageVersionsLink = ((Map<String, String>) packageDetails.get("packages").get(0)).get("link");
		
		// Generate random package version to retrieve. 
		String randomPackageName = RandomStringUtils.randomAlphabetic(32);
		URL invalidPackageResource = new URL(packageVersionsLink + "/" + randomPackageName);
		response = jsonGetRequest(invalidPackageResource);
		
		// API should return a standard HTTP 404 code. 
		assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());	
	}
	
	/**
	 * Test retrieval of package details for an invalid package. Service should
	 * respond with a standard HTTP 404.
	 * 
	 * @throws ClientProtocolException
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	@Test
	public void test_RetrieveInvalidPackageAndInvalidVersionDetails() throws ClientProtocolException, URISyntaxException, IOException {
		// Generate random package name to retrieve. 
		String randomChars = RandomStringUtils.randomAlphabetic(32);
		URL invalidPackageResource = new URL(Options.getTestAPILocation(packageAPIPath) + randomChars + "/" + randomChars);
		HttpResponse response = jsonGetRequest(invalidPackageResource);
		assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());	
	}
	
	/**
	 * Test the creation of a new package, from a compressed user application. 
	 * Create the POST request, containing multipart form fields, and check response 
	 * contains a package identifier for the temporary package along with module dependencies.
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test
	public void test_CreateNewPackage() throws URISyntaxException, ClientProtocolException, IOException {
		// Create HTTP Post request to base package URL
		URL url = new URL(Options.getTestAPILocation(packageAPIPath));
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(url.toURI());
		
		// We want to upload our sample application, containing custom module
		// definitions and DTK dependencies.
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("user_app.zip");		
		InputStreamBody bin = new InputStreamBody(is, "application/zip", "user_app.zip");
		reqEntity.addPart("user_app", bin);
		
		httpPost.setEntity(reqEntity);
		HttpResponse response = httpclient.execute(httpPost);
		
		// HTTP 201, resource was created. 
		assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
		
		// Check for HTML encompassed JSON response. 
		String httpEncodedJson = IOUtils.toString(response.getEntity().getContent());
		
		// Extract JSON from textarea tag in returned HTML.
		Document doc = Jsoup.parse(httpEncodedJson);
		Elements textareas = doc.getElementsByTag("textarea");
		String jsonContent = textareas.get(0).text();
		
		// Convert JSON objet to generic map collection.
		ObjectMapper om = new ObjectMapper();	
		Map<String, Object> details = om.readValue(jsonContent, Map.class);
		
		// Extract response values for standard Dojo modules within user application,
		// user defined custom modules and reference id for new package. Confirm they 
		// match our expected values.
		List<String> dojoModules = (List<String>) details.get("requiredDojoModules");
		Collections.sort(dojoModules);
		assertEquals(Arrays.asList("dijit.form.Button", "dojo.cache", "dojox.form.BusyButton"), 
			dojoModules);
		
		// Make sure list elements are in the same order
		List<String> availableModules = (List<String>) details.get("availableModules");
		Collections.sort(availableModules);
		
		assertEquals(Arrays.asList("user_app.module_a", "user_app.module_b", "user_app.module_c", "user_app.util.util"), 
			availableModules);
		
		List<Map<String, String>> packages = (List<Map<String, String>>) details.get("packages");
		
		assertEquals(packages.size(), 1);
		Map<String, String> tempPackage = packages.get(0);		
		assertNotNull(tempPackage.get("name"));
		assertNotNull(tempPackage.get("version"));
	}
	
	/**
	 * Test a new package creation request with the missing body 
	 * parameter containing the actual user application zip. Service
	 * should response with a status code 400.  
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * 
	 */
	@Test
	public void test_CreateNewPackageMissingResource() throws URISyntaxException, ClientProtocolException, IOException {
		// Create HTTP Post request to base package URL
		URL url = new URL(Options.getTestAPILocation(packageAPIPath));
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(url.toURI());
		
		// We want to upload our sample application, containing custom module
		// definitions and DTK dependencies.
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		
		httpPost.setEntity(reqEntity);
		HttpResponse response = httpclient.execute(httpPost);
		
		// HTTP 400, invalid request. 
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
	}
	
	/**
	 * Validate a parameter list. The parameter list should be non-empty
	 * and contain map collections, which contain non-null values for the
	 * keys "value" and "label". 
	 * 
	 * @param parameterList - Collection of parameters object, containing 
	 * value and label atrributes. 
	 */
	protected void validateNonEmptyParameterList (List<Map<String, String>> parameterList) {
		// Check list has at least one member. 
		assertNotNull(parameterList);
		assertTrue(parameterList.size() > 0);
		// Check all members for non-null value and label attributes 
		Iterator<Map<String, String>> iter = parameterList.iterator();
		
		while(iter.hasNext()) {
			Map<String, String> parameter = iter.next();
			assertNotNull(parameter.get("value"));
			assertNotNull(parameter.get("label"));	
		}
	}
	
	/**
	 * Generate the default package request for all package and 
	 * compiler details.
	 * 
	 * @return HTTP response for request
	 * @throws ClientProtocolException
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	protected HttpResponse generateDefaultPackageRequest() throws ClientProtocolException, MalformedURLException, URISyntaxException, IOException {
		return jsonGetRequest(new URL(Options.getTestAPILocation(packageAPIPath)));
	}
	
	/**
	 * Generate a HTTP GET request for JSON content to the 
	 * specified URL.
	 * 
	 * @param resourceLocation - URL for request.
	 * @return HTTP response for request
	 * @throws URISyntaxException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	protected HttpResponse jsonGetRequest(URL resourceLocation) throws URISyntaxException, ClientProtocolException, IOException {
		// Set up HTTP client connection....
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(resourceLocation.toURI());
		
		// Content type headers
		httpGet.setHeader("Content-Type", "application/json");
		httpGet.setHeader("Accept", "application/json");
		
		return httpClient.execute(httpGet);
	}
}
