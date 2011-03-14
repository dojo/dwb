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

	/** System property for fixed build result cache path */
	protected static final String cachePathParam = "dwb.cachepath";

	/** Relative path to custom Dojo build script */
	protected static final String buildScriptsDirParam = "/js/build/";

	/** Relative path to custom build parameters configuration */
	protected static final String buildParametersConfig = "/WEB-INF/config/build_options.json";

	/** Relative directory path for package details */
	protected static final String packageRepoPath = "/WEB-INF/config/packages/";
	
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
		ServletContext servletContext = contextEvent.getServletContext();
		
		String cachePath = getCacheDirectoryPath();
		String packagePath = servletContext.getRealPath(packageRepoPath);
		String buildScriptsDir = servletContext.getRealPath(buildScriptsDirParam);

		PackageRepository packageRepo = PackageRepository.getInstance();
		packageRepo.setPackageBaseLocation(packagePath);
		packageRepo.setBuildParametersLocation(servletContext.getRealPath(buildParametersConfig));
		
		BuildStatusManager buildStatusManager = BuildStatusManager.getInstance();
		buildStatusManager.setBuildResultCachePath(cachePath);
		buildStatusManager.setBuildScriptsDir(buildScriptsDir);
		
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
     * using the system property, dwb.cachepath.
     * 
     * @return Directory path for built result cache
     */
    protected String getCacheDirectoryPath() {
    	String dirPath = System.getProperty(cachePathParam);

    	// If parameter wasn't specified or it's an empty string, create a new temporary
    	// cache path.
		if (dirPath == null || dirPath.isEmpty()) {
			try {
				dirPath = FileUtil.createTempDirectory().getAbsolutePath();
			} catch (IOException ioe) {
				throw new NullPointerException("Fatal error create temporary cache path, try to specify this " +
					"manually using system property: dwb.cachepath");
			}
		}
    	
		// Sanity check the path value to ensure it exists and is a directory.
		File dirPathFile = new File(dirPath);
		if (!dirPathFile.exists() || !dirPathFile.isDirectory()) {
			throw new NullPointerException("Init param, "+ dirPath +", does not exist or is not a directory.");
		}
        
        return dirPath;
    }
}
