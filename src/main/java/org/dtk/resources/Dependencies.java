package org.dtk.resources;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.wink.common.model.multipart.BufferedInMultiPart;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.dtk.analysis.ModuleAnalysis;
import org.dtk.analysis.ModuleFormat;
import org.dtk.analysis.RecursiveModuleAnalysis;
import org.dtk.analysis.exceptions.FatalAnalysisError;
import org.dtk.analysis.exceptions.ModuleSourceNotAvailable;
import org.dtk.analysis.exceptions.UnknownModuleIdentifier;
import org.dtk.analysis.page.LocalWebPage;
import org.dtk.analysis.page.RemoteWebPage;
import org.dtk.resources.dependencies.DependenciesResponse;
import org.dtk.resources.dependencies.DojoScriptVersions;
import org.dtk.resources.dependencies.ExplicitModuleFormatAnalysisDependenciesResponse;
import org.dtk.resources.dependencies.InputType;
import org.dtk.resources.exceptions.ConfigurationException;
import org.dtk.resources.exceptions.IncorrectParameterException;
import org.dtk.resources.packages.PackageRepository;
import org.dtk.util.FileUtil;
import org.dtk.util.HttpUtil;
import org.dtk.util.JsonUtil;
import org.jsoup.Jsoup;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * RESTful Dependencies API. This API provides access to Dojo module discovery 
 * functionality, allowing automatic analysis of a variety of input sources 
 * (html pages, URLs, build profiles) for Dojo module dependencies. When input types
 * have custom modules, that are accessible, these will be automatically converted 
 * into temporary packages on the server.
 * 
 * @author James Thomas
 */

@Path("/dependencies")
public class Dependencies {
	/** Request parameter containing the input type */
	protected static final String typeParameter = "type";

	/** Request parameter containing the input type */
	protected static final String valueParameter = "value";

	/** Response messages **/
	/** Error text when module analysis uses missing or invalid source type. */
	protected static final String invalidTypeErrorText = "Request contains an invalid value for the " +
	"\"type\" parameter. Valid values for this mandatory parameter include \"web_page\", \"url\" or \"profile\"";

	/** Error text when module analysis uses missing or invalid source type. */
	protected static final String invalidValueErrorText = "Request contains an invalid value for the " +
	"\"value\" parameter. Parameter value is either missing or empty.";

	/** Error text when module analysis uses missing or invalid source type. */
	protected static final String parsingFailureErrorText = "Unable to parse source web page for module analysis. " +
	"Check request parameter value.";

	/** Error text when module analysis uses missing or invalid source type. */
	protected static final String incorrectUrlErrorText = "Unable to parse URL parameter for remote Web Application. " +
	"Check format of the request parameter value";

	/** Error text when json processing class throws parsing error. */
	protected static final String internalServerErrorText = "Internal error processing module dependencies. Please try again.";

	/** Evaluator exception thrown parsing JavaScript build profile */
	protected static final String buildProfileParseErrorText = "Error parsing JavaScript build profile. Check parameter source.";
	
	/** Log messages **/
	/** Unable to generate and save JSON encoded HTML **/
	protected static final String errorGeneratingJsonLogMsg = "Exception caught generating HTML encoded JSON for input " +
		"type, %1$s, and input value, %2$s.";
	
	/** New dependency request details **/
	protected static final String analyseDependenciesLogMsg = "New analyse dependencies request for input type, %1$s, and input value, %2$s";
	
	/** Unable to parse build profile details **/
	protected static final String buildProfileParseLogMsg = "Unable to parse the following build profile submitted, %1$s.";
	
	/** WebPage class threw error parsing source material. Log root exception **/
	protected static final String failedWebPageParseLogMsg = "Fatal error parsing user submitted web page for dependencies, root exception: %1$s";
	
	/** Information log about modules and package discovered **/
	protected static final String webPageParseLogMsg = "Web page analysis discovered %1$s modules and created temporary package (%2$s) containing %3$s";
	
	/** Listener logging class */
	protected static Logger logger = Logger.getLogger(Dependencies.class.getName());
	
	/**
	 * Analyse source input for Dojo module dependencies. Allows a request
	 * to provide a html page, remote URL, build profile, which will be parsed
	 * and analysed to extract the dojo module dependencies. Response contains 
	 * discovered modules and any temporary module packages discovered. 
	 * 
	 * @param dependencyDetails - Source input details.
	 * @return Dependency analysis results, JSON encoded.
	 */
	@POST 
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_HTML)
	public String analyseDependencies(BufferedInMultiPart multiPartForm) {
		logger.entering(this.getClass().getName(), "analyseDependencies");

		String encodedJson = null;
		
		ModuleAnalysis moduleAnalysis = null;
		
		// Process form submission, parsing text strings from values.
		MultivaluedMap<String, Object> formFields = HttpUtil.retrieveMultiPartFormValues(multiPartForm, String.class);

		// Check request contains valid input parameters, value and type. 
		InputType inputType = retrieveInputType(formFields); 
		String inputValue = retrieveInputValue(formFields);
		
		logger.log(Level.INFO, String.format(analyseDependenciesLogMsg, inputType.name(), inputValue));
		
		// Invoke analysis based upon input type (web_page, url or profile).
		switch(inputType) {
		case WEB_PAGE:
			moduleAnalysis = analyseModulesFromWebpage(inputValue);
			break;
		case URL: 
			moduleAnalysis = analyseModulesFromUrl(inputValue);
			break;
		}

		// Convert Java Map to Json object and then encode inside HTML.
		// If parsing errors are thrown, return an internal server error
		// HTTP response.
		try {						
			DependenciesResponse response;
			
			if (moduleAnalysis instanceof RecursiveModuleAnalysis) {
				String customPackageIdentifier = createTemporaryPackageForRetrievedSource((RecursiveModuleAnalysis) moduleAnalysis);
				response = new ExplicitModuleFormatAnalysisDependenciesResponse(moduleAnalysis, 
					customPackageIdentifier, ModuleFormat.NON_AMD);
			} else {
				response = new ExplicitModuleFormatAnalysisDependenciesResponse(moduleAnalysis, ModuleFormat.NON_AMD);	
			}
			
			encodedJson = JsonUtil.writeJavaToHtmlEncodedJson(response);
		} catch (FatalAnalysisError e) {
			logger.log(Level.SEVERE, String.format(errorGeneratingJsonLogMsg, inputType.name(), inputValue));					
			throw new ConfigurationException(internalServerErrorText);
		} catch (JsonParseException e) {
			logger.log(Level.SEVERE, String.format(errorGeneratingJsonLogMsg, inputType.name(), inputValue));					
			throw new ConfigurationException(internalServerErrorText);
		} catch (JsonMappingException e) {
			logger.log(Level.SEVERE, String.format(errorGeneratingJsonLogMsg, inputType.name(), inputValue));					
			throw new ConfigurationException(internalServerErrorText);
		} catch (IOException e) {
			logger.log(Level.SEVERE, String.format(errorGeneratingJsonLogMsg, inputType.name(), inputValue));					
			throw new ConfigurationException(internalServerErrorText);			
		} catch (UnknownModuleIdentifier e) {
			logger.log(Level.SEVERE, String.format(errorGeneratingJsonLogMsg, inputType.name(), inputValue));					
			throw new ConfigurationException(internalServerErrorText);		
		} catch (ModuleSourceNotAvailable e) {
			logger.log(Level.SEVERE, String.format(errorGeneratingJsonLogMsg, inputType.name(), inputValue));					
			throw new ConfigurationException(internalServerErrorText);		
		}

		logger.exiting(this.getClass().getName(), "analyseDependencies");
		
		// Encoded response and return 
		return encodedJson;
	}

	protected String createTemporaryPackageForRetrievedSource(RecursiveModuleAnalysis analysis) 
	throws FatalAnalysisError, UnknownModuleIdentifier, ModuleSourceNotAvailable {
		String packageIdentifier = null;
		
		Set<String> ignoredPackages = analysis.getIgnoredPackages();
		
		Map<String, String> customModules = new HashMap<String, String>();
		
		for (Entry<String, List<String>> packageAndModules: analysis.getModules().entrySet()) {
			String packageName = packageAndModules.getKey();
			List<String> packageModules = packageAndModules.getValue();
			
			if (!ignoredPackages.contains(packageName)) {
				for(String moduleIdentifier: packageModules) {
					if (analysis.isModuleSourceAvailable(moduleIdentifier)) {
						customModules.put(moduleIdentifier, analysis.getModuleSource(moduleIdentifier));
					}
				}
			}
		}
		
		if (customModules.keySet().size() > 0) {
			packageIdentifier = createTemporaryPackage(customModules);	
		}
		
		return packageIdentifier;		
	}	
	
	/**
	 * Analyse remote web application for Dojo module dependencies. Web crawler
	 * will analyse the site, retrieving all HTML and JavaScript source files
	 * referenced. Each source file will be parsed and analysed to discover Dojo
	 * module dependencies.
	 * 
	 * @param webPage - Instance of web page 
	 * @return Modules discovered and temporary packages
	 * @throws IncorrectParameterException
	 */
	protected RecursiveModuleAnalysis analyseModulesFromUrl(String textUrl)  {
		URL url;
		try {
			// Prefix http protocol when URL is missing a protocol identifier.
			if (!textUrl.startsWith("http")) {
				textUrl = "http://" + textUrl;
			}
			url = new URL(textUrl);
			RecursiveModuleAnalysis remotePage = new RemoteWebPage(Jsoup.connect(url.toString()).get(), url, new DefaultHttpClient(), new HashSet<String>() {{
				add("dojo");
				add("dojox");
				add("dijit");
			}});
			
			return remotePage;
		} catch (MalformedURLException e) {
			throw new IncorrectParameterException(incorrectUrlErrorText);
		} catch (IOException e) {
			throw new IncorrectParameterException(incorrectUrlErrorText);
		} catch (IllegalArgumentException e) {
            throw new IncorrectParameterException(incorrectUrlErrorText);
		}		
	}

	/**
	 * Analyse HTML content string for Dojo module dependencies. Content will
	 * be parsed and searched for JavaScript scripts containing Dojo references.
	 * 
	 * @param webPage - Instance of web page 
	 * @return Modules discovered and temporary packages
	 * @throws IncorrectParameterException
	 */
	protected ModuleAnalysis analyseModulesFromWebpage(String htmlContent)  {
		org.dtk.analysis.page.WebPage webPage = new LocalWebPage(htmlContent);
				
		return webPage;
	}

	/**
	 * Analyse WebPage instance for Dojo modules, creating temporary packages
	 * where needed.
	 * 
	 * @param webPage - Instance of web page 
	 * @return Modules discovered and temporary packages
	 * @throws IncorrectParameterException
	 */
	protected Map<String, Object> analyseModulesFromWebpage(RecursiveModuleAnalysis webPage) 
	throws IncorrectParameterException {
		
		Map<String, Object> moduleAnalysis = new HashMap<String, Object>();
		List<String> requiredDojoModules = new ArrayList<String>();
		List<String> customModuleNames = new ArrayList<String>();
		Map<String, String> customModules = new HashMap<String, String>();
		Map<String, String> packageDetails = new HashMap<String, String>();
		List<Map<String, String>> temporaryPackages = new ArrayList<Map<String, String>>();
		
		try {
			Map<String, List<String>> pageModules = webPage.getModules();
			Set<String> packageNames = pageModules.keySet();
			Set<String> DTK = new HashSet<String>() {{
				add("dojo"); add("dijit"); add("dojox");
			}};
			
			for(String packageName: packageNames) {
				List<String> packageModules = pageModules.get(packageName);
				if (packageModules != null) {
					if (DTK.contains(packageName)) {
						requiredDojoModules.addAll(packageModules);
					} else {
						customModuleNames.addAll(packageModules);
						for(String moduleName: packageModules) {
							customModules.put(moduleName, webPage.getModuleSource(moduleName));
						}
					}					
				}				
			}
			
			String temporaryPackageId = createTemporaryPackage(customModules);
			// Turn discovered custom modules into a new temporary package, allowing reference 
			// when building. 		
			packageDetails.put("name", temporaryPackageId);
			
			// Temporary package details, use arbitrary version for temporary packages.
			packageDetails.put("version", "1.0.0");

			temporaryPackages.add(packageDetails);

			logger.log(Level.INFO, String.format(webPageParseLogMsg, webPage.getModules().size(), temporaryPackageId, customModuleNames.size()));
			
		} catch (FatalAnalysisError e) {
			logger.log(Level.SEVERE, String.format(failedWebPageParseLogMsg, e));
			throw new IncorrectParameterException(parsingFailureErrorText);
		} catch (ModuleSourceNotAvailable e) {
			logger.log(Level.SEVERE, String.format(failedWebPageParseLogMsg, e));
			throw new IncorrectParameterException(parsingFailureErrorText);
		} catch (UnknownModuleIdentifier e) {
			logger.log(Level.SEVERE, String.format(failedWebPageParseLogMsg, e));
			throw new IncorrectParameterException(parsingFailureErrorText);
		}
		
				
		moduleAnalysis.put("requiredDojoModules", requiredDojoModules);
		
		// Custom module list stored in temporary package
		moduleAnalysis.put("availableModules", customModuleNames);
		
		// Store temporary package references into response
		moduleAnalysis.put("packages", temporaryPackages);
		
		moduleAnalysis.put("dojoVersion", DojoScriptVersions.Versions.UNKNOWN);
		
		
		return moduleAnalysis;					
	}

	/**
	 * Create a new temporary package, writing the module contents to a new
	 * package location in the package store. Unique package reference for
	 * the new temporary package is returned.
	 * 
	 * @param modules - Map of module names and module contents
	 * @return Temporary package references and prefixes
	 */
	protected String createTemporaryPackage(Map<String, String> modules) {
		Map<String, String> packageFileContents = new HashMap<String, String>();
		
		for(String moduleName: modules.keySet()) {
			// Convert module name to relative package file path
			String modulePathInPackage = convertModuleNameToPath(moduleName);
			packageFileContents.put(modulePathInPackage, modules.get(moduleName));
		}
		
		// Create new package from relative file paths and module contents
		String packageIdentifier = FileUtil.createTemporaryPackage(packageFileContents);

		// Add identifier to package repo
		PackageRepository.getInstance().addTemporaryPackageReference(packageIdentifier);
		
		return packageIdentifier;
	}

	/**
	 * Convert module name into a file name relative to the package
	 * root.
	 * 
	 * @param moduleName - Module name
	 * @return Relative file path for this module in a package
	 */
	protected String convertModuleNameToPath(String moduleName) {
		String modulePath = StringUtils.join(moduleName.split("\\."), '/') + ".js";
		return modulePath;
	}

	/**
	 * Pull input type parameter from request content. Exception 
	 * throw if value doesn't match pre-specified type.
	 * 
	 * @param request
	 * @return
	 * @throws IncorrectParameterException
	 */
	protected InputType retrieveInputType(MultivaluedMap<String, Object> request) 
	throws IncorrectParameterException {
		InputType type;

		try {
			String typeStr = (String) request.getFirst(typeParameter);
			type = InputType.valueOf(typeStr.toUpperCase());
		} catch (NullPointerException npe) {
			throw new IncorrectParameterException(invalidTypeErrorText);
		} catch (IllegalArgumentException iae) {
			throw new IncorrectParameterException(invalidTypeErrorText);
		}
		return type;
	}

	/** 
	 * Pull value parameter from request content. Exception
	 * thrown if null or empty string.
	 * 
	 * @param request - Request object
	 * @return Input value content.
	 * @throws IncorrectParameterException - Null or empty request value.
	 */
	protected String retrieveInputValue(MultivaluedMap<String, Object> request) 
	throws IncorrectParameterException {
		String value = (String) request.getFirst(valueParameter);

		if (value == null || value.isEmpty()) {
			throw new IncorrectParameterException(invalidValueErrorText);
		}

		return value;
	}
}
