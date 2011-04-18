package org.dtk.resources.build;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utility class providing an easier mechanism to access
 * layer properties in JS environment rather than complex
 * Java collection types. Processes input parameters, 
 * providing variables as simple types. 
 * 
 * @author James Thomas
 */

public class Layer {
	private String name;
	private String[] dependencies; 
	
	public Layer(Map<String, Object> layerProps) {
		// Extract layer name from properties map
		this.name = (String) layerProps.get("name");
		
		List<Map<String, String>> layerModules = (List<Map<String, String>>) layerProps.get("modules");
		
		if (layerModules != null) {
			int moduleCount = 0; 
			this.dependencies = new String[layerModules.size()];
			
			Iterator<Map<String, String>> modulesIter = layerModules.iterator();
			
			while (modulesIter.hasNext()) {
				Map<String, String> layerModule = modulesIter.next();
				this.dependencies[moduleCount] = layerModule.get("name");				
				moduleCount++;
			}
		}
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String[] getDependencies() {
		return this.dependencies;
	}
	
	public void setDependencies(String[] dependencies) {
		this.dependencies = dependencies;
	}
}
