package org.dtk.resources.dependencies;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.impl.client.DefaultHttpClient;
import org.dtk.exception.ParseException;
import org.dtk.resources.dependencies.DojoScriptVersions.Versions;
import org.junit.Test;

/**
 * Test the ability of the DojoScript class to recognise known 
 * Dojo script versions. 
 * 
 * Test against a series of predefined Dojo script locations to verify that 
 * the correct Dojo script version is returned. For unknown script contents, 
 * class should be able to recognise Dojo script based upon URL. 
 * 
 * Also, check correct response for non-Dojo script returned.
 * 
 * @author James Thomas
 */
public class DojoScriptTest {

	/**
	 * Map of Dojo script locations and version identifier
	 */
	protected static final Map<String, Versions> dojoVersionScriptLocations;
	
	/**
	 * Valid instances of Dojo script names
	 */
	protected static final List<String> validDojoScriptNames = Arrays.asList(
		"http://some.host.com/dojo/dojo.js", 
		"http://some.host.com/dojo/dojo.xd.js", 
		"http://some.host.com/dojo/dojo.js?q=blah");
	
	/**
	 * Invalid Dojo script tags names
	 */
	protected static final List<String> invalidDojoScriptNames = Arrays.asList(
			"http://some.host.com/dojo/dijit.js", 
			"http://some.host.com/lib/dojosasd.js", 
			"http://some.host.com/dojo/dojo.xx");
	
	/**
	 * Fill map with available Dojo script tags versions the parser is supposed to be 
	 * able to detect.
	 */
    static {
    	dojoVersionScriptLocations = new HashMap<String, Versions>();
    	// Google CDN - Compressed
    	dojoVersionScriptLocations.put("http://ajax.googleapis.com/ajax/libs/dojo/1.6.0/dojo/dojo.xd.js", Versions.ONE_SIX_ZERO);
    	dojoVersionScriptLocations.put("http://ajax.googleapis.com/ajax/libs/dojo/1.5.0/dojo/dojo.xd.js", Versions.ONE_FIVE_ZERO);
    	dojoVersionScriptLocations.put("http://ajax.googleapis.com/ajax/libs/dojo/1.4.3/dojo/dojo.xd.js", Versions.ONE_FOUR_THREE);
    	// Google CDN - Uncompressed 
    	dojoVersionScriptLocations.put("http://ajax.googleapis.com/ajax/libs/dojo/1.6.0/dojo/dojo.xd.js.uncompressed.js", Versions.ONE_SIX_ZERO);
    	dojoVersionScriptLocations.put("http://ajax.googleapis.com/ajax/libs/dojo/1.5.0/dojo/dojo.xd.js.uncompressed.js", Versions.ONE_FIVE_ZERO);
    	dojoVersionScriptLocations.put("http://ajax.googleapis.com/ajax/libs/dojo/1.4.3/dojo/dojo.xd.js.uncompressed.js", Versions.ONE_FOUR_THREE);
    	// Dojo SVN - Source 
    	dojoVersionScriptLocations.put("http://svn.dojotoolkit.org/src/tags/release-1.6.0/dojo/dojo.js", Versions.ONE_SIX_ZERO);
    	dojoVersionScriptLocations.put("http://svn.dojotoolkit.org/src/tags/release-1.5.0/dojo/dojo.js", Versions.ONE_FIVE_ZERO);
    	dojoVersionScriptLocations.put("http://svn.dojotoolkit.org/src/tags/release-1.4.3/dojo/dojo.js", Versions.ONE_FOUR_THREE);
    }
	
    /**
     * Run through the collection of known Dojo script tag locations, using the parser to extract
     * the Dojo version for the source file. Confirm the version returned is our expected one. 
     * @throws URISyntaxException 
     * @throws ParseException 
     */
	@Test
	public void test_RetrieveKnownDojoVersions() throws URISyntaxException, ParseException {
		Iterator<Entry<String, Versions>> iter = dojoVersionScriptLocations.entrySet().iterator();
		
		while(iter.hasNext()) {
			Entry<String, Versions> knownDojoScript = iter.next();
			DojoScript parser = new DojoScript(new URI(knownDojoScript.getKey()), new DefaultHttpClient());
			assertEquals(knownDojoScript.getValue(), parser.getVersion());
		}
	}

	/**
	 * Verify that parser recognises Dojo script tags by name.
	 * @throws URISyntaxException 
	 */
	@Test 
	public void test_RecogniseValidDojoScriptName() throws URISyntaxException {
		for(String scriptName: validDojoScriptNames) {
			DojoScript parser = new DojoScript(new URI(scriptName), new DefaultHttpClient());	
			assertTrue(scriptName, parser.isDojoScriptName());
		}
	}
	
	/**
	 * Verify that parser doesn't accidentally classify other script tags
	 * as Dojo scripts.
	 * @throws URISyntaxException 
	 */
	@Test 
	public void test_IgnoreNonValidDojoScriptName() throws URISyntaxException {
		for(String scriptName: invalidDojoScriptNames) {
			DojoScript parser = new DojoScript(new URI(scriptName), new DefaultHttpClient());
			assertFalse(scriptName, parser.isDojoScriptName());
		}
	}
}
