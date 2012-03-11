package org.dtk.resources.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.dtk.util.FileUtil;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

/**
 * This class is responsible for generating custom Dojo builds by executing the existing
 * JavaScript build system. Build properties are controlled using a profile file, a reference
 * to which is passed in at runtime. 
 * 
 * @author James Thomas
 */

public class ProfileBuilder {
	/** Script arguments, key value pairs, which are translated to a 
	 * single string with key=value attributes. All arguments are used
	 * to control the build scripts. */
	protected Map<String, String> scriptArguments = new HashMap<String, String>() {
		{
			put("load", "build");
			put("action", "release");
		}
	};
	
	/** AMD module loader script path, e.g. dojo */
	protected String moduleLoaderPath;

	/** AMD build package location */
	protected String buildPackagePath;
	
	/** Unique reference for this build, used for logging */
	protected String buildReference;
	
	/** All script arguments should be key=value format */
	protected static final String scriptArgFormat = "%1$s=%2$s";
	
	/** Local AMD package descriptors format, picked up by AMD module loader */
	protected static final String djConfigPrefixFormat 
		= "djConfig = {buildReference: '%1$s', packages:[{name:'build', lib:'.', location:'%2$s'}]};";	
	
	protected Exception buildError;
	
	/**
	 * Generate new ProfileBuilder using the arguments passed to control
	 * the build process. All file paths are santised, swapping back slashes 
	 * encountered in Windows paths for forward slashes. 
	 * 
	 * Leaving backslashes causes numerous issues with the the build scripts on 
	 * the Windows platform.   
	 * 
	 * @param profileFile - Location to profile file for this build
	 * @param resultDir - Where to store the resulting build artifacts
	 * @param moduleLoaderPath - Location to AMD loader package
	 * @param baseUrl - Base Dojo URL being built with 
	 * @param buildPackagePath - AMD Builder package location
	 */
	public ProfileBuilder(String profileFile, String resultDir, String moduleLoaderPath, 
		String baseUrl, String buildPackagePath, String buildReference) {
		this.moduleLoaderPath = santisePath(moduleLoaderPath);
		this.buildPackagePath = santisePath(buildPackagePath);
		this.buildReference = buildReference;
		
		scriptArguments.put("profile", santisePath(profileFile));
		scriptArguments.put("releaseDir", santisePath(resultDir));
		scriptArguments.put("baseUrl", santisePath(baseUrl));
	}
	
	/**
	 * Initiate the build process for the profile given. Will try to execute
	 * the JS build system scripts for this profile, reporting whether it 
	 * executed successfully.
	 * 
	 * @return Build completed successfully
	 */
	public boolean executeBuild() {
		boolean buildCompleted = true;
		
		// Use Rhino's global object's as prototype for top scope because
		// logger.js assumes access to "print" function. 
		Global global = new Global(); 
		Context cx = ContextFactory.getGlobal().enterContext(); 
		global.init(cx); 

		// Set up standard scripts objects
		Scriptable topScope = cx.initStandardObjects(global);

		// Enforce conversion of the Java string arguments array to JavaScript native versions.
		// Leaving this as true, causes issues in the build scripts. 
		cx.getWrapFactory().setJavaPrimitiveWrap(false);

		// Rhino may throw a number of exceptions due to a variety of the build errors, use generic catch to 
		// get details and store for access. 
		try {
			String moduleLoaderScript = readModuleLoaderSource();			
			Script moduleLoader = cx.compileString(moduleLoaderScript, "moduleLoader", 1, null);
			
			// Pretend these arguments came from the command line by stuffing them into the top context,
			// module loader expects to read them from here. 
			ScriptableObject.putConstProperty(topScope, "arguments", getBuildScriptArguments());

			// Execute the build system scripts to generate optimised dojo builds
			moduleLoader.exec(cx, topScope);	
		} catch (Exception buildError) {
			buildCompleted = false;
			this.buildError = buildError;
		}		
		
		return buildCompleted; 
	}
	
	/**
	 * Read AMD loader script source, with local package
	 * descriptor information prefixed at the top.
	 * 
	 * @return Module loader script source 
	 * @throws IOException - Unable to find module loader 
	 */
	protected String readModuleLoaderSource() throws IOException {
		String djConfigScriptPrefix = String.format(djConfigPrefixFormat, this.buildReference, this.buildPackagePath);
		String moduleLoaderScript = FileUtils.readFileToString(new File(this.moduleLoaderPath));
		
		return djConfigScriptPrefix + moduleLoaderScript;
	}
	
	/**
	 * Return last exception captured generating a build 
	 * correctly. 
	 * 
	 * @return Exception - Error thrown by rhino running the build scripts
	 */
	protected Exception getBuildError() {
		return buildError;
	}
	
	/**
	 * Convert map of key value pairs to ordered array containing 
	 * arguments formated correctly, key=value key1=value1 key2=value2 etc. 
	 * 
	 * @return String[] Script arguments array 
	 */
	protected String[] getBuildScriptArguments() {
		Iterator<Entry<String, String>> scriptArgsIter = scriptArguments.entrySet().iterator();
		List<String> scriptArgsList = new ArrayList<String>();
		
		while(scriptArgsIter.hasNext()) {
			Map.Entry<String, String> scriptArg = scriptArgsIter.next();
			scriptArgsList.add(String.format(scriptArgFormat, scriptArg.getKey(), scriptArg.getValue()));
		}
		
		return scriptArgsList.toArray(new String[0]);
	}
	
	/**
	 * Ensure all file paths have no backslashes, replacing 
	 * any found with forwardslash equivalents. This is to 
	 * remove problems encountered on the Windows platform. 
	 * 
	 * @param path - Path to a file we're referencing 
	 * @return Santised path 
	 */
	protected String santisePath(String path) {
		return path.replace('\\', '/');
	}
}
