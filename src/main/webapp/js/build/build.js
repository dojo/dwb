// This file contains customised versions of functions used in the Dojo build process 
// that are required for the Web Builder to operate correctly.

function writeLogLine(reference, logLine) {
	var newLogLine = logLine + "\n";	  
	writeLog(reference, newLogLine);
}

function writeLog(reference, logLine) {	
	//summary: Utility function to wrap Java build logger package	  
	// Grab logger instance
	var logger = Packages.org.dtk.resources.build.manager.BuildStatusManager.getInstance();
	logger.addNewBuildLog(reference, logLine);
}


//function load(/*String*/fileName){
	//summary: opens the file at fileName and evals the contents as JavaScript.
	
	//Read the file
//	var fileContents = readFile(fileName);

	//Eval the contents.
//	var Context = Packages.org.mozilla.javascript.Context;
//	var context = Context.enter();
//	try{
//		return context.evaluateString(this, fileContents, fileName, 1, null);
//	}finally{
//		Context.exit();
//	}
//}

//function readFile(/*String*/path, /*String?*/encoding){
/*    return fileUtil.readFile(path, encoding);
	//summary: reads a file and returns a string
	encoding = encoding || "utf-8";
	var file = new java.io.File(path);
	var lineSeparator = "\n";
	var input = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(file), encoding));
	try {
		var stringBuffer = new java.lang.StringBuffer();
		var line = "";
		while((line = input.readLine()) !== null){
			stringBuffer.append(line);
			stringBuffer.append(lineSeparator);
		}
		//Make sure we return a JavaScript string and not a Java string.
		return new String(stringBuffer.toString()); //String
	} finally {
		input.close();
	}
}*/


/* Our build process has modified multiple parts of the existing Dojo build scripts.
 * All modified functions are included below and used instead of their original versions */

function i18nUtil_setup (/*Object*/kwArgs){
	//summary: loads dojo so we can use it for i18n bundle flattening.
	
	//Do the setup only if it has not already been done before.
	if(typeof djConfig == "undefined" || !(typeof dojo != "undefined" && dojo["i18n"])){
        // Pull Dojo path direct from module prefixes
        var dojoPath = kwArgs.profileProperties.dependencies.prefixes[0][1] + '/';		

		djConfig={
			locale: 'xx',
			extraLocale: kwArgs.localeList,
			baseUrl: dojoPath
		};

		load(dojoPath + "dojo.js");

		//Now set baseUrl so it is current directory, since all the prefixes
		//will be relative to the release dir from this directory.
		//dojo.baseUrl = "./";

		//Also be sure we register the right paths for module prefixes.
		buildUtil.configPrefixes(kwArgs.profileProperties.dependencies.prefixes);

		dojo.require("dojo.i18n");


        // Replace the load function to transform all source input before 
        // eval'ing 
        dojo._loadUri = function (uri, cb) {
            buildUtil._load(uri);

            if (cb) {
                cb();
            }
            return true;
        };
	}
}

function flattenLayerFileBundles (/*String*/fileName, /*String*/fileContents, /*Object*/ resultFileArchive,
	/*Object*/kwArgs, /*String packageRef*/ packageRef){
	//summary:
	//		This little utility is invoked by the build to flatten all of the JSON resource bundles used
	//		by dojo.requireLocalization(), much like the main build itself, to optimize so that multiple
	//		web hits will not be necessary to load these resources.  Normally, a request for a particular
	//		bundle in a locale like "en-us" would result in three web hits: one looking for en_us/ another
	//		for en/ and another for ROOT/.  All of this multiplied by the number of bundles used can result
	//		in a lot of web hits and latency.  This script uses Dojo to actually load the resources into
	//		memory, then flatten the object and spit it out using dojo.toJson.  The bootstrap
	//		will be modified to download exactly one of these files, whichever is closest to the user's
	//		locale.
	//fileName:
	//		The name of the file to process (like dojo.js). This function will use
	//		it to determine the best resource name to give the flattened bundle.
	//fileContents:
	//		The contents of the file to process (like dojo.js). This function will look in
	//		the contents for dojo.requireLocation() calls.
	//kwArgs:
	//		The build's kwArgs.
	var destDirName = "nls";
	var nlsNamePrefix = fileName.replace(/\.js$/, "");
	
	//nlsNamePrefix = nlsNamePrefix.substring(nlsNamePrefix.lastIndexOf("/") + 1, nlsNamePrefix.length);

	i18nUtil_setup(kwArgs);
	var djLoadedBundles = [];
	
	//TODO: register plain function handler (output source) in jsonRegistry?
	var drl = dojo.requireLocalization;
	var dupes = {};
	dojo.requireLocalization = function(modulename, bundlename, locale){
		var dupName = [modulename, bundlename, locale].join(":");
		if(!dupes[dupName]){
			drl(modulename, bundlename, locale);
			djLoadedBundles.push({modulename: modulename, module: eval(modulename), bundlename: bundlename});
			dupes[dupName] = 1;
		}
	};
	
	var requireStatements = fileContents.match(/dojo\.requireLocalization\(.*\)\;/g);
	if(requireStatements){	
		writeLogLine(packageRef, "Discovered localisation bundles referenced by layer "+fileName);
		
		eval(requireStatements.join(";"));
		
		var djBundlesByLocale = {};
		var jsLocale, entry, bundle;
		
		for (var i = 0; i < djLoadedBundles.length; i++){
			entry = djLoadedBundles[i];
			bundle = entry.module.nls[entry.bundlename];
			
			for (jsLocale in bundle){
				if (!djBundlesByLocale[jsLocale]){djBundlesByLocale[jsLocale]=[];}
				djBundlesByLocale[jsLocale].push(entry);
			}
		}
		
		localeList = [];
		
		// All NLS files will reside under root archive NLS directory. 
		var modulePrefix = "dojo.nls." + nlsNamePrefix;

		writeLog(packageRef, "Creating combined localisation bundles... ");
		for (jsLocale in djBundlesByLocale){
			var locale = jsLocale.replace(/\_/g, '-');
			
			var resourceFilePath = "nls/" + nlsNamePrefix + "_" + locale + ".js";
			
			var resourceContents = "dojo.provide(\""+modulePrefix+"_"+locale+"\");";
			for (var j = 0; j < djLoadedBundles.length; j++){
				entry = djLoadedBundles[j];
				var bundlePkg = [entry.modulename,"nls",entry.bundlename].join(".");
				var translationPkg = [bundlePkg,jsLocale].join(".");
				bundle = entry.module.nls[entry.bundlename];
				if(bundle[jsLocale]){ //FIXME:redundant check?
					resourceContents += "dojo.provide(\""+bundlePkg+"\");";
					resourceContents += bundlePkg+"._built=true;";
					resourceContents += "dojo.provide(\""+translationPkg+"\");";
					resourceContents += translationPkg+"="+dojo.toJson(bundle[jsLocale])+";";
				}
			}
			
			resultFileArchive[resourceFilePath] = resourceContents;
				
			localeList.push(locale);
		}
		
		//Remove dojo.requireLocalization calls from the file.
		fileContents = fileContents.replace(/dojo\.requireLocalization\(.*\)\;/g, "");

		var preloadCall = '\ndojo.i18n._preloadLocalizations("' + modulePrefix + '", ' + dojo.toJson(localeList.sort()) + ');\n';
		//Inject the dojo._preloadLocalizations call into the file.
		//Do this at the end of the file, since we need to make sure dojo.i18n has been loaded.
		//The assumption is that if dojo.i18n is not in this layer file, dojo.i18n is
		//in one of the layer files this layer file depends on.
		//Allow call to be inserted in the dojo.js closure, if that is in play.
		i18nUtil.preloadInsertionRegExp.lastIndex = 0;
		if(fileContents.match(i18nUtil.preloadInsertionRegExp)){
			i18nUtil.preloadInsertionRegExp.lastIndex = 0;
			fileContents = fileContents.replace(i18nUtil.preloadInsertionRegExp, preloadCall);
		}else{
			fileContents += preloadCall;
		}

		writeLogLine(packageRef, "done");
	}

	return fileContents; //String
}

//TODO: inlining this function since the new shrinksafe.jar is used, and older
//versions of Dojo's buildscripts are not compatible.
function optimizeJs(/*String fileName*/fileName, /*String*/fileContents, /*String*/copyright, /*String*/optimizeType, /*String*/stripConsole, /*String*/ packageRef){
	//summary: either strips comments from string or compresses it.
	copyright = copyright || "";

	//Use rhino to help do minifying/compressing.
	var context = Packages.org.mozilla.javascript.Context.enter();
	try{
		// Use the interpreter for interactive input (copied this from Main rhino class).
		context.setOptimizationLevel(-1);

		// the "packer" type is now just a synonym for shrinksafe
		if(optimizeType.indexOf("shrinksafe") == 0 || optimizeType == "packer"){
			writeLog(packageRef, "Running Shrinksafe compression on the layer contents...");
			//Apply compression using custom compression call in Dojo-modified rhino.
			fileContents = new String(Packages.org.dojotoolkit.shrinksafe.Compressor.compressScript(fileContents, 0, 1, stripConsole));
			if(optimizeType.indexOf(".keepLines") == -1){
				fileContents = fileContents.replace(/[\r\n]/g, "");
			}
			writeLogLine(packageRef, " done");
		}else if (optimizeType.indexOf("closure") == 0) {
			writeLog(packageRef, "Running Closure compression on the layer contents...");
			var jscomp = com.google.javascript.jscomp;

			//Fake extern
			var externSourceFile = buildUtil.closurefromCode("fakeextern.js", " ");

			//Set up source input
			var jsSourceFile = buildUtil.closurefromCode(String(fileName), String(fileContents));
		
			//Set up options
			var options = new jscomp.CompilerOptions();
			options.prettyPrint = optimizeType.indexOf(".keepLines") !== -1;
			
			var FLAG_compilation_level = jscomp.CompilationLevel.SIMPLE_OPTIMIZATIONS;			
			FLAG_compilation_level.setOptionsForCompilationLevel(options);			
			
			var FLAG_warning_level = jscomp.WarningLevel.DEFAULT;			
			FLAG_warning_level.setOptionsForWarningLevel(options);

			//Run the compiler
			var compiler = new Packages.com.google.javascript.jscomp.Compiler(Packages.java.lang.System.err);
			result = compiler.compile(externSourceFile, jsSourceFile, options);
			fileContents = compiler.toSource();
			writeLogLine(packageRef, " done");
		}else if(optimizeType == "comments"){
			writeLog(packageRef, "Stripping comments from layer contents...");
			//Strip comments
			var script = context.compileString(fileContents, fileName, 1, null);
			fileContents = new String(context.decompileScript(script, 0));
			
			//Replace the spaces with tabs.
			//Ideally do this in the pretty printer rhino code.
			fileContents = fileContents.replace(/    /g, "\t");

			//If this is an nls bundle, make sure it does not end in a ;
			//Otherwise, bad things happen.
			if(fileName.match(/\/nls\//)){
				fileContents = fileContents.replace(/;\s*$/, "");
			}
			writeLogLine(packageRef, " done");
		}
	}finally{
		Packages.org.mozilla.javascript.Context.exit();
	}


	return copyright + fileContents;
}

// Special version of flatten CSS that reads from a local cache of file contents rather than the file system.
function flattenCss(/*String*/fileName, /*Object*/ themeFiles){
	//summary: inlines nested stylesheets that have @import calls in them.
	var fileContents = themeFiles[fileName];
	
	// Find all @import CSS statements
	return fileContents.replace(buildUtil.cssImportRegExp, function(fullMatch, urlStart, importFileName, urlEnd, mediaTypes){
		//Only process media type "all" or empty media type rules.
		if(mediaTypes && ((mediaTypes.replace(/^\s\s*/, '').replace(/\s\s*$/, '')) != "all")){
			return fullMatch;
		}

		// Normalise url quotes
		importFileName = buildUtil.cleanCssUrlQuotes(importFileName);

		// Split parent and child file paths into dir parts
		var ifnPathParts = importFileName.split("/"), 
			fnParts = fileName.split("/");
		
		// Remove CSS file suffix, leaving just directory path.
		fnParts.pop();
		
		// Construct absolute import path, from current directory and 
		// relative file import path. Resolve '..' path statements. 
		for (var i = 0; i < ifnPathParts.length; i++) {
			// For each '..' encountered in import URL, go up a 
			// directory.
			if (ifnPathParts[i] === "..") {
				fnParts.pop();
			} else {
				// No more '..' parts, add remaining import parts to construct
				// absolute path and exit loop.
				ifnPathParts = ifnPathParts.slice(i, ifnPathParts.length);
				break;
			}
		}
		
		// Make path string from file path parts
		var importFileName = fnParts.concat(ifnPathParts).join("/");
		
		// Recursively inline any @import statement in this inlined CSS
		// content.
		var flattenImport = flattenCss(importFileName, themeFiles);
		
		// Any remaining CSS URL statement refer to images, fix paths to be relative to parent CSS we are 
		// inlined into rather than normal file. 
		return flattenImport.replace(buildUtil.cssUrlRegExp, function(fullMatch, urlMatch){
			var fixedUrlMatch = buildUtil.cleanCssUrlQuotes(urlMatch);
			fixedUrlMatch = fixedUrlMatch.replace(buildUtil.backSlashRegExp, "/");

			//Only do the work for relative URLs. Skip things that start with / or have
			//a protocol.
			var colonIndex = fixedUrlMatch.indexOf(":");
			
			var importPath = importFileName.slice(0, importFileName.lastIndexOf('/') + 1);
			
			if(fixedUrlMatch.charAt(0) != "/" && (colonIndex == -1 || colonIndex > fixedUrlMatch.indexOf("/"))){
				//It is a relative URL, tack on the path prefix
				urlMatch = importPath + fixedUrlMatch;
			}else{
				print(importFileName + "\n  URL not a relative URL, skipping: " + urlMatch);
			}

			//Collapse .. and .
			var parts = urlMatch.split("/");
			for(var i = parts.length - 1; i > 0; i--){
				if(parts[i] == "."){
					parts.splice(i, 1);
				}else if(parts[i] == ".."){
					if(i != 0 && parts[i - 1] != ".."){
						parts.splice(i - 1, 2);
						i -= 1;
					}
				}
			}
			
			// Now convert this to be relative to the current path, from common root.
			var fileNameParts = fileName.slice(0, fileName.lastIndexOf('/')).split("/");
			
			var prefixMatches = true;
			
			i = 0;
			
			// Remove all common path prefix dirs, then replace all remaining 
			// dirs in importing CSS file path with ".." elements, to get back to common root. 
			// Finally, add file suffix path from imported URL relative to local CSS.
			while (i < fileNameParts.length) {
				if (prefixMatches === true && fileNameParts[i] === parts[i]) {
					// Remove this matching path prefix 
					fileNameParts = fileNameParts.splice(i + 1, fileNameParts.length);
					parts = parts.splice(i + 1, parts.length);
				} else {
					// Don't match anymore, paths have diverged
					if (prefixMatches) { 
						prefixMatches = false;
					}
					// Replace additional parent directories with '..' to reach common root.
					fileNameParts[i++] = "..";
				}
			}
			
			// Add on path suffix to create new relative URL.
			var newRelativePath = fileNameParts.concat(parts).join("/");
			
			return "url('" + newRelativePath + "')";
		});
	});
}

function findModulesAndRequires(/*String fileName*/srcRoot, /*String rootDojoDir*/dojoDir) {
    return [findModules(srcRoot), findRequires(srcRoot)];
}

function findRequires(/*String fileName*/srcRoot) {
	//Get a list of files that might be modules.
	var fileList = fileUtil.getFilteredFileList(srcRoot, /\.js$/, true);

	// Rhino RegExp engine doesn't seem to work properly, failing back to Java's.
	var regex = "(dojo.require\\(\")([dojox|dijit|dojox](.)+?)(\")";

	var p = java.util.regex.Pattern.compile(regex);
	
	// Use object rather that list to handle multiple modules
	var requires = [];
	
	//Search the modules with dojo.require() calls.
	for(var i = 0; i < fileList.length; i++){
		var fileName = fileList[i];
		var fileContents = new fileUtil.readFile(fileName);

		var m = p.matcher(java.lang.String(fileContents));

		while (m.find()) {
			// Convert Java String object to JS String
			var module = m.group(2) + "";
			if (requires.indexOf(module) == -1) {
				requires.push(module);
			}				
		}	
	}
	
	return requires;
}

function findModules(/*String fileName*/srcRoot) {
	//Get a list of files that might be modules.
	var fileList = fileUtil.getFilteredFileList(srcRoot, /\.js$/, true);

	var provideRegExp = /dojo\.provide\(\".*\"\)/g;

	//Search the modules for a matching dojo.provide() call.
	//Need to do this because some files (like nls/*.js files) are
	//not really modules.
	var provideList = [];
	for(var i = 0; i < fileList.length; i++){
		var fileName = fileList[i];
		var fileContents = new fileUtil.readFile(fileName);

		var matches = fileContents.match(provideRegExp);
		
		if(matches){
			for(var j = 0; j < matches.length; j++){
				//strip off the .js file extension
				var modFileName = fileName.substring(0, fileName.length - 3);
				var provideName = matches[j].substring(matches[j].indexOf('"') + 1, matches[j].lastIndexOf('"'));

				//Skip certain kinds of modules not needed in end use.
				if(provideName.indexOf("tests.") != -1
					|| provideName.indexOf("._") != -1
					|| provideName.indexOf(".robot") != -1){
					continue;
				}

				//Only allow the provides that match the file name.
				//So, the different dijit permutations of dijit.form.Button will not show up.
				if (modFileName.lastIndexOf(provideName.replace(/\./g, "/")) == modFileName.length - provideName.length){
					provideList.push(provideName);
					break;
				}
			}
		
		}
	}

	provideList = provideList.sort();
	
	return provideList;
}


function findDijitThemeFiles(/*String filename*/ dojoSrcDir, /*String dirName*/ dijitThemeName) {
	// Store relative file path to contents look up 
	var themeContents = {};

	// Generic, non-theme specific, files, e.g. general dijit CSS
	var fileList = [
	    dojoSrcDir + "dijit/themes/dijit.css",
	    dojoSrcDir + "dijit/themes/dijit_rtl.css"
	];
	
	// Find all theme related files, i.e. any files under the dijit/themename directory. 
	var themeDir = dojoSrcDir + "dijit/themes/" + dijitThemeName;
	fileList = fileList.concat(fileUtil.getFilteredFileList(themeDir, /./, true));

	// Add all theme icons
	fileList = fileList.concat(fileUtil.getFilteredFileList(dojoSrcDir + "dijit/icons", /./, true));
	
	// Convert to relative paths and read contents
	for (var i = 0, file, trimmed; i < fileList.length; i++) {
	    file = fileList[i];
	    trimmed = file.substring(dojoSrcDir.length, file.length);
	    // Ensure content is a JS String rather than NativeStringObj
	    themeContents[trimmed] = "" + fileUtil.readFile(file);
	}
	
	return themeContents;
}

// Optimise all CSS files within the file lookup, key = file_path, value = file_content.
function optimiseCSS(/*String*/theme, /*Object fileCache*/ themeFiles, /*String optimisation*/ optimizeType) {
	var optimisedFiles = {};
	
	// Finally, add all remaining theme files to layer object.
	for (var i in themeFiles) {
		var fileContents = themeFiles[i];
		
		// Find theme's CSS file, all other required CSS files are linked via @import statements
		// and will be inlined. Both main theme CSS and rtl support.
		if (i.match(theme + "(_rtl)*\\.css$")) {
			// Should only incur flatten theme is needed.
			fileContents = flattenCss(i, themeFiles);
			
			//Do comment removal.
			try{
				var startIndex = -1;
				//Get rid of comments.
				while((startIndex = fileContents.indexOf("/*")) != -1){
					var endIndex = fileContents.indexOf("*/", startIndex + 2);
					if(endIndex == -1){
						throw "Improper comment in CSS file: " + i;
					}
					fileContents = fileContents.substring(0, startIndex) + fileContents.substring(endIndex + 2, fileContents.length);
				}
				//Get rid of newlines.
				if(optimizeType.indexOf(".keepLines") == -1){
					fileContents = fileContents.replace(/[\r\n]/g, "");
					fileContents = fileContents.replace(/\s+/g, " ");
					fileContents = fileContents.replace(/\{\s/g, "{");
					fileContents = fileContents.replace(/\s\}/g, "}");
				}else{
					//Remove multiple empty lines.
					fileContents = fileContents.replace(/(\r\n)+/g, "\r\n");
					fileContents = fileContents.replace(/(\n)+/g, "\n");
				}
			}catch(e){
				fileContents = themeFiles[i];
				logger.error("Could not optimized CSS file: " + i + ", error: " + e);
			}
			
			optimisedFiles[i] = fileContents;	
		// Add all other files that aren't CSS files
	    } else if (!i.match("\\.css$")) {
			optimisedFiles[i] = fileContents;	
	    }
	}
	
	return optimisedFiles;
}

build = {	
	make: function(
		//The path to the global dojo directory. 
        /*String*/dojoDir,
		
		//"1.1.1" or "1.3.2": used to choose directory of dojo to use.
		/*String*/version,
		
		//"google" or "aol" 
		/*String*/cdnType,
		
		//"all" or "webkit" 
		/*String*/platforms,
		
		// "none", "tundra", "claro", "soria", "nihilo"
		/*String*/theme,
		
		//Array of layer details
		/*Array*/layers,
		
		//comments, shrinksafe, none
		/*String*/optimizeType,
		
		// none, flatten
		/*String*/ cssOptimise,
		
		// Array of paths to temporary packages
		/*String*/userAppPaths,
		
		// unique reference for built package, used for logging
		/*String*/packageRef){

		//Validate.
		writeLogLine(packageRef, "Beginning build process...");
	
		// Check for local or XD build, with optional CDN path for XD build.
		var xdDojoPath = "";
		var loader = "";
		
		if(cdnType === "aol"){
			xdDojoPath = "http://o.aolcdn.com/dojo/" + version;
			loader = "xdomain";
			writeLogLine(packageRef, "Cross domain build selected, using AOL CDN.");
		} else if (cdnType === "google") {
			xdDojoPath = "http://ajax.googleapis.com/ajax/libs/dojo/" + version;
			loader = "xdomain";
			writeLogLine(packageRef, "Cross domain build selected, using Google CDN.");
		} else if (cdnType === "custom") {
			loader = "xdomain";
			writeLogLine(packageRef, "Cross domain build selected, no CDN specified");
		} else {
			writeLogLine(packageRef, "Local build selected.");
		}
		
		//Directory that holds dojo source distro. Ensure it ends with a forward slash
        if (!dojoDir.match("/$")) { 
            dojoDir = dojoDir + '/';
        }
		
		//Normalize the dependencies so that have double-quotes
		//around each dependency.
		/*var normalizedDependencies = dependencies || "";
		if(normalizedDependencies){
			normalizedDependencies = '"' + normalizedDependencies.split(",").join('","') + '"';
		}*/		

		writeLog(packageRef, "Loading build scripts... ");
		
		// Global referenced needed by i18n utils.
		buildscriptDir = dojoDir + "util/buildscripts/";
		
		//Load the stripped down and modified build libraries.
		load("build/logger.js");
		load("build/fileUtil.js");
		load("build/buildUtil.js");
		load("build/buildUtilXd.js");
		load("build/i18nUtil.js");

		writeLogLine(packageRef, "done");
		
		var userAppLayer = ''; 
		var userAppPrefixes = '';
		
		if (userAppPaths !== null) {	
			writeLogLine(packageRef, "Found custom user application packages, analysing...");
			
			for(var i = 0, userAppPath; i < userAppPaths.length, userAppPath = userAppPaths[i]; i++) {				
				// Find modules from top level user directory.
				var userAppModules = findModules(userAppPath);
				var userAppDependencies = '"' + userAppModules.join('","') + '"';
				var topLevelModule = userAppModules[0].split(".")[0];
				
				userAppLayer += ' },	{ name: "'+topLevelModule+'.js", dependencies: ['+userAppDependencies+']';			
				userAppPrefixes += ', [ "'+topLevelModule+'", "' + userAppPath + '/' + topLevelModule + '" ]';
				
				writeLogLine(packageRef, "Enabled build profile for custom package " + topLevelModule);
			}
			
			writeLogLine(packageRef, "Custom user package analysis... done");
		}
		
		writeLogLine(packageRef, "Creating layer profiles....");
		
		//Set up the build args.
		var kwArgs = buildUtil.makeBuildOptions([
			"loader=" + loader,
			"version=" + version,
			"xdDojoPath=" + xdDojoPath,
			"layerOptimize=" + optimizeType
		]);
		
		// Check for WebKit mobile as target platform
		if (platforms === "webkit") {
			kwArgs.webkitMobile = true;
		}
		
		var profileText = 'dependencies = {'
			+ 'prefixes: ['
			+ '	[ "dojo", "' + dojoDir + 'dojo" ],'
			+ '	[ "dijit", "' + dojoDir + 'dijit" ],'
			+ '	[ "dojox", "' + dojoDir + 'dojox" ]'
			+ userAppPrefixes
			+ ']'
		+ '}';		
		
		//Bring the profile into existence
		// TODO: Create object directory
		var profileProperties = buildUtil.evalProfile(profileText, true);
				
		// Add layer details into build profile
		// TODO: Use Obj + "", to force Java Strings to JS strings. Fix this...
		profileProperties.dependencies.layers = [];
		
		for(var j = 0; j < layers.length; j++) {
			var layer = layers[j];
			
			writeLogLine(packageRef, "Discovered layer, " + layer.name);
			
			var layerDetails = {
				"name": layer.name + "",
				"dependencies": []
			};
				
			for(var i = 0; i < layer.dependencies.length; i++) {
				var moduleName = layer.dependencies[i] + "";				
				layerDetails.dependencies.push(moduleName);
			}
				
			profileProperties.dependencies.layers.push(layerDetails);
		}
		
		kwArgs.profileProperties = profileProperties;		
		
		//Set up some helper variables.
		dependencies = kwArgs.profileProperties.dependencies;
		var prefixes = dependencies.prefixes;
		var lineSeparator = fileUtil.getLineSeparator();
		// TODO: Needs to use layer specific copyright text.
		var layerLegalText = fileUtil.readFile(buildscriptDir + "copyright.txt")
			+ lineSeparator
			+ fileUtil.readFile(buildscriptDir + "build_notice.txt");
		
		//Manually set the loader on the dependencies object. Ideally the buildUtil.loadDependencyList() function
		//and subfunctions would take kwArgs directly.
		dependencies.loader = kwArgs.loader;
		
		writeLog(packageRef, "Creating layer contents from profile... ");
		
		//Build the layer contents.
		var depResult = buildUtil.makeDojoJs(buildUtil.loadDependencyList(kwArgs.profileProperties, null, buildscriptDir), kwArgs.version, kwArgs);
		var compressedLayers = {};
		
		// Check for i18n resource in layer depencies, if so we'll need to include the 
		// files.
		var i18n = false;
		writeLogLine(packageRef, "done");
		
		for (var i = 0; i < depResult.length; i++) {			
			//Grab the content from the "dojo.xd.js" layer.
			var layerName = depResult[i].layerName;
			var layerContents = depResult[i].contents;
			
			// Ignore non-XD layers when user has selected a XD build
			if (kwArgs.loader === "xdomain" && !layerName.match(/\.xd\.js/)) {
				continue;
			}			
			
			//Burn in xd path for dojo if requested, and only do this in dojo.xd.js.
			if(layerName.match(/dojo\.xd\.js/) && kwArgs.xdDojoPath){
				layerContents = buildUtilXd.setXdDojoConfig(layerContents, kwArgs.xdDojoPath);
			}
			
			// Flatten all referenced resource bundles
			layerContents = flattenLayerFileBundles(layerName, layerContents, compressedLayers, kwArgs, packageRef);
			
			//Intern strings
			if(kwArgs.internStrings){
				writeLog(packageRef, "Interning strings for layer "+layerName+"... ");
				prefixes = dependencies["prefixes"] || [];
				var skiplist = dependencies["internSkipList"] || [];
				layerContents = buildUtil.interningRegexpMagic(layerName, layerContents, dojoDir, prefixes, skiplist);
				writeLogLine(packageRef, "done");
			}
			
			writeLogLine(packageRef, "Optimising contents for layer "+layerName+"... ");
			
			// TODO: Strip console should come from user preference rather than hard coding....
			//Minify the contents
			compressedLayers[layerName] = optimizeJs(layerName, layerContents, layerLegalText, kwArgs.layerOptimize, "all", packageRef);
		}
		
		// Do we need to include theme files? If so, search through theme directory
		// for all matching files.
		if (theme !== "none") {
			writeLog(packageRef, "Finding all theme files for '" + theme + "'... ");
			var files = findDijitThemeFiles(dojoDir, theme);

			writeLogLine(packageRef, "done.");
			
			// Run optimisation if needed...
			if (cssOptimise !== "none") {
				writeLog(packageRef, "Running CSS optimisations... ");
				files = optimiseCSS(theme, files, cssOptimise);
				writeLogLine(packageRef, "done.");
			}
			
			var counter = 0;
			
			// Finally, add all remaining theme files to layer object.
			// Use manually loop to add new files rather than dojo.mixin
			// because I want to count the new files.
			for (var i in files) {
			    compressedLayers[i] = files[i];
				counter++;	
			}
			writeLogLine(packageRef, "Found " + counter + " theme related files.");
		}
		
		writeLogLine(packageRef, "Build process completed successfully!");
		
		return compressedLayers;
	}
};
