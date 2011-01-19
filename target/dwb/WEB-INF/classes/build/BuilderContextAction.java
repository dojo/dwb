package org.dtk.resources.build;

import java.util.HashMap;
import java.util.Map;

import org.dtk.util.FileUtil;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

public class BuilderContextAction implements ContextAction {
	private String buildScriptPath;
	private BuildRequest buildRequest;

	private Exception exception;
	private Scriptable topScope;
	private Context context;
	private Map<String, String> result;

	public BuilderContextAction(String buildScriptPath, BuildRequest buildRequest) {
		this.buildScriptPath = buildScriptPath;
		this.buildRequest = buildRequest;

		this.exception = null;
		this.context = null;
		this.topScope = null;
	}

	public Object run(Context newContext) {
		context = newContext;
		context.setOptimizationLevel(-1);

		// context.getWrapFactory().setJavaPrimitiveWrap(false);

		// Use Rhino's global object's as prototype for top scope because
		// logger.js assumes access to "print" function. 
		Global global = new Global(); 
		Context cx = ContextFactory.getGlobal().enterContext(); 
		global.init(cx); 

		// Set up standard scripts
		topScope = context.initStandardObjects(global);

		try {
			//String fileName = builderPath + "build.js";
			String fileContent = FileUtil.readFromFile(this.buildScriptPath, null);
			Script script = context.compileString(fileContent, this.buildScriptPath, 1, null);

			// Expose top level scope as the "global" scope.
			//TODO: only need this for the load function, maybe there is a built in way
			//to get this.
			ScriptableObject.putProperty(topScope, "global", Context.javaToJS(topScope, topScope));            

			// Exec the build script.
			script.exec(context, topScope);

			// Call build.make(builderPath)
			Scriptable build = Context.toObject(topScope.get("build", topScope), topScope);

			/*Object args[] = {
                    builderPath,
                    version,
                    cdn,
                    layers,
                    optimize,
                    userAppPaths,
                    cachedFilePath
            };
			 */
			Object args[] = {
				buildRequest.getPackageLocation(),
				buildRequest.getVersion(),
				buildRequest.getCdn(),
				buildRequest.getPlatforms(),
				buildRequest.getThemes(),
				buildRequest.getBuildLayersArray(),
				buildRequest.getOptimise(),
				buildRequest.getCssOptimise(),
				buildRequest.temporaryApplicationPaths(),
				buildRequest.getBuildReference()
			};

			Function make = (Function) build.get("make", topScope);
			//Function make = (Function) build.get("makeTest", topScope);
			//String result = (String) make.call(context, topScope, build, args);
			Object resultObj = make.call(context, topScope, build, args);
			result = unwrapObject((ScriptableObject) Context.jsToJava(resultObj, ScriptableObject.class));     
		} catch (Exception e) {
			this.exception = e;
		}
		return null;
	}

	public static Map<String, String> unwrapObject (final ScriptableObject sObj) {
		return new HashMap<String, String> () {{
			for (Object id: sObj.getAllIds()) {
				put(id.toString(), (String) sObj.get(id.toString(), null));
			}
		}};
	}

	public Map<String, String> getResult() {
		return result;
	}

	public Exception getException() {
		return exception;
	}
}
