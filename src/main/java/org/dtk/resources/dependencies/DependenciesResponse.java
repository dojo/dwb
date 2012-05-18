package org.dtk.resources.dependencies;

import java.util.List;
import java.util.Map;

import org.dtk.resources.dependencies.DojoScriptVersions.Versions;

public interface DependenciesResponse {
	public List<String> getRequiredDojoModules();
	
	public List<String> getAvailableModules();
	
	public List<Map<String, String>> getPackages();
	
	public Versions getDojoVersion();
}
