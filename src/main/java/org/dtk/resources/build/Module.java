package org.dtk.resources.build;

/**
 * Utility class providing an easier mechanism to access
 * module properties in JS environment rather than complex
 * Java collection types. 
 * 
 * @author James Thomas
 */

public class Module {
	private String name;
	private String packageId;
	
	public Module(String name, String packageId) {
		this.name = name;
		this.packageId = packageId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPackageId() {
		return packageId;
	}

	public void setPackageId(String packageId) {
		this.packageId = packageId;
	}
}
