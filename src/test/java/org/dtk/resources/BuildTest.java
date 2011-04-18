package org.dtk.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.dtk.resources.build.manager.BuildState;
import org.dtk.util.Options;
import org.dtk.util.JsonUtil;
import org.junit.Test;

import static org.junit.Assert.* ;

/**
 * Test the RESTful Build API. This API provides access to the underlying 
 * Dojo build system, allowing a user to submit build jobs, check the status 
 * of a build request and access the result.  
 * 
 * Tests use a series of valid build request, containing DTK modules, non-DTK modules
 * custom layers and more, alongside invalid build requests to ensure we are receiving
 * correct HTTP responses (4xx vs 5xx). 
 * 
 * @author James Thomas
 */

public class BuildTest {

	/**
	 * API end point details.
	 */
	final protected String baseResourcePath = "/api/build/";

	final protected String statusResourcePath = "/api/build/status/";

	/**
	 * Default build profile for base dojo.js with no added modules.
	 */
	final protected String defaultBuildProfile = "default_build_request.json";
	
	/**
	 * Default build profile for base dojo.js with no added modules.
	 */
	final protected String dtkModulesBuildProfile = "dtk_modules_build_request.json";
	
	/**
	 * Build profile for base dojo.js with additional modules from Dijit.
	 */
	final protected String dijitModulesBuildProfile = "dijit_modules_build_request.json";
	
	/**
	 * Dojo base build with additional modules from Dojo Web Builder package.
	 */
	final protected String dwbModulesBuildProfile = "dwb_modules_build_request.json";
	
	/**
	 * Dojo base build with additional build layer containing standard DTK modules.
	 */
	final protected String customLayerBuildProfile = "custom_layer_build_request.json";
	
	/**
	 * Invalid build request, missing mandatory dojo package.
	 */
	final protected String missingDojoPackageBuildProfile = "missing_dojo_build_request.json";
	
	/**
	 * Invalid build request, missing dojo package version.
	 */
	final protected String missingDojoPackageVersionBuildProfile = "missing_dojo_version_build_request.json";
	
	/**
	 * Invalid build request, missing package name. 
	 */
	final protected String missingPackageNameBuildProfile = "missing_package_name_request.json";
	
	/**
	 * Invalid build request, incorrect version for valid package
	 */
	final protected String invalidVersionForValidPackageBuildProfile = "invalid_version_for_valid_package_build_request.json";
	
	/**
	 * Invalid build request, completely missing packages parameter
	 */
	final protected String missingPackageReferenceBuildProfile = "missing_package_reference_build_request.json";
	
	/**
	 * Default build profile for base dojo.js with no added modules.
	 */
	final protected String emptyBuildProfile = "{}";
	
	/**
	 * Test typical case using valid build parameters. 
	 * We are expecting returned response to have a status
	 * reference URL, which successfully resolves after a 
	 * period. The HTTP response should be a HTTP 202. 
	 */
	@Test 
	public void test_ValidDojoBaseBuildRequest() throws URISyntaxException, ClientProtocolException, IOException, InterruptedException  {
		startAndCompleteDojoBuild(defaultBuildProfile);
	}

	/**
	 * Test typical case where build parameters have 
	 * previously been submitted and a cached version 
	 * of the same build is available. The build status
	 * link should always return "completed" for the build 
	 * state without any polling.
	 */
	@Test 
	public void test_ValidDojoBaseCachedBuildRequest() throws URISyntaxException, ClientProtocolException, IOException {
		// Generate default build request
		HttpResponse response = generateBuildRequest((getClass().getClassLoader().getResourceAsStream(defaultBuildProfile)));
		String link = extractBuildStatusPollingLink(response);
		String buildResultLocation = verifyCompletedBuildStatus(link);
		verifyBuildResult(buildResultLocation, new HashSet<String>() {{
			add("dojo.js");
		}});
	}
	
	/**
	 * Test build example using dojo base plus some additional
	 * modules from the Dojo Toolkit package only. 
	 * We are expecting returned response to have a status
	 * reference URL, which successfully resolves after a 
	 * period. The HTTP response should be a HTTP 202. 
	 */
	@Test 
	public void test_ValidDTKModulesBuildRequest() 
	throws URISyntaxException, ClientProtocolException, IOException, InterruptedException  {
		startAndCompleteDojoBuild(dtkModulesBuildProfile);
	}
	
	/**
	 * Test build example using modules from the dojo web builder's source. This will
	 * test the ability to generate custom builds using non-DTK modules. Should just
	 * contain a single "dojo.js" file in the compressed build zip result. 
	 */
	@Test
	public void test_ValidNewBuildRequestUsingModulesFromNonDojoPackages() 
	throws ClientProtocolException, IOException, URISyntaxException, InterruptedException {
		startAndCompleteDojoBuild(dtkModulesBuildProfile);
	}
	
	public void test_ValidNewBuildRequestUsingTemporaryPackage() {
		// TODO WHEN TEMPORARY PACKGES API IS WORKING
	}
	
	/**
	 * Test build example with standard DTK modules split into multiple build layers. 
	 * Build should complete successfully and return a zip archive with two non-empty 
	 * layers files in, "dojo.js" & "custom.js". 
	 */
	@Test
	public void test_ValidBuildRequestWithMultipleLayers() 
	throws ClientProtocolException, IOException, URISyntaxException, InterruptedException {
		Set<String> filesInCustomLayerBuild = new HashSet<String>() {{
			add("dojo.js");
			add("custom.js");
		}};
		
		startAndCompleteDojoBuild(customLayerBuildProfile, filesInCustomLayerBuild);
	}
	
	/**
	 * Test build example with modules from dijit package, which contain localisation
	 * bundle reference. Builder should automatically detect and bundle appropriate 
	 * resources files in the resulting archive. When build has completed successfully,
	 * verify resulting zip file contains base "dojo.js" layer along with NLS resource files. 
	 */
	@Test
	public void test_BuildWithDijitModulesMustContainsNLSResources() 
	throws ClientProtocolException, IOException, URISyntaxException, InterruptedException {
		Set<String> filesInCustomLayerBuild = new HashSet<String>() {{
			add("dojo.js");
			// Check for sample NLS files
			add("nls/dojo_ROOT.js");
			add("nls/dojo_en-gb.js");
			add("nls/dojo_en.js");
		}};
		
		startAndCompleteDojoBuild(dijitModulesBuildProfile, filesInCustomLayerBuild);
	}
	
	// All the following build requests are invalid and should fail. 
	
	/**
	 * Invalid build test, missing reference to mandatory Dojo package used for
	 * building the base "dojo.js" layer.   
	 * 
	 * Should return a HTTP 400 to indicate that user has submitted an invalid
	 * build request.
	 */
	@Test
	public void test_MissingDojoPackageReferenceBuild () 
	throws ClientProtocolException, IOException, URISyntaxException {
		HttpResponse response = generateBuildRequest(
			getClass().getClassLoader().getResourceAsStream(missingDojoPackageBuildProfile));
		
		// Service must return a 400 for invalid build requests.
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode()); 
	}
	
	/**
	 * Invalid build test, missing package version for the mandatory Dojo package used for
	 * building the base "dojo.js" layer.   
	 * 
	 * Should return a HTTP 400 to indicate that user has submitted an invalid
	 * build request.
	 */
	@Test
	public void test_MissingDojoPackageReferenceVersionBuild ()
	throws ClientProtocolException, IOException, URISyntaxException {
		HttpResponse response = generateBuildRequest(
			getClass().getClassLoader().getResourceAsStream(missingDojoPackageVersionBuildProfile));

		// Service must return a 400 for invalid build requests.
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode()); 
	}
	
	/**
	 * Invalid build test, package reference missing mandatory name parameter.    
	 * 
	 * Should return a HTTP 400 to indicate that user has submitted an invalid
	 * build request.
	 */
	@Test
	public void test_MissingNameInPackageReferenceBuild() 
	throws ClientProtocolException, IOException, URISyntaxException {
		HttpResponse response = generateBuildRequest(
			getClass().getClassLoader().getResourceAsStream(missingPackageNameBuildProfile));

		// Service must return a 400 for invalid build requests.
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode()); 
	}
	
	/**
	 * Invalid build test, package reference contains an invalid version for the valid
	 * package reference, "dojo".  
	 * 
	 * Should return a HTTP 400 to indicate that user has submitted an invalid
	 * build request.
	 */
	@Test
	public void test_InvalidVersionForValidPackageReferenceBuild() 
	throws ClientProtocolException, IOException, URISyntaxException {
		HttpResponse response = generateBuildRequest(
			getClass().getClassLoader().getResourceAsStream(invalidVersionForValidPackageBuildProfile));

		// Service must return a 400 for invalid build requests.
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode()); 
	}
	
	/**
	 * Invalid build test, completely missing mandatory build parameter, "packages".    
	 * 
	 * Should return a HTTP 400 to indicate that user has submitted an invalid
	 * build request.
	 */
	@Test
	public void test_MissingPackageReferenceForModuleBuild() 
	throws ClientProtocolException, IOException, URISyntaxException {
		HttpResponse response = generateBuildRequest(
			getClass().getClassLoader().getResourceAsStream(missingPackageReferenceBuildProfile));

		// Service must return a 400 for invalid build requests.
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode()); 		
	}
	
	/**
	 * Test submission of invalid build request. Missing 
	 * vital submission parameters. We should have a 
	 * correct HTTP error response, 400, returned.
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test 
	public void test_MalformedBuildRequest() throws ClientProtocolException, IOException, URISyntaxException {
		// Generate build request using empty profile (invalid).
		HttpResponse response = generateBuildRequest(emptyBuildProfile);

		// Service must return a 400 for invalid build requests.
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode()); 
	}

	/**
	 * Test retrieval of the status for an invalid build request
	 * identifier. We should have HTTP error code 404 returned. 
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test 
	public void test_RetrieveInvalidBuildStatus() throws URISyntaxException, ClientProtocolException, IOException {
		// Use a fixed short string as the invalid resource id
		String invalidBuildURL = statusResourcePath + "invalid_resource_id";
		// Set up HTTP client connection....
		URL serviceEndPoint = new URL(Options.getTestAPILocation(invalidBuildURL));
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(serviceEndPoint.toURI());

		// Content type headers
		httpGet.setHeader("Content-Type", "application/json");
		httpGet.setHeader("Accept", "application/json");

		HttpResponse response = httpClient.execute(httpGet);
		// Invalid status request should come back with a HTTP 404. 
		assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
	}

	/**
	 * Test the retrieval of an invalid/missing build 
	 * resource. We should have a HTTP error code 404 returned. 
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@Test 
	public void test_AccessInvalidBuildResult() throws URISyntaxException, ClientProtocolException, IOException {
		// Use a fixed short string as the invalid resource id
		String invalidBuildURL = baseResourcePath + RandomStringUtils.randomAlphabetic(32);
		// Set up HTTP client connection....
		URL serviceEndPoint = new URL(Options.getTestAPILocation(invalidBuildURL));
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(serviceEndPoint.toURI());

		// Content type headers
		httpGet.setHeader("Accept", "application/zip");

		HttpResponse response = httpClient.execute(httpGet);
		// Invalid status request should come back with a HTTP 404. 
		assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
	}

	/**
	 * Validate response for a new build request, extracting 
	 * status polling URL and returing. 
	 * 
	 * @param response - Http response from build request API.
	 * @return String containing URL to poll the build status
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	protected String extractBuildStatusPollingLink(HttpResponse response) throws JsonParseException, JsonMappingException, IllegalStateException, IOException {
		// Service must return a 202 while build is completing with URL for status 
		// update resource. 
		assertTrue(HttpStatus.SC_ACCEPTED == response.getStatusLine().getStatusCode()); 
		HttpEntity responseEntity = response.getEntity();
		assertNotNull(responseEntity);

		// Parse JSON response and retrieve URL to poll for build status.
		ObjectMapper om = new ObjectMapper();	
		Map<String, String> stateDetails = om.readValue(responseEntity.getContent(), Map.class);

		// Should have a state value and a URL to follow;
		String link = stateDetails.get("buildStatusLink");
		assertNotNull(link);

		return link;
	}

	/**
	 * Poll the given URL waiting for the build status to complete. 
	 * When this happens, return result URL. Enforce a total polling
	 * limit of six iterations, with a ten seconds sleep executions. 
	 * 
	 * @param statusURL - URL to poll for build completion status
	 * @return String - URL to access build result
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws InterruptedException 
	 */
	protected String pollStatusUntilCompleted(String statusURL, int timeOutSeconds) throws ClientProtocolException, IOException, InterruptedException {
		// Set up bounded polling, maximum one minute, when status should 
		// change to completed.
		String buildResultLocation = null;
		int pollCount = 0;
		HttpGet httpGet = null;
		HttpClient httpClient = new DefaultHttpClient();
		ObjectMapper om = new ObjectMapper();
		
		// Calculate future time when we stop timeout polling.... 
		long futureTimeMillis = System.currentTimeMillis() + (timeOutSeconds * 1000);
		
		do {
			Map<String, String> stateDetails = (Map<String, String>) getJsonResponse(statusURL);
			BuildState buildState = BuildState.valueOf(stateDetails.get("state"));

			if (buildState.equals(BuildState.COMPLETED)) {
				// If we have a status of completed, check result! 
				buildResultLocation = stateDetails.get("result");
				assertNotNull(buildResultLocation);
				break;	
			} else {
				assertTrue(buildState.equals(BuildState.NOT_STARTED) || buildState.equals(BuildState.BUILDING));
			}
		} while (System.currentTimeMillis() < futureTimeMillis);

		// If we didn't receive a build response by this time, 
		// we've given up! Simple build should not take more than
		// a minute. 
		assertNotNull(buildResultLocation);

		return buildResultLocation;
	}

	/**
	 * Verify a build status has been completed at the 
	 * given location and return URL for build result.
	 * The build status should be completed without 
	 * any polling required.
	 * 
	 * @param statusURL - URL for status link
	 * @return Resource link for build result
	 * @throws ClientProtocolException - Error contacting host
	 * @throws IOException - Error contacting host
	 */
	protected String verifyCompletedBuildStatus(String statusURL) 
		throws ClientProtocolException, IOException {
		Map<String, String> response = (Map<String, String>) getJsonResponse(statusURL);
		
		BuildState buildState = BuildState.valueOf(response.get("state"));
		assertEquals(BuildState.COMPLETED, buildState);
		
		String buildResultLocation = response.get("result");
		assertNotNull(buildResultLocation);
		
		return buildResultLocation;
	}		
	
	/**
	 * 
	 * @param url
	 * @return
	 * @throws IOException 
	 * @throws IllegalStateException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	protected Map getJsonResponse(String url) throws JsonParseException, JsonMappingException, IllegalStateException, IOException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);
		// Content type headers
		httpGet.setHeader("Content-Type", MediaType.APPLICATION_JSON);
		httpGet.setHeader("Accept", MediaType.APPLICATION_JSON);
		HttpResponse response = httpClient.execute(httpGet);
		HttpEntity responseEntity = response.getEntity();

		// Must return a HTTP 200.
		assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		Map<String, Object> jsonResponse = JsonUtil.genericJSONMapper(responseEntity.getContent());
		
		return jsonResponse;
	}
	

	protected void startAndCompleteDojoBuild(String buildRequestResource) throws ClientProtocolException, IOException, URISyntaxException, InterruptedException {
		Set<String> baseDojoBuildFiles = new HashSet<String>() {{
			add("dojo.js");
		}};
		startAndCompleteDojoBuild(buildRequestResource, baseDojoBuildFiles);
	}
	
	protected void startAndCompleteDojoBuild(String buildRequestResource, Set<String> expectedResultFiles) throws ClientProtocolException, IOException, URISyntaxException, InterruptedException {
		HttpResponse response = generateBuildRequest(getClass().getClassLoader().getResourceAsStream(buildRequestResource));
		String link = extractBuildStatusPollingLink(response);
		String buildResultLocation = pollStatusUntilCompleted(link, 60);
		verifyBuildResult(buildResultLocation, expectedResultFiles);
	}
	
	
	/**
	 * Download a build result, given by method parameter, and verify that
	 * contents of the zip file contain a single non-empty file called "dojo.js".
	 * 
	 * @param buildResultLocation - URL to access build result.
	 * @throws ClientProtocolException 
	 * @throws IOException 
	 */
	protected void verifyBuildResult(String buildResultLocation, Set<String> expectedBuildFiles) throws ClientProtocolException, IOException {
		ZipInputStream zip = retrieveBuildResult(buildResultLocation);
		
		ZipEntry entry = zip.getNextEntry();
		
		// Loop through all zip entries, looking presence of the build files
		// that must be present and checking they aren't empty.
		while(entry != null) {
			String entryName = entry.getName();			
			if (expectedBuildFiles.contains(entryName)) {
				// Make sure zip entry contains some data...
				byte[] buffer = new byte[2048];
				int len = zip.read(buffer, 0, buffer.length);
				assertTrue(len != -1);
				
				// Seen this file, remove from set. 
				expectedBuildFiles.remove(entryName);
			}
			entry = zip.getNextEntry();
		}
		
		// We should have seen every entry we were expecting.
		assertEquals(expectedBuildFiles.size(), 0);
	}
	
	/**
	 * 
	 * @param buildResultLocation
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	protected ZipInputStream retrieveBuildResult(String buildResultLocation) throws ClientProtocolException, IOException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(buildResultLocation);
		httpGet.setHeader("Accept", "application/zip");
		HttpResponse response = httpClient.execute(httpGet);

		// HTTP 200 OK with content as zip! 
		assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);
		Header contentDisposition = response.getFirstHeader("Content-disposition");
		assertEquals("attachment; filename=dojo.zip", contentDisposition.getValue());

		// Retrieve zipped response, unzip and confirm it contains a single zip file.
		HttpEntity entity = response.getEntity();
		assertNotNull(entity);
 
		ZipInputStream zip = new ZipInputStream(entity.getContent());
		
		return zip;
	}

	/**
	 * Kick-off a build request using following input stream
	 * as build request.
	 * 
	 * @return Http response object associated build request
	 */
	protected HttpResponse generateBuildRequest(InputStream is) throws ClientProtocolException, IOException, URISyntaxException {
		StringWriter writer = new StringWriter();
		IOUtils.copy(is, writer, "utf-8");
		return generateBuildRequest(writer.toString());
	}
	
	/**
	 * Kick-off a build request using the given build profile. 
	 * 
	 * @return Http response object associated build request
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	protected HttpResponse generateBuildRequest(String buildProfile) throws ClientProtocolException, IOException, URISyntaxException {
		// Set up HTTP client connection....
		URL serviceEndPoint = new URL(Options.getTestAPILocation(baseResourcePath));
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(serviceEndPoint.toURI());

		// Content type headers
		httpPost.setHeader("Content-Type", "application/json");
		httpPost.setHeader("Accept", "application/json");

		// Build profile JSON 
		HttpEntity entity = new StringEntity(buildProfile);
		httpPost.setEntity(entity);

		// Execute the request...
		return httpClient.execute(httpPost);
	}
}