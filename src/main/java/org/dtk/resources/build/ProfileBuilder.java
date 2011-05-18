package org.dtk.resources.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
 * Generate custom Dojo builds using new JavaScript build tools, backdraftBuilder, 
 * and 
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

	/** All script arguments should be key=value format */
	protected static final String scriptArgFormat = "%1$s=%2$s";
	
	protected Exception buildError;
	
	public ProfileBuilder(String profileFile, String resultDir, String moduleLoaderPath) {
		this.moduleLoaderPath = moduleLoaderPath;
		
		scriptArguments.put("profile", profileFile);
		scriptArguments.put("releaseDir", resultDir);
		scriptArguments.put("baseUrl", (new File(this.moduleLoaderPath)).getParent());
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
			String moduleLoaderScript = FileUtil.readFromFile(this.moduleLoaderPath, null);
			
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
	 * Return last exception captured generating a build 
	 * correctly. 
	 * 
	 * @return Exception - Error thrown by rhino running the build scripts
	 */
	protected Exception getBuildError() {
		return buildError;
	}
	
	/**
	 * Return paraent directory for module loader
	 * 
	 * @return String - Parent module loading path
	 */
	protected String getModuleLoaderBaseDir() {
		return (new File(moduleLoaderPath)).getParent();
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
			java.util.Map.Entry<String, String> scriptArg = scriptArgsIter.next();
			scriptArgsList.add(String.format(scriptArgFormat, scriptArg.getKey(), scriptArg.getValue()));
		}
		
		return scriptArgsList.toArray(new String[0]);
	}
}
