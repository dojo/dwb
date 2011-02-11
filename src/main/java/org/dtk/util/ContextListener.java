package org.dtk.util;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.dtk.resources.build.manager.BuildStatusManager;
import org.dtk.resources.exceptions.ConfigurationException;
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
	protected static final String buildPathScript = "/js/build.js";

	/** Relative path to custom build parameters configuration */
	protected static final String buildParametersConfig = "/WEB-INF/config/build_options.json";

	/** Relative directory path for package details */
	protected static final String packageRepoPath = "/WEB-INF/config/packages/";
	
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
		String buildScript = servletContext.getRealPath(buildPathScript);
		
		PackageRepository packageRepo = PackageRepository.getInstance();
		packageRepo.setPackageBaseLocation(packagePath);
		packageRepo.setBuildParametersLocation(servletContext.getRealPath(buildParametersConfig));
		
		BuildStatusManager buildStatusManager = BuildStatusManager.getInstance();
		buildStatusManager.setBuildResultCachePath(cachePath);
		buildStatusManager.setBuildScriptPath(buildScript);
	}
	
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
