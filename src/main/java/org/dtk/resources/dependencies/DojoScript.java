package org.dtk.resources.dependencies;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.dtk.exception.ParseException;
import org.dtk.resources.dependencies.DojoScriptVersions.Versions;

/**
 * Discover the Dojo version for a given JavaScript source file. 
 * 
 * If a supported Dojo version is discovered, the value is returned. 
 * Otherwise, if the URI likely points to a Dojo JavaScript file whose
 * version is unknown, return Versions.VALID. 
 * 
 * If we can't detect Dojo source file at URI, return Versions.INVALID.
 * 
 * @author James Thomas
 */

public class DojoScript {
	/** Constants for digest type and source encoding */
	private static final String DIGEST_ALGO = "MD5";

	/** HTTP client abstraction, used for pulling script contents */
	protected HttpClient client;
	
	/** Full URI path to script */
	protected URI scriptLocation;
	
	/** Dojo version discovered for this script, unknown until parsed.  */
	protected Versions dojoScriptVersion = Versions.UNKNOWN;
	
	/**
	 * Regular expression used to match Dojo script by URI path.
	 */
	protected static final String dojoScriptPatternStr = "^dojo\\.(.)*js$";
	protected static final Pattern dojoScriptPattern = Pattern.compile(dojoScriptPatternStr);
	
	/**
	 * Constructor. Create new Dojo Script instance from 
	 * URI parameter.
	 * 
	 * @param scriptLocation
	 */
    public DojoScript(URI scriptLocation, HttpClient client) {
    	this.scriptLocation = scriptLocation; 
    	this.client = client;
    }
    
    /**
     * Discover and return Dojo Script version for this URI. 
     * 
     * @return Dojo Script Version
     */
	public Versions getVersion() throws ParseException {
		if (dojoScriptVersion.equals(Versions.UNKNOWN)) {
			discover();
		}
		
		return dojoScriptVersion;
	}
	
	/**
	 * Attempt to discover a Dojo script at the source location. 
	 * Look up is performed on digest of URI contents, to match exact version.
	 * Failing that, check if URI path conforms to expected Dojo script tag name.
	 * Otherwise, set value to INVALID.
	 * 
	 * @throws ParseException - Error performing digest lookup
	 */
	protected void discover() throws ParseException {
		dojoScriptVersion = lookupExactDojoVersionForScript();
		
		if (dojoScriptVersion.equals(Versions.UNKNOWN)) {
			dojoScriptVersion = isDojoScriptName() ? Versions.VALID : Versions.INVALID;
		}
	}
	
	/**
	 * Check whether URI path matches expected Dojo Script tag format. 
	 * 
	 * @return URI path matches Dojo script tag.
	 */
	protected boolean isDojoScriptName() {
		// Extract out final URI path segment from full URI
		String[] scriptSourcePaths = scriptLocation.getPath().split("/");
		String scriptName = scriptSourcePaths[scriptSourcePaths.length - 1];
		
		// Now, see if it matches dojo script name pattern...
		return dojoScriptPattern.matcher(scriptName).find();	
	}
	
	/**
	 * Download script contents and use digest as lookup for 
	 * an exact Dojo version on match. 
	 * 
	 * @return Dojo script tag found or Versions.UNKNOWN
	 * @throws ParseException - Error creating digest
	 */
	protected Versions lookupExactDojoVersionForScript() throws ParseException {
		MessageDigest md;
		Versions scriptVersion = Versions.UNKNOWN;
		try {
			// Create digest from script contents
			md = MessageDigest.getInstance(DIGEST_ALGO);
			md.update(getScriptSource());
			
			// We want a hex-encoded key, not raw bytes
			String scriptDigest = new String(Hex.encodeHex(md.digest()));
			if (DojoScriptVersions.lookup.containsKey(scriptDigest)) {
				scriptVersion = DojoScriptVersions.lookup.get(scriptDigest);
			}
		} catch (NoSuchAlgorithmException e) {
			throw new ParseException(e);
		}
		
		return scriptVersion;
	}
	
	/**
	 * Return scripts contents from URI path.
	 * 
	 * @return Script contents 
	 * @throws ParseException - Failed to retrieve contents.
	 */
	protected byte[] getScriptSource() throws ParseException {				
		if ("".equals(scriptLocation.toString())) {
			return new byte[0];
		}
		
		byte[] scriptSource = new byte[0];			
		
		try {
			HttpGet httpget = new HttpGet(scriptLocation);
			HttpResponse response = client.execute(httpget);
			// Ignore anything other than a 200 OK response
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				scriptSource = EntityUtils.toByteArray(response.getEntity());
			}
		} catch (ClientProtocolException e) {
			throw new ParseException(e);
		} catch (IOException e) {
			throw new ParseException(e);
		}

		return scriptSource;
	}
	
	/** 
	 * Sample main used for generating digest hashes for remote URLs. 
	 * Used for populating the DojoScriptVersions lookup table.
	 */
	public static void main(String[] args) {
		HttpClient httpclient = new DefaultHttpClient();
		
		for(String scriptLocation: args) {
			HttpGet httpget = new HttpGet(scriptLocation);
			try {
				HttpResponse response = httpclient.execute(httpget);
				// Ignore anything other than a 200 OK response
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					MessageDigest md = MessageDigest.getInstance(DIGEST_ALGO);
					md.update(EntityUtils.toByteArray(response.getEntity())); 
					System.out.println(scriptLocation + ": " + new String(Hex.encodeHex(md.digest())));
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {			
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}	
		}
	}
}
