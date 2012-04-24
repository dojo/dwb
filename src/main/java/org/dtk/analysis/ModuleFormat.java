package org.dtk.analysis;

/**
 * Enumeration of the supported module formats by the Dojo Web Builder's
 * auto-analysis tools. 
 * 
 * @author James Thomas
 */

public enum ModuleFormat {
	AMD,
	NON_AMD;
	
	/**
	 * Lookup path separator character used in module paths for a 
	 * given module format
	 * 
	 * @param format - Module format
	 * @return path separator, defaults to '.'
	 */
	public static char getPathSeparator(ModuleFormat format) {
		switch(format) {
			case AMD:
				return '/';
			case NON_AMD:
			default:
				return '.';
		}
	}
}
