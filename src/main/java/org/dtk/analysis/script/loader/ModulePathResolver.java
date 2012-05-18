package org.dtk.analysis.script.loader;

import java.net.URL;

/**
 * Interface to provide an API for turning absolute module identifiers 
 * into remote file paths. Format of the module identifier left to the 
 * implementations. API provides methods for turning identifiers into paths
 * relative to the module loader and an absolute URL reference. 
 * 
 * @author James Thomas
 */

public interface ModulePathResolver {
	
	/**
	 * Return local file path reference relative to the module loader. 
	 *  
	 * @param moduleIdentifier - Absolute module identifier
	 * @return Relative module path
	 */
	public String getRelativePath(final String moduleIdentifier);
	
	/**
	 * Return absolute file path for a resolved module identifier. 
	 *  
	 * @param moduleIdentifier - Absolute module identifier
	 * @return Absolute module location
	 */
	public URL getAbsolutePath(final String moduleIdentifier);
}