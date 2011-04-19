package org.dtk.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FileUtils;
import org.apache.wink.common.model.multipart.BufferedInMultiPart;
import org.dtk.resources.dependencies.ScriptParser;
import org.dtk.resources.exceptions.ConfigurationException;
import org.dtk.resources.exceptions.IncorrectParameterException;
import org.dtk.resources.packages.PackageRepository;
import org.dtk.util.HttpUtil;
import org.dtk.util.JsonUtil;

/**
 * RESTful Packages API. This API provides access to the packages 
 * available to the build system, allowing a user to retrieve a list of all
 * packages present, retrieve details about specific packages (versions and 
 * modules) and create temporary packages from user applications.
 * 
 * @author James Thomas
 */

@Path("/packages")
public class Packages {
	/** Global access to servlet and uri information */
	@Context ServletConfig config;
	@Context UriInfo info;

	/** Form submission field containing user application */
	protected static final String USER_APP_FIELD = "user_app";
	
	/** Arbitrary package version for temporary packages */
	protected static final String temporaryPackageVersion = "1.0.0";

	/** Resource path format, "context_path/servet_path/resource_path" */
	protected static final String resourcePathFormat = "%1$s%2$s";

	/** Response messages **/
	/** Generic failure creating temporary package from uploaded application. */
	protected static final String fatalProcessingErrorMsg
	= "Internal server error, unable to process new package";
	
	/** Unable to decompress user application error message */
	protected static final String invalidRequestErrorMsg 
	= "Invalid client request, unable to decompress user application.";
	
	/** Request missing mandatory application parameter */
	protected static final String missingAppParameterErrorMsg
	= "Invalid client request, missing mandatory field containing user application";
	
	/** Log messages **/
	/** File path for decompressed temporary application is not valid */
	protected static final String errorCreatingPackageLogMsg 
	= "Fatal error decompressing user's application into a temporary package, details: %1$s";

	/** Unable to construct valid URL for new temporary application */
	protected static final String errorCreatingPackageLocationLogMsg 
	= "Unable to construct valid URL for new temporary package, details: %1$s";
	
	/** Packages logging class */
	protected static Logger logger = Logger.getLogger(Packages.class.getName());
	
	/**
	 * Retrieve the global packages list and build options
	 * available.
	 * 
	 * @return Packages information
	 * @throws ConfigurationException - Couldn't access package details due to configuration error.
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public HashMap<String, Object> getPackages(@Context HttpServletRequest request) throws ConfigurationException {
		logger.entering(this.getClass().getName(), "getPackages");
		HashMap<String, Object> packageBuildOptions = null;

		// Instantiate package repository and retrieve default build options
		PackageRepository packageRepo = PackageRepository.getInstance();
		packageBuildOptions = packageRepo.getBuildParameters();

		// Add all available packages and versions information to the build options
		List<String> packageNames = packageRepo.getPackages();
		packageBuildOptions.put("packages", getResourceDetails(request, packageNames));

		logger.exiting(this.getClass().getName(), "getPackages");
		return packageBuildOptions;
	}	

	/**
	 * Retrieve versions available for a given package. 
	 * 
	 * @param name - Package identifier
	 * @return List of available versions
	 * @throws ConfigurationException - Couldn't access package details due to configuration error.
	 */
	@GET
	@Path("{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, String>> getPackage(@Context HttpServletRequest request, @PathParam("name") String name) {	
		logger.entering(this.getClass().getName(), "getPackage");
		
		PackageRepository packageRepo = PackageRepository.getInstance();
		List<String> packageVersions = packageRepo.getPackageVersions(name);		
		List<Map<String, String>> packageDetails = getResourceDetails(request, packageVersions);
		
		logger.exiting(this.getClass().getName(), "getPackage");
		return packageDetails;
	}

	/**
	 * Retrieve package meta-data for requested parameters.  
	 * 
	 * @param id - Package identifier
	 * @param version - Package version
	 * @return Package modules, name and description.
	 */
	@GET
	@Path("{name}/{version}")
	@Produces(MediaType.APPLICATION_JSON)
	public HashMap<String, Object> getPackageDetails(@PathParam("name") String id, @PathParam("version") String version) {
		logger.entering(this.getClass().getName(), "getPackageDetails");
		
		HashMap<String, Object> packageDetails = null;

		// Retrieve package meta-data, HTTP 404 if invalid name/version given.
		PackageRepository packageRepo = PackageRepository.getInstance();
		packageDetails = packageRepo.getPackageDetails(id, version);

		// Remove package location from response, not relevant for the user. 
		packageDetails.remove("location");
		
		logger.exiting(this.getClass().getName(), "getPackageDetails");		
		return packageDetails;
	}
	
	/**
	 * Create a new temporary package resource. Client must include a compressed user application
	 * in the request, present under the "user_app" key. This application will be uncompressed and 
	 * stored in a temporary location. The temporary package identifier and module details will be
	 * returned to the user. HTTP status code 201 indicates a successful request.  
	 * 
	 * @param multiPartForm - Form submission parameters, must contain user_app.
	 * @return String - HTML encoded JSON package representation. 
	 * @throws URISyntaxException 
	 */
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_HTML)    
	public Response createTemporaryPackage(BufferedInMultiPart multiPartForm) {		
		logger.entering(this.getClass().getName(), "createTemporaryPackage");
		// Complex response needed, HTTP 201.
		Response created = null;
		
		// Process multipart form data into useful format. All form part bodies will be extracted
		// when it has a name property and stored as strings.
		MultivaluedMap<String, Object> formFields = HttpUtil.retrieveMultiPartFormValues(multiPartForm, InputStream.class);

		if (formFields.containsKey(USER_APP_FIELD)) {
			// Map, holding package details, that will be rendered as JSON.
			Map<String, Object> temporaryPackageDetails = new HashMap<String, Object>();

			PackageRepository packageRepo = PackageRepository.getInstance();

			try {
				// Decompress temporary package, returning unique reference
				String packageIdentifier = packageRepo.createTemporaryPackage((InputStream) formFields.getFirst(USER_APP_FIELD));
				
				// Find temporary package location from identifier
				String packageLocation = packageRepo.getPackageLocation(packageIdentifier, temporaryPackageVersion);

				Iterator<File> packageJsFilesIter = FileUtils.iterateFiles(new File(packageLocation), 
					new String[]{"js"}, true);

				// List holding module definitions & requirements 
				List<String> modulesProvided = new ArrayList<String>();
				List<String> modulesRequired = new ArrayList<String>();
				
				// Hold new temporary package details 
				Map<String, String> packageDetails = new HashMap<String, String>();

				// Run script parser on each file, pulling out requires and provides
				while(packageJsFilesIter.hasNext()) {
					String fileContents = FileUtils.readFileToString(packageJsFilesIter.next());
					ScriptParser scriptParser = new ScriptParser(fileContents);

					// Add all module dependencies discovered to global state holder
					modulesProvided.addAll(scriptParser.retrieveModuleProvides());
					modulesRequired.addAll(scriptParser.retrieveModuleRequires());
				}

				// Store package modules, requires & provides, in response JSON.
				temporaryPackageDetails.put("requiredDojoModules", modulesRequired);
				temporaryPackageDetails.put("availableModules", modulesProvided);
				
				// Temporary package details, use arbitrary version for temporary packages.
				packageDetails.put("name", packageIdentifier);
				packageDetails.put("version", "1.0.0");
				
				// Store package reference in the details object
				temporaryPackageDetails.put("packages", Arrays.asList(packageDetails));

				// Render provides, requires and temporary package id as JSON
				String htmlEncodedJsonPackageDetails = JsonUtil.writeJavaToHtmlEncodedJson(temporaryPackageDetails);
				
				// Construct HTTP 201 response, containing text body.
				created = Response.created(new URI(packageLocation)).entity(htmlEncodedJsonPackageDetails).build();
			} catch (FileNotFoundException e) {
				logger.log(Level.SEVERE, String.format(errorCreatingPackageLogMsg, e.getMessage()));
				throw new ConfigurationException(fatalProcessingErrorMsg);
			} catch (IOException e) {
				logger.log(Level.SEVERE, String.format(errorCreatingPackageLogMsg, e.getMessage()));
				throw new IncorrectParameterException(invalidRequestErrorMsg);
			} catch (URISyntaxException e) {
				logger.log(Level.SEVERE, String.format(errorCreatingPackageLocationLogMsg, e.getMessage()));
				throw new ConfigurationException(fatalProcessingErrorMsg);
			} 
		} else {
			throw new IncorrectParameterException(missingAppParameterErrorMsg);
		}

		logger.exiting(this.getClass().getName(), "createTemporaryPackage");	
		return created;
	}

	/**
	 * Construct resource collections details, map of names and links to resources.
	 * Using current request context to obtain request URL.
	 * 
	 * @param request - Current HTTP request
	 * @return Collection of resource details
	 */
	protected List<Map<String, String>> getResourceDetails(HttpServletRequest request, List<String> resourceIds) {
		List<Map<String, String>> packageDetails = new ArrayList<Map<String, String>>();

		// For each package, store name and version link
		Iterator<String> iter = resourceIds.iterator();
		while (iter.hasNext()) {
			final String resourceId = iter.next();
			// Make sure we end with a slash
			String requestURL = request.getRequestURL().toString();
			if (!requestURL.endsWith("/")) {
				requestURL += "/";
			}
			final String resourcePath = String.format(resourcePathFormat, requestURL, resourceId);
			packageDetails.add(new HashMap<String, String>() {{
				put("name", resourceId);
				put("link", resourcePath);
			}});

		}

		return packageDetails;
	}
}
