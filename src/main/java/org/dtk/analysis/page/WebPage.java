package org.dtk.analysis.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.ParseException;
import org.dtk.analysis.ModuleAnalysis;
import org.dtk.analysis.ModuleFormat;
import org.dtk.analysis.exceptions.FatalAnalysisError;
import org.dtk.analysis.script.AMDScriptParser;
import org.dtk.analysis.script.NonAMDScriptParser;
import org.dtk.analysis.script.ScriptDependencyParser;
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
 * retrieve and scanned for module dependencies contained within. 
 * 
 * All module dependencies discovered are maintained into an internal Map, arranging 
 * by package.
 * 
 * @author james
 *
 */

public abstract class WebPage implements ModuleAnalysis {
	
	protected Document document;
	
	protected Map<String, List<String>> discoveredModules = new HashMap<String, List <String>>();
	
	protected ParsePhase parsePhase = ParsePhase.PRE_DOJO;
	protected ModuleFormat moduleFormat = ModuleFormat.NON_AMD;
	
	public WebPage(Document document) {
		this.document = document;
		parse();
	}
	
	@Override
	public Map<String, List<String>> getModules() throws FatalAnalysisError {
		return discoveredModules;
	}
	
	protected void parse() {
		Elements scriptTags = findAllScriptTags();

		for (Element scriptTag: scriptTags) {
			// Before we've found Dojo, just look for djConfig 
			// variable and actual Dojo script. 
			if (!hasFoundDojoScript()) {
				parsePreDojoScript(scriptTag);
			} else {
				parsePostDojoScript(scriptTag);
			}
		}		
	}
	
	protected void parsePreDojoScript(Element script) {
		if (isDojoScript(script)) {
			parsePhase = ParsePhase.POST_DOJO;
		}			
	}
	
	abstract protected boolean isDojoScript(Element script);
		
	/**
	 * Extract complete script contents and search through
	 * for any dojo.require calls. 
	 * 
	 * @param script - <script> tag element 
	 * @throws ParseException - Error parsing this script 
	 * @throws IOException - Error retrieving this element
	 */
	protected void parsePostDojoScript(Element script) {
		String scriptContents = retrieveScriptContents(script);

		// If there was a problem retrieving this script source, don't try
		// to parse result.
		if (scriptContents != null) {
			List<String> moduleDependencies = analyseModuleDependencies(scriptContents);
			
			for(String moduleIdentifier: moduleDependencies) {
				 String absoluteModuleIdentifier = getAbsoluteModuleIdentifer(moduleIdentifier, script),
				 	packageName = getPackageIdentifier(absoluteModuleIdentifier);
				 			 
				 updateDiscoveredModules(packageName, absoluteModuleIdentifier);			 
			 }
		}
	}
	
	abstract protected String getAbsoluteModuleIdentifer(String moduleIdentifer, Element script);	
	
	abstract protected String getPackageIdentifier(String moduleIdentifer);
	
	abstract protected String retrieveScriptContents(Element script);
	
	protected List<String> analyseModuleDependencies(String scriptContents) {
		ScriptDependencyParser scriptParser = getScriptParser(scriptContents);		 
		return scriptParser.getModuleDependencies();		 
	}
	
	protected void updateDiscoveredModules(String packageName, String moduleIdentifier) {
		List<String> modules = getPackageModules(packageName);
		
		if (!modules.contains(moduleIdentifier)) {
			modules.add(moduleIdentifier);
		}		
	}
	
	
	
	protected List<String> getPackageModules(String packageName) {
		List<String> modules = discoveredModules.get(packageName);
		
		if (modules == null) {
			modules = new ArrayList<String>();
			discoveredModules.put(packageName, modules);
		} 
		
		return modules;
	}
	
	protected ScriptDependencyParser getScriptParser(String scriptContents) {
		if (moduleFormat.equals(ModuleFormat.NON_AMD)) {
			return new NonAMDScriptParser(scriptContents);
		}
		
		return new AMDScriptParser(scriptContents);
	}

	protected boolean hasFoundDojoScript() {
		return parsePhase.equals(ParsePhase.POST_DOJO);
	}
	
	protected Elements findAllScriptTags () {
		return this.document.getElementsByTag("script");
	}
}
