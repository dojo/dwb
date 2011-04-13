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
// packageMetaData.name = "Dojo Toolkit";
// packageMetaData.description= "Dojo Toolkit, release X.Y.Z";

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
// packageMetaData.location = (new java.io.File(srcRoot)).getCanonicalFile();

// Use custom file loading to handle AMD transformation 
// back to old format when reading module contents 
load("../../webapp/js/build/fileUtil.js");

function findSummary(fileContents) {
  // Find first line match "summary:", match groups consisting of 
  // any text after the summary prefix and any additional lines with
  // two tabs. This will match the following summary formats:
  // summary: some summary text (single line)
  // & 
  // summary:
  // 		some summary text (multiple lines)
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
  } else {
    summary = "Could not find summary for this module";
  }

  return summary.replace(/\"/g, '\'');
}

print("Searching for JavaScript files under the \"" + srcRoot + "\"");

//Get a list of files that might be modules in the src root dir
var fileList = fileUtil.getFilteredFileList(srcRoot, includeFilter, true);

print("Found " + fileList.length + " matching files, proceeding to scan for module definitions");

// Module definition rege
var provideRegExp = /dojo\.provide\(\".*\"\)/g;

packageMetaData.modules = [];

for(var i = 0; i < fileList.length; i++){
	var fileName = fileList[i];
	var fileContents = fileUtil.readFile(fileName);

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

            print("Discovered module: " + fileName);
			//Only allow the provides that match the file name.
			//So, the different dijit permutations of dijit.form.Button will not show up.
			if (modFileName.lastIndexOf(provideName.replace(/\./g, "/")) == modFileName.length - provideName.length){
                                var summary = findSummary(fileContents);
                                var jsonStr = "[\""+provideName+"\",\""+summary+"\"]";
				packageMetaData.modules.push(jsonStr);
				break;
			}
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
    fileUtil.saveFile(outputFileName, metaDataToStr(metadata));
}

savePackageMetaData(packageMetaData);
