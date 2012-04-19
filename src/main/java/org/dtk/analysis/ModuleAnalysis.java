package org.dtk.analysis;

import java.util.List;
import java.util.Map;

import org.dtk.analysis.exceptions.FatalAnalysisError;

/**
 * Interface that provides API for retrieving module identifiers discovered
 * during automatic resource analysis.
 * 
 * @author James Thomas
 */

public interface ModuleAnalysis {
	
	/**
	 * Retrieve the module identifiers identified during analysis of the
	 * implementing resource source material. Modules identified are organised
	 * by their respective packages. 
	 * 
	 * @return Map of packages -> module identifers discovered during analysis
	 * @throws FatalErrorPerformingAnalysis - Unable to perform analysis, internal 
	 * error prevented module discovery from completing
	 */
	public Map<String, List<String>> getModules() throws FatalAnalysisError;
}
