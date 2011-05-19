package org.dtk.resources;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.http.HttpStatus;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.dtk.resources.build.BuildRequest;
import org.dtk.resources.build.manager.BuildState;
import org.dtk.resources.build.manager.BuildStatusManager;
import org.dtk.resources.exceptions.ConfigurationException;
import org.dtk.resources.exceptions.IncorrectParameterException;
import org.dtk.resources.exceptions.MissingResourceException;
import org.dtk.resources.packages.PackageRepository;
import org.dtk.util.FileUtil;
import org.dtk.util.HttpUtil;
import org.dtk.util.JsonUtil;

/**
 * RESTful Build API. This API provides access to the package build system, 
 * allowing a user to generate custom builds by specifying a selection of 
 * modules to combine into distinct layers. The compiler build options can
 * be specified here, including cross-domain builds and optimisation levels. 
 * 
 * @author James Thomas
 */

@Path("/build")
public class Build {
	@Context ServletConfig config;

	protected static final String tempBuildPrefix = "dojo";

	protected static final String tempBuildSuffix = ".temp";

	/** Base Dojo layer, must be present in any build request */
	protected static final Map<String, Object> defaultDojoLayer = new HashMap<String, Object> () {{
		put("name", "dojo.js");
		put("modules", new ArrayList<String>());
	}};
	
	/** Response messages **/
	/** Error text when build request misses mandatory parameter */
	protected static final String missingParameterErrorText 
	= "Missing mandatory parameter, %1$s, from build request.";

	/** Error text when build request misses dojo package details */
	protected static final String invalidPackageErrorText 
	= "Invalid package reference, %1$s, in the build request.";
	
	/** Error text when build request misses dojo package details */
	protected static final String invalidModulePackageErrorText 
	= "Invalid package reference for module, %1$s, in the build request.";
	
	/** Error text when build request misses mandatory parameter */
	protected static final String urlConstructionErrorText 
	= "Internal server error generating absolute resource path. Please try again.";
	
	/** Error text when json mapper fails converting layers parameters */
	protected static final String layersParsingErrorText 
	= "Unable to parse layers parameter, check request contents.";
	
	/** Error text when server doesn't have SHA-1 hash algo available */
	protected static final String missingAlgorithmErrorText 
	= "Failed to create build request, issues accessing hashing algorithm SHA-1";
	
	/** Error text when user has requested a build result that isn't available */
	protected static final String missingBuildResourceErrorText 
	= "Unable to access build result, build process hasn't completed.";
	
	/** Log messages **/
	/** We have successfully parsed a user's new build request */
	protected static final String newBuildRequestLogMsg 
	= "New build request submitted using request object: %1$s";

	/** Fatal error caught trying to process user's build request. Log all details. **/
	protected static final String fatalBuildRequestLogMsg 
	= "Fatal error occurred processing the following build request, %1$s. " +
		"The following exceptions was caught: %2$s";	
	
	/** Logging class instance */
	protected static final Logger logger = Logger.getLogger(Build.class.getName());
	
	/** Access to the package repository */
	protected PackageRepository repo = PackageRepository.getInstance();		
	
	/**
	 * Initiate a build request, passing parameters to the Dojo build system, generating
	 * a compressed version of the requested JavaScript layers. User will have a link
	 * to the status polling API returned, which allows them to verify a build has finished
	 * and download the result. Incorrect and/or missing parameters will generate appropriate
	 * JavaScript error response
	 * 
	 * @param request - Http request details
	 * @param buildDetails - Json object request details.
	 * @return Build result details, contains link to status checking resource.
	 */
	@POST 
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response  generateBuild(@Context HttpServletRequest request, HashMap<String, Object> buildDetails) {
		logger.entering(this.getClass().getName(), "generateBuild");
		
		// We need to generate a non-standard HTTP response, 201.
		Response accepted = null;
		
		BuildRequest buildRequest = generateNewBuildRequest(buildDetails);
		
		// Schedule build request with status manager 
		BuildStatusManager buildStatusManager = BuildStatusManager.getInstance();
		buildStatusManager.scheduleBuildRequest(buildRequest);
		
		// Access unique identifier for these build parameters
		String buildResultId = buildRequest.getBuildReference();
		
		// Store full URL reference for build status link, allowing client to navigate
		// to resource verifying build status.
		HashMap<String, Object> buildResponse = new HashMap<String, Object>();
		buildResponse.put("buildStatusLink", buildRequestStatusPath(request, buildResultId));

		// HTTP 202, request accepted for processing, with JSON content response.
		accepted = Response.status(HttpStatus.SC_ACCEPTED).entity(buildResponse).build();

		// TODO: Do we want this? If user clicks to build for a second time after making
		// some changes they will get an error because their app is gone. 
		// Clean up temporary directory used for user application now build has finished.
		//if (userAppPath != null) {
		//	FileUtil.deleteDirectory(new File(userAppPath));
		///}		

		logger.exiting(this.getClass().getName(), "generateBuild");
		
		return accepted;
	}

	/**
	 * Return build status for a particular build reference. 
	 * Builds can either be completing or building. Builds 
	 * not completed, contain the latest build results. Builds
	 * finished, have a link to the finished resource.
	 * 
	 * @param request - Http Request
	 * @param ref - Build Reference Identifier
	 * @return Build job details
	 */
	@GET 
	@Path("status/{ref}")
	@Produces(MediaType.APPLICATION_JSON)
	public HashMap<String, String> retrieveBuildStatus(@Context HttpServletRequest request, @Context HttpServletResponse response, 
		@PathParam("ref") String reference) {
		logger.entering(this.getClass().getName(), "retrieveBuildStatus");
		
		HashMap<String, String> buildStatus = new HashMap<String, String>();

		BuildStatusManager buildStateManager = BuildStatusManager.getInstance();
		
		// Retrieve current build state for reference build
		BuildState buildState = buildStateManager.retrieveBuildState(reference);
		
		// Store resulting state in the response
		buildStatus.put("state", buildState.toString());
		buildStatus.put("logs", buildStateManager.getCompleteBuildLog(reference));
		// If build has completed, include resource link, otherwise just include 
		// current logs.
		if (buildState == BuildState.COMPLETED) {
			buildStatus.put("result", layersBuildResourcePath(request, reference));
		// Unless build is complete, response contents will likely change so ensure result isn't
		// cached. I'm looking at you Internet Explorer...
		} else {
			response.addHeader("Cache-Control", "no-cache, must-revalidate");
			response.addHeader("Pragma", "no-cache");
		}
		
		logger.exiting(this.getClass().getName(), "retrieveBuildStatus");		
		return buildStatus;
	}

	/**
	 * Retrieve the build result for a given reference.
	 * Invalid resources result in a 404 response.
	 * 
	 * @param response - HTTP Response
	 * @param reference - Build resource identifier
	 * @return Output stream for compressed Dojo build.
	 */
	@GET 
	@Path("{reference}")
	@Produces("application/zip")
	public StreamingOutput retrieveBuildResult(@Context HttpServletResponse response, @PathParam("reference") String reference) {
		logger.entering(this.getClass().getName(), "retrieveBuildResult");
		BuildStatusManager buildStateManager = BuildStatusManager.getInstance();
		
		// Retrieve current build state for reference build
		BuildState buildState = buildStateManager.retrieveBuildState(reference);
		
		if (buildState != BuildState.COMPLETED) {
			throw new MissingResourceException(missingBuildResourceErrorText);
		}

		// Set header to force download of content rather than display.
		response.setHeader(HttpUtil.contentDisposition, HttpUtil.contentDispositionAttachment);
		
		logger.exiting(this.getClass().getName(), "retrieveBuildResult");			
		// Stream file output back to the user
		return FileUtil.streamingFileOutput(buildStateManager.getBuildResultPath(reference), false);
	}

	/**
	 * Construct a new instance of the BuildRequest object. Extract mandatory
	 * parameters from the request map, verifying that parameters are valid, 
	 * before passing into new build request instance. 
	 * 
	 * @param buildDetails - JSON request parameters
	 * @return New build request instance
	 * @throws IncorrectParameterException - User error in request details
	 * @throws ConfigurationException - Internal server error occurred. 
	 */
	protected BuildRequest generateNewBuildRequest(Map<String, Object> buildDetails) 
	throws IncorrectParameterException, ConfigurationException {
		// Retrieve mandatory package descriptions, must contain Dojo package
		List<Map<String, String>> packages = (List<Map<String, String>>) extractBuildPackages(buildDetails);
		
		// Retrieve standard mandatory and optional build parameters from JSON request.
		String cdn = (String) extractMandatoryParameter(buildDetails, "cdn"), 
			optimise = (String) extractMandatoryParameter(buildDetails, "optimise"), 
			cssOptimise = (String) extractMandatoryParameter(buildDetails, "cssOptimise"), 
			platforms = (String) extractMandatoryParameter(buildDetails, "platforms"), 
			themes = (String) extractMandatoryParameter(buildDetails, "themes");

		// Construct list of reference package identiers
		Set<String> packageIds = new HashSet<String>();
		for(Map<String, String> packageRef: packages) {
			packageIds.add(packageRef.get("name"));
		}
		
		// Extract additional build layers, checking any module dependencies reference valid 
		// packages. 
		List<Map<String, Object>> layers = extractBuildLayers(buildDetails, packageIds);		
		
		// Instantiate new build request with user parameters, catch construction exceptions
		// and throw extended WebApplicationException.
		BuildRequest buildRequest;
		try {
			buildRequest = new BuildRequest(packages, cdn, optimise, cssOptimise, platforms, themes, layers);
			logger.log(Level.INFO, String.format(newBuildRequestLogMsg, buildRequest.serialise()));
		} catch (JsonMappingException e) {
			logFatalBuildRequest(buildDetails, e);
			throw new IncorrectParameterException(layersParsingErrorText);
		} catch (JsonParseException e) {
			logFatalBuildRequest(buildDetails, e);
			throw new IncorrectParameterException(layersParsingErrorText);
		} catch (IOException e) {
			logFatalBuildRequest(buildDetails, e);
			throw new IncorrectParameterException(layersParsingErrorText);
		} catch (NoSuchAlgorithmException e) {
			throw new ConfigurationException(missingAlgorithmErrorText);
		}
		
		return buildRequest;
	}
	
	/**
	 * Return full URL link for a build resource. 
	 * 
	 * @param request - Request param, used to access path info.
	 * @param digest - Resource identifier
	 * @return Full URL for build resource 
	 * @throws MalformedURLException 
	 */
	protected String layersBuildResourcePath(HttpServletRequest request, String digest) throws ConfigurationException {
		String absolutePath; 
		try {
			absolutePath = HttpUtil.constructFullURLPath(request, "/build/" + digest);
		} catch (MalformedURLException e) {
			throw new ConfigurationException(urlConstructionErrorText);
		}
		return absolutePath;
	}

	/**
	 * Return full URL link for a the status resource for a build. 
	 * 
	 * @param request - Request param, used to access path info.
	 * @param digest - Resource identifier
	 * @return Full URL for build resource 
	 * @throws MalformedURLException 
	 */
	protected String buildRequestStatusPath(HttpServletRequest request, String reference) throws ConfigurationException {
		String absolutePath; 
		try {
			absolutePath = HttpUtil.constructFullURLPath(request, "/build/status/" + reference);
		} catch (MalformedURLException e) {
			throw new ConfigurationException(urlConstructionErrorText);
		}
		return absolutePath;
	}
	
	/**
	 * Pull out the JavaScript packages referenced in this
	 * build request. All packages must reference known packages in our
	 * repository and must contain the Dojo package. 
	 * 
	 * @param buildRequest - User's build request
	 * @return Reference packages for this build
	 * @throws IncorrectParameterException - Unable to find valid dojo package reference
	 */
	protected List<Map<String, String>> extractBuildPackages(Map<String, Object> buildRequest) 
	throws IncorrectParameterException {		
		List<Map<String, String>> packages 
			= (List<Map<String, String>>) extractMandatoryParameter(buildRequest, "packages");
		
		// Dojo package reference is mandatory for all build requests
		boolean containsDojoPackage = false;
		
		// Verify all reference packages are present in our repository
		Iterator<Map<String, String>> it = packages.iterator();
	    while (it.hasNext()) {
	        Map<String, String> entry = it.next();
	        String name = extractMandatoryParameter(entry, "name"), 
	        	version = extractMandatoryParameter(entry, "version");
	        if (!repo.packageVersionExists(name, version)) {
	        	throw new IncorrectParameterException(String.format(invalidPackageErrorText, name));		
	        } else if (name.equals("dojo")) {
	        	containsDojoPackage = true;
	        }
	    }
		
	    // Dojo package is mandatory!
		if (!containsDojoPackage) {
			throw new IncorrectParameterException(String.format(invalidPackageErrorText, "dojo"));
		}
		
		return packages;
	}
 	
	/**
	 * Retrieve and verify (optional) layers parameters from build request. If parameter
	 * is present, verify each dependency in every layer references a defined package.
	 * 
	 * @param buildRequest - Request parameters
	 * @param validPackages - Set of referenced packages
	 * @return User's build layers
	 */
	protected List<Map<String, Object>> extractBuildLayers(Map<String, Object> buildRequest, 
		Set<String> validPackages) {
		// Build should always contains default dojo layer 
		boolean containsBaseDojoLayer = false;
		
		List<Map<String, Object>> layers = new ArrayList<Map<String, Object>>();
				
		// For every user layer, confirm every referenced dependency 
		// refers to a valid package.
		if (buildRequest.containsKey("layers")) {
			layers = (List<Map<String, Object>>) buildRequest.get("layers");
			Iterator<Map<String, Object>> layerIter = layers.iterator();
			
			while(layerIter.hasNext()) {
				// For each module layer....
				Map<String, Object> layer = layerIter.next();
				List<Map<String, String>> dependencies = (List<Map<String, String>>) layer.get("modules");
				//.. verify each one is valid.
				verifyModuleDependencies(dependencies, validPackages);
				
				// Check each layer to see if we come across dojo base
				containsBaseDojoLayer |= isBaseDojoLayer(layer);
			}
		}
		
		// If user hasn't explicitly asked for base dojo layer, 
		// ensure it's present. This layer will always be generated
		// by the build system. 
		if (!containsBaseDojoLayer) {						
			layers.add(defaultDojoLayer);
		}
		
		return layers;
	}
	
	/**
	 * Confirm each dependency module has a recognised package.
	 * Iterate through the list checking all dependencies against
	 * package set.  
	 * 
	 * @param dependencies - Module dependencies 
	 * @param validPackages - All valid packages
	 */
	protected void verifyModuleDependencies(List<Map<String, String>> dependencies, Set<String> validPackages) {
		Iterator<Map<String, String>> depIter = dependencies.iterator();
		// ... pull out each dependency
		while (depIter.hasNext()) {
			Map<String, String> dependency = depIter.next();
			String packageId = dependency.get("package");
			// ... and check it has a valid package reference
			if (packageId == null || !validPackages.contains(packageId)) {
				throw new IncorrectParameterException(String.format(invalidModulePackageErrorText, dependency.get("name")));
			}
		}
	}
	
	/**
	 * Check with custom layer matches dojo base layer. 
	 * Compare layer identifiers. 
	 * 
	 * @param buildLayer - User layer
	 * @return Custom layer represent's dojo base
	 */
	protected Boolean isBaseDojoLayer(Map<String, Object> buildLayer) {
		String defaultLayerName =  (String) defaultDojoLayer.get("name"),
			buildLayerName = (String) buildLayer.get("name");
		
		return defaultLayerName.equals(buildLayerName);
	}
	
	/**
	 * Retrieve mandatory parameter from the build request. Any missing
	 * parameters are fatal errors and cause exceptions to be thrown.
	 * @param <T>
	 * 
	 * @param request - Build request details 
	 * @param identifier - Parameter identifier 
	 * @throws IncorrectParameterException - Missing mandatory build parameter
	 */
	protected <T> T extractMandatoryParameter(Map<String, T> request, String identifier) {
		if (!request.containsKey(identifier) || request.get(identifier) == null) {
			throw new IncorrectParameterException(String.format(missingParameterErrorText, identifier));
		}
		
		return request.get(identifier);
	}
	
	/**
	 * Log details of the error caught when trying to process a user's
	 * build request. Attempt to serialise original build request object.
	 * 
	 * @param buildDetails - User's build request
	 * @param e - Exception throw during processing
	 */
	protected void logFatalBuildRequest(Map<String, Object> buildDetails, Exception e) {
		String serialisedBuildDetails;
		try {
			serialisedBuildDetails = JsonUtil.writeJavaToJson(buildDetails);
		} catch (JsonParseException exception) {
			serialisedBuildDetails = exception.getMessage();
		} catch (JsonMappingException exception) {
			serialisedBuildDetails = exception.getMessage();
		} catch (IOException exception) {
			serialisedBuildDetails = exception.getMessage();
		}
		
		logger.log(Level.SEVERE, String.format(fatalBuildRequestLogMsg, e.getMessage(), serialisedBuildDetails));
	}
}
