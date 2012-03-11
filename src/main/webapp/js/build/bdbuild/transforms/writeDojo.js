///
// \amd-mid build/lib/transforms/writeBdLoad
//
// A function to write the bdLoad resource.
//
// The function writes the results of transforming the loader source. has.js is integrated as follows:
//	 * if bc.has=="*build", then build/has/bdBuildHas is provided to the loader boot; otherwise...
//	 * if bc.has.getText exists and is a function, then the result of that function is provided to the loader boot; otherwise...
//	 * build/has/naiveHas is provided to the loader boot bc.loader.boots
//
// Other transforms may request a bootstrap be written for them that includes the loader and loader config. They
// may execute such a request by pushing a function into bc.loader.boots. The function must return a [filename, text]
// pair that indicates the bootstrap text to append to the loader and the destination to write the result.
define([
	"../buildControl",
	"../fileUtils",
	"../fs",
	"../stringify",
	"./writeAmd",
	"../process",
	"dojo/text!./dojoBoot.js"
], function(bc, fileUtils, fs, stringify, writeAmd, process, dojoBootText) {
	return function(resource, callback) {
		var
			getUserConfig= function() {
				if (!bc.userConfig) {
					return "this.dojoConfig || this.djConfig || this.require || {}";
				}
				var result= stringify(bc.userConfig);
				if (result.unsolved) {
					bc.log("configUnresolvedValues");
				}
				return result;
			},

			computeLocation= function(basePath, path) {
				if (path.indexOf(basePath + "/")==0) {
					return "." + path.substring(basePath.length);
				}
				var
					parts= basePath.split("/"),
					prefix= "";
				for (var i= parts.length-1; i>=0; i--) {
					 prefix+= (prefix ? "/.." : "..");
					var check= parts.slice(0, i).join("/") + "/";
					if (path.indexOf(check)==0) {
						return prefix + path.substring(check.length-1);
					}
				}
				return path;
			},

			getPackage= function(name) {
				// the purpose of this somewhat verbose routine is to write a minimal package object for each
				// package, yet allow client code to pass extra (i.e., outside the scope of CJS specs) config
				// information within the package object
				var destPack= bc.destPackages[name],
					result= {};
				result.name = destPack.name;
				if(destPack.main!="main"){
					result.name = destPack.main;
				}
				// everything relative to the dojo dir
				// TODO: making everything relative to dojo needs to be optional
				if (name=="dojo") {
					result.location= ".";
				} else {
					result.location= computeLocation(bc.destPackageBasePath + "/dojo", destPack.location);
				}
				var packageDefaultConfig = bc.defaultConfig && bc.defaultConfig.packages && bc.defaultConfig.packages[name];
				for(var p in packageDefaultConfig){
					result[p] = packageDefaultConfig[p];
				}
				return result;
			},

			getDefaultConfig= function() {
				var p, config = {packages:[], hasCache:{}};
				if (bc.baseUrl) {
					config.baseUrl= bc.baseUrl;
				}
				for (p in bc.packages) {
					config.packages.push(getPackage(p));
				}
				for(p in bc.defaultConfig){
					if(p!=="packages"){
						// per-package default config was handled above
						config[p] = bc.defaultConfig[p];
					}
				}
				config= stringify(config);
				if (config.unsolved) {
					bc.log("configUnresolvedValues");
				}
				return config;
			},

			stampVersion= function(text){
				//summary: Changes the version number for dojo. Input should be the fileContents
				//of a file that contains the version number.
				version= bc.version;
				if(version){
					//First, break apart the version string.
					var verSegments = (version+"").match(/^(\d*)\.?(\d*)\.?(\d*)\.?(.*)$/);
					var majorValue = verSegments[1] || 0;
					var minorValue = verSegments[2] || 0;
					var patchValue = verSegments[3] || 0;
					var flagValue  = verSegments[4] || "";

					//Do the final version replacement.
					return text.replace(
							/major:\s*\d*,\s*minor:\s*\d*,\s*patch:\s*\d*,\s*flag:\s*".*?"\s*,/g,
						"major: " + majorValue + ", minor: " + minorValue + ", patch: " + patchValue + ", flag: \"" + flagValue + "\","
					);
				}else{
					return text;
				}
			},

			waitCount= 1, // matches *1*

			errors= [],

			onWriteComplete= function(err) {
				if (err) {
					errors.push(err);
				}
				if (--waitCount==0) {
					callback(resource, errors.length && errors);
				}
			},

			doWrite= function(filename, text) {
				fileUtils.ensureDirectoryByFilename(filename);
				waitCount++;
				fs.writeFile(filename, text, "utf8", onWriteComplete);
			};

		// the writeDojo transform...
		try {
			// all layer modules compute moduleSet, which may be used for reporting
			resource.moduleSet= writeAmd.computeLayerContents(0, resource.layer.include, resource.layer.exclude);

			var
				// the default application to the loader constructor is replaced with purpose-build user and default config values
				configText= "(" + getUserConfig() + ", " + getDefaultConfig() + ");",

				// the construction of the layer is slightly different than standard, so don't pass a module to getLayerText
				layerText= writeAmd.getLayerText(0, resource.layer.include, resource.layer.exclude);

			// assemble and write the dojo layer
			resource.layerText= resource.getText() + configText + stampVersion(layerText) + (bc.dojoBootText || dojoBootText);
			doWrite(writeAmd.getDestFilename(resource), resource.layer.copyright + resource.layerText);

			//write any bootstraps; boots is a vector of resources that have been marked as bootable by the discovery process
			resource.boots.forEach(function(item) {
				// each item is a hash of include, exclude, boot, bootText
				item.layerText= resource.layerText + writeAmd.getLayerText(item, item.layer.include, item.layer.exclude) + (item.bootText || "");
				doWrite(writeAmd.getDestFilename(item), resource.layer.copyright + item.layerText);
			});

			onWriteComplete(0); // matches *1*
		} catch (e) {
			if (waitCount) {
				// can't return the error since there are async processes already going
				errors.push(e);
				return 0;
			} else {
				return e;
			}
		}
		return callback;
	};
});
