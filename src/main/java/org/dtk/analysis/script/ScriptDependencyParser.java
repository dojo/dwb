package org.dtk.analysis.script;

import java.util.List;

/**
 * Interface to provide an API for accessing module 
 * dependencies from a parsed JavaScript source file. 
 * 
 * @author James Thomas
 */

public interface ScriptDependencyParser {		
	
	/**
	 * List of all identified module dependencies
	 * found during analysis of a JavaScript source file.
	 * 
	 * @return Identifier module dependencies
	 */
	public List<String> getModuleDependencies();
}
