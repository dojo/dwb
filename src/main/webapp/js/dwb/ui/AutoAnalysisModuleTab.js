dojo.provide("dwb.ui.AutoAnalysisModuleTab");

dojo.require("dwb.ui.ModuleTab");
dojo.require("dwb.util.Config");
dojo.require("dojo.DeferredList");

dojo.declare("dwb.ui.AutoAnalysisModuleTab", [dwb.ui.ModuleTab], {
	// Override ContentPane and default to true
	closable: false,
	
	analysisContentFragments: {
		"profile": dojo.cache("dwb.ui.fragments", "ExistingProfileModuleTab.html"),
		"web_app": dojo.cache("dwb.ui.fragments", "AnalyseRemoteWebAppModuleTab.html"),
		"dojo_app": dojo.cache("dwb.ui.fragments", "UploadUserAppModuleTab.html"),
		"html_file": dojo.cache("dwb.ui.fragments", "AnalyseHTMLModuleTabLocal.html")
	},
	
	analysisType: "web_app",
	
	// Hint text to display when the modules tab is empty
	//content: dojo.cache("dwb.ui.fragments", "AnalyseHTMLModuleTabLocal.html"),

	analysisURL : dwb.util.Config.get("dependenciesApi"),
	packageURL : dwb.util.Config.get("packagesApi"),
	
	globalModulesStore: null,
	
	customLayerName: null,
	
	constructor: function () {
		this.content = this.analysisContentFragments[this.analysisType];

		dojo.subscribe("dwb/analysis/sourceType", dojo.hitch(this, function (message) {
			this.set("analysisType", message.source);
		}));
		dojo.subscribe("dwb/layers/addAnalysisModules", dojo.hitch(this, function (message) {
			this.moduleGrid.store.fetch({
				onComplete: dojo.hitch(this, function (items) {
					dojo.publish("dwb/layers/addModules", [{
						layer: message.layer,
						modules: items,
						layerName: this.customLayerName
					}]);	
					// Reset module grid on tab pane
					this.set("analysisType", this.analysisType);
				})
			});
		}));
	},
	
	// Override default postCreate to stop render of the module grid.
	postCreate: function () {
		dojo.query(".dijitButton", this.domNode).forEach(dojo.hitch(this, function(btn) {
			this.connect(dijit.byNode(btn), "onClick", dojo.hitch(this, "_onSubmit"));
		}));
	},
	
	_setAnalysisTypeAttr : function (type) {
		this.analysisType = type;
		this.set("content", this.analysisContentFragments[this.analysisType]);
		
		dojo.query(".dijitButton", this.domNode).forEach(dojo.hitch(this, function(btn) {
			this.connect(dijit.byNode(btn), "onClick", dojo.hitch(this, "_onSubmit"));
		}));	
	},
	
	_displayAllModules : function (modules) {
		// Remove any previous module grids
		if (this.moduleGrid) {
			this.moduleGrid.destroy();
		}
		
		// Create new grid for module data. Cannot use autoHeight
		// as we run into problems referenced here, http://bugs.dojotoolkit.org/ticket/9261
        this.moduleGrid = new dojox.grid.EnhancedGrid({
            structure: this.moduleGridLayout,
            escapeHTMLInData: false,
            height: this.moduleGridHeight
        });
        
        dojo.connect(this.moduleGrid, "onRowClick", dojo.hitch(this, function (e) {
        	// Check user clicked remove cell and the remove icon 
        	if (e.cellIndex === 2 && dojo.hasClass(e.target, "rowCloseIcon")) {
        		var row_item = e.grid.getItem(e.rowIndex);
        		e.grid.store.deleteItem(row_item);
        		e.grid.store.save();
        		// If grid is now empty, hide! 
        		this.updateGridDisplay();
        	}
        }));        
        
        dojo.place(this.moduleGrid.domNode, this.domNode);        
        
        // Render grid on content pane.
        this.moduleGrid.startup();
        
        // Hide empty grid, displaying help text, until
        // modules store is set with some content.
        this.set("gridIsEmpty", true);
        var modulesStore = new dojo.data.ItemFileWriteStore({data: {
			identifier: "name",
    		items: modules
    	}});
        
        this.moduleGrid.setStore(modulesStore);
		this.updateGridDisplay();
		
		// FIX: Having trouble with grid rendering on some pages, force render again
			// at this point.
		this.moduleGrid.render();
		//})); 
	},
	
	_parseDiscoveredModules : function (response) {
		var modules = [];
		switch (this.analysisType) {
		case "web_app":
		case "html_file":
			modules = response.requiredDojoModules;
			break;
		case "dojo_app":
			modules = response.requiredDojoModules.concat(response.availableModules);
			break;
		case "profile":
			// Find all unique module names
			dojo.forEach(response.layers, function (layer) {
				dojo.forEach(layer.dependencies, function (moduleName) {
					if (modules.indexOf(moduleName) === -1) {
						modules.push(moduleName);
					}
				});
			});
			break;
		}
		
		// Ensure modules are sorted in order
		return modules.sort();
	},
	
	_parseCustomPackages : function (response) {
		var packages = {};
		if (this.analysisType === "web_app") {
			packages = response.packages;
		} else if (this.analysisType === "dojo_app") {
			packages.packageRef = response.packageReference; 
		}
		return packages;
	},
	
	_handleResponse : function (response) {
		this._cancelBusyBtn();
		
		var discoveredModules = this._parseDiscoveredModules(response);
		var customPackages = this._parseCustomPackages(response);
		
		// Process results, try to find module description from modules store. 
		var completed = dwb.util.Util._populateModuleDetails(this.globalModulesStore, discoveredModules);
		
		// Previous call involves multiple async. dojo.data fetch operations.
		// Use Deferred List to wait for completion. 
		completed.then(dojo.hitch(this, function(data) {
			var modules = [];
			
			var r = this.removeFragment;
			// Add module details if deferred resolved correctly.
			dojo.forEach(data, function(result) {				
				if (result[0]) {
					var module = result[1];
					module.remove = r;
					modules.push(module);
				}
			});
			
			this._displayAllModules(modules);
		}));

		// Reset package layer name for each analysis
		this.customLayerName = null;
		
		// We only need the package reference, server side component
		// which automatically extract module reference.
		for(var key in customPackages) {
			dojo.publish("dwb/layers/temporaryPackage", [{"packageId": customPackages[key]}]);
		}
	},
	
	// Background upload of HTML file, returning modules found.
	_onSubmit : function (e) {		
		dojo.stopEvent(e);
			
		var form = dojo.query("form", this.domNode)[0];
		// HTML files, web apps and profiles go through dependencies end point. User applications
		// need turning into temporary packages, must go through package api.
		var url = (this.analysisType === "dojo_app") ? this.packageURL : this.analysisURL;
		
		dojo.io.iframe.send({
			url: url, 
			method: "post", 
			handleAs: "json", 
			form: form, 
			handle: dojo.hitch(this, "_handleResponse"), 
			error: dojo.hitch(this, function (e) {
				this._cancelBusyBtn();
				// TODO: Need to support proper error handling.
				console.log(e);
			})
		});			
	},
	
	// Signal that we want to remove this tab 
	// from container parent.
	_onRemove : function () {
	},
	
	_cancelBusyBtn : function () {
		dojo.query(".dijitButton", this.domNode).forEach(function (button) {
			dijit.byNode(button).cancel();
		});
	}
});
