package org.dtk.analysis.script.loader;

import java.net.URL;
import java.util.Map;

import org.dtk.analysis.ModuleFormat;

/**
 * Module path resolver for the AMD module format.
 * 
 * @author James Thomas
 */

public class AmdModulePathResolver extends AbstractModulePathResolver {
	
	/**
	 * Default constructor. 
	 * 
	 * @param baseUrl - Directory path containing module loader 
	 * @param modulePaths - Configuration paths for module locations
	 */
	public AmdModulePathResolver(URL baseUrl, Map<String, String> modulePaths) {
		super(baseUrl, modulePaths);
	}
	
	/**
	 * Module path fragment separator for the AMD module format.
	 * 
	 * @return Path separator character
	 */
	@Override
	protected char getModulePathSeparator() {
		return ModuleFormat.getPathSeparator(ModuleFormat.AMD);
	}
}
