dojo.provide("dwb.ui.AutoAnalysisModuleTab");

dojo.require("dwb.ui.ModuleTab");
dojo.require("dwb.ui.ValidationTextBox");
dojo.require("dwb.util.Config");
dojo.require("dojo.DeferredList");
dojo.require("dojo.NodeList-fx");

dojo.declare("dwb.ui.AutoAnalysisModuleTab", [dwb.ui.ModuleTab], {
	// Override ContentPane and default to true
	closable: false,
	
	// Hint tooltip delay till hide timeout reference
	_timeout: null,
	
	analysisContentFragments: {
		"profile": dojo.cache("dwb.ui.fragments", "ExistingProfileModuleTab.html"),
		"web_app": dojo.cache("dwb.ui.fragments", "AnalyseRemoteWebAppModuleTab.html"),
		"dojo_app": dojo.cache("dwb.ui.fragments", "UploadUserAppModuleTab.html"),
		"html_file": dojo.cache("dwb.ui.fragments", "AnalyseHTMLModuleTabLocal.html"),
		"failure": dojo.cache("dwb.ui.fragments", "AnalysisFailureModuleTab.html")
	},
	
	analysisType: "web_app",
	
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
	
	_handleResponse : function (response) {
		this._cancelBusyBtn();
		
		var discoveredModules = this._parseDiscoveredModules(response);
		var customPackages = response.packages || [];
		
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
                    // TODO: Dependencies API should return 
                    // package information for each module rather 
                    // than asking us to work out which module is 
                    // for which package.
                    var packageId = module.name.match(/^dojo|^dijit/) ? "dojo" : customPackages[0].name;
                    module["package"] = packageId;

					modules.push(module);
				}
			});
			
			this._displayAllModules(modules);
		}));

		// Reset package layer name for each analysis
		this.customLayerName = null;
		
        // Add all temporary package references to global profile
        dojo.publish("dwb/package/temporary", [customPackages]);
	},
	
	// Background upload of HTML file, returning modules found.
	_onSubmit : function (e) {		
		dojo.stopEvent(e);
		
		// Check user has filled in form fields before submission
		if (this._isValid()) {		
			var form = dojo.query("form", this.domNode)[0];
			// HTML files, web apps and profiles go through dependencies end point. User applications
			// need turning into temporary packages, must go through package api.
			var url = (this.analysisType === "dojo_app") ? this.packageURL : this.analysisURL;
			
			dojo.io.iframe.send({
				url: url, 
				method: "post", 
				handleAs: "json", 
				form: form, 
				load: dojo.hitch(this, "_handleResponse"), 
				error: dojo.hitch(this, "_moduleAnalysisError")
			});	
		} else {
			this._cancelBusyBtn();
		}
	},
	
	// Signal that we want to remove this tab 
	// from container parent.
	_onRemove : function () {
	},
	
	// Validate form parameters for auto-analysis module tab, this may be either validation input 
	// boxes or file input fields. 
	_isValid : function () {
		return (this.analysisType === "web_app" ? this._isValidInputText() : this._isValidInputFile());	
	},
	
	// Checking input file field isn't empty, show tooltip with hint
	// if user hasn't filled it out.
	_isValidInputFile : function () {
		var inputField = dojo.query("input[type=file]", this.domNode)[0];
		var file = dojo.attr(inputField, "value");
		// Ensure some file has been selected
		var valid = (file !== "");
		if (!valid) {
			// Tooltip not shown at the moment.
			if (this._timeout === null) {
				dijit.showTooltip("This value is required", inputField);
			// Just reset hide timeout for shown tooltip
			} else {
				clearTimeout(this._timeout);
			}
			
			// Hide tooltip after a second
			this._timeout = setTimeout(dojo.hitch(this, function () {
				dijit.hideTooltip(inputField);
				this._timeout = null;
			}), 1000);
		}
		
		return valid;
	},
	
	// Check validation on input text fields in this content pane
	_isValidInputText : function () {
		// Find validation text box within content pane.
		var tb = dijit.byNode(dojo.query(".dijitValidationTextBox", this.domNode)[0]);
		
		// Validation messages won't show until user has at 
		// least focused on field. Fake this behaviour.  
		tb._hasBeenBlurred = true;
		
		// Check for empty field and focus if not valid.
		var valid = tb.validate();
		if (!valid) {
			tb.focus();
		}
		
		return valid;
	},
	
	_cancelBusyBtn : function () {
		dojo.query(".dijitButton", this.domNode).forEach(function (button) {
			dijit.byNode(button).cancel();
		});
	}, 
	
	_moduleAnalysisError : function (timeout) {
		this.set("content", this.analysisContentFragments.failure);
		
		dojo.query(".analyse_container", this.domNode).fadeOut({
			onEnd: dojo.hitch(this, function () {
				this._setAnalysisTypeAttr(this.analysisType);
			}),
			duration: 750,
			delay: 1250
		}).play(); 
	}
});
