define([
	"require",
	"./buildControlBase",
	"./fs", "./fileUtils",
	"./process",
	"dojo",
	"dojo/text!./copyright.txt",
	"dojo/text!./buildNotice.txt"], function(require, bc, fs, fileUtils, process, dojo, defaultCopyright, defaultBuildNotice) {
	eval(require.scopeify("./fs, ./fileUtils"));
	var
		mix= function(dest, src) {
			dest= dest || {};
			src= src || {};
			for (var p in src) dest[p]= src[p];
			return dest;
		},

		defaultBuildProps= {
			// v1.6- default values
			profile:"base",
			profileFile:"",
			htmlFiles:"",
			htmlDir:"",
			version:"0.0.0.dev",
			localeList:"ar,ca,cs,da,de-de,el,en-gb,en-us,es-es,fi-fi,fr-fr,he-il,hu,it-it,ja-jp,ko-kr,nl-nl,nb,pl,pt-br,pt-pt,ru,sk,sl,sv,th,tr,zh-tw,zh-cn",
			releaseName:"dojo",
			releaseDir:"../../release/",
			internStrings:true,
			internSkipList:[],
			optimize:"",
			layerOptimize:"shrinksafe",
			cssOptimize:"",
			cssImportIgnore:"",
			stripConsole:"normal",
			scopeMap:[["dojo", "dojo"], ["dijit", "dijit"], ["dojox", "dojox"]],
			replaceLoaderConfig:1,
			insertAbsMids:1,
			applyDojoPragmas:1,

			// these will be set in buildControl; see buildControl.js for details
			//copyTests:false,
			//mini:true,

			// the following configuration variables are deprecated in 1.7 compared to 1.6 and have no effect
			//query,
			//buildLayers,
			//scopeDjConfig,
			//symbol,
			//xdDojoPath,
			//log,
			//loader,
			//xdScopeArgs,
			//xdDojoScopeName,
			//expandProvide,
			//removeDefaultNameSpaces,
			//addGuards,

			// the following values are settings understood by the v1.7+ builder that cause behavior like the v1.6- builder
			destPackageBasePath:".",

			staticHasFeatures: {
				// consider turning these hard on for standard 1.x build
				//'config-publishRequireResult':1,
				//'config-tlmSiblingOfDojo':1,

				'dojo-1x-base':1,
				'dojo-amd-factory-scan':0,
				'dojo-built':1,
				'dojo-combo-api':0,
				'dojo-log-api':1,
				'dojo-test-sniff':0,// must be turned on for several tests to work
				'dojo-config-addOnLoad':1,
				'dojo-config-api':1,
				'dojo-config-require':1,
				'dojo-dom-ready-api':1,
				'dojo-guarantee-console':1,
				'dojo-has-api':1,
				'dojo-inject-api':1,
				'dojo-loader':1,
				'dojo-modulePaths':1,
				'dojo-moduleUrl':1,
				'dojo-publish-privates':0,
				'dojo-requirejs-api':0,
				'dojo-scopeMap':1,
				'dojo-sniff':1,
				'dojo-sync-loader':1,
				'dojo-timeout-api':1,
				'dojo-trace-api':0,
				'dojo-undef-api':0,
				'dojo-v1x-i18n-Api':1,
				'dojo-xhr-factory':1,
				'dom':1,
				'host-browser':1,
				'host-node':0,
				'host-rhino':0
			},

			defaultConfig:{
				hasCache:{
					// these are the values given above, not-build client code may test for these so they need to be available
					'dojo-built':1,
					'dojo-loader':1,
					'dom':1,
					'host-browser':1,

					// default
					"config-selectorEngine":"acme"
				},
				async:0
			}
		},

		processProfile= function(profile, args) {
			// process a v1.6- profile
			//
			// v1.6- has the following relative path behavior:
			//
			//	 * the util/buildscripts directory is assumed to be the cwd upon build program startup
			//	 * the dojo directory as specified in profile dependencies.prefixes (if relative) is
			//     assumed to be relative to util/buildscripts
			//	 * similarly the releaseDir directory (if relative) is assumed to be relative to util/buildscripts
			//	 * all other relative paths are relative to the dojo directory (in spite of what some docs say)
			//	 * all non-specified paths for top-level modules are assummed to be siblings of dojo.
			//     For example, myTopModule.mySubModule is assumed to reside at dojo/../myTopModule/mySubModule.js
			//
			// This has the net effect of forcing the assumption that build program is be executed from util/buildscripts.
			// when relative paths are used; this may be convenient. The behavior is probably consequent to rhino's design
			// that does not report the full path of the script being executed. In order to help, the following v1.7+
			// options are available:
			//
			//	 -buildPath path/to/util/buildscripts/build
			//	 -baseUrl	path/to/use/instead/of/path/to/util/buildscripts
			//
			// This doesn't eliminiate the strange behavior of releaseDir. Users who find releaseDir inconvenient should
			// use destBasePath.

			var
				p,
				result= {},
				layers= profile.layers || [],
				prefixes= profile.prefixes || [],
				copyright= profile.copyright!==undefined ? profile.copyright : defaultCopyright,
				buildNotice= profile.buildNotice!==undefined ? profile.buildNotice : defaultBuildNotice,
				getTopLevelModule= function(mid){
					return mid.split(".")[0];
				};

			for (p in defaultBuildProps) {
				result[p]= defaultBuildProps[p];
			}

			// find all the top-level modules by traversing each layer's dependencies (a vector of dotted module names)
			var topLevelMids= {dojo:1};
			layers.forEach(function(layer){
				(layer.dependencies || []).forEach(function(mid) {
					topLevelMids[getTopLevelModule(mid)]= 1;
				});
			});

			// convert the prefix vector to a map; make sure all the prefixes are in the top-level map
			var prefixMap =
					// map from top-level mid --> path
					{},
				copyrightMap =
					// map from top-level mid --> copyright message (usually undefined)
					{},
				runtimeMap =
					// map from top-level mid --> runtime environment for computing depenencies in transforms/depsScan (usually undefined)
					{};
			prefixes.forEach(function(pair){
				// pair a [mid, path], mid, a top-level module id, path relative to dojo directory
				var mid = pair[0];
				topLevelMids[mid]= 1;
				prefixMap[mid]= pair[1];
				copyrightMap[mid]= pair[2];
				runtimeMap[mid]= pair[3];
			});

			// make sure we have a dojo prefix; memorize it;
			// notice we default to the dojo being used to run the build program; this seems weak, but the only alternative is to quit
			var activeDojoPath= fileUtils.computePath(require.toUrl("dojo/package.json").match(/(.+)\/package\.json$/)[1], process.cwd());
			if(!prefixMap.dojo) {
				// use the loader to find the real dojo path
				prefixMap.dojo= activeDojoPath;
			}else{
				if (profile.basePath===undefined && /^\./.test(prefixMap.dojo) && compactPath(catPath(activeDojoPath, "../util/buildscripts"))!=process.cwd()){
					bc.log("pacify", "oddDojoPath");
				}
				if(computePath(prefixMap.dojo, profile.basePath || process.cwd())!=activeDojoPath){
					bc.log("pacify", "buildUsingDifferentDojo");
				}
			}

			var dojoPath= prefixMap.dojo= compactPath(prefixMap.dojo);

			// make sure we have a prefix for each top-level module
			// normalize dojo out of the non-dojo prefixes
			for(var mid in topLevelMids){
				var path= prefixMap[mid] || ("../" + mid);
				if (mid!="dojo") {
					prefixMap[mid]= computePath(path, dojoPath);
				}
			}

			// now make a package for each top-level module
			var packages= result.packages= [];
			for(mid in prefixMap){
				packages.push({
					name:mid,
					location:prefixMap[mid],
					copyright:copyrightMap[mid]!==undefined ? copyrightMap[mid] : defaultCopyright,
					runtime:runtimeMap[mid]
				});
			}

			// remember the doh package info (this is done here to get the location and destLocation)
			// this will be added to packages in buildControl after the command line switches are processed
			// iff !mini && copyTests (remember, they're not processed here)
			result.dohPackageInfo= {
				name:"doh",
				location:dojoPath + "/../util/doh",
				destLocation:"util/doh"
			};

			// resolve all the layer names into module names;
			var
				filenameToMid= function(filename) {
					for (var topLevelMid in prefixMap) {
						if (filename.indexOf(prefixMap[topLevelMid])==0) {
							var
								mid= filename.substring(prefixMap[topLevelMid].length),
								match= mid.match(/(.+)\.js$/);
							if (match) {
								return topLevelMid + match[1];
							}
						}
					}
					return 0;
				},
				layerNameToLayerMid= {};
			layers.forEach(function(layer) {
				var mid= filenameToMid(computePath(layer.name, dojoPath));
				if (!mid) {
					bc.log("layerToMidFailed", ["layer", layer.name]);
					return;
				}
				layerNameToLayerMid[layer.name]= mid;
			});

			var
				getLayerCopyrightMessage= function(explicit, mid){
					// this is a bit obnoxious as a default, but it's the v1.6- behavior
					// TODO: consider changing
					if(explicit!==undefined){
						return explicit;
					}
					if(copyrightMap[getTopLevelModule(mid)]!==undefined){
						return copyrightMap[getTopLevelModule(mid)];
					}else{
						return defaultCopyright + defaultBuildNotice;
					}
				},
				fixedLayers= {"dojo/dojo": {copyright:defaultCopyright + defaultBuildNotice, include:["dojo/main"], exclude:[]}};
			layers.forEach(function(layer) {
				var
					mid= layerNameToLayerMid[layer.name],
					result= {
						copyright:getLayerCopyrightMessage(layer.copyright, mid),
						include:(layer.dependencies || []).map(function(item) { return item.replace(/\./g, "/"); }),
						exclude:(layer.layerDependencies || []).map(function(item) {
							var mid= layerNameToLayerMid[item];
							if (!mid) {
								bc.log("layerMissingDependency", ["layer", layer.name, "dependency", item]);
							}
							return mid;
						})
					};
				if(mid=="dojo/dojo"){
					if(!layer.customBase){
						result.include.push("dojo/main");
					}
				}else{
					if(!layer.customBase){
						result.exclude.push("dojo/dojo");
					}
				}
				if (layer.discard) {
					result.discard= true;
				}
				if(layer.boot){
					result.boot= true;
				}
				if (layer.copyright) {
					result.copyright= layer.copyright;
				}
				fixedLayers[mid]= result;
			});
			result.layers= fixedLayers;

			if (profile.destBasePath) {
				if (profile.releaseDir || profile.releaseName) {
					bc.log("ignoringReleaseDirName");
				}
			} else {
				var
					releaseName= (profile.releaseName || args.releaseName || result.releaseName).replace(/\\/g, "/"),
					releaseDir= (profile.releaseDir || args.releaseDir || result.releaseDir).replace(/\\/g, "/");
				profile.destBasePath= computePath(catPath(releaseDir, releaseName), profile.basePath);
			}

			// change the name of v1.6- log property
			// TODO: warn if command line arg steps on a known, used build control property
			// see similarly processing in argv
			if(result.log!==undefined){
				result.logLevel= result.log;
				delete result.log;
			}
			for (p in profile) {
				if (/^(loader|xdDojoPath|symbol|scopeDjConfig|xdScopeArgs|xdDojoScopeName|expandProvide|buildLayers|query|removeDefaultNameSpaces|addGuards)$/.test(p)) {
					bc.log("inputDeprecated", ["switch", p]);
				}else if(p=="layers"){
					result.rawLayers= profile[p];
				}else if(p=="staticHasFeatures"){
					mix(result.staticHasFeatures, profile.staticHasFeatures);
				}else if(p!="defaultConfig"){
					// TODO: recognition of "false" should be deprecated
					result[p]= (profile[p]=="false" ? false : profile[p]);
				}
			}
			if(profile.defaultConfig){
				for(p in profile.defaultConfig){
					if(p=="hasCache"){
						mix(result.defaultConfig.hasCache, profile.defaultConfig.hasCache);
					}else{
						result.defaultConfig[p]= profile.defaultConfig[p];
					}
				}
			}

			// TODOC: we now take care of the console without shrink safe
			// TODO/TODOC: burn in dojoConfig, djConfig
			// TODO/TODOC: dojoConfig, djConfig should be able to be objects (string restrinction lifted)
			// TODOC: action is assumed to be build, no more clean, help if you want it explicitly

			return result;
		},

		processProfileFile= function(filename, args){
			var text= readFileSync(filename, "utf8");

			//Remove the call to getDependencyList.js because it is not supported anymore.
			if (/load\(("|')getDependencyList.js("|')\)/.test(text)) {
				bc.log("getDependencyListRemoved", ["profile", filename]);
				text.replace(/load\(("|')getDependencyList.js("|')\)/, "");
			}

			// how about calling it a profile (instead of v1.6- dependencies)...
			var profile= (function(__text){
				var
					// the logger is currently deprecated; stub it out so profiles won't cause exceptions on undefined
					// TODO: should we bring this back?
					warn = function(message){
						bc.log("inputLoggerRemoved");
					},
					logger = {
						TRACE: 0,
						INFO: 1,
						WARN: 2,
						ERROR: 3,
						level: 0,
						logPrefix: "",
						trace:warn,
						info:warn,
						warn:warn,
						error:warn,
						_print:warn
					},
					dependencies= {};
				eval(__text);
				return dependencies;
			})(text);
			return processProfile(profile, args);
		},

		processHtmlFiles= function(files){
			bc.log("processHtmlFiles", ["files", files.join(", ")]);
			var
				layers = {},
				prefix = "",
				prefixes = {dijit: true, dojox: true};
			files.forEach(function(htmlFile){
				var
					priorLayers = [],
					addLayer = function(scriptName){
						if(layers[scriptName]){
						// if this module has been added before, find the intersection of dependencies
							layers[scriptName] = layers[scriptName].filter(function(scriptName){
								return priorLayers.indexOf(scriptName) > -1;
							});
						}else{
							layers[scriptName] = priorLayers.concat();
						}
						if(scriptName.indexOf('.') > -1){
							prefixes[scriptName.substring(scriptName, scriptName.indexOf('.'))] = true;
						}
						priorLayers.push(scriptName);
					};

				var html = fs.readFileSync(htmlFile);
				html.replace(/<script [^>]*src=["']([^'"]+)["']/gi, function(t, scriptName){
					// for each script tag
					if(scriptName.indexOf("dojo/dojo.js") > -1){
						// use dojo.js to determine the prefix for our namespaces
						prefix = scriptName.substring(0, scriptName.indexOf("dojo/dojo.js"));
					}else{
						// non-dojo.js script files, add it to our list of layers
						addLayer(scriptName = scriptName.substring(prefix.length, scriptName.length - 3).replace(/\//g, '.'));
					}
				});
				html.replace(/dojo\.require\(["']([^'"]+)["']\)/g, function(t, scriptName){
					// for each dojo.require call add it to the layers as well
					addLayer(scriptName);
				});
			});

			var prefixPaths = [];
			// normalize the prefixes into the arrays that the build expects
			for(prefix in prefixes){
				prefixPaths.push([prefix, "../" + prefix]);
			}
			var layersArray = [];
			for(var name in layers){
				// for each layer, create a layer object
				layersArray.push({
					name: "../" + name.replace(/\./g,'/') + ".js", // use filename
					dependencies: [
						name.replace(/\//g,'.') // use module name
					],
					//use all previous layers as layer dependencies
					layerDependencies: layers[name].map(function(name){
						return "../" + name.replace(/\./g,'/') + ".js";
					})
				});
			}
			var profileProperties = {
				layers: layersArray,
				prefixes: prefixPaths
			};
			if(bc.profileFile){
				fs.writeFileSync(bc.profileFile, "dependencies = " + dojo.toJson(profileProperties), "utf8");
			}
			processProfile(profileProperties);
		};

	return {
		processProfile:processProfile,
		processProfileFile:processProfileFile,
		processHtmlFile:processHtmlFiles
	};
});
