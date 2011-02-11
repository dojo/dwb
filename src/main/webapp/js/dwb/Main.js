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

	packageEndPoint: dwb.util.Config.get("packagesApi"),

	_lastQuery : "",

	_modulesDisplayedFormat: "Showing ${0} - ${1} of ${2} modules",

	displayMode: "simple",

	content: dojo.cache("dwb.ui.fragments", "ExistingProfileModuleTab.html"),

	// When we are manually updating selection 
	// rows for a new filter result set, lots of selection
	// changed events are generated. We don't want to publish
	// these until we have finished.
	_searchFilteringInProgress: false,

	temporaryPackages: [],

	baseProfile : {
		"package": null,
		"version": null,
		"optimise": null,
		"cdn": null,
		"platforms": null, 
		"themes": null,
		"cssOptimise": null
	},

	constructor : function () {
		// Fire off module loading service call straight away
		var d = dojo.xhrGet({
			url: this.packageEndPoint,
			handleAs: "json"
		});
		d.then(dojo.hitch(this, "_handlePackagesResponse"), dojo.hitch(this, "_moduleLoadingError"));

		dojo.subscribe("dwb/search/updateFilter", dojo.hitch(this, "updateModuleFilter"));
		dojo.subscribe("dwb/build/request", dojo.hitch(this, "triggerBuildRequest"));

		dojo.subscribe("dwb/displayMode/advanced", dojo.hitch(this, function () {
			this.displayMode = "advanced";
			// Toggle two items around
			dijit.byId("simpleModeItem").set("checked", false);
			dijit.byId("displayModeBtn").set("label", "Advanced Mode");
			this.tabContainer.addChild(this.layersPane, 1);
			this._refreshViewPanels(this.tabContainer.selectedChildWidget.get("title"), this.displayMode);
		}));

		dojo.subscribe("dwb/displayMode/simple", dojo.hitch(this, function () {
			this.displayMode = "simple";
			// Toggle two items around
			dijit.byId("advancedModeItem").set("checked", false);
			dijit.byId("displayModeBtn").set("label", "Simple Mode");
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

		dojo.subscribe("dwb/layers/temporaryPackage", dojo.hitch(this, function (message) {
			this.temporaryPackages.push(message.packageId);
		}));

		dojo.subscribe("dwb/layer/selected/titleChange", dojo.hitch(this, function (message) {
			this.updateSelectedLayerTitle(message.title);
		}));

		this.newLayersCount = 0;

		// Initialise base data store layer items
		this.baseLayer = { 
			"label": "name",
			"items": [
			      {"name": "New Layer", "ignored": true, "labelChange":false}
	        ]
		},

		// Set up datastore to hold data about build layers  
		this.layers_store = new dojo.data.ItemFileWriteStore({"data":this.baseLayer}); 
	},

	startup: function () {		
		this.inherited(arguments);

		// Push default mode message to other components
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

	// User has picked a package to select modules from. Fire off XHR 
	// request to retrieve package information and hand off results 
	// to event handler. 
	_newPackageSelected : function (location) {
		var d = dojo.xhrGet({
			url: location,
			handleAs: "json"
		});

		d.then(dojo.hitch(this, function(response) {
			this._modulesAvailable(response.modules);
		}), dojo.hitch(this, "_moduleLoadingError"));
	},

	triggerBuildRequest: function () {
		var buildProfile = (this.displayMode === "simple") ? this._constructSimpleProfile() : this._constructLayersProfile();

		dojo.when(buildProfile, dojo.hitch(this, function (profile) {
			// Gather build parameters from form and include in profile.
			var completeProfile = dojo.mixin(dojo.clone(this.baseProfile), profile);

			var url = dwb.util.Config.get("buildApi");

			// Push build request over XHR. 
			var d = dojo.xhrPost({
				url: url,
				handleAs: "json",
				headers: {"Content-Type":"application/json"},
				postData: dojo.toJson(completeProfile)
			});
			
			// If either of the XHR requests fails, tell user and cancel
			// build progress dialog.
			var errorHandling = dojo.hitch(this, function() {
				this._buildProgressFinished("failure");
			});
			
			// Once complete, cancel build button animation 
			// and forward user to response location
			d.then(dojo.hitch(this, function (response) {
				var d = dojo.xhrGet({
					url: response.buildStatusLink,
					handleAs: "json"
				});

				d.then(dojo.hitch(this, "buildStatusPoller", response.buildStatusLink), errorHandling);
			}), errorHandling);

			// Clear any previous log lines and show progress indicator
			this._updateLogView([]);
			this.buildProgress.show();
		}));
	},

	// Simple build profile, just use visual module selection on
	// a base Dojo profile. 
	_constructSimpleProfile : function () {
		var baseLayer = {
				name: "dojo.js",
				modules: []
		};

		// Return all modules selected, both rendered and not currently rendered. 
		var allSelectedModules = this.module_grid.getAllSelectedItems();
		
		dojo.map(allSelectedModules, dojo.hitch(this, function (item) {
			baseLayer.modules.push(this.store.getValue(item, "name"));
		}));

		return {"layers":[baseLayer], "userPackages": this.temporaryPackages};
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
				"layers" : [],	
				"userPackages" : this.temporaryPackages
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
						return modulesStore.getValue(module, "name");
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
			elem.innerHTML = (line === undefined) ? "" : line; 
		});
	},

	// Repeating process to check status of build process
	buildStatusPoller : function (statusUrl, response) {
		this._updateLogView(response.logs.split("\n"));

		// If the build has completed, redirect to package URL to force download.
		if (response.state === "COMPLETED") {
			this._buildProgressFinished("success", function () {
				window.location.assign(response.result);
			});
			// Otherwise, keep polling for log changes.
		} else if (response.state === "BUILDING" || response.state === "NOT_STARTED") {
			setTimeout(dojo.hitch(this, function () {
				var d = dojo.xhrGet({
					url: statusUrl,
					handleAs: "json"
				});

				d.then(dojo.hitch(this, "buildStatusPoller", statusUrl), function () {
					this._buildProgressFinished("failure")
				});	
			}), 500);
		// An error occurred, indicate this.
		} else {
			this._buildProgressFinished("failure");
		}
	},

	_buildProgressFinished : function(status, callback) {
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
		}), 500);
		
		// Inform all listeners we have finished building.
		dojo.publish("dwb/build/finished");
	},
	
	// Packages modules have been retrieved and can be rendered 
	// using the module grid. 
	_modulesAvailable : function(modules) {		
		var modulesInfo = [];

		// Split modules into groups based upon their top level module reference.  
		// We show these groups in separate title panes. 
		dojo.forEach(modules, function (module) {		
			var name = module[0], desc = module[1];

			// Extract first module component from name
			var baseModule = name.split(".")[0];

			modulesInfo.push({"name":name,"desc": desc, "baseModule": baseModule});			
		});

		// Create new module store.
		this.store = new dojox.data.AndOrWriteStore({data:{
			"identifier": "name",        		
			"items": modulesInfo
		}});

		this.module_grid.setStore(this.store);

		this.analysePane.set("globalModulesStore", this.store);

		// Propagate current filter options to new store view
		this.updateModuleFilter();	 	       
	}, 

	// Remove all possible title classes and add the current one.
	_refreshViewPanels : function (currentTitle, currentMode) {
		var allTitles = ["Modules", "Layers", "Auto-Analyse", "Help", "simple"];
		dojo.removeClass(this.buildOptionsContent.domNode, allTitles);
		dojo.addClass(this.buildOptionsContent.domNode, currentTitle)
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
					item = this._createNewModule(name, module.desc[0]);
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
	
	_createNewModule: function (name, desc) {
		var item = this.module_grid.store.newItem({name: name, desc: desc});
		this.module_grid.lastResultSet.push(item);
		return item;
	},
	
	// Add selected modules to the chosen module layer. 
	newModulesSelection : function (layer, moduleItems, name) {
		if (moduleItems.length > 0) {
			// TODO: Fix this! Lazy...
			var modules = dojo.map(moduleItems, function(row) {
				return {"name": row.name[0], "desc": row.desc[0]};
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
		dojo.query(".dijitButton, .dijitRadio, .dijitCheckBox, .dijitSelect, .dijitComboBox").forEach(function (node) {
			dijit.byNode(node).set("disabled", true)
		});
	},
	
	_handlePackagesResponse: function (response) {
		// TODO: Just use first Dojo package until 
		// we can handle switching versions.
		var packageObj = response.packages[0];

		var d = dojo.xhrGet({
			url: packageObj.link,
			handleAs: "json"
		});

		
		d.then(dojo.hitch(this, "_handlePackageVersionsResponse"), dojo.hitch(this, "_moduleLoadingError"));

		// Store result to use when building
		this.baseProfile["package"] = packageObj.name;

		this.buildOptionsContent.addBuildParameters(response);

		// TODO: We shouldn't do this....
		this.baseProfile["cdn"] = response.cdn[0].value;
		this.baseProfile["optimise"] = response.optimise[0].value;
		this.baseProfile["platforms"] = response.platforms[0].value;
		this.baseProfile["themes"] = response.themes[0].value;
		this.baseProfile["cssOptimise"] = response.cssOptimise[0].value;
	},

	_handlePackageVersionsResponse: function (packageVersions) {
		// TODO: Just use first Dojo package version until 
		// we can handle switching versions.
		var packageVersion = packageVersions[0];

		this._newPackageSelected(packageVersion.link);

		this.baseProfile.version = packageVersion.name;
	},

	checkForEnter: function(keyEvent) {
		if(keyEvent.keyCode == 13) {
			this.updateModuleFilter();
		}
	}
});