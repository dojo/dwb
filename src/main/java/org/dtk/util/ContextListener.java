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

	/** Init parameter key for build result cache path */
	protected static final String cachePathParam = "cachePath";

	/** Relative path to custom Dojo build script */
	protected static final String buildPathScript = "/js/build.js";

	/** Relative path to custom build parameters configurtion */
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
		
		//String cachePath = getDirectoryPathParam(cachePathParam, servletContext);
		String cachePath = "/tmp/dwb";
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
	 * Retrieve a context parameter that must point to a valid
	 * directory path. Checks are performed to confirm directory
	 * exists. 
	 * 
	 * @param param - Parameter name
	 * @param servletContext - Handle to context
	 * @return Full directory path
	 * @throws NullPointerException - Invalid parameter value
	 */
	protected String getDirectoryPathParam(String param, ServletContext servletContext) throws NullPointerException {
		String dirPath = servletContext.getInitParameter(param);
		
		// Sanity check retrieved path, shouldn't be null or empty.
		if (dirPath == null || dirPath.isEmpty()) {
			throw new NullPointerException("Init param, "+param+", was null or empty. Please check web.xml.");
		}

		// Sanity check path exists and is a directory...
		File dirPathFile = new File(dirPath);		
		if (!dirPathFile.exists() || !dirPathFile.isDirectory()) {
			throw new NullPointerException("Init param, "+param+", does not exist or is not a directory.");
		}
		return dirPath;
	}

}
