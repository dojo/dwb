package org.dtk.analysis.page;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.dtk.analysis.ModuleAnalysis;
import org.dtk.analysis.ModuleFormat;
import org.dtk.analysis.RecursiveModuleAnalysis;
import org.dtk.analysis.exceptions.FatalAnalysisError;
import org.dtk.analysis.exceptions.ModuleSourceNotAvailable;
import org.dtk.analysis.exceptions.UnknownModuleIdentifier;
import org.dtk.analysis.script.config.DojoConfigAttrs;
import org.dtk.analysis.script.config.LoaderConfigParser;
import org.dtk.analysis.script.config.ScriptConfigParser;
import org.dtk.analysis.script.dependency.AMDScriptParser;
import org.dtk.analysis.script.dependency.NonAMDScriptParser;
import org.dtk.analysis.script.dependency.ScriptDependencyParser;
import org.dtk.analysis.script.node.ObjectLiteral;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Base class for recursively analysing web pages for modules dependencies, discovered module
 * dependencies are retrieved and parsed. Each instance is initialised with a Document representing
 * the parsed HTML page and a remote location for that page. This is used to construct the remote 
 * paths locations for any discovered module dependencies.  
 * 
 * Once the Dojo loader has been discovered, each subsequent script tag is analysed for module dependencies. 
 * Each dependency is converted to a remote location and its source retrieved. This module source is then
 * recursively analysed for any dependencies specified. Module dependencies will be ignored if the containing
 * package is already set to "ignore" or we have previously seen this module dependency.
 * 
 * Retrieved module source is made available through the "getModuleSource" and "isModuleSourceAvailable" methods. 
 * 
 * @author James Thomas
 */

public abstract class RecursiveWebPage extends WebPage implements RecursiveModuleAnalysis {
	
	/**
	 * Collection of packages whose modules should not be recursively 
	 * retrieved. 
	 */
	protected Set<String> ignoredPackages = new HashSet<String>();
	
	/**
	 * Retrieved module source contents, look up by absolute module identifier.
	 */
	protected Map<String, String> moduleSource = new HashMap<String, String>();

	/**
	 * Loader configuration, resolved module paths discovered. 
	 */
	protected Map<String, String> modulePaths = new HashMap<String, String>();

	/**
	 * Configuration for URL path from current page location that module identifiers are 
	 * resolved relative to. 
	 */
	protected String baseUrl = null; 
	
	/**
	 * URL containing page location being analysed. Used to resolve linked
	 * resource paths.
	 */
	protected URL location = null;
	
	/**
	 * Static logging instance.
	 */
	protected static final Logger logger = Logger.getLogger(RecursiveWebPage.class.getName());	
	
	/**
	 * Default constructor, pass parsed HTML page and original location.
	 *  
	 * @param document - Parsed HTML page
	 * @param location - Page location
	 * @throws IOException - Unable to analyse HTML page
	 */	
	protected RecursiveWebPage(Document document, URL location) {
		super(document);		
		this.location = location;
		this.document.setBaseUri(location.toString());
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
	@Override
	protected void parsePostDojoScript(Element script) {
		String scriptContents = retrieveScriptContents(script);

		if (scriptContents != null) {
			recursivelyAnalyseScriptDependencies(scriptContents);
		}
	}
	
	/**
	 * Analyse module dependencies from a JavaScript source string. May be either 
	 * in AMD or non-AMD format. Any discovered modules, that haven't already been seen
	 * and don't belong to an ignored package, are recursively retrieved and analysed for
	 * their dependencies. 
	 * 
	 * @param scriptSource - JavaScript source text to analyse for module dependencies
	 */
	protected void recursivelyAnalyseScriptDependencies(String scriptSource) {
		List<String> moduleDependencies = analyseModuleDependencies(scriptSource);
		
		for(String moduleIdentifier: moduleDependencies) {
			String absoluteModuleIdentifier = getAbsoluteModuleIdentifier(moduleIdentifier),
				packageName = getPackageIdentifier(absoluteModuleIdentifier);

			if (shouldIncludeDiscoveredModule(packageName, absoluteModuleIdentifier)) {
				updateDiscoveredModules(packageName, absoluteModuleIdentifier);			 
	 			 
				if (shouldAnalyseForDependencies(packageName, absoluteModuleIdentifier)) {
					String moduleContents = retrieveModuleSource(absoluteModuleIdentifier);		
					if (moduleContents != null) {
						moduleSource.put(absoluteModuleIdentifier, moduleContents);
						recursivelyAnalyseScriptDependencies(moduleContents);
					}
				}
			 }			
		 }
	}
	
	/**
	 * Should the package module has its module dependencies analysed? 
	 * Ignore any modules for pre-specified packages or those we have already
	 * analysed.
	 * 
	 * @param packageName - Module package identifer
	 * @param absoluteModuleIdentifier - Absolute module identifier
	 * @return Module should be analysed for dependencies
	 */
	protected boolean shouldAnalyseForDependencies(String packageName, String absoluteModuleIdentifier) {
		try {
			return !isPackageIgnored(packageName) && !isModuleSourceAvailable(absoluteModuleIdentifier);
		} catch (UnknownModuleIdentifier e) {
			logger.warning(String.format("Unable to analyse module dependencies for unknown package & module identifier (%s) %s", 
				packageName, absoluteModuleIdentifier));
			return false;
		}
	}
	
	/**
	 * Override parent loader configuration to support parsing module paths.
	 * Used to calculate absolute module locations from relative identifiers and 
	 * find any custom base URL parameters set.
	 * 
	 * @param parsedScriptConfig - Converted JavaScript object value containing loader config.
	 */
	@Override	
	protected void updateInternalLoaderConfig(Map<String, Object> parsedScriptConfig) {
		super.updateInternalLoaderConfig(parsedScriptConfig);
		
		if (parsedScriptConfig.containsKey(DojoConfigAttrs.MODULE_PATHS_CONFIG_FLAG)) {
			updateInternalPathsConfig((ObjectLiteral) parsedScriptConfig.get(DojoConfigAttrs.MODULE_PATHS_CONFIG_FLAG));
		}
		
		if (parsedScriptConfig.containsKey(DojoConfigAttrs.PATHS_CONFIG_FLAG)) {
			updateInternalPathsConfig((ObjectLiteral) parsedScriptConfig.get(DojoConfigAttrs.PATHS_CONFIG_FLAG));
		}
		
		if (parsedScriptConfig.containsKey(DojoConfigAttrs.BASE_URL_CONFIG_FLAG)) {
			baseUrl = (String) parsedScriptConfig.get(DojoConfigAttrs.BASE_URL_CONFIG_FLAG);
		}
	}
	
	/**
	 * Update the internal loader configuration paths for each value found. 
	 * Must be a string value. 
	 * 
	 * @param paths - Module paths
	 */
	protected void updateInternalPathsConfig(ObjectLiteral paths) {
		for(String packageName: paths.getKeys()) {
			Object packagePath = paths.getValue(packageName);
			if (packagePath instanceof String) {
				modulePaths.put(packageName, (String) packagePath);
			}
		}
	}
	
	/**
	 * Is this package being ignored for recursively module analysis?
	 * 
	 * @param packageIdentifier - Global package identifier
	 * @return Package is ignored
	 */
	protected boolean isPackageIgnored(String packageIdentifier) {
		return ignoredPackages.contains(packageIdentifier);
	}

	/**
	 * Have we seen this module identifier during analysis? 
	 *  
	 * @param packageName - Package identifier
	 * @param moduleIdentifier - Module identifier
	 * @return Module discovered during analysis
	 */
	protected boolean hasSeenModuleIdentifier(String packageName, String moduleIdentifier) {
		List<String> packageModules = discoveredModules.get(packageName);
		return packageModules != null && packageModules.contains(moduleIdentifier);				
	}
	
	/**
	 * Retrieve and return associated module source for an identifier.
	 * Implementation responsible for details of where and how module
	 * source is available. 
	 * 
	 * @param moduleIdentifier - Absolute module identifier
	 * @return Module source 
	 */
	abstract protected String retrieveModuleSource(String moduleIdentifier);	
	
	/**
	 * Return module source that was parsed during recursive analysis.
	 * 
	 * @param moduleIdentifier - Module identifier for source
	 * @return Module's definition source
	 * @throws ModuleSourceNotAvailable - Module identifier belongs to a package that had
	 * recursive parsing turned off
	 * @throws UnknownModuleIdentifier - Module identifier provided wasn't discovered during analysis 
	 */
	public String getModuleSource(String moduleIdentifier) throws ModuleSourceNotAvailable, UnknownModuleIdentifier {
		String packageName = getPackageIdentifier(moduleIdentifier);
		
		if (!hasSeenModuleIdentifier(packageName, moduleIdentifier)) {
			throw new UnknownModuleIdentifier();
		} else if (!moduleSource.containsKey(moduleIdentifier)) {
			throw new ModuleSourceNotAvailable();
		}
		
		return moduleSource.get(moduleIdentifier);
	}
	
	/**
	 * Has the referenced module being recursively analysed during parsing? If so, 
	 * the module source will be available for reading.
	 * 
	 * @param moduleIdentifier - Module identifier discovered during analysis
	 * @return Module source contents
	 * @throws UnknownModuleIdentifier - Module identifier wasn't discovered during analysis
	 */
	public boolean isModuleSourceAvailable(String moduleIdentifier) throws UnknownModuleIdentifier {
		String packageName = getPackageIdentifier(moduleIdentifier);
		
		if (!hasSeenModuleIdentifier(packageName, moduleIdentifier)) {
			throw new UnknownModuleIdentifier();
		}
		
		return moduleSource.containsKey(moduleIdentifier);
	}	
	
	/**
	 * Set a list of explicit package identifiers whose modules shouldn't 
	 * be recursively parsing during analysis. These modules will be identified 
	 * but source files won't be pulled down and analysed for module dependencies.
	 * 
	 * @param packagesToIgnore - Package identifiers
	 */
	public void setIgnoredPackages(Set<String> packagesToIgnore) {
		this.ignoredPackages = packagesToIgnore;
	}
	
	/**
	 * Retrieve the set of package identifiers whose modules will be ignored
	 * during recursively module analysis.  These modules will be identified 
	 * but source files won't be pulled down and analysed for module dependencies.
	 * 
	 * @return List of package identifiers
	 */
	public Set<String> getIgnoredPackages() {
		return ignoredPackages;		
	}
}