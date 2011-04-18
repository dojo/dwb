load("build/build.js");

//var dojoDir = "/Users/james/Code/DTK/dojo-release-1.5.0-src/";

// Overwrite reference to log line, just dumped 
// build output to the console
writeLog = function(filename, contents) {
  print(contents);
}

var dojoSrc = "/Users/james/Code/DTK/dojo-release-1.5.0-src";
var version = "1.5";

var dojoSrc = "/Users/james/Code/DTK/dojo-release-1.6.0-src";
var version = "1.6.0";

var localBuildDir = java.lang.System.getProperty("user.dir") + "/build";

var cdnType = "";
var platforms = "all";
var theme = "none";

var layers = [{
   "name":"dojo.js",
   "dependencies":["dijit.Editor", "dijit.Calendar", "dijit.Dialog"]
}];

var userAppPaths = [["dwb", "/Users/james/IBM/Code/dwb/src/main/webapp/js"]];
userAppPaths = [];

var optimizeType = "shrinksafe";
var cssOptimise = "none";

var results = build.make(dojoSrc, localBuildDir, version, cdnType, platforms, theme, layers, optimizeType, cssOptimise, userAppPaths, "test");

for(var i in results) {
    //if (!i.match("^nls")) {
        print(i);
        print(results[i].length);
    //}
}
