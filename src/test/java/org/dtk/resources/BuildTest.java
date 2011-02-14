package org.dtk.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
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
import org.dtk.util.JsonUtil;
import org.junit.Test;

import static org.junit.Assert.* ;

/**
 * Test the RESTful Build API. This API provides access to the underlying 
 * Dojo build system, allowing a user to submit build jobs, check the status 
 * of a build request and access the result.  
 * 
 * @author James Thomas
 */

public class BuildTest {

	/**
	 * Service end point details.
	 */
	final protected String protocol = "http";

	final protected String host = "localhost";

	final protected int port = 8080;

	final protected String baseResourcePath = "/dwb/api/build/";

	final protected String statusResourcePath = "/dwb/api/build/status/";

	/**
	 * Default build profile for base dojo.js with no added modules.
	 */
	final protected String defaultBuildProfile = "default_build_request.json";
	
	/**
	 * Default build profile for base dojo.js with no added modules.
	 */
	final protected String emptyBuildProfile = "{}";

	/**
	 * Test typical case using valid build parameters. 
	 * We are expecting returned response to have a status
	 * reference URL, which successfully resolves after a 
	 * period. The HTTP response should be a HTTP 202. 
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws InterruptedException 
	 */
	@Test 
	public void test_ValidNewBuildRequest () throws URISyntaxException, ClientProtocolException, IOException, InterruptedException  {
		// Generate default build request
		HttpResponse response = generateBuildRequest();
		String link = extractBuildStatusPollingLink(response);
		String buildResultLocation = pollStatusUntilCompleted(link, 60);
		verifyBuildResult(buildResultLocation);
	}

	/**
	 * Test typical case where build parameters have 
	 * previously been submitted and a cached version 
	 * of the same build is available. The build status
	 * link should always return "completed" for the build 
	 * state without any polling.
	 * 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws InterruptedException 
	 */
	@Test 
	public void test_ValidCachedBuildRequest () throws URISyntaxException, ClientProtocolException, IOException {
		// Generate default build request
		HttpResponse response = generateBuildRequest();
		String link = extractBuildStatusPollingLink(response);
		String buildResultLocation = verifyCompletedBuildStatus(link);
		verifyBuildResult(buildResultLocation);
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

		// Service must return a 400 for invliad build requests.
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode()); 
	}

	/**
	 * Test retrieval of the status for a build request 
	 * that exists. We should have two valid responses, building
	 * and complete with relevant links. 
	 */
	@Test
	public void test_RetrieveValidBuildStatus() {
		// TODO: We have already ran this test as part of the 
		// valid build request. Is there any point in running it
		// again? 
	}

	/**
	 * FIXME: This test will fail until we have abstracted build process
	 * into an actual state manager.
	 *  
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
		URL serviceEndPoint = new URL(protocol, host, port, invalidBuildURL);
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
	 * Test the retrieval of a valid build resource, returned
	 * from a previous build request. We should have a normal
	 * HTTP code 200 returned with the built resource.
	 */
	@Test 
	public void test_AccessValidBuildResult() {
		// TODO: We have already ran this test as part of the 
		// valid build request. Is there any point in running it
		// again? 
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
		URL serviceEndPoint = new URL(protocol, host, port, invalidBuildURL);
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
	
	/**
	 * Download a build result, given by method parameter, and verify that
	 * contents of the zip file contain a single non-empty file called "dojo.js".
	 * 
	 * @param buildResultLocation - URL to access build result.
	 * @throws ClientProtocolException 
	 * @throws IOException 
	 */
	protected void verifyBuildResult(String buildResultLocation) throws ClientProtocolException, IOException {
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

		// Unzip and check contents... 
		ZipInputStream zip = new ZipInputStream(entity.getContent());
		ZipEntry entry = zip.getNextEntry();

		// Very simple test that we have only a single dojo.js file and 
		// it isn't empty! 
		assertEquals("dojo.js", entry.getName());

		// Make sure zip entry contains some data...
		byte[] buffer = new byte[2048];
		int len = zip.read(buffer, 0, buffer.length);
		assertTrue(len != -1);
		
		// No more entries....
		assertNull(zip.getNextEntry());
	}

	/**
	 * Kick-off a build request using the default build profile.
	 * 
	 * @return Http response object associated build request
	 */
	protected HttpResponse generateBuildRequest() throws ClientProtocolException, IOException, URISyntaxException {
		InputStream is = getClass().getClassLoader().getResourceAsStream(defaultBuildProfile);
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
		URL serviceEndPoint = new URL(protocol, host, port, baseResourcePath);
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