load("build.js");

var dojoDir = "/Users/james/Code/DTK/dojo-release-1.5.0-src/";

load(dojoDir + "util/buildscripts/jslib/logger.js");
load(dojoDir + "util/buildscripts/jslib/fileUtil.js");
load(dojoDir + "util/buildscripts/jslib/buildUtil.js");

// Overwrite reference to log line, just dumped 
// build output to the console
writeLog = function(filename, contents) {
  print(contents);
}

var dojoSrc = "/Users/james/Code/DTK/dojo-release-1.5.0-src";
var version = "1.5";
var cdnType = "";
var platforms = "all";
var theme = "none";

var layers = [{
    "name":"dojo.js", 
    "dependencies": []
}, {
  "name": "editor.js",
  "dependencies": ["dijit.Editor", "dijit.Dialog"]
}];


var optimizeType = "shrinksafe";
var cssOptimise = "none";

var results = build.make(dojoSrc, version, cdnType, platforms, theme, layers, optimizeType, cssOptimise, [], "test");

for(var i in results) {
    print(i);
}
