package org.dtk.analysis;

import java.util.Set;

import org.dtk.analysis.exceptions.ModuleSourceNotAvailable;
import org.dtk.analysis.exceptions.UnknownModuleIdentifier;

/**
 * Extension to the ModuleAnalysis interface that provides the ability to recursively
 * analysis source materials for module references. Source files for modules discovered 
 * during the analysis phase will be retrieved and recursively scanned for further module 
 * dependencies. 
 * 
 * Retrieved source files will be made available for access after analysis has completed.
 * Recursive parsing can be explicitly turned off for modules from a list of pre-defined packages. 
 * 
 * @author James Thomas
 */

public interface RecursiveModuleAnalysis extends ModuleAnalysis {
	
	/**
	 * Return module source parsed during analysis.
	 * 
	 * @param moduleIdentifier - Module identifier for source
	 * @return Module's definition source
	 * @throws ModuleSourceNotAvailable - Module identifier belongs to a package that had
	 * recursive parsing turned off
	 * @throws UnknownModuleIdentifier - Module identifier provided wasn't discovered during analysis 
	 */
	public String getModuleSource(String moduleIdentifier) throws ModuleSourceNotAvailable, UnknownModuleIdentifier;
	
	/**
	 * Has the referenced module being recursively analysed during parsing? If so, 
	 * the module source will be available for reading.
	 * 
	 * @param moduleIdentifier - Module identifier discovered during analysis
	 * @return Module source contents
	 * @throws UnknownModuleIdentifier - Module identifier wasn't discovered during analysis
	 */
	public boolean isModuleSourceAvailable(String moduleIdentifier) throws UnknownModuleIdentifier;
	
	/**
	 * Set a list of explicit package identifiers whose modules shouldn't 
	 * be recursively parsing during analysis. These modules will be identified 
	 * but source files won't be pulled down and analysed for module dependencies.
	 * 
	 * @param packagesToIgnore - Package identifiers
	 */
	public void setIgnoredPackages(Set<String> packagesToIgnore);
	
	/**
	 * Retrieve the set of package identifiers whose modules will be ignored
	 * during recursively module analysis.  These modules will be identified 
	 * but source files won't be pulled down and analysed for module dependencies.
	 * 
	 * @return List of package identifiers
	 */
	public Set<String> getIgnoredPackages();
}