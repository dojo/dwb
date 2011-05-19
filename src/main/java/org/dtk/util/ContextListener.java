package org.dtk.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.dtk.resources.Build;
import org.dtk.resources.Dependencies;
import org.dtk.resources.Packages;
import org.dtk.resources.build.manager.BuildStatusManager;
import org.dtk.resources.packages.PackageRepository;

/**
 * Simple context listener to set package paths context parameter
 * on the single instance of the package repository. 
 * 
 * @author James Thomas
 */

public class ContextListener implements ServletContextListener {

	/** Current application context, reference passed in during initialization */
	protected ServletContext currentContext;
	
	/** System property for fixed build result cache path */
	protected static final String cachePathParam = "cachepath";

	/** User property to override default location for package repository */
	protected static final String packageRepoPathParam = "packagespath";
	
	/** Relative path to custom Dojo build script */
	protected static final String buildModulePathParam = "/js/build/bdbuild/";

	/** Relative path to custom Dojo build script */
	protected static final String loaderModulePathParam = "/js/build/amd_loader/dojo.js";
	
	/** Relative path to custom build parameters configuration */
	protected static final String buildParametersConfig = "/WEB-INF/config/build_options.json";

	/** Relative directory path for default package details directory */
	protected static final String defaultPackageRepoPath = "/WEB-INF/config/packages/";
	
	/** Class loggers which should be logged to a file **/
	protected static final List<String> fileLoggingClasses = Arrays.asList(
		Feedback.class.getName(),
		Build.class.getName(),
		Dependencies.class.getName(),
		Packages.class.getName()
	);
	
	/** Log filename format for application logs **/
	protected static final String LOG_FILE_FORMAT = "logs/%1$s.log";
	
	/** Error message when exception caught adding log file handler **/
	protected static final String LOG_HANDLER_ERROR = "Unable to add file handler to logger for %1$s. " +
		"All application logs will be lost when the application exits.";
	
	/** Log path information statement **/
	protected static final String LOG_HANDLER_INFO = "Logging all logs from %1$s to '%2$s'";
	
	/** Listener logging class */
	protected static Logger logger = Logger.getLogger(ContextListener.class.getName());
	
	@Override
	public void contextDestroyed(ServletContextEvent contextEvent) {
		// Intentionally empty...
	}

	/**
	 * Event listener fired when context is initialised. Check for existance
	 * of correct context parameter, confirming this is a valid parameter,
	 * and then set of the single instance of the package repository.
	 * 
	 * @param contextEvent - Handle to servlet context
	 */
	@Override
	public void contextInitialized(ServletContextEvent contextEvent) {
		currentContext = contextEvent.getServletContext();
		
		// Check for user configuration overrides to cache and package
		// directories
		String cachePath = getCacheDirectoryPath();
		String packagePath = getPackageRepoPath();
		
		String builderModulePath = currentContext.getRealPath(buildModulePathParam);
		String loaderModulePath = currentContext.getRealPath(loaderModulePathParam);

		PackageRepository packageRepo = PackageRepository.getInstance();
		packageRepo.setPackageBaseLocation(packagePath);
		packageRepo.setBuildParametersLocation(currentContext.getRealPath(buildParametersConfig));
		
		BuildStatusManager buildStatusManager = BuildStatusManager.getInstance();
		buildStatusManager.setBuildResultCachePath(cachePath);
		buildStatusManager.setBuildModulePath(builderModulePath);
		buildStatusManager.setLoaderModulePath(loaderModulePath);
		
		// Add file handlers to certain class loggers
		initialiseLoggingHandlers();
	}
	
	/**
	 * Add file handlers for all configured class loggers. All these logs will
	 * be persisted in appending log files. 
	 */
	protected void initialiseLoggingHandlers()  {
		for(String classLoggerName: fileLoggingClasses) {
			Logger classLogger = Logger.getLogger(classLoggerName);
			try {
				String logFile = String.format(LOG_FILE_FORMAT, classLoggerName);				
				// Set log file output and change format from XML to simple text.
				FileHandler handler = new FileHandler(logFile, true);
				handler.setFormatter(new SimpleFormatter());
				
				classLogger.addHandler(handler);
				logger.log(Level.INFO, String.format(LOG_HANDLER_INFO, classLoggerName, logFile));
			} catch (IOException io) {
				logger.log(Level.WARNING, String.format(LOG_HANDLER_ERROR, classLoggerName));
			}	
		}		
	};
	
    /**
     * Retrieve the cache directory for build results. By default, create a new temporary
     * directory to hold the build results unless the user has manually specified the location
     * using the context parameter property or system property, cachepath.
     * 
     * @return Directory path for built result cache
     */
    protected String getCacheDirectoryPath() {
    	String dirPath = lookupUsersConfigParam(cachePathParam);

    	// If parameter wasn't specified or it's an empty string, create a new temporary
    	// cache path.
		if (isParameterMissing(dirPath)) {
			try {
				dirPath = FileUtil.createTempDirectory().getAbsolutePath();
			} catch (IOException ioe) {
				throw new NullPointerException("Fatal error create temporary cache path, try to specify this " +
					"manually using context parameter or system property: cachepath");
			}
		} 
    	
		// Sanity check the path value to ensure it exists and is a directory.		
		if (!isValidDirectory(dirPath)) {
			throw new NullPointerException("Cache path, "+ dirPath +", does not exist or is not a directory.");
		}
		
        return dirPath;
    }
    
    /**
     * Retrieve the directory path for the package repository. This value 
     * may be overridden by the user, if missing we fall back to a local default
     * value, packages_path, as a init or system parameter.
     * 
     * Value will be checked to ensure it's an actual directory and errors 
     * thrown if not.
     * 
     * @return Directory path to package repository  
     */
    protected String getPackageRepoPath() {
    	String dirPath = lookupUsersConfigParam(packageRepoPathParam);
    	
    	// If user hasn't given a path, use default 
    	if (isParameterMissing(dirPath)) {
    		dirPath = currentContext.getRealPath(defaultPackageRepoPath);
    	} 
    	
    	if (!isValidDirectory(dirPath)) {
    		throw new NullPointerException("Packages path, "+ dirPath +", does not exist or is not a directory.");
    	}    		
    	
    	return dirPath;
    }
    
    /**
     * Look up a user configurable parameter value. 
     * 
     * Properties are resolved as init parameters in the local application context, 
     * failing back to system properties if missing no value was found. 
     * 
     * We also resolve environment variables here in case these are included in
     * the path string.
     * 
     * @return User's cache path parameter if available
     */
    protected String lookupUsersConfigParam(String configParamName) {
		String configParam = currentContext.getInitParameter(configParamName);
		
		if (isParameterMissing(configParam)) {
			configParam = System.getProperty(configParamName);
		}
		
		// Resolve environment variables in user's parameter to 
		// actual values.
		configParam = FileUtil.resolveEnvironmentVariables(configParam);
		
    	return configParam;
    }    
    
    /**
     * Confirm the path parameter points to an existing directory
     * on the system.
     * 
     * @param dirPath - Path to directory 
     * @return Path is a valid directory 
     */
    protected boolean isValidDirectory(String dirPath) {
		File dirPathFile = new File(dirPath);		

		return (dirPathFile.exists() && dirPathFile.isDirectory());
    }
    
    /**
     * Missing parameter values are either null values of empty
     * strings. 
     * 
     * @param parameterValue - User configuration parameter value
     * @return Parameter is missing valid value
     */
    private boolean isParameterMissing(String parameterValue) {
    	return (parameterValue == null || parameterValue.isEmpty());
    }
}
