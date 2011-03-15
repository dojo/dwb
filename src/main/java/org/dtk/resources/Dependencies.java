package org.dtk.resources;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.StringUtils;
import org.apache.wink.common.model.multipart.BufferedInMultiPart;
import org.dtk.resources.dependencies.InputType;
import org.dtk.resources.dependencies.WebPage;
import org.dtk.resources.exceptions.ConfigurationException;
import org.dtk.resources.exceptions.IncorrectParameterException;
import org.dtk.util.ContextListener;
import org.dtk.util.FileUtil;
import org.dtk.util.HttpUtil;
import org.dtk.util.JsonUtil;
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
	protected static final String webPageParseLogMsg = "Web page analysis discovered %1$s modules and created %2$s temporary packages";
	
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
		Map<String, Object> dependencies = null;
		
		// Process form submission, parsing text strings from values.
		MultivaluedMap<String, Object> formFields = HttpUtil.retrieveMultiPartFormValues(multiPartForm, String.class);

		// Check request contains valid input parameters, value and type. 
		InputType inputType = retrieveInputType(formFields); 
		String inputValue = retrieveInputValue(formFields);
		
		logger.log(Level.INFO, String.format(analyseDependenciesLogMsg, inputType.name(), inputValue));
		
		// Invoke analysis based upon input type (web_page, url or profile).
		switch(inputType) {
		case WEB_PAGE:
			dependencies = analyseModulesFromWebpage(inputValue);
			break;
		case PROFILE:
			dependencies = extractLayersFromProfile(inputValue);
			break;
		case URL: 
			dependencies = analyseModulesFromUrl(inputValue);
			break;
		}

		// Convert Java Map to Json object and then encode inside HTML.
		// If parsing errors are thrown, return an internal server error
		// HTTP response.
		try {
			encodedJson = JsonUtil.writeJavaToHtmlEncodedJson(dependencies);
		} catch(IOException e) {
			logger.log(Level.SEVERE, String.format(errorGeneratingJsonLogMsg, inputType.name(), inputValue));					
			throw new ConfigurationException(internalServerErrorText);
		}

		logger.exiting(this.getClass().getName(), "analyseDependencies");
		
		// Encoded response and return 
		return encodedJson;
	}

	/**
	 * Extract layer information from an existing build profile.
	 * The profile is evaluated in a JS engine and result interrogated. 
	 * 
	 * @param buildProfile - Dojo build profile text
	 * @return Layers details for a given build profile
	 */
	protected Map<String, Object> extractLayersFromProfile(String buildProfile) throws IncorrectParameterException {
		Map<String, Object> moduleAnalysis = new HashMap<String, Object>();

		try {
			// Create new JS evaluator context 
			org.mozilla.javascript.Context cx = ContextFactory.getGlobal().enterContext();
			Scriptable scope = cx.initStandardObjects();			       
			
			// Return dependency layers JS object as the result of script evaluation
			NativeArray result = (NativeArray) cx.evaluateString(scope, buildProfile + "; dependencies.layers", "profile.js", 1, null);
			
			// For each dependency layer, convert details to Java context
			// and return.
			List<Object> profileLayers = new ArrayList<Object>();

			for (Object o : result.getIds()) {
				Map<String, Object> layerDetails = extractLayerDetails(result, (Integer) o);
				profileLayers.add(layerDetails);
			}	        

			moduleAnalysis.put("layers", profileLayers);
		// Caught error from JS engine parsing source file, return to user as 400 status. 
		} catch (EvaluatorException e) {
			logger.log(Level.SEVERE, String.format(buildProfileParseLogMsg, buildProfile));
			throw new IncorrectParameterException(buildProfileParseErrorText);
		}
		
		return moduleAnalysis;
	}

	/** 
	 * Convert an object at given index point to Java instance.
	 * 
	 * @param layers - Collection of JS objects
	 * @param index - Index of element to extract from
	 * @return JS object details from result
	 */
	protected Map<String, Object> extractLayerDetails(NativeArray layers, int index) {
		Map<String, Object> details = new HashMap<String, Object>();

		// Get layer name from JS object 
		ScriptableObject layer = (ScriptableObject) layers.get(index, null);
		String layerName = (String) layer.get("name", null);
		details.put("name", layerName);

		// Pull all dependencies from layer details
		NativeArray nativeDependencies = (NativeArray) layer.get("dependencies", null);
		List<String> dependencies = new ArrayList<String>();	            
		for (Object idx : nativeDependencies.getIds()) {   
			int nidx = (Integer) idx;
			String moduleName = (String) nativeDependencies.get(nidx, null);
			dependencies.add(moduleName);
		}
		details.put("dependencies", dependencies);

		return details;
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
	protected Map<String, Object> analyseModulesFromUrl(String textUrl)  {
		URL url;
		try {
			// Prefix http protocol when URL is missing a protocol identifier.
			if (!textUrl.startsWith("http")) {
				textUrl = "http://" + textUrl;
			}
			url = new URL(textUrl);
			WebPage webPage = new WebPage(url);
			return analyseModulesFromWebpage(webPage);
		} catch (MalformedURLException e) {
			throw new IncorrectParameterException(incorrectUrlErrorText);
		} catch (IOException e) {
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
	protected Map<String, Object> analyseModulesFromWebpage(String htmlContent)  {
		WebPage webPage = new WebPage(htmlContent);
		return analyseModulesFromWebpage(webPage);
	}

	/**
	 * Analyse WebPage instance for Dojo modules, creating temporary packages
	 * where needed.
	 * 
	 * @param webPage - Instance of web page 
	 * @return Modules discovered and temporary packages
	 * @throws IncorrectParameterException
	 */
	protected Map<String, Object> analyseModulesFromWebpage(WebPage webPage) 
	throws IncorrectParameterException {
		Map<String, Object> moduleAnalysis = new HashMap<String, Object>();

		// If web page fails to parse, invalid source or unable to contact 
		// remote resources, flag to user as an error.
		if (!webPage.parse()) {
			logger.log(Level.SEVERE, String.format(failedWebPageParseLogMsg, webPage.getParsingFailure()));
			throw new IncorrectParameterException(parsingFailureErrorText);
		} 

		List<String> modules = webPage.getModules();
		// Store list of discovered modules within repsonse map.
		moduleAnalysis.put("requiredDojoModules", modules);

		// Turn discovered custom modules into temporary packages, allowing reference 
		// when building. 
		Map<String, String> temporaryPackages = createTemporaryPackages(webPage.getCustomModuleContent());
		// Store temporary package references into response
		moduleAnalysis.put("packages", temporaryPackages);

		logger.log(Level.INFO, String.format(webPageParseLogMsg, modules.size(), temporaryPackages.size()));
		
		return moduleAnalysis;
	}

	/**
	 * Write a set of temporary package details to the filesystem, returning 
	 * collection of create package references for each element. 
	 * 
	 * @param modules - Map of module names and module contents
	 * @return Temporary package references and prefixes
	 */
	protected Map<String, String> createTemporaryPackages(Map<String, String> modules) {
		Map<String, String> packageResourceInfo = new HashMap<String, String>();

		Map<String, Map<String, String>> moduleRepository = constructModuleRepository(modules);

		for(String modulePrefix : moduleRepository.keySet()) {
			// Create temporary directory
			String packageIdentifier = FileUtil.createTemporaryPackage(moduleRepository.get(modulePrefix));

			if (packageIdentifier != null) {
				packageResourceInfo.put(modulePrefix, packageIdentifier);	
			}
		}

		return packageResourceInfo;
	}

	/**
	 * Process module list into sublists stored in a map, indexed on module prefix.
	 * Each prefix collection with contain converted local paths to module
	 * and the full module name.  
	 * 
	 * @param modules - List of required modules
	 * @return Map collection, containing sublists of modules indexed againts module prefix
	 */
	protected Map<String, Map<String, String>> constructModuleRepository(Map<String, String> modules) {
		// TODO: This should be retrieved as a tree data structure straight from the web page.
		Map<String, Map<String, String>> packageRepository = new HashMap<String, Map<String, String>>();

		// Convert individual modules into a prefix oriented resource tree. 
		for (String moduleName : modules.keySet()) {
			String[] moduleParts = moduleName.split("\\.");
			String modulePrefix = moduleParts[0]; 

			if (!packageRepository.containsKey(modulePrefix)) {
				packageRepository.put(modulePrefix, new HashMap<String, String>());
			}

			Map<String, String> packageModules = packageRepository.get(modulePrefix);

			// Convert remaining module name items back into a module path, missing 
			// initial prefix. 
			String modulePath = StringUtils.join(moduleParts, '/') + ".js";

			// Store file path to store module at and contents
			packageModules.put(modulePath, modules.get(moduleName));
		}

		return packageRepository;
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
