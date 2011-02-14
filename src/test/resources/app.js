dojo.provide("web_builder.app");

dojo.require("dojo.back");
dojo.require("web_builder.child");
dojo.require("web_builder.child");

dojo.declare("web_builder.app", null, {
  constructor: function () {
    console.log("Constructor called...");
  },

  init: function () {
    console.log("Called init");
  }
});

// Add in some custom module paths
dojo.registerModulePath("some.path", "../../modules");
dojo.registerModulePath("another.path", "../../more_modules");
dojo.registerModulePath("foo.bar", "../other_modules");
dojo.registerModulePath("misc", "/misc/more/modules");
dojo.registerModulePath("a", "../b");
dojo.registerModulePath("a", "../a");
