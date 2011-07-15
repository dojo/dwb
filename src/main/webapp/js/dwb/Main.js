dojo.provide("dwb.Main");

// _base module handles all our dojo.requires
dojo.require("dwb.Main._base");

dojo.declare("dwb.Main", dwb.Main._base, {
	templateString: dojo.cache("dwb.ui.templates", "Main.html"),	
	widgetsInTemplate: true,

	// Global store for Dojo modules
    store: null,

	// Enhanced Grid displaying Dojo modules
	module_grid: null,

	// Data store for build layers 
	layers_store: null,

	// Internal counter used to generate random layer name
	// suffix
	newLayersCount: null,

	_lastQuery : "",

	_modulesDisplayedFormat: "Showing ${0} - ${1} of ${2} modules",

    _dojoVersionFormat: '<label>Dojo Toolkit ${0}<input value="${0}" dojoType="dijit.form.RadioButton"></label>',

	displayMode: "simple",

	content: dojo.cache("dwb.ui.fragments", "ExistingProfileModuleTab.html"),

    // Referenced to currently open menu pop-up
    lastPopup: null,

    packageService: null,
    buildService: null,

    // Handle for display mode dropdown
    displayModeDialog: null,

	// When we are manually updating selection 
	// rows for a new filter result set, lots of selection
	// changed events are generated. We don't want to publish
	// these until we have finished.
	_searchFilteringInProgress: false,

	baseProfile : {
		"optimise": null,
		"cdn": null,
		"platforms": null, 
		"themes": null,
		"cssOptimise": null,
        "packages": null
	},

    // Deferred object representing current in-flight 
    // build result polling XHR request. 
    _inflight: null,

	constructor : function () {
        // Instantiate the package service controller
        // and ask it to load all package data. Package 
        // details are published to appropriate topics when 
        // loaded.
        this.packageService = new dwb.service.Package();
        this.packageService.load();

        // Create the new build service controller
        this.buildService = new dwb.service.Build();

		dojo.subscribe("dwb/search/updateFilter", dojo.hitch(this, "updateModuleFilter"));
		dojo.subscribe("dwb/build/request", dojo.hitch(this, "triggerBuildRequest"));

		dojo.subscribe("dwb/displayMode/advanced", dojo.hitch(this, function () {
			this.displayMode = "advanced";
			// Toggle two items around
            dojo.attr(dojo.byId("display_mode_link"), "innerHTML", "Advanced");
            dojo.addClass(dojo.byId("display_mode_link"), "narrow");
			this.tabContainer.addChild(this.layersPane, 1);
			this._refreshViewPanels(this.tabContainer.selectedChildWidget.get("title"), this.displayMode);
		}));

		dojo.subscribe("dwb/displayMode/simple", dojo.hitch(this, function () {
			this.displayMode = "simple";
            dojo.attr(dojo.byId("display_mode_link"), "innerHTML", "Simple");
            dojo.removeClass(dojo.byId("display_mode_link"), "narrow");
			this.tabContainer.removeChild(this.layersPane);
			this._refreshViewPanels(this.tabContainer.selectedChildWidget.get("title"), this.displayMode);
		}));	

		dojo.subscribe("dwb/layers/addModules", dojo.hitch(this, function (message) {
			if (this.displayMode === "simple") {
				this.simpleModuleSelection(message.modules);
			} else if (this.displayMode === "advanced") {
				this.newModulesSelection(message.layer, message.modules, message.layerName);	
			}
		}));


		dojo.subscribe("dwb/layer/selected/titleChange", dojo.hitch(this, function (message) {
			this.updateSelectedLayerTitle(message.title);
		}));

        // TODO: Push this subscription to the actual widget
        dojo.subscribe("dwb/build/options", dojo.hitch(this, function (response) {
            this.buildOptionsContent.addBuildParameters(response);
            dojo.forEach(["cdn", "optimise", "platforms", "themes", "cssOptimise"], dojo.hitch(this, function (key) {
                var options = response[key];
                if (options && options.length > 0) {
                    this.baseProfile[key] = options[0].value;
                }
            }));
        }));

        dojo.subscribe("dwb/build/started", dojo.hitch(this, function () {
            this._updateLogView([]);
            this.buildProgress.show();
        }));

        dojo.subscribe("dwb/build/status", dojo.hitch(this, "_updateLogView"));

        dojo.subscribe("dwb/build/finished", dojo.hitch(this, function (location) {
            this._buildRequestHasFinished("success", function () {
				window.location.assign(location);
            });
        }));

        dojo.subscribe("dwb/build/failed", dojo.hitch(this, "_buildRequestHasFinished", "failure"));

        // Listen for package version notifications to retrieve available Dojo versions. 
        dojo.subscribe("dwb/package/versions", dojo.hitch(this, "_packageVersionsAvailable"));

        // Initial package and modules meta-data has been loaded
        dojo.subscribe("dwb/package/modules", dojo.hitch(this, "_packageModulesAvailable"));


        // Reset all user selections when they change the package version used.... 
        // Short term fix because it's not trivial to manually check what modules are being
        // used and not available in the new version. Feature request..... 
        dojo.subscribe("dwb/package/change_to_version", dojo.hitch(this, "_resetUserSelection")); 

        // New temporary package discovered, through auto-analysis. 
		dojo.subscribe("dwb/package/temporary", dojo.hitch(this, function (packages) {
            dojo.forEach(packages, dojo.hitch(this, function (pkge) {
                this.baseProfile.packages.push({
                    "name": pkge.name,
                    "version": pkge.version
                });
            }));
		}));

        dojo.subscribe("dwb/error/loading_packages", dojo.hitch(this, "_moduleLoadingError"));

		this.newLayersCount = 0;

		// Initialise base data store layer items
		this.baseLayer = { 
			"label": "name",
			"items": [
			      {"name": "New Layer", "ignored": true, "labelChange":false}
	        ]
		};

		// Set up datastore to hold data about build layers  
		this.layers_store = new dojo.data.ItemFileWriteStore({"data":this.baseLayer}); 

        // MUST HAPPEN DYNAMICALLY...
        // Add tooltip to elements
        this.versionDialog = new dijit.TooltipDialog({
            "class": "option_dropdown"
        });

       // Create tooltip dialog to allow changing of the display mode
        this.displayModeDialog = new dijit.TooltipDialog({
            content: dojo.cache("dwb.ui.fragments", "DisplayModeTooltipContent.html")
        });
	},

	startup: function () {		
		this.inherited(arguments);

        dojo.publish("dwb/displayMode/simple");

		// When the grid filtering is finished, we can now update the module overview. This is
		// blocking during filtering as rows as toggled individually and it would cause too many
		// reflows.
		this.connect(this.module_grid, "filter", dojo.hitch(this, function () {
			this._searchFilteringInProgress = false;
			this.module_grid.onSelectionChanged();
		}));
		
		// When the user scrolls the module grid, we need to update the row indices and counter shown. 
		this.connect(this.module_grid, "onVisibleRowsChange", dojo.hitch(this, function (firstVisibleRow, lastVisibleRow, rowCount) {
			dojo.attr(this.moduleCounter, "innerHTML", dojo.string.substitute(this._modulesDisplayedFormat, arguments));
        }));

		// When user modifies the selected modules in the grid, propagate changes through to module overview panel.
		this.connect(this.module_grid, "onSelectionChanged", dojo.hitch(this, function () {
			// Ignore events when manual selection re-painting is occurring.
			if (!this._searchFilteringInProgress) {
				var selection = this.module_grid.getAllSelectedItems();
				this.buildOptionsContent.set("modulesSelected", selection);
			}
		}));

		// Remove module selection when user attempts to remove a module from the overview panel 
		this.connect(this.buildOptionsContent, "onModuleRemove", dojo.hitch(this.module_grid, "removeItemSelection"));
		
		// Scroll to selected module when user click in module overview panel
		this.connect(this.buildOptionsContent, "onModuleSelected", dojo.hitch(this.module_grid, this.module_grid.scrollToItem));

        // Add tooltip dialogs to the menu links
        this.addHoverMenu(this.displayModeDialog, "display_mode_link");
        this.addHoverMenu(this.versionDialog, "version_link");

		// Fix initial layer item to layer pane and reference layers store 
		// on base layer.	
		this.baseLayerPane.set("layersStore", this.layers_store);

		// Set the new data store on filtering select and default to first item
		this.buildOptionsContent.set("layersStore", this.layers_store);		
	},

	// Change currently displayed modules based upon  
	// user's module and text filtering selection 
	updateModuleFilter : function () {
		var query = this.get("currentFilterQuery");

		// Don't go to the effort of re-rendering the
		// grid if the current query matches the previous query 
		if (query === this._lastQuery) {
			return;
		}

		this._searchFilteringInProgress = true;

		// Initiate re-rendering of filtered results set, force internal cache clear
		this.module_grid.filter(query, true);

		// Cache current query
		this._lastQuery = query;
	},

	_getCurrentFilterQueryAttr : function () {
		// Find user's configuration selection values;
		var currentSelection = this.buildOptionsContent.get("configurationValues");
		var textSearchValue = this._searchInputField.get("value");
		
		return dwb.util.Util.constructModuleFilterQuery(textSearchValue, currentSelection);
	},

	// Show the global module selection page 
	_showModulePage : function () {
		this.tabContainer.selectChild(this.modulesPane);
	},

	// Event handler for row click on global module page. 
	// Allows user to click anywhere on a row to select
	// that row.
	_moduleGridRowClick : function (e) {
        var selectRow = !this.module_grid.selection.isSelected(e.rowIndex);
		this.module_grid.rowSelectCell.toggleRow(e.rowIndex, selectRow);
	},

	// When user modifies layer title box, ensure that this change 
	// is reflected in the layers store. 
	// TODO: Use notifications API on data store to handle this automatically.
	updateSelectedLayerTitle : function (title) {
		var selectedModuleLayer = this.layersTabContainer.selectedChildWidget;

		// Update tab title and associated store item
		selectedModuleLayer.set("title", title);			
		this.layers_store.setValue(selectedModuleLayer.get("layerItem"), "name", title);
	},

	triggerBuildRequest: function () {
		var buildProfile = (this.displayMode === "simple") ? this._constructSimpleProfile() : this._constructLayersProfile();

		dojo.when(buildProfile, dojo.hitch(this, function (profile) {
			// Gather build parameters from form and include in profile.
			var completeProfile = dojo.mixin(dojo.clone(this.baseProfile), profile);

            this.buildService.schedule(completeProfile);
		}));
	},

	// Simple build profile, just use visual module selection on
	// a base Dojo profile. 
	_constructSimpleProfile : function () {
		var baseLayer = {
				name: "dojo.js",
				modules: []
		};
        var profile = {"layers": [baseLayer]};

		// Return all modules selected, both rendered and not currently rendered. 
		var allSelectedModules = this.module_grid.getAllSelectedItems();
		
        var hasDijitModule = false;

		dojo.forEach(allSelectedModules, dojo.hitch(this, function (item) {
            var module = {
                "name": this.store.getValue(item, "name"),
                "package": this.store.getValue(item, "package")
            };

            // If user has included a dijit module, always include 
            // the Claro theme in the build results. 
            // FIXME: This is a nasty hack and should use the actual build
            // options rather than assuming Claro is available. 
            if(!hasDijitModule && module.name.match(/^dijit/)) {
                profile.themes = "claro";
                hasDijitModule = true;
            }

			baseLayer.modules.push(module);
		}));

		return profile;
	},

	// User has triggered a build request. Construct build profile
	// from module tabs and send XHR request. 
	_constructLayersProfile : function (e) {
		var d = new dojo.Deferred();

		// Dojo.Deferred used to synchronise asynchronous requests to multiple data stores.
		// used to build up the profile.
		var profileLayersAvailable = this._retrieveLayersDetails();

		profileLayersAvailable.then(dojo.hitch(this, function (profileLayers) {
			var buildProfile = {
				"layers" : []
			};

			// Pull out layer profiles from deferred responses. Ignore 
			// any null responses, corresponds to an ignored layer.
			dojo.forEach(profileLayers, function (resolvedLayer) {
				var newLayer = resolvedLayer[1]; 
				// Filter out ignored layers
				if (newLayer !== null) {
					buildProfile.layers.push(newLayer);
				}
			});

			// Gather build parameters from form and include in profile.
			dojo.mixin(buildProfile, this.buildOptionsContent.get("buildParameters"));

			d.callback(buildProfile);
		}));

		return d;
	},

	// Construct the build profile from the user's
	// module tabs. Fetch all layers and for each 
	// pull out the modules contained within. 
	_retrieveLayersDetails : function () {
		var complete = new dojo.Deferred();

		// Construct layer profile for data store item. Unless 
		// layer is marked as "ignored", fetch all modules and 
		// add module name to the layer. Special handling if this 
		// layer has a custom user package associated with it. 
		var buildLayerProfile = function (ref, item) {
			var d = new dojo.Deferred();

			// Should layer be included in the build profile?
			if (ref.store.getValue(item, "ignored") !== true) {
                var layer = {
                    name: ref.store.getValue(item, "name"),
                    modules: []
                };				

				// If this layer contains modules from a user's application, 
				// include that within build profile. 
				if (ref.store.hasAttribute(item, "tempUserPackage")) {
					layer.userPackage = ref.store.getValue(item, "tempUserPackage"); 
				}				

				var modulesStore = ref.store.getValue(item, "modules");
				// Fetch all the modules and extract name attribute for each.
				// Include this information in the layer profile. 

				// Finally, signal completion of inner asychr. fetch to 
				// parent listener. 
				modulesStore.fetch({onComplete:function (modules) {
					layer.modules = dojo.map(modules, function (module) {
                        return {
                            "name": modulesStore.getValue(module, "name"),
                            "package": modulesStore.getValue(module, "package")
                        }
                    });

					d.callback(layer);
				}});				
			} else {
				// Pass null value as deferred result to indicate an ignored layer.
				d.callback(null);
			}

			return d;
		};

		var buildProfileFromLayers = function (layers, ref) {
			// For each layer, build individual profile with async operation. 
			// Deferred used to indicate completion for each layer. 
			var deferreds = dojo.map(layers, dojo.partial(buildLayerProfile, ref));

			// Allow synchronisation of individual layer construction. 
			var dfl = new dojo.DeferredList(deferreds);

			// When each layer has been individually resolved, 
			// pass complete profile back to listeners. 
			dfl.then(complete.callback);
		};

		// Find all layers and process module in each result.
		this.layers_store.fetch({onComplete:buildProfileFromLayers});

		return complete;
	},


	_updateLogView : function(logs) {
		var logLines = ["", "", "", "", ""];

		for(var i = 0, idx = logs.length - 1; i < 5 && idx >= 0; i++, idx--) {
			logLines[i] = logs[idx];
		}

		dojo.query("td", this.buildProgress.domNode).forEach(function (elem) {
			var line = logLines.pop();
			elem.innerHTML = (typeof line == "undefined") ? "" : line; 
		});
	},

	_buildRequestHasFinished: function(status, callback) {
        // Show associated status message
        dojo.addClass(this.buildProgress.domNode, status);
		
        // After brief period, hide the dialog and remove message
	    setTimeout(dojo.hitch(this, function () {
		    this.buildProgress.hide();
		    dojo.removeClass(this.buildProgress.domNode, status);
			
		    // If the user has asked for notification, execute callback.
    	    if (callback) {
                callback();
            }
        }), 1000);
	},
	
    // Prematurely finish polling of the build status
    // over XHR. 
    cancelCurrentBuild : function () {
        this.buildService.cancel();
    },

    // Listener for all package version notifications. We want to 
    // access available versions of Dojo to set up the Tooltip dialog for
    // switching between them. 
    _packageVersionsAvailable: function (pkge) { 
        if (pkge.name === "dojo") {
            // Sort Dojo package versions so that they are in ascending order
            var comparator = function (a,b) {
                return (a.name > b.name) ? -1 : (a.name < b.name) ? 1 : 0;
            };
            // Construct input & label for each version and set on tooltip dialog
            var dialogContent = dojo.map(pkge.versions.sort(comparator), dojo.hitch(this, function (version) {
                return dojo.string.substitute(this._dojoVersionFormat, [version.name]);  
            })).join("");
            this.versionDialog.set("content", dialogContent);

            // Set highest Dojo version as selected and show version in header
            dojo.query(".dijitRadio", this.versionDialog.domNode).at(0).forEach(function (node) {
                dijit.byNode(node).set("checked", "true");
                dojo.attr(dojo.byId("version_link"), "innerHTML", dijit.byNode(node).get("value")); 
            });

            // Add event handlers to signal change in Dojo version when user selects...
            dojo.query("input", this.versionDialog.domNode).connect("onclick", dojo.hitch(this, function (e) {
                dojo.attr(dojo.byId("version_link"), "innerHTML", e.target.value); 
                dojo.publish("dwb/package/change_to_version", [ 
                    { "name": "dojo", "version": e.target.value }
                ]);
            }));
        }
    },

	// Packages modules have been retrieved and can be rendered 
	// using the module grid. 
	_packageModulesAvailable : function(packageModules) {		
		var modulesInfo = [];
        this.baseProfile.packages = [];

        // For every package, create module grid item and store 
        // identifying references in build profile.
        dojo.forEach(packageModules, dojo.hitch(this, function(pkge) {
            this.baseProfile.packages.push({
                "name": pkge.name,
                "version": pkge.version
            });
            dojo.forEach(pkge.modules, dojo.hitch(this, function (module) {
                var item = this._generateModuleGridItem(pkge.name, module);
                modulesInfo.push(item);
            }));
        }));

		// Create new data store holding module information,
        // setting on the grid and analyse panel
		this.store = new dojox.data.AndOrWriteStore({data:{
            "identifier":"name",
            "items": modulesInfo
		}});

		this.module_grid.setStore(this.store);
		this.analysePane.set("globalModulesStore", this.store);

		// Propagate current filter options to new store view
        this.updateModuleFilter();
	}, 

    // Process package module and produce corresponding dojo store
    // item.
    _generateModuleGridItem : function (packageId, module) {
	    var name = module[0], desc = module[1];

        // Extract first module component from name
        var baseModule = name.split(".")[0];

        // Trim description, lines over ~300 chars cause issues
        // with grid height being larger than background image
        // used for styling.
        if (desc.length > 340) {
            desc = dojo.trim(desc.substring(0, 300))  + "...";
        }

	    return {"name":name, "desc": desc, "baseModule": baseModule, "package": packageId};		
    },

	// Remove all possible title classes and add the current one.
	_refreshViewPanels : function (currentTitle, currentMode) {
		var allTitles = ["Modules", "Layers", "Auto-Analyse", "Help", "simple"];
		dojo.removeClass(this.buildOptionsContent.domNode, allTitles);
		dojo.addClass(this.buildOptionsContent.domNode, currentTitle);
	},

	// When top-level tab container displays a new child, modify the  
	// displayed option panels on the right hand side.
	_tabChildSelect : function (tabChild) {
		this._refreshViewPanels(tabChild.get("title"), this.displayMode);
	},

	// Change module layer title in update panel when user 
	// selects a new module tab. Hide/show appropriate panels.
	_layersTabChildSelect : function (tabChild) {
		var label = this.layers_store.getLabel(tabChild.layerItem);

		// Disable ability to update layer name for pseudo-layer that acts as 
		// home tab for user. 
		var readOnly = (this.layers_store.getValue(tabChild.layerItem, "labelChange") === false);

		dojo.publish("dwb/layer/selected", [{
			title: label,
			readOnly: readOnly
		}]);		
	},

	simpleModuleSelection : function (modules) {
		// Show layers tab.
		this._showModulePage();

		// Reset existing selection of modules
		this.module_grid.rowSelectCell.toggleAllSelection(false);
		
		var firstVisibleRow = null;
		
		var fetchCompleted = [];
		
		// Ignore selection change event until we have finished all changes
		this._searchFilteringInProgress = true;
		
		dojo.forEach(modules, dojo.hitch(this, function (module) {
			var name = module.name[0];
			// Used to signify async. operation completion
			var d = new dojo.Deferred();
			this.module_grid.store.fetchItemByIdentity({identity:name, onItem:dojo.hitch(this, function(item){
				// If item wasn't found, create a new entry, otherwise look up 
				// index for item.
				if (!item) {
					item = this._createNewModule(name, module.desc[0], module["package"][0]);
				} 
				
				var rowIdx = this.module_grid.addItemSelection(item);
				
				// Store first row that is selected
				if (firstVisibleRow === null && rowIdx !== -1) {
					firstVisibleRow = rowIdx;
				}
				
				d.callback();
			})});
			fetchCompleted.push(d);
		}));
		
		// Once all rows have been selected, scroll to first row if 
		// one was found.
		var dfl = new dojo.DeferredList(fetchCompleted);
		dfl.then(dojo.hitch(this, function () {
			// Update module overview
			this._searchFilteringInProgress = false;
			this.module_grid.onSelectionChanged();
			
			if (firstVisibleRow !== null) {
				this.module_grid.scrollToRow(firstVisibleRow);
			}
		}));
	},
	
	_createNewModule: function (name, desc, packageId) {
        var details = this._generateModuleGridItem(packageId, [name, desc]);
		var item = this.module_grid.store.newItem(details);
		this.module_grid.lastResultSet.push(item);
		return item;
	},
	
	// Add selected modules to the chosen module layer. 
	newModulesSelection : function (layer, moduleItems, name) {
		if (moduleItems.length > 0) {
			// TODO: Fix this! Lazy...
			var modules = dojo.map(moduleItems, function(row) {
				return {"name": row.name[0], "desc": row.desc[0], "package":row["package"][0]};
			});

			// Pseudo-layer, with new module links, has ignored attribute set.
			var ignored = this.layers_store.getValue(layer, "ignored");

			// Show layers tab.
			this.tabContainer.selectChild(this.layersPane);

			// Used chose to create a new layer, render tab and modules.
			if (ignored === true) {
				this._createNewBaseModuleTab(modules, name);
				// Add module to existing layer and show associated tab.
			} else {		        
				var moduleLayer = this.layers_store.getValue(layer, "layerTab");				
				this.layersTabContainer.selectChild(moduleLayer);				
				moduleLayer.set("modules", modules);				
			}

			// Reset existing selection of modules
			this.module_grid.rowSelectCell.toggleAllSelection(false);
		}		
	},

	// Create a new modules tab, add it to the tab container
	// and make it visible.
	_createNewBaseModuleTab : function (modules, name) {
		var newModuleTab = new dwb.ui.ModuleTab({
			layersStore: this.layers_store,
			modules: modules,
			title: name
		});

		this._displayLayer(this.layersTabContainer, newModuleTab);
	},

	// Allow creation of a new module type layer, which is 
	// automatically added and displayed to the tab container.
	_createModuleTabLayer : function (moduleTabType) {
		var layer = new moduleTabType({
			layersStore: this.layers_store,
			globalModulesStore: this.store
		});

		this._displayLayer(this.layersTabContainer, layer);

		return layer;
	},

	// Utility function to add layer to container and display 
	// that child.
	_displayLayer : function (container, layer) {
		container.addChild(layer);
		container.selectChild(layer);
	}, 

	changeCurrentLayerName : function (newName) {		
		// Update layer name to correspond to user's chosen filename, minus extension.
		this.buildOptionsContent.layerNameTextBox.set("value", newName);
		// Simulate manual user name change
		this.buildOptionsContent.layerNameUpdate.onClick();
	},

	// When any path of the module loading process
	// fails, lock down UI to stop further interaction
	// and display error message.
	_moduleLoadingError : function () {
		dojo.addClass(this.modulesPane.domNode, "failure");
		
		// Disable all action buttons, radio buttons, select boxes. Don't want
		// user triggered that interact with the module list.
		dojo.query(".dijitButton, .dijitRadio, .dijitCheckBox, .dijitSelect, .dijitComboBox, .dijitDropDownButton").forEach(function (node) {
			dijit.byNode(node).set("disabled", true);
		});

        // No easy way to disable tab list, override the onclick handler
        // to swallow the event. 
        this.tabContainer.tablist.onButtonClick = function () {
            return false;
        };
	},

    // Reset all user interactions with the UI, returning 
    // to the post-loaded state. This includes modules selected,
    // search fields, layers created and more....
    _resetUserSelection: function () {
        // Clear search string, all modules will be shown
        this._searchInputField.set("value", "");
        this.updateModuleFilter();
        // Manually unselect any chosen modules
        this.module_grid.indirectSelector.toggleAllSelection(false);


        // Remove any custom module layers....
        dojo.forEach(this.layersTabContainer.getChildren(), dojo.hitch(this, function (tab) {
            if (tab.closable) {
                tab.onClose();
                this.layersTabContainer.removeChild(tab);
            }
        }));
    },

    addHoverMenu: function (dialog, link_name) {
        var link = dojo.byId(link_name);
        this.connect(link, "onmouseover", dojo.hitch(this, dojo.hitch(function (e) {
            dojo.stopEvent(e);
            // If user has moved mouse directly across the header, 
            // previous drop-down menu won't have been set to close on exit yet.
            dijit.popup.close(this.lastPopup);
            dijit.popup.open({
                popup: dialog,
                around: link,
                orient: {BR: "TR"}
            });
            this.lastPopup = dialog;
        })));

        dojo.connect(dialog, 'onMouseLeave', function() {
            dijit.popup.close(dialog);
        });
    },
    
	checkForEnter: function(keyEvent) {
		if(keyEvent.keyCode == 13) {
			this.updateModuleFilter();
		}
	}
});
