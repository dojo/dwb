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
		
		List<String> modulesDetails = (List<String>)  layerProps.get("modules");
		
		if (modulesDetails != null) {
			this.dependencies = new String[modulesDetails.size()];
			
			int moduleCount = 0; 
			
			Iterator<String> modulesIter = modulesDetails.iterator();
			
			while (modulesIter.hasNext()) {
				this.dependencies[moduleCount] = modulesIter.next();				
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
