//This file generates a list of modules that can be used in a web build.
// This script searches a source location for Dojo module definitions 
// and generates the associated meta-data from the results. It is used 
// to create the package descriptors files needed by the Dojo Web Builder.
//
// CONFIGURATION PARAMETERS BELOW

// Modify these varibles out with details for your package.
var packageMetaData = {
    name: "Custom Package",
    description: "This is a custom package and contains custom modules"
};

// Output file for the package meta-data...
// BUG: FileUtil.saveFile needs to have parent 
// directory reference in this filename 
var outputFileName = "./package.json";

// What files are we looking for? Modify this to exclude
// certain directories like test if needed.
var includeFilter = /\.js/;

// DOJO TOOLKIT PACKAGE INFO
// Dojo Toolkit package details, left here to 
// save someone re-writing when generating new 
// details for a toolkit release

// Dojo specific include filter, we want to ignore 
// util and other dirs in dojo source.
//includeFilter = /(dojo\/|dojox\/|dijit\/).*\.js$/;

// Custom parameter used for generating Dojo toolkit meta-data
//packageMetaData.name = "Dojo Toolkit";
//packageMetaData.description= "Dojo Toolkit, release X.Y.Z";

if (arguments.length === 0) {
    print("Missing directory path argument, you must specify a directory location to search for module definitions.");
    quit();
}

var srcRoot = arguments[0];

// Set module location being the parent directory of the module root directory
packageMetaData.location = (new java.io.File(srcRoot)).getCanonicalFile().getParent();

// DOJO-CONFIG: For Dojo, we want all Dojo, Dijit and DojoX modules
// under the same package therefore, don't we search from parent
// dir as source folder, just use this folder as location.
//packageMetaData.location = (new java.io.File(srcRoot)).getCanonicalFile();

// Find first line match "summary:", match groups consisting of 
// any text after the summary prefix and any additional lines with
// two tabs. This will match the following summary formats:
// summary: some summary text (single line)
// & 
// summary:
// 		some summary text (multiple lines)
function findSummary(fileContents) {
 var summaryRegex = new RegExp("//(\\s)*summary:(.*\n)((.*//\t\t.*\n)*)");
  var summary = null;

  matches = fileContents.match(summaryRegex);

  //  return matches;
  if (matches != null) {
    if (matches[2] && matches[2].replace(/(\s|\n)/g, "") !== "") {
      // Remove newline from single line summary
      summary = matches[2].replace(/(\n|^ )/g,"");
    } else {
      // Remove tabs and double-backslashes, replace newlines with a space.
      summary = matches[3].replace(/(\t|\/\/)/g,"").replace(/\n/g, " ");
    }

    // Ugh, module has not got first summary filed in, 
    // try and recursively find a non-empty summary.
    if (summary.indexOf("TODOC") !== -1) {
        summary = findSummary(fileContents.substring(fileContents.indexOf("TODOC")));
    }

    // Make JSON safe, replacing quotations and strip tabs
    summary = summary.replace(/\"/g, '\'').replace(/\t/g, "");
  } else {
    summary = "Could not find summary for this module";
  }

  return summary;
}

function getFilteredFileList(/*String*/startDir, /*RegExp*/regExpFilters, /*boolean?*/makeUnixPaths, /*boolean?*/startDirIsJavaObject, /*boolean?*/dontRecurse){
	//summary: Recurses startDir and finds matches to the files that match regExpFilters.include
	//and do not match regExpFilters.exclude. Or just one regexp can be passed in for regExpFilters,
	//and it will be treated as the "include" case.
	//Ignores files/directories that start with a period (.).
	var files = [];

	var topDir = startDir;
	if(!startDirIsJavaObject){
		topDir = new java.io.File(startDir);
	}

	var regExpInclude = regExpFilters.include || regExpFilters;
	var regExpExclude = regExpFilters.exclude || null;

	if(topDir.exists()){
		var dirFileArray = topDir.listFiles();
		for (var i = 0; i < dirFileArray.length; i++){
			var file = dirFileArray[i];
			if(file.isFile()){
				var filePath = file.getPath();
				if(makeUnixPaths){
					//Make sure we have a JS string.
					filePath = new String(filePath);
					if(filePath.indexOf("/") == -1){
						filePath = filePath.replace(/\\/g, "/");
					}
				}

				var ok = true;
				if(regExpInclude){
					ok = filePath.match(regExpInclude);
				}
				if(ok && regExpExclude){
					ok = !filePath.match(regExpExclude);
				}

				if(ok && !file.getName().match(/^\./)){
					files.push(filePath);
				}
			}else if(file.isDirectory() && !file.getName().match(/^\./) && !dontRecurse){
				var dirFiles = this.getFilteredFileList(file, regExpFilters, makeUnixPaths, true);
				files.push.apply(files, dirFiles);
			}
		}
	}

	return files; //Array
}

print("Searching for JavaScript files under the \"" + srcRoot + "\"");

//Get a list of files that might be modules in the src root dir
var fileList = getFilteredFileList(srcRoot, includeFilter, true);

print("Found " + fileList.length + " matching files, proceeding to scan for module definitions");

// Match either new AMD module definition or old-style
// dojo.provide definition.
var moduleRegex = /^define\(|^dojo\.provide\(/;

packageMetaData.modules = [];

// Turn a filename into a module name
function computeModuleName(fileName, baseDir) {
    // Snip off the parent directory & js extension to reveal module path
    fileName = fileName.substring(baseDir.length + 1, fileName.length - 3);
    // Convert module path to module name
    return fileName.replace('/', '.', "g");
}

// If module name contains any of the following strings, ignore it. 
// This means we don't incorrectly expose tests/robots/etc
function isInvalidModule(moduleName) {
    var matches = ["tests", "_base", "nls", "robot", "Mixin", "Base", "firebug"];
    for (var i = 0; i < matches.length ; i++) {
        if (moduleName.indexOf(matches[i]) !== -1) {
            return true;
        }
    }
    return false;
}

// For every possible module, check for module definition 
// and find summary text.
for(var i = 0; i < fileList.length; i++){
	var fileName = fileList[i];
	var fileContents = readFile(fileName);

    // Confirm JS file contains a module definition, this 
    // may be new AMD style or old dojo.provide style. 
    if (fileContents.match(moduleRegex)) {
        var moduleName = computeModuleName(fileName, packageMetaData.location + "");
        
        //Skip certain kinds of modules not needed in end use, like tests
        if (!isInvalidModule(moduleName)) {
            print("Discovered module: " + moduleName);
            var summary = findSummary(fileContents);
            var jsonStr = "[\""+moduleName+"\",\""+summary+"\"]";
		    packageMetaData.modules.push(jsonStr);
        }
    }
}

// JSON-ify strings and arrays 
function line_format (key, value) {
    if (value.constructor === Array) {
        value = "[" + value.join(", ") + "]";
    } else {
        value = "\"" + value + "\"";
    }
        
    return "  \"" + key + "\": " + value;
}

function metaDataToStr(metadata) {
    var serialised = "{\n", i, props = [];

    for (i in metadata) {
        if (metadata.hasOwnProperty(i)) {
           props.push(line_format(i, metadata[i])); 
        }
    }

    serialised += props.join(",\n") + "\n}";

    return serialised;
}

function savePackageMetaData(metadata) {
    metadata.modules = metadata.modules.sort();
	var outFile = new java.io.File(outputFileName),
    outWriter = new java.io.OutputStreamWriter(new java.io.FileOutputStream(outFile)), 
    os = new java.io.BufferedWriter(outWriter);

	try{
	    os.write(metaDataToStr(metadata));
	}finally{
	    os.close();
	}
}

savePackageMetaData(packageMetaData);
