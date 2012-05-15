package org.dtk.analysis.page;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.dtk.analysis.ModuleFormat;
import org.dtk.resources.Build;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/**
 * Extension of the WebPage module analysis to support parsing of HTML 
 * files whose source is provided directly. The page has no access to any linked 
 * resources, e.g. CSS, JavaScript, images, that are referenced. 
 * 
 * Therefore, contents for any referenced scripts will only be provided if the script source
 * is inline. Detecting whether a script tag is a Dojo file uses a regular expression to match
 * the source filename.       
 * 
 * @author James Thomas
 */

public class LocalWebPage extends WebPage {
	/**
	 * Regular expression used to match Dojo script by URI path.
	 * Assume we have dojo.js somewhere in the filename.
	 */
	protected static final String dojoScriptPatternStr = "^dojo\\.(.)*js$";
	protected static final Pattern dojoScriptPattern = Pattern.compile(dojoScriptPatternStr);
	
	/**
	 * Static logging instance.
	 */
	protected static final Logger logger = Logger.getLogger(LocalWebPageTest.class.getName());	
	
	/**
	 * Default constructor, start parsing of HTML page contents.
	 * 
	 * @param htmlPageContent - Page content
	 */
	public LocalWebPage(String htmlPageContent) {
		super(Jsoup.parse(htmlPageContent));
	}
	
	/**
	 * Match filename against expected Dojo script tag source 
	 * path to determine whether script is a Dojo script.
	 * 
	 * @param script - Script tag element
	 * @return Script is a Dojo script tag
	 */
	@Override
	protected boolean isDojoScript(Element script) {		
		return doesScriptNameMatchDojo(script);
	}
	
	/**
	 * Return absolute module identifier for dependency from referenced script.
	 * For local web pages, there is no additional path information. 
	 * 
	 * @param moduleIdentifier - Relative module identifier
	 * @param script - Script tag
	 * @return Absolute module identifier
	 */
	@Override
	protected String getAbsoluteModuleIdentifier(String moduleIdentifier) {
		return moduleIdentifier;
	}

	/**
	 * Retrieve package identifier for a module path. 
	 * Usually the first path name in a module identifier.
	 * 
	 * @param moduleIdentifier - Relative module identifier
	 * @return Package identifier
	 */
	@Override
	protected String getPackageIdentifier(String moduleIdentifier) {		
		char separator = ModuleFormat.getPathSeparator(moduleFormat);
		return moduleIdentifier.split("\\" + Character.toString(separator))[0];						
	}

	/**
	 * Retrieve script contents, only source contents are available
	 * for inline scripts.
	 * 
	 * @param script - Script tag
	 * @return String - script contents, null if non-inline script
	 */
	@Override
	protected String retrieveScriptContents(Element script) {
		if (script.hasAttr("src")) {
			return null;
		}
		return script.html();
	}

	/**
	 * Match script tag name against expected dojo script name format. 
	 * 
	 * @param script - Script tag element
	 * @return Script name matches dojo format
	 */
	static protected boolean doesScriptNameMatchDojo(Element script) {
		boolean isDojoScript = false;		
		String scriptSrcAttr = script.attr("src");		
		
		if (!"".equals(scriptSrcAttr)) {
			String scriptFileName = getScriptFileName(scriptSrcAttr); 			
			isDojoScript = dojoScriptPattern.matcher(scriptFileName).find();				
		}		
		
		return isDojoScript;
	}

	/**
	 * Return file name for a script path. 
	 * 
	 * @param scriptURL - Full script URL
	 * @return File name containing script
	 */
	static protected String getScriptFileName(String scriptURL) {
		String scriptPath = getScriptPath(scriptURL);
		String[] scriptSourcePaths = scriptPath.split("/");			
		
		return scriptSourcePaths[scriptSourcePaths.length - 1];		
	}
	
	/**
	 * Return relative script path from path URL.
	 * 
	 * @param URL - URL to script file
	 * @return Script path if available, empty string if not.
	 */
	static protected String getScriptPath(String URL) {
		String scriptPath = "";
		try { 
			URI scriptPathURI = new URI(URL);
			scriptPath = scriptPathURI.getPath();
		} catch (URISyntaxException e) {
			logger.log(Level.WARNING, "Unable to construct URI reference from URL: " + URL);
		}
		return scriptPath;
	}
}
