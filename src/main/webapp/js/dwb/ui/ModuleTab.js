dojo.provide("dwb.ui.ModuleTab");

dojo.require("dwb.util.Util");

dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.require("dijit.layout.ContentPane");

dojo.declare("dwb.ui.ModuleTab", [dijit.layout.ContentPane], {
	// Override ContentPane and default to true
	closable: true,
	
	// Hint text to display when the modules tab is empty
	content: dojo.cache("dwb.ui.fragments", "ModuleTab.html"),
	
	// Simulate close button in HTML
	removeFragment: dojo.cache("dwb.ui.fragments", "CloseIcon.html"),
	
	layersStore: null,
	
	layerItem: null,
	
	// Used to force the new module operation to  
	// 
	_layersStoreReady: null,
	
	moduleGrid: null,
	
	/** Having layout issues with overflow on 100% height for second level tabs **/
	moduleGridHeight: "99.5%",
	
	labelChange: true,
	
	moduleGridLayout: [
        {field: 'name', name: 'Module Name', width: '25%'},
        {field: 'desc', name: 'Description', width: '60%'},
        {field: 'remove', name: 'Remove', width: '10%'}
    ],
	
    constructor : function () {
		this._layersStoreReady = new dojo.Deferred();			
	},
    
	postMixInProperties : function () {
		this.inherited(arguments);
		// If explicit title wasn't specified, give the module tab a 
		// generated title. 
		if (!this.title) {
			this.title = dwb.util.Util.generateModuleTabTitle();
		}
	},
	
	postCreate: function () {
		this.inherited(arguments);		
		
		// Create empty grid widget displaying the layer modules.
		this._initialiseModuleGrid();		
	},
	
	onClose : function () {
		if (this.layerItem) {
			this.layersStore.deleteItem(this.layerItem);
		}
		
		return this.inherited(arguments);
	},
	
	_initialiseModuleGrid : function() {		
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
        
        // Once grid has been rendered, set up conditional when 
		// layers store is available we set the grid store on this.
        // Layers store may be already available or hasn't been set yet.
		this._layersStoreReady.then(dojo.hitch(this, function () {			
			var modulesStore = this.layersStore.getValue(this.layerItem, "modules");
			this.moduleGrid.setStore(modulesStore);
			this.updateGridDisplay();
			// FIX: Having trouble with grid rendering on some pages, force render again
			// at this point.
			this.moduleGrid.render();
		})); 
	},
	
	updateGridDisplay : function() {
		// Ensure grid has been rendered, otherwise always "empty".
		if (this.moduleGrid && this.moduleGrid.store) {		
			this.moduleGrid.store.fetch({onBegin: dojo.hitch(this, function (size, request) {
				// Rendered grid is empty if store has no items
				this.set("gridIsEmpty", (size === 0));			
			})});
		// No grid available, ensure help text is shown
		} else {
			this.set("gridIsEmpty", true);
		}
	},
	
	// Process existing Dojo modules and raise event to have these added
	// to the base layer. 
	_displayDojoModules : function (dojoModules) {
		// Process results, try to find module description from modules store. 
		var completed = dwb.util.Util._populateModuleDetails(this.globalModulesStore, dojoModules);
		
		// Previous call involves multiple async. dojo.data fetch operations.
		// Use Deferred List to wait for completion. 
		completed.then(dojo.hitch(this, function(data) {
			var modules = [];
			
			// Add module details if deferred resolved correctly.
			dojo.forEach(data, function(result) {				
				if (result[0]) {
					modules.push(result[1]);
				}
			});

			this.onNewBaseModules(modules);
		}));
	},
	
	_displayCustomModules : function (moduleNames) {		
		// Display custom modules grid. 
		var customModules = this._populateCustomModuleDetails(moduleNames);
		
		// Remove any Dojo widgets and clean up properly.
		dojo.query("[widgetid]", this.domNode).forEach(function(widget){
			dijit.byNode(widget).destroy();
		});
		
		// Destroy HTML elements now that user has analysed file.
		dojo.query("div.analyse_container", this.domNode).orphan();
		
		// Change placeholder from HTML file upload to empty module hint
		this.set("content", dojo.cache("dwb.ui.fragments", "ModuleTab.html"));
		
		// Set discovered modules in data store
		this.set("modules", customModules);
		
		this._initialiseModuleGrid();								
	},
	
	_populateCustomModuleDetails : function (response) {
		var details = dojo.map(response, function (module) {
			return {"name": module, "desc": "Unable to find a description for this module."};
		});
		
		return details;
	},
	
	// Event handler to signal module tab has 
	// modules to add to the base layer
	onNewBaseModules : function (modules) {
	},
	
	
	// When grid is empty, display the hint placeholder. Otherwise,
	// ensure grid is displayed.
	_setGridIsEmptyAttr : function (empty) {
		var gridDisplay = empty ? "none" : "block";
		var hintTextDisplay = !empty ? "none" : "block";
		
		dojo.query("div.dojoxGrid", this.domNode).style("display", gridDisplay);
		dojo.query("div.analyse_container", this.domNode).style("display", hintTextDisplay);		
	},
	
	// Register this layer in the global layer repository, once layersStore & modules 
	// attributes are set.
	_setLayersStoreAttr : function (layersStore){
		this.layersStore = layersStore;
		
		// Child store holding modules details, connecting to module grid and later
		// when generating build request.
		var layerModulesStore = new dojo.data.ItemFileWriteStore({data: {
			identifier: "name",
    		items: []
    	}});
		
		this.layerItem = layersStore.newItem({
			"name": this.title,
			"layerTab": this,
			"labelChange": this.labelChange,
			"modules": layerModulesStore			
		});
		
		// Broadcast layers store 
		this._layersStoreReady.callback();
	},
	
	// Add extra modules to this layer's module store.
	_setModulesAttr : function (modules) {
		
		if (modules.length > 0) {
			// Force module grid show now modules have been added.
			this.set("gridIsEmpty", false);
			
			// Ensure layers store has been initialised 
			// before attempting to use...
			this._layersStoreReady.then(dojo.hitch(this, function() {
				var modulesStore = this.layersStore.getValue(this.layerItem, "modules");
				dojo.forEach(modules, dojo.hitch(this, function(module) {
					module.remove = this.removeFragment;
					// If user tries to add the same module again, we'll 
					// get an exception. Handle exception silently so it
					// doesn't interfere with other module insertions.
					try {
						modulesStore.newItem(module);
					} catch (e) {
						console.warn(e);
					}
				}));
				modulesStore.save();						
			}));
		}
	}
});
