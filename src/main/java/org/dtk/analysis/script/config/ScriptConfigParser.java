package org.dtk.analysis.script.config;

import java.util.Map;

/**
 * Interface to provide an API for accessing module configuration
 * values from a parsed JavaScript source file. 
 * 
 * @author James Thomas
 */

public interface ScriptConfigParser {		
	
	/**
	 * Look up for all identified configuration values 
	 * found during analysis of a JavaScript source file.
	 * 
	 * @return Configuration values 
	 */
	public Map<String, Object> getScriptConfig();
}
