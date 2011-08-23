package org.dtk.resources.dependencies;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.nodes.Element;

public class DojoScriptParser {
	
	protected String scriptLocation;
	
	protected boolean isDojoScript;
	
	protected String dojoScriptVersion;
	
	protected boolean parsed = false; 
	
	// Don't have EOL match because we've seen people use the ?version=number trick to force 
	// cache refreshes on long-lived resources. 
	protected static final String dojoScriptPatternStr = "^dojo(.)*\\.js";

	protected static final Pattern dojoScriptPattern = Pattern.compile(dojoScriptPatternStr);
	
	// Dump this into an enum......
	protected static final Map<String, String> knownDojoVersionHashLookup;
    static {
    	knownDojoVersionHashLookup = new HashMap<String, String>();
    	
    	/** Dojo Toolkit 1.6.0 **/   	
    	/** Local, Source */
    	knownDojoVersionHashLookup.put("3a4681ac7cc73ce89c7a29e3027f0195", "1.6.0");
    	
    	/** Local, Compressed - Shrinksafe */
    	// TODO:
    	
    	/** Cross Domain, Source - Google */ 
    	knownDojoVersionHashLookup.put("1995bd4489903f5a4b26c047fc4bb2b7", "1.6.0");
    	
    	/** Cross Domain, Compressed - Google */
    	knownDojoVersionHashLookup.put("0e310578c14068dfe712d6bf74a7fc48", "1.6.0");
    	
    	
    	/** Dojo Toolkit 1.5.0 **/
    	/** Local, Source */
    	knownDojoVersionHashLookup.put("96755520900bfff458dc44a55a21702e", "1.5.0");
    	
    	/** Local, Compressed - Shrinksafe */
    	// TODO:
    	
    	/** Cross Domain, Source */ 
    	knownDojoVersionHashLookup.put("1fcd329ab67b02a4dec0016d8b1dffa3", "1.5.0");
    	
    	/** Cross Domain, Compressed */
    	knownDojoVersionHashLookup.put("a000e7767ef0c1faa6dd233a06ef1526", "1.5.0");
    	
    	/** Dojo Toolkit 1.4.3 **/
    	/** Local, Source */
    	knownDojoVersionHashLookup.put("d93c7f52748f962b4646d7f0e6973e88", "1.4.3");
    	
    	/** Local, Compressed - Shrinksafe */
    	// TODO:
    	
    	/** Cross Domain, Source */ 
    	knownDojoVersionHashLookup.put("17bbc3dd3213cb9f9830fc90125ca664", "1.4.3");
    	
    	/** Cross Domain, Compressed */
    	knownDojoVersionHashLookup.put("5dddc7943b6b614642ecf631fbf40ac9", "1.4.3");
    }
	
    public DojoScriptParser(String scriptLocation) {
    	this.scriptLocation = scriptLocation; 

    }
    
	public boolean isDojoScript() {
		if (!parsed) {
			parse();
		}
		return isDojoScript; 
	}
	
	public String getDojoVersion() {
		if (isDojoScript()) {
			return dojoScriptVersion;
		} else {
			throw new NullPointerException("Haven't seen Dojo");
		}
	}
	
	protected void parse() {
		dojoScriptVersion = lookupDojoVersionForScript();
		
		if (dojoScriptVersion != null || isDojoScriptName()) {
			isDojoScript = true;
		}
		
		parsed = true;
	}
	
	protected boolean isDojoScriptName() {
		boolean isDojoScript = false;
		String[] scriptSourcePaths = scriptLocation.split("/");

		// Find actual script name from URI path 
		String scriptName = scriptSourcePaths[scriptSourcePaths.length - 1];

		Matcher match = dojoScriptPattern.matcher(scriptName);

		if (match.find()) {
			isDojoScript = true;
		}

		return isDojoScript;	
	}
	
	protected String lookupDojoVersionForScript() {
		MessageDigest md;
		String dojoVersion = null;
		try {
			md = MessageDigest.getInstance("MD5");
			String scriptSource = getScriptSource();
			md.update(scriptSource.getBytes("UTF-8")); 
			String scriptDigest = new String(Hex.encodeHex(md.digest()));
			dojoVersion = knownDojoVersionHashLookup.get(scriptDigest);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return dojoVersion;
	}
	
	// Should throw exception if I can't retrieve the source
	protected String getScriptSource() {
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(scriptLocation);
		
		String scriptSource = null;
		
		try {
			HttpResponse response = httpclient.execute(httpget);
			// Ignore anything other than a 200 OK response
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				scriptSource = EntityUtils.toString(response.getEntity());	
			}
		} catch (ClientProtocolException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}

		return scriptSource;
	}
	
	
	public static void main(String[] args) {
		HttpClient httpclient = new DefaultHttpClient();
		
		for(String scriptLocation: args) {
			HttpGet httpget = new HttpGet(scriptLocation);
			try {
				HttpResponse response = httpclient.execute(httpget);
				// Ignore anything other than a 200 OK response
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String scriptSource = EntityUtils.toString(response.getEntity());	
					MessageDigest md = MessageDigest.getInstance("MD5");
					md.update(scriptSource.getBytes("UTF-8")); 
					System.out.println(scriptLocation + ": " + new String(Hex.encodeHex(md.digest())));
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {			
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	}
}
