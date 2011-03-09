package org.dtk.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Test options manager. This static utility class manages the test parameters used
 * by the integration tests. Provides default values for the API endpoint being tested
 * and can be overridden by manually set system properties for the JVM. 
 * 
 * @author James Thomas
 */

public class Options {
	// Test options unique identifiers, which are host name, port, app path and protocol.
	// i.e. {http}://{localhost}:{8080}/{dwb}
	private static final String TEST_HOST_PARAM = "test.host";
	private static final String TEST_PORT_PARAM = "test.port";
	private static final String TEST_APP_PATH_PARAM = "test.path";
	private static final String TEST_PROTOCOL_PARAM = "test.protocol";
	
	// Default test parameters which can be overridden by setting system parameters when
	// invoking the tests. Default to localhost. 
	private static final Map<String, String> defaultParameterValues = new HashMap<String, String>() {{
		put(TEST_HOST_PARAM, "localhost");
		put(TEST_PORT_PARAM, "8080");
		put(TEST_APP_PATH_PARAM, "/dwb");
		put(TEST_PROTOCOL_PARAM, "http");
	}};
	
	/**
	 * Return the hostname to run tests against.
	 * 
	 * @return String - Test host.
	 */
	public static String getTestHost() {
		return Options.getTestParameter(TEST_HOST_PARAM);
	}
	
	/**
	 * Return the server port to run tests against.
	 * 
	 * @return String - Test server port.
	 */
	public static Integer getTestPort() {
		String testPort = Options.getTestParameter(TEST_PORT_PARAM);
		return new Integer(testPort);
	}
	
	/**
	 * Return the protocol scheme to run tests against.
	 * 
	 * @return String - Test protocol scheme.
	 */
	public static String getTestProtocol() {
		return Options.getTestParameter(TEST_PROTOCOL_PARAM);
	}
	
	/**
	 * Return the app deployment path to run tests against.
	 * 
	 * @return String - Test path for app deployment.
	 */
	public static String getTestAppPath() {
		return Options.getTestParameter(TEST_APP_PATH_PARAM);
	}
	
	/**
	 * Return the full test path for an API endpoint. Uses test properties
	 * specifying remote endpoint, whether specified by system properties
	 * or using default values. 
	 * 
	 * @param apiPath - Path to API to test
	 * @return String - Full API path URL for test host 
	 * @throws MalformedURLException - Source option was invalid 
	 */
	public static String getTestAPILocation(String apiPath) throws MalformedURLException {
		URL testAPILocation = new URL(Options.getTestProtocol(), Options.getTestHost(), 
			Options.getTestPort(), Options.getTestAppPath() + apiPath);
		
		return testAPILocation.toString();
	}
	
	/**
	 * Find test parameter value, returning default value if this
	 * parameter wasn't explicitly set by the user.
	 * 
	 * @param parameterName - Test property identifier
	 * @return String - Test parameter value for name.
	 */
	private static String getTestParameter(String parameterName) {
		String testParameter = System.getProperty(parameterName);
		
		if (testParameter == null) {
			testParameter = Options.defaultParameterValues.get(parameterName);
		}
		
		return testParameter;
	}
}
