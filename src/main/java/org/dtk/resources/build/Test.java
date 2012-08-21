package org.dtk.resources.build;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.dtk.resources.packages.PackageRepository;
import org.dtk.util.FileUtil;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	public static void main(String[] args) {
		// Use Rhino's global object's as prototype for top scope because
		// logger.js assumes access to "print" function. 
		Global global = new Global(); 
		Context cx = ContextFactory.getGlobal().enterContext(); 
		global.init(cx); 

		// Set up standard scripts
		Scriptable topScope = cx.initStandardObjects(global);

		cx.getWrapFactory().setJavaPrimitiveWrap(false);
		
		try {
			String scriptContents = FileUtils.readFileToString(new File("/Users/james/IBM/Code/dwb/src/main/webapp/js/build/amd_loader/dojo.js"));
		
			scriptContents = "djConfig = {buildReference: false, packages:[{name:'build', lib:'.', location:'/Users/james/IBM/Code/dwb/src/main/webapp/js/build/bdbuild'}]};" + scriptContents;
			
			Script script = cx.compileString(scriptContents, "testing", 1, null);
		
			String[] test = {
					"load=build",
					"baseUrl=/Users/james/Code/JavaScript/Libraries/DTK/gh/dojo", 
					"action=release",
					"releaseDir=/tmp",					
					
					"profile=/var/folders/vW/vWWvubkrGQigEDaT1bf7mk+++TI/-Tmp-/dojo_web_builder2882495873173706775.tmp/Rb9dt193EcK9g6GjQjJOhhuL0NE_/build.profile.js",
					};
			/*
			baseUrl=
			action=release
			releaseDir=/var/folders/vW/vWWvubkrGQigEDaT1bf7mk+++TI/-Tmp-/dojo_web_builder176030933906566663.tmp/Rb9dt193EcK9g6GjQjJOhhuL0NE_
			load=build
			profile=/var/folders/vW/vWWvubkrGQigEDaT1bf7mk+++TI/-Tmp-/dojo_web_builder176030933906566663.tmp/Rb9dt193EcK9g6GjQjJOhhuL0NE_/build.profile.js
			*/
			
			ScriptableObject.putConstProperty(topScope, "arguments", test);
			
			// Exec the build script.
			script.exec(cx, topScope);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	
	/*
	public static void main(String[] args) throws JsonParseException, JsonMappingException, NoSuchAlgorithmException, IOException {
		ProfileBuilder pb = new ProfileBuilder("/var/folders/vW/vWWvubkrGQigEDaT1bf7mk+++TI/-Tmp-/dojo_web_builder7490853362885271835.tmp/KR6OSJbEp0eXF3nDepe_I46t54U_/build.profile.js", "/tmp/dojo", "/Users/james/Code/DTK/dojotoolkit/dojo/dojo.js");
		
		if (!pb.executeBuild()) {
			System.out.println(pb.getBuildError());
		} else {
			
		}
		
		
		return;
		/*
		PackageRepository.getInstance().setPackageBaseLocation("/Users/james/IBM/Code/dwb/src/main/config/packages/");
		
		List<Map<String, String>> packages = new ArrayList<Map<String, String>>();
		
		Map dojoPackage = new HashMap<String, String>();
		dojoPackage.put("name", "dojo");
		dojoPackage.put("version", "1.6.0");
		
		packages.add(dojoPackage);
		
		
		List<Map<String, Object>> layers = new ArrayList<Map<String, Object>>();
		
		Map simpleLayer = new HashMap<String, Object> ();
		
		simpleLayer.put("name", "dojo.js");
		simpleLayer.put("modules", new ArrayList<Map<String, String>>() {{ 
			add(new HashMap<String, String>() {{
				put("name", "dijit.form.Button");
				put("package", "dojo");
			}});
		}});

		layers.add(simpleLayer);
		
		//{"optimise":"shrinksafe","cdn":"none","platforms":"all","themes":"none","cssOptimise":"comments",,"layers":[{"name":"dojo.js","modules":[{"name":"dijit.form.Button","package":"dojo"}]}]}
		
		BuildRequest br = new BuildRequest(packages, "none", "shrinksafe", "comments", "all", "none", layers);
		
		System.out.println(br.getProfileText());*/
//	}
}
