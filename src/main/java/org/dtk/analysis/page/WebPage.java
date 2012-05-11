package org.dtk.analysis.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dtk.analysis.ModuleAnalysis;
import org.dtk.analysis.ModuleFormat;
import org.dtk.analysis.exceptions.FatalAnalysisError;
import org.dtk.analysis.script.config.DojoConfigAttrs;
import org.dtk.analysis.script.config.LoaderConfigParser;
import org.dtk.analysis.script.config.ScriptConfigParser;
import org.dtk.analysis.script.dependency.AMDScriptParser;
import org.dtk.analysis.script.dependency.NonAMDScriptParser;
import org.dtk.analysis.script.dependency.ScriptDependencyParser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Base class for analysing web pages for modules dependencies. Each instance
 * is initialised with a Document representing the parsed HTML page. During the 
 * parsing phase, the class searches through each script tag present within the 
 * page looking for module dependencies and module paths configuration. 
 * 
 * At the start, each script tag is checked to ascertain whether it contains the 
 * Dojo loader. Once this has been detected, each subsequent script has its source
 * retrieved and scanned for module dependencies contained within. 
 * 
 * All module dependencies discovered are maintained into an internal Map, arranging 
 * by package.
 * 
 * @author James Thomas
 */

public abstract class WebPage implements ModuleAnalysis {
	
	/**
	 * Parsed HTML source into a Document instance. Used to access page's
	 * scripts tags used in module analysis.  
	 */
	protected Document document;
	
	/**
	 * Module identifiers discovered during web page analysis, 
	 * organised by global package names.
	 */
	protected Map<String, List<String>> discoveredModules = new HashMap<String, List <String>>();
	
	/**
	 * Phase of the module analysis parsing being carried out. Script parsing 
	 * is handled differently before and after the Dojo script has been encountered.
	 */
	protected ParsePhase parsePhase = ParsePhase.PRE_DOJO;
	
	/**
	 * Module format being used by the web page, updated during analysis
	 * from any dojo configurations values defined.
	 */
	protected ModuleFormat moduleFormat = ModuleFormat.NON_AMD;
	
	/**
	 * Dojo configuration wrapper for custom config attributes passed as 
	 * into the <script> tag custom attribute.
	 */
	static protected final String DOJO_CONFIG_DECLARATION = "var " 
		+ DojoConfigAttrs.LOADER_CONFIG_DOJO_CONFIG + " = { %s };";
	
	/**
	 * Default WebPage constructor, must pass in the parsed 
	 * HTML source for the page to be analysed. Module analysis
	 * is automatically kicked off on creation.
	 *  
	 * @param document - Parsed HTML source for page
	 */
	protected WebPage(Document document) {
		this.document = document;
		parse();
	}
	
	/**
	 * Return the list of discovered modules identified during parsing, 
	 * organised by their packages.
	 * 
	 * @return Analysed module identifiers
	 * @throws FataAnalysisError - Exception details for fatal analysis issue
	 */
	@Override
	public Map<String, List<String>> getModules() throws FatalAnalysisError {
		if (ParsePhase.ERROR.equals(parsePhase)) {
			throw new FatalAnalysisError();
		}
		return discoveredModules;
	}
	
	/**
	 * Does this document script tag contain the DTK loader code?
	 * 
	 * @param script - Document script tag
	 * @return Script contains Dojo loader
	 */
	abstract protected boolean isDojoScript(Element script);
	
	/**
	 * Return the absolute module identifer for a relative module identifier
	 * associated with this document script. 
	 * 
	 * @param moduleIdentifer - Relative module identifier
	 * @param script - Script tag for identifier
	 * @return Absolute module identifier
	 */
	abstract protected String getAbsoluteModuleIdentifier(String moduleIdentifer, Element script);	
	
	/**
	 * Return package identifier for this module.
	 * 
	 * @param moduleIdentifer - Module identifier discovered
	 * @return Package identifying string
	 */
	abstract protected String getPackageIdentifier(String moduleIdentifer);
	
	/**
	 * Extract the full source contents for the Document script tag.
	 * 
	 * @param script - Document script tag
	 * @return Script contents or null if there was an issue accessing source.
	 */
	abstract protected String retrieveScriptContents(Element script);
	
	/**
	 * Parse the page document for module identifiers. Scanning 
	 * all the available script tags on the page, before Dojo's found
	 * we are looking for configuration parameters that set up package
	 * paths. Once the Dojo loader has been found, search for all module
	 * declarations and dependency identifiers. 
	 */
	protected void parse() {
		Elements scriptTags = findAllScriptTags();

		for (Element scriptTag: scriptTags) { 
			if (!hasFoundDojoScript()) {
				parsePreDojoScript(scriptTag);
			} else {
				parsePostDojoScript(scriptTag);
			}
		}		
	}
	
	/**
	 * Check whether this script tag contain the Dojo loader,
	 * updating the internal parse state if found. We also look 
	 * for AMD loader configuration flags.
	 * 
	 * @param script - Document script 
	 */
	protected void parsePreDojoScript(Element script) {
		String scriptSource;
		
		if (isDojoScript(script)) {			
			scriptSource = constructConfigDeclarationFromScriptAttr(script);
			parsePhase = ParsePhase.POST_DOJO;
		} else {
			scriptSource = retrieveScriptContents(script); 
		}				
		 
		Map<String, Object> parsedScriptConfig = parseScriptConfiguration(scriptSource);
		if (parsedScriptConfig.containsKey(DojoConfigAttrs.AMD_CONFIG_FLAG)) {
			enableAmdModuleFormat((Boolean) parsedScriptConfig.get(DojoConfigAttrs.AMD_CONFIG_FLAG));
		}
	}		
	
	/**
	 * Parse document script tag for all module identifiers listed as 
	 * application dependencies. Each discovered identifier will update the 
	 * global package directory with the absolute module identifier.
	 * 
	 * If there are problems retrieving script contents, ignore and return.
	 * 
	 * @param script - Document script tag
	 */
	protected void parsePostDojoScript(Element script) {
		String scriptContents = retrieveScriptContents(script);

		if (scriptContents != null) {
			List<String> moduleDependencies = analyseModuleDependencies(scriptContents);
			
			for(String moduleIdentifier: moduleDependencies) {
				 String absoluteModuleIdentifier = getAbsoluteModuleIdentifier(moduleIdentifier, script),
				 	packageName = getPackageIdentifier(absoluteModuleIdentifier);
				 			 
				 updateDiscoveredModules(packageName, absoluteModuleIdentifier);			 
			 }
		}
	}
	
	/**
	 * Return list of module dependencies within a JavaScript source
	 * contents. 
	 * 
	 * @param scriptContents - JavaScript source text to analyse
	 * @return List of discovered module dependencies
	 */
	protected List<String> analyseModuleDependencies(String scriptContents) {
		ScriptDependencyParser scriptParser = getScriptParser(scriptContents);		 
		return scriptParser.getModuleDependencies();		 
	}
	
	/**
	 * Add module identifier to the global dependencies state, 
	 * unless it is already present.
	 * 
	 * @param packageName - Package identifier for module
	 * @param moduleIdentifier - Absolute module identifier
	 */
	protected void updateDiscoveredModules(String packageName, String moduleIdentifier) {
		List<String> modules = getPackageModules(packageName);
		
		if (!modules.contains(moduleIdentifier)) {
			modules.add(moduleIdentifier);
		}		
	}
	
	/**
	 * Retrieve the list used to store discovered modules for a given package.
	 * If this package has no previously discovered modules, instantiate a new list. 
	 * 
	 * @param packageName - Package identifier
	 * @return List used to store discovered packages modules, will contain any previously
	 * discovered modules for this package.
	 */
	protected List<String> getPackageModules(String packageName) {
		List<String> modules = discoveredModules.get(packageName);
		
		if (modules == null) {
			modules = new ArrayList<String>();
			discoveredModules.put(packageName, modules);
		} 
		
		return modules;
	}
	
	/**
	 * Instantiate script parser for the JavaScript source contents given.
	 * Implementation of the script parsing will be dependent on the module
	 * format being used on the page. 
	 * 
	 * @param scriptContents - JavaScript source
	 * @return Parser for script dependencies
	 */
	protected ScriptDependencyParser getScriptParser(String scriptContents) {
		if (moduleFormat.equals(ModuleFormat.NON_AMD)) {
			return new NonAMDScriptParser(scriptContents);
		}
		
		return new AMDScriptParser(scriptContents);
	}

	/**
	 * Parse any script configuration values present in the script source.
	 * 
	 * @param scriptSource - JavaScript source text
	 * @return Lookup for configuration values, empty if no config found
	 */
	protected Map<String, Object> parseScriptConfiguration(String scriptSource) {
		ScriptConfigParser scriptConfigParser = new LoaderConfigParser(scriptSource); 
		return scriptConfigParser.getScriptConfig();
	}
	
	/**
	 * Extract available configuration passed as custom HTML attr on the script
	 * tag. May be either "data-dojo-config" or "djConfig".
	 * 
	 * @param script - Script element
	 * @return Custom configuration value, empty string otherwise.
	 */
	protected String extractDojoConfigAttrContents(Element script) {
		String dojoScriptConfigAttr = script.attr(DojoConfigAttrs.LOADER_CONFIG_DJCONFIG);
		
		if ("".equals(dojoScriptConfigAttr)) {
			dojoScriptConfigAttr = script.attr(DojoConfigAttrs.LOADER_CONFIG_DATA_DOJO_CONFIG);
		}
		
		return dojoScriptConfigAttr;
	}	
	
	/**
	 * Extract custom loader config wrapped within script tag attr and wrap
	 * within normal dojo config literal declaration.
	 * 
	 * @param script - Script source
	 * @return Dojo config literal declaration
	 */
	protected String constructConfigDeclarationFromScriptAttr(Element script) {
		String configLiteralContents = extractDojoConfigAttrContents(script);		
		return generateConfigDeclarationFromLiteral(configLiteralContents);
	}
	
	/**
	 * Wrap literal contents within outer configuration declaration.
	 * 
	 * @param literal - Object literal contents
	 * @return Dojo config declaration
	 */
	protected String generateConfigDeclarationFromLiteral(String literal) {				
		return String.format(DOJO_CONFIG_DECLARATION, literal);
	}
	
	/**
	 * Has the script tag containing the Dojo loader been
	 * discovered during parsing?
	 * 
	 * @return Dojo script has been discovered
	 */
	protected boolean hasFoundDojoScript() {
		return parsePhase.equals(ParsePhase.POST_DOJO);
	}
	
	/**
	 * Return all the document scripts within the web page.
	 * Empty list returned if document has none.
	 * 
	 * @return List of script tags
	 */
	protected Elements findAllScriptTags () {
		return this.document.getElementsByTag("script");		
	}		
	
	/**
	 * Ensure script parsing uses AMD module format parser rather than old style.
	 * 
	 * @param enabled - AMD module format enabled
	 */
	protected void enableAmdModuleFormat(boolean enabled) {
		moduleFormat = enabled ? ModuleFormat.AMD : ModuleFormat.NON_AMD;
	}
}