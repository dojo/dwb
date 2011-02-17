//This file generates a list of modules that can be used in a web build.
//This file should be called from ant/command line, and the output file
//needs to be generated before the web build will work.

//START of the "main" part of the script.
//This is the entry point for this script file.
var srcRoot = arguments[0];
var outputFileName = arguments[1];
load("build/logger.js");
load("build/fileUtil.js");
load("findModuleSummary.js");

//quit();

print("Looking for modules....");
//Get a list of files that might be modules, ignoring util and other dirs
var fileList = fileUtil.getFilteredFileList(srcRoot, /(dojo\/|dojox\/|dijit\/).*\.js$/, true);

var provideRegExp = /dojo\.provide\(\".*\"\)/g;

//Search the modules for a matching dojo.provide() call.
//Need to do this because some files (like nls/*.js files) are
//not really modules.
var provideList = [];

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

            print("Found: " + fileName);
			//Only allow the provides that match the file name.
			//So, the different dijit permutations of dijit.form.Button will not show up.
			if (modFileName.lastIndexOf(provideName.replace(/\./g, "/")) == modFileName.length - provideName.length){
                                var summary = findSummary(fileContents);
                                var jsonStr = "[\""+provideName+"\",\""+summary+"\"]";
				provideList.push(jsonStr);
				break;
			}
		}
	
	}
}

provideList = provideList.sort();

fileUtil.saveFile(outputFileName, "{\n  \"name\": \"Dojo Toolkit\",\n  \"description\": \"Dojo Toolkit, release X.X.X\",\n  \"location\": \""+srcRoot+"\",\n  \"modules\": [" + provideList + "]\n}");
