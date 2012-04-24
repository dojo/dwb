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

public class LocalWebPage extends WebPage {
	/**
	 * Regular expression used to match Dojo script by URI path.
	 */
	protected static final String dojoScriptPatternStr = "^dojo\\.(.)*js$";
	protected static final Pattern dojoScriptPattern = Pattern.compile(dojoScriptPatternStr);
	
	/**
	 * Static logging instance.
	 */
	protected static final Logger logger = Logger.getLogger(LocalWebPageTest.class.getName());
	
	public LocalWebPage(String htmlPageContent) {
		super(Jsoup.parse(htmlPageContent));
	}
	
	@Override
	protected boolean isDojoScript(Element script) {
		boolean isDojoScript = false;		
		String scriptSrcAttr = script.attr("src");		
		
		if (!"".equals(scriptSrcAttr)) {
			String scriptFileName = getScriptFileName(scriptSrcAttr); 			
			isDojoScript = dojoScriptPattern.matcher(scriptFileName).find();				
		}		
		
		return isDojoScript;
	}

	@Override
	protected String getAbsoluteModuleIdentifier(String moduleIdentifier,
			Element script) {
		return moduleIdentifier;
	}

	@Override
	protected String getPackageIdentifier(String moduleIdentifier) {		
		char separator = ModuleFormat.getPathSeparator(moduleFormat);
		return moduleIdentifier.split(Character.toString(separator))[0];						
	}

	@Override
	protected String retrieveScriptContents(Element script) {
		if (script.hasAttr("src")) {
			return null;
		}
		return script.html();
	}

	protected String getScriptFileName(String scriptURL) {
		String scriptPath = getScriptPath(scriptURL);
		String[] scriptSourcePaths = scriptPath.split("/");			
		
		return scriptSourcePaths[scriptSourcePaths.length - 1];		
	}
	
	protected String getScriptPath(String URL) {
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
