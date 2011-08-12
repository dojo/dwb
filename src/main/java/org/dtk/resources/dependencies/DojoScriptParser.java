package org.dtk.resources.dependencies;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class DojoScriptParser {
	
	protected Element scriptTag;
	
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
    	
    	/** Dojo 1.6, source release */
    	knownDojoVersionHashLookup.put("3a4681ac7cc73ce89c7a29e3027f0195", "1.6");
    	
    	/** Dojo 1.5, source release */
    	knownDojoVersionHashLookup.put("96755520900bfff458dc44a55a21702e", "1.5");
    	
    	/** Dojo 1.5, source release */
    	knownDojoVersionHashLookup.put("d93c7f52748f962b4646d7f0e6973e88", "1.4.3");
    }
	
    public DojoScriptParser(Element scriptTag) {
    	this.scriptTag = scriptTag; 

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

		String scriptLocation = scriptTag.attr("abs:src");
		
		
		if (scriptLocation != null) {
			String[] scriptSourcePaths = scriptLocation.split("/");

			// Find actual script name from URI path 
			String scriptName = scriptSourcePaths[scriptSourcePaths.length - 1];

			Matcher match = dojoScriptPattern.matcher(scriptName);

			if (match.find()) {
				isDojoScript = true;
			}
		}

		return isDojoScript;
		
	}
	
	protected String lookupDojoVersionForScript() {
		MessageDigest md;
		String dojoVersion = null;
		try {
			md = MessageDigest.getInstance("MD5");
			String scriptSource = scriptTag.html();		
			md.update(scriptSource.getBytes());
			String scriptDigest = new String(md.digest());
			dojoVersion = knownDojoVersionHashLookup.get(scriptDigest);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return dojoVersion;
	}
	
	public static void main(String[] args) {
		String host = "http://localhost/~james/index.html";
		Document doc;
		try {
			doc = Jsoup.connect(host).get();

			Element script = doc.getElementsByTag("script").get(0);
			
			DojoScriptParser dsp = new DojoScriptParser(script);
			
			System.out.println(dsp.isDojoScript());
			System.out.println(dsp.getDojoVersion());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
