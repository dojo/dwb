package org.dtk.resources.dependencies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dtk.analysis.ModuleAnalysis;
import org.dtk.analysis.ModuleFormat;
import org.dtk.analysis.exceptions.FatalAnalysisError;
import org.dtk.resources.dependencies.DojoScriptVersions.Versions;

public class ExplicitModuleFormatAnalysisDependenciesResponse extends ModuleAnalysisDependenciesResponse {
	
	ModuleFormat explicitModuleFormat, currentModuleFormat;
	
	public ExplicitModuleFormatAnalysisDependenciesResponse(ModuleAnalysis moduleAnalysis, 
		ModuleFormat moduleFormat) throws FatalAnalysisError  {
		super(moduleAnalysis);		

		this.explicitModuleFormat = moduleFormat;
		this.currentModuleFormat = moduleAnalysis.getModuleFormat();						
	}
	
	public ExplicitModuleFormatAnalysisDependenciesResponse(ModuleAnalysis moduleAnalysis, String customModuleId,
		ModuleFormat moduleFormat) throws FatalAnalysisError  {
			super(moduleAnalysis, customModuleId);		

			this.explicitModuleFormat = moduleFormat;
			this.currentModuleFormat = moduleAnalysis.getModuleFormat();						
	}
	
	@Override
	public List<String> getRequiredDojoModules() {
		if (mustTransformModuleIdentifiers()) {
			updateAllModuleIdentifiers();
		}
		
		return requiredDojoModules;
	}

	@Override
	public List<String> getAvailableModules() {
		if (mustTransformModuleIdentifiers()) {
			updateAllModuleIdentifiers();
		}
		
		return availableModules;
	}
	
	protected void updateAllModuleIdentifiers() {
		requiredDojoModules = convertModuleIdentifiers(requiredDojoModules);
		availableModules = convertModuleIdentifiers(availableModules);
		currentModuleFormat = explicitModuleFormat;
	}
	
	protected List<String> convertModuleIdentifiers(List<String> moduleIdentifiers) {
		List<String> convertedIdentifiers = new ArrayList<String>();
		
		for(String moduleIdentifier: moduleIdentifiers) {
			convertedIdentifiers.add(moduleIdentifier.replace(ModuleFormat.getPathSeparator(currentModuleFormat), 
				ModuleFormat.getPathSeparator(explicitModuleFormat)));
		}
		
		return convertedIdentifiers;
	}

	protected boolean mustTransformModuleIdentifiers() {
		return !explicitModuleFormat.equals(currentModuleFormat); 
	}
}
