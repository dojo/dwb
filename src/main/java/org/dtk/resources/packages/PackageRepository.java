package org.dtk.resources.packages;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.dtk.resources.Packages;
import org.dtk.resources.build.manager.BuildStatusManager;
import org.dtk.resources.exceptions.ConfigurationException;
import org.dtk.resources.exceptions.MissingResourceException;
import org.dtk.util.FileUtil;
import org.dtk.util.JsonUtil;

/**
 * Wrapper for directory-based package repository. Provides
 * utility methods to retrieve package, version and meta-data
 * details. 
 * 
 * @author James Thomas
 */

public class PackageRepository {
	/** Base repository for package directories */
	protected String packageBaseLocation;
	
	/** Real location for build parameters configuration file */
	protected String buildParametersLocation;
	
	/** Package details configuration file */
	protected static final String DEFAULT_PACKAGE_METADATA = "%1$s/%2$s/package.json"; 
	
	/** Log messages */
	/** Fatal accessing accessing package repository for package & version */
	protected static final  String invalidMetaDataErrorMsg 
	= "Unable to access meta-data for valid package (%1$s) & version (%2$s). Error in package repository location.";
	
	/** Build configuration file is not accessible **/
	protected static final String buildConfigNotFoundErrorMsg 
	= "Unable to access package repository build configuration @ %1$s";
	
	/** Fatal error parsing JSON build configuration */
	protected static final String jsonParsingErrorMsg
	= "Unable to parse JSON from package repository build configuration.";
	
	/** Fatal error mapping to Java Objects from build configuration from JSON */
	protected static final String jsonMappingErrorMsg 
	= "Unable to parse JSON Object from package repository build configuration.";
	
	/** Package repository is empty, unable to find any package meta-data. */
	protected static final String packagesDirectoryEmptyErrorMsg 
	= "Fatal error, unable to find any packages in the package repository location (%1$s).";
	
	/** Single instance of package repository */
	private static final PackageRepository INSTANCE = new PackageRepository();

	/** Packages logging class - All package error should be logged in global
	 *  packages log rather than instance log */
	protected static Logger logger = Logger.getLogger(Packages.class.getName());
	
	/**
	 * Private constructor to enforce singleton pattern.
	 */
	private PackageRepository() {
		if (INSTANCE != null) {
			throw new IllegalStateException("Already instantiated");
		}
	}
	
	/**
	 * Return static instance of PackageRepository
	 * 
	 * @return Package repository instance.
	 */
	public static PackageRepository getInstance() {
		return INSTANCE;
	}

	/**
	 * Return list of all packages in the base location.
	 * 
	 * @return List of all packages
	 */
	public List<String> getPackages() {
		return getAllPackageDirectories(null);
	}
	
	/**
	 * Find all package versions for the given package identifier 
	 * in the base repository location.
	 * 
	 * @param packageName - Package identifier
	 * @return List of all package versions
	 */
	public List<String> getPackageVersions(String packageName) {
		if (!packageExists(packageName)) {
			throw new MissingResourceException("Unable to find package with that identifier.");
		}
		return getAllPackageDirectories(packageName);
	}
	
	/**
	 * Return meta-data for a given package version. Will contain
	 * all modules, full name and description.
	 * 
	 * @param packageName - Package name 
	 * @param packageVersion - Package version
	 * @return Generic map containing converted JSON package meta-data
	 * @throws JsonParseException - Failure to parse package descriptor
	 * @throws JsonMappingException - Failure to parse package descriptor
	 * @throws IOException - Couldn't access package descriptor
	 */
	public HashMap<String, Object> getPackageDetails(String packageName, String packageVersion) {
		// Verify package existence retrieving details
		if (!packageVersionExists(packageName, packageVersion)) {
			throw new MissingResourceException("Unable to find package version details with that identifier.");
		}
		
		HashMap<String, Object> packageMetaData = null;
		
		try {
			String packageMetaDataPath = String.format(DEFAULT_PACKAGE_METADATA, packageName, packageVersion);
			packageMetaData = JsonUtil.genericJSONMapper(new File(packageBaseLocation, packageMetaDataPath));
		} catch (IOException e) {
			String errorMessage = String.format(invalidMetaDataErrorMsg, packageName, packageVersion);
			logger.log(Level.SEVERE, errorMessage);
			throw new ConfigurationException(errorMessage);
		}
		
		return packageMetaData;
	}
	
	/**
	 * Retrieve build parameters, parse default file in source folder.
	 * 
	 * @return Converted JSON object with build parameters
	 * @throws ConfigurationException - Unable to parse or access configuration
	 */
	public HashMap<String, Object> getBuildParameters() throws ConfigurationException {
		HashMap<String, Object> buildParameters = null;
		
		// Read and parse build option configuration from package repository. 
		try {
			buildParameters = JsonUtil.genericJSONMapper(new File(buildParametersLocation));
		} catch (JsonMappingException e) {			
			logger.log(Level.SEVERE, jsonMappingErrorMsg);
			throw new ConfigurationException(jsonMappingErrorMsg);
		} catch (JsonParseException e) {
			logger.log(Level.SEVERE, jsonParsingErrorMsg);
			throw new ConfigurationException(jsonParsingErrorMsg);
		} catch (IOException e) {			
			String errorMsg = String.format(buildConfigNotFoundErrorMsg, buildParametersLocation);
			logger.log(Level.SEVERE, errorMsg);
			throw new ConfigurationException(errorMsg);
		}
		
		// Instantiate generic build options from the base level configuration file. 
		return buildParameters; 
	}
	
	/**
	 * Create a temporary package from a compressed user application. User application
	 * contained within byte stream of string parameter.
	 * 
	 * @param compressedPackage - Compressed zip file containing user application
	 * @return Temporary package reference 
	 * @throws IOException - Failed to uncompressed archive to temporary location
	 */
	public String createTemporaryPackage(InputStream compressedPackage) throws IOException {
		// Inflate user's application to a temporary location
		String temporaryPackagePath = FileUtil.inflateTemporaryZipFile(compressedPackage);
		
		// Something went wrong, throw exception to indicate.
		if (temporaryPackagePath == null) {
			throw new IOException("Unable to inflate zip file");
		}
		
		// Use file class to handle extract filename from full path.
		String packageReference = (new File(temporaryPackagePath)).getName();
		
		return packageReference;
	}
	
	/**
	 * Return full path location for a temporary package reference. All temporary
	 * packages are located in the default system temp directory.
	 * 
	 * @param packageReference - Temporary package reference 
	 * @return Package location
	 * @throws FileNotFoundException - Package does not exist
	 */
	public String getTemporaryPackageLocation(String packageReference) throws FileNotFoundException {
		String tempDirectory = System.getProperty("java.io.tmpdir");
		File temporaryPackage = new File(tempDirectory, packageReference);
		
		if (!temporaryPackage.exists() || !temporaryPackage.isDirectory()) {
			throw new FileNotFoundException("Cannot find temporary package location");
		}
		
		// Return absolute package path
		return temporaryPackage.getAbsolutePath();
	}
	
	/**
	 * Check for existence of a package within the base location.
	 * 
	 * @param packageName - Identifier
	 * @return Does package exist?
	 */
	public boolean packageExists(String packageName) {
		return baseLocationPathExists(packageName);
	}
	
	/**
	 * Check for existence of a package version within the base location.
	 * 
	 * @param packageName - Identifier
	 * @return Does package version exist?
	 */
	public boolean packageVersionExists(String packageName, String packageVersion) {
		return baseLocationPathExists(packageName + "/" + packageVersion);
	}
	
	/**
	 * Setter for packageBaseLocation.
	 * 
	 * @param packageBaseLocation - Base package location
	 */
	public void setPackageBaseLocation(String packageBaseLocation) {
		this.packageBaseLocation = packageBaseLocation;
	}
	
	/**
	 * Setter for packageBaseLocation.
	 * 
	 * @param buildParametersLocation - Base package location
	 */
	public void setBuildParametersLocation(String buildParametersLocation) {
		this.buildParametersLocation = buildParametersLocation;
	}
	
	/**
	 * Return all the directories under the given package path root. Allow
	 * an optional package child name to be given. 
	 * 
	 * @param subPath - Optional package path to use as search base.
	 * @return Directory labels for constructed package repository.
	 * @throws ConfigurationException - Unable to access directories under package store.
	 */
	protected List<String> getAllPackageDirectories(String subPath) throws ConfigurationException {
		List<String> packages = new ArrayList<String>();

		// If a sub path is specified, use that file as directory base.
		File basePath = (subPath == null) ? new File(packageBaseLocation) : new File(packageBaseLocation, subPath);

		// Find all sub-directories under the root parameter path.
		File[] packageDirectories = FileUtil.findAllDirectories(basePath);
		if (packageDirectories == null) {			
			String errorMsg = String.format(packagesDirectoryEmptyErrorMsg, basePath.getAbsolutePath());
			logger.log(Level.SEVERE, errorMsg);
			throw new ConfigurationException(errorMsg);
		}

		// Loop through all directories found, adding name to global list.
		for (File packageDirectory: packageDirectories) {
			packages.add(packageDirectory.getName());
		}

		// Return versions in order.
		Collections.sort(packages);
		
		return packages;
	}
	
	/**
	 * Check whether a given directory in the package repository directory
	 * exists.
	 * 
	 * @param packageName - Identifier
	 * @return Does package exist?
	 */
	protected boolean baseLocationPathExists(String location) {
		File packageDir = new File(packageBaseLocation, location);
		
		return (packageDir.exists() && packageDir.isDirectory());
	}
}
