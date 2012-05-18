package org.dtk.analysis.page;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.dtk.analysis.ModuleFormat;
import org.dtk.analysis.script.loader.AmdModulePathResolver;
import org.dtk.analysis.script.loader.ModulePathResolver;
import org.dtk.analysis.script.loader.NonAmdModulePathResolver;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Implementation of the RecursiveWebPage class to support analysing 
 * pages available at a remote location. Module source files will be 
 * dynamically loaded as they are discovered by downloading the remote 
 * resources.
 * 
 * Modules are retrieved based upon their absolute module identifiers, 
 * internally this is translated into an external URL based upon the 
 * user's configuration set in the loader. 
 * 
 * @author James Thomas
 */

public class RemoteWebPage extends RecursiveWebPage {
	
	/**
	 * Http client used to access remote resources.
	 */
	HttpClient httpClient;
	
	/**
	 * Resolver used to translate module identifiers into URLs
	 */
	ModulePathResolver resolver;
	
	/**
	 * Static logging instance.
	 */
	protected static final Logger logger = Logger.getLogger(RemoteWebPage.class.getName());		
	
	/**
	 * Default constructor, store http client internal and delegate to super class.
	 * 
	 * @param document - Parsed HTML document to analyse
	 * @param location - Remote resource location
	 * @param httpClient - Http Client
	 */
	public RemoteWebPage(Document document, URL location, HttpClient httpClient, Set<String> ignoredPackages) {
		super(document, location);
		this.httpClient = httpClient;
		this.setIgnoredPackages(ignoredPackages);
		parse();	
	}
	
	/**
	 * Default constructor, store http client internal and delegate to super class.
	 * 
	 * @param document - Parsed HTML document to analyse
	 * @param location - Remote resource location
	 * @param httpClient - Http Client
	 */
	public RemoteWebPage(Document document, URL location, HttpClient httpClient) {
		super(document, location);
		this.httpClient = httpClient;
		parse();					
	}
	
	/**
	 * When we detect the Dojo script tag, initialise 
	 * a new module path resolver with current configuration.
	 * 
	 * @param script - Page script tag
	 */
	@Override
	protected void parsePreDojoScript(Element script) {
		super.parsePreDojoScript(script);
		
		if (parsePhase == ParsePhase.POST_DOJO) {			
			if (missingBaseUrlConfig()) {
				baseUrl = script.attr("src");
			}
			
			instantiatePathResolver();
		}
	}
		
	/**
	 * Retrieve the remote source file for this absolute module
	 * identifier on the page. Resolve module identifier to a remote
	 * file path, download the result and then pass text back.
	 * 
	 * @param moduleIdentifier - Absolute module identifier for page dependency
	 * @return module source, null if issues retrieving
	 */
	@Override
	protected String retrieveModuleSource(String moduleIdentifier) {
		URL absoluteModulePath = resolver.getAbsolutePath(moduleIdentifier);
		return retrieveUrl(absoluteModulePath.toString());			
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
		return LocalWebPage.doesScriptNameMatchDojo(script);
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
	 * Extract the full source contents for the Document script tag. Contents may 
	 * be either inline or as a linked "src" reference.
	 * 
	 * @param script - Document script tag
	 * @return Script contents or null if there was an issue accessing source.
	 */
	@Override
	protected String retrieveScriptContents(Element script) {
		String scriptContents = null;
		
		if (script.hasAttr("src")) {
			scriptContents = retrieveUrl(script.attr("abs:src"));
		} else {
			scriptContents = script.html();
		}
		
		return scriptContents;
	}
	
	/**
	 * Create the new path resolver based upon the current discovered
	 * module format.
	 */
	protected void instantiatePathResolver() {
		URL baseUrl = getAbsoluteBaseUrl();
		
		if (ModuleFormat.AMD.equals(moduleFormat)) {
			resolver = new AmdModulePathResolver(baseUrl, modulePaths);
		} else {
			resolver = new NonAmdModulePathResolver(baseUrl, modulePaths);
		}	
	}	
	
	/**
	 * Return absolute URL for the base url 
	 * for module resolution. 
	 * 
	 * @return Absolute base URL path, null if URL 
	 * can't be resolved.
	 */
	protected URL getAbsoluteBaseUrl() {
		URL absBaseUrl = null;
		
		try {
			// base url may be a file or directory path, ensure we always 
			// return a directory.
			absBaseUrl = new URL(new URL(location, baseUrl), "./");
		} catch (MalformedURLException e) {
			parsePhase = ParsePhase.ERROR;
		}
		
		return absBaseUrl;
	}

	/**
	 * Is the page missing custom base url configuration?
	 *  
	 * @return Page has not custom base url config
	 */
	protected boolean missingBaseUrlConfig() {
		return baseUrl == null;
	}	
	
	/**
	 * Use HttpClient to request and return response content 
	 * for a given URL. Returns a null response when there's an
	 * error retrieving URL content.
	 * 
	 * @param location - URL to retrieve 
	 * @return Response content
	 */
	protected String retrieveUrl(String location) {
		String moduleContents = null;

		try {
			HttpGet httpget = new HttpGet(location);
			HttpResponse response = httpClient.execute(httpget);
	
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				moduleContents = EntityUtils.toString(response.getEntity());	
			}
		} catch (IOException ioe) {
			logger.warning("Unable to retrieve resource at location: " + location);
		}

		return moduleContents;
	}
}