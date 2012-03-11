package org.dtk.resources.dependencies;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.dtk.resources.dependencies.DojoScriptVersions.Versions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

public class WebPage {
	protected Document document;
	protected Exception parsingFailure;
	protected Map<String, String> modulePaths = new HashMap<String, String>();
	protected List<String> modules = new ArrayList<String>();
	protected Map<String, String> customModuleContent = new HashMap<String, String>();

	protected Versions dojoVersion = Versions.UNKNOWN;
	
	// When analysing a static HTML file, we can't access source for any custom modules requires.
	// Just ignore these references and carry on...
	protected boolean ignoreCustomModules = false;

	protected static final String dtkModulePrefixPatternStr = "^(dojo|dijit|dojox)\\.";

	protected static final Pattern dtkModulePrefixPattern = Pattern.compile(dtkModulePrefixPatternStr);

	/** Regular expression used to match module path attributes in user's djConfig variable */
	protected static final String regexPattern = "modulePaths:\\s*(\\{.*\\})";		
	
	protected static final Pattern modulePathsPattern = Pattern.compile(regexPattern);

	public WebPage(URL location) throws IOException {
		this.document = Jsoup.connect(location.toString()).get();	
	}

	public WebPage(String htmlContent) {
		this.document = Jsoup.parse(htmlContent);
		this.ignoreCustomModules = true;
	}

	public boolean parse() {
		boolean parsed = false;

		// If parsing fails, log causing exception and return false
		// to callee. This may be due to malformed source content or
		// inability to retrieve source files.
		try {
			Elements scriptTags = findAllScriptTags();

			for (Element scriptTag: scriptTags) {
				// Before we've found Dojo, just look for djConfig 
				// variable and actual Dojo script. 
				if (dojoVersion.equals(Versions.UNKNOWN)) {
					parsePreDojoScript(scriptTag);
				} else {
					parsePostDojoScript(scriptTag);
				}
			}

			parsed = true;
		} catch (ParseException pe) {
			this.parsingFailure = pe;
		} catch (IOException ioe) {
			this.parsingFailure = ioe;
		} catch (EvaluatorException ee) {
			this.parsingFailure = ee;
		} catch (URISyntaxException ee) {
			this.parsingFailure = ee;	
		} catch (org.dtk.exception.ParseException ee) {
			this.parsingFailure = ee;
		}

		return parsed;
	}

	protected String retrieveModuleContents(String moduleName) throws ParseException, IOException {
		String moduleLocation = customModuleLocation(moduleName);

		// Use JSoup to handle URL resolution from relative module link.
		// Create a new anchor element and use special 'abs:' attribute
		// selector to resolve module location into to an absolute URL.
		Element moduleLink = this.document.createElement("a");
		moduleLink.attr("href", moduleLocation);
		String absoluteModuleLocation = moduleLink.attr("abs:href");

		// Download the module contents
		String moduleContents = retrieveURLContents(absoluteModuleLocation);

		return moduleContents;
	}

	protected String retrieveURLContents(String moduleLocation) throws ParseException, IOException {
		String moduleContents = null;

		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(moduleLocation);

		HttpResponse response = httpclient.execute(httpget);

		// Ignore anything other than a 200 OK response
		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			moduleContents = EntityUtils.toString(response.getEntity());	
		}

		return moduleContents;
	}

	/**
	 * Return the absolute URL path for a custom Dojo module, given the module
	 * name. Trace through the defined module paths, looking for matches, otherwise
	 * we assume module is sibling to dojo's parent directory.
	 * 
	 * TODO: Refactor this method, it's grown too big.
	 * 
	 * @param moduleName - Full dojo package name 
	 * @return Absolute URL for module source
	 */
	protected String customModuleLocation(String moduleName) {
		// Custom module locations are relative to parent of base Dojo path. Find parent
		// location of core DTK modules.
		String moduleLocation = null;

		// Search through defined paths for match, specific to generic. 
		String[] moduleSearchPaths = moduleName.split("\\.");

		// Store path parts which aren't matched, 
		Stack<String> nonMatchingModulePaths = new Stack<String>();

		while(moduleSearchPaths.length > 0) {
			// Create module path from module parts
			String currentSearchpath = StringUtils.join(moduleSearchPaths, '/');

			// Check if this path has a registered location...
			if (this.modulePaths.containsKey(currentSearchpath)) {
				String modulePath = this.modulePaths.get(currentSearchpath);
				// If custom module path points to absolute path, don't start with base dojo path.
				if (modulePath.startsWith("./") || modulePath.startsWith("/") || modulePath.startsWith("http://")) {
					moduleLocation = modulePath;
				} else {
					moduleLocation = this.modulePaths.get("dojo") + modulePath + "/";
				}
				break;
			}

			nonMatchingModulePaths.push(moduleSearchPaths[moduleSearchPaths.length - 1]);

			// Pop last module path from array.
			moduleSearchPaths = (String[]) ArrayUtils.remove(moduleSearchPaths, moduleSearchPaths.length - 1);
		}

		// If user hasn't explicitly registered any matching path for this module, assume it starts
		// in a sibling directory to the dojo directory.
		if (moduleLocation == null) {
			moduleLocation = this.modulePaths.get("dojo") + "../";
		}
		
		while(!nonMatchingModulePaths.isEmpty()) {
			String path = nonMatchingModulePaths.pop();
			moduleLocation += "/" + path;
		}
		
		// Append JavaScript file extension to module path.
		moduleLocation += ".js";

		return moduleLocation;
	}

	/**
	 * Parse page script tag before we have identified Dojo script. This may be Dojo script
	 * tag or another JS source file. Attempt to determine if script is Dojo, with exact version,
	 * otherwise check for djConfig. 
	 * 
	 * @param script - Page script
	 * @throws ParseException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws org.dtk.exception.ParseException
	 */
	protected void parsePreDojoScript(Element script) throws ParseException, IOException, 
		URISyntaxException, org.dtk.exception.ParseException {
		
		DojoScript pageScript = new DojoScript(new URI(script.attr("abs:src")), new DefaultHttpClient());

		// Detect whether script is Dojo 
		Versions dojoVersionDetected = pageScript.getVersion();

		// Check if script is dojo, otherwise analyse for
		// module path definitions
		if(Versions.INVALID.equals(dojoVersionDetected)) {
			parseDjConfig(script);
		} else {
			parseModulePaths(script);
			dojoVersion = dojoVersionDetected;
		}
	}

	// Parse all JavaScript files for djConfig definitions. 
	protected void parseDjConfig(Element script) throws ParseException, IOException, EvaluatorException {
		// Pull script contents, inline or remote, for analysis. 
		String scriptContent = retrieveScriptContents(script);

		// Parse script contents, extracting relevant dojo calls. 
		ScriptParser scriptParser = new ScriptParser(scriptContent);
		// Merge module paths into existing map, overwrite any conflicts. 
		Map<String, String> modulePaths = scriptParser.retrieveModulePaths();
		this.modulePaths.putAll(modulePaths);
	}

	protected void parseModulePaths(Element dojoScript) {
		// Set up standard module paths using script location. 
		// Enforce absolute URLs.
		String baseModulePath = parseBaseModulePath(dojoScript.attr("abs:src"));

		this.modulePaths.put("dojo", baseModulePath);

		// Check for additional module path definitions.
		if (dojoScript.hasAttr("djConfig")) {
			parseDjConfigModulePaths(dojoScript.attr("djConfig"));
		}
	}

	protected String parseBaseModulePath(String scriptPath) {
		int baseParentPath = scriptPath.lastIndexOf('/') + 1;

		return scriptPath.substring(0, baseParentPath);
	}

	/**
	 * Update this page's Dojo module paths by parsing 
	 * the djConfig attribute on the script tag which includes
	 * Dojo.  
	 * 
	 * @param djConfigAttr - djConfig script tag attribute
	 */
	protected void parseDjConfigModulePaths(String djConfigAttr) {
		// Use Rhino to evaluate choice instead of parsing manually with
		// brittle regular expressions. Assign object to variable and return 
		// result from script.
		String djConfigObject = "var djconfig = {";
		djConfigObject += djConfigAttr;
		djConfigObject += "}; djconfig";

		org.mozilla.javascript.Context cx = ContextFactory.getGlobal().enterContext();
		Scriptable scope = cx.initStandardObjects();			       
		NativeObject result = (NativeObject) cx.evaluateString(scope, djConfigObject, "modulePaths.js", 1, null);

		// Check for presence of the djConfig attribute and pull out user-defined modules
		// paths.
		if (result.has("modulePaths", scope)) {
			NativeObject userModulePaths = (NativeObject) result.get("modulePaths", scope);
			for(Object moduleId: userModulePaths.getAllIds()) {
				String thisModulePath = (String) userModulePaths.get((String) moduleId, scope);
				this.modulePaths.put((String) moduleId, thisModulePath);
			}
		} 			
	}

	/**
	 * 
	 * @param scriptPath
	 * @return
	 */
	protected String extractScriptFileName(String scriptPath) {
		// Strip off any query parameters, used for versioning 
		// scripts to override local cache 
		if (scriptPath.contains("?")) {
			scriptPath = scriptPath.substring(0, scriptPath.indexOf('?'));
		}
		
		// 
		String[] scriptSourcePaths = scriptPath.split("/");

		// Find actual script name from URI path 
		return scriptSourcePaths[scriptSourcePaths.length - 1];
	}
	
	/**
	 * Extract complete script contents and search through
 	 * for any dojo.require calls. 
 	 * 
	 * @param script - <script> tag element 
	 * @throws ParseException - Error parsing this script 
	 * @throws IOException - Error retrieving this element
	 */
	protected void parsePostDojoScript(Element script) throws ParseException, IOException {
		String scriptContents = retrieveScriptContents(script);
		
		// If there was a problem retrieving this script source, don't try
		// to parse result.
		if (scriptContents != null) {
			analyseModuleDependencies(scriptContents);
		}
	}

	protected String retrieveScriptContents(Element script) throws ParseException, IOException {
		// Assume script is inline by default
		String scriptContent = script.html();

		// Retrieve non-inline script source
		if (script.hasAttr("src")) {
			String scriptLocation = script.attr("abs:src");
			scriptContent = retrieveURLContents(scriptLocation);
		}

		return scriptContent;
	}

	protected void analyseModuleDependencies(String scriptSource) throws ParseException, IOException, EvaluatorException {
		ScriptParser scriptParser = new ScriptParser(scriptSource);

		// Run through any dojo.registerModulePaths before we analyse dojo.requires.
		// If we need to download custom module paths, must ensure we are pulling 
		// them from the correct location.
		this.modulePaths.putAll(scriptParser.retrieveModulePaths());

		// Parse dojo.require calls from script...
		for (String moduleName : scriptParser.retrieveModuleRequires()) {	
			// Don't add duplicates we have already seen.
			if (!modules.contains(moduleName)) {
				modules.add(moduleName);

				// When parsing static HTML, rather than from a URL, ignore
				// custom modules as we have no access to the source.
				if (!ignoreCustomModules) {
					// If module referenced isn't a standard DTK module,
					// fetch content and recursively parse for child dependencies
					Matcher matcher = dtkModulePrefixPattern.matcher(moduleName);
					if (!matcher.find()) {
						// Download the entire module!
						String customModuleContents = retrieveModuleContents(moduleName);

						this.customModuleContent.put(moduleName, customModuleContents);

						// Recursively fetch modular dependencies
						analyseModuleDependencies(customModuleContents);
					}
				}
			}
		}	
	}

	public Versions getDojoVersion() {
		return dojoVersion;
	}
	
	public List<String> getModules() {
		return this.modules;
	}

	public Map<String, String> getCustomModuleContent() {
		return this.customModuleContent;
	}

	protected Elements findAllScriptTags () {
		return this.document.getElementsByTag("script");
	}

	public String getDocumentContents() {
		return this.document.outerHtml();
	}

	public Exception getParsingFailure () {
		return this.parsingFailure;
	}
}
