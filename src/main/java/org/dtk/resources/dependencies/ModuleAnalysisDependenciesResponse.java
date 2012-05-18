package org.dtk.resources.dependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dtk.analysis.ModuleAnalysis;
import org.dtk.analysis.exceptions.FatalAnalysisError;
import org.dtk.resources.dependencies.DojoScriptVersions.Versions;

public class ModuleAnalysisDependenciesResponse implements DependenciesResponse {

	List<String> requiredDojoModules = new ArrayList<String>(),
		availableModules = new ArrayList<String>();
	
	List<Map<String, String>> temporaryPackages = new ArrayList<Map<String, String>>();
	
	public ModuleAnalysisDependenciesResponse(ModuleAnalysis moduleAnalysis) throws FatalAnalysisError {
		Map<String, List<String>> pageModules = moduleAnalysis.getModules();
		Set<String> packageNames = pageModules.keySet();
		Set<String> DTK = new HashSet<String>() {{
			add("dojo"); add("dijit"); add("dojox");
		}};
		
		for(String packageName: packageNames) {
			List<String> packageModules = pageModules.get(packageName);
			if (packageModules != null) {
				if (DTK.contains(packageName)) {
					requiredDojoModules.addAll(packageModules);
				} else {
					availableModules.addAll(packageModules);					
				}					
			}				
		}
	}
	
	public ModuleAnalysisDependenciesResponse(ModuleAnalysis moduleAnalysis, String customPackageId) throws FatalAnalysisError {
		this(moduleAnalysis);
		
		Map<String, String> packageDetails = new HashMap<String, String>();
						
		packageDetails.put("name", customPackageId);
		packageDetails.put("version", "1.0.0");

		temporaryPackages.add(packageDetails);
	}
	
	@Override
	public List<String> getRequiredDojoModules() {
		return requiredDojoModules;
	}

	@Override
	public List<String> getAvailableModules() {
		// TODO Auto-generated method stub
		return availableModules;
	}

	@Override
	public List<Map<String, String>> getPackages() {
		return temporaryPackages;
	}

	@Override
	public Versions getDojoVersion() {
		return DojoScriptVersions.Versions.UNKNOWN;
	}

}
