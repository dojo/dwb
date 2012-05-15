package org.dtk.analysis.script.loader;

import java.net.URL;
import java.util.Map;

import org.dtk.analysis.ModuleFormat;

/**
 * Module path resolver for the old style Dojo module format.
 * 
 * @author James Thomas
 */

public class NonAmdModulePathResolver extends AbstractModulePathResolver {
	
	public NonAmdModulePathResolver(URL baseUrl, Map<String, String> modulePaths) {
		super(baseUrl, modulePaths);
	}

	@Override
	protected char getModulePathSeparator() {
		return ModuleFormat.getPathSeparator(ModuleFormat.NON_AMD);
	}
}
