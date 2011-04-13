dojo.provide("dwb.ui.BuildConfigPanel");

dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.require("dwb.util.Config");

dojo.declare("dwb.ui.BuildConfigPanel", [dijit._Widget, dijit._Templated], {
	templateString : dojo.cache("dwb.ui.templates", "ConfigurationPanel.html"),
	
	widgetsInTemplate: true,
	
	endPoint: dwb.util.Config.get("packagesApi"),
	
	build: null,
	
	moduleItemLookup: {},
	
	// Default to the simple mode and one open pane
	_nonOverviewTitlePanesOpen: 2,
	
	//lastRemovedModule: null,
	
	constructor : function () {
		dojo.subscribe("dwb/layer/selected", dojo.hitch(this, function (layer) {
			this.set("currentLayerTitle", layer.title);
			this.set("readOnlyLayerTitle", layer.readOnly);
		}));
		
		dojo.subscribe("dwb/build/finished", dojo.hitch(this, function () {
			// Cancel any buttons that may have been triggered.... 
			this.modulesBuildBtn.cancel();
			this.layersBuildBtn.cancel();
		}));
		
		// Subscribe to events about the module change
		dojo.subscribe("dwb/displayMode/advanced", dojo.hitch(this, function () {
			dojo.removeClass(this.domNode, "simpleMode");
			dojo.addClass(this.domNode, "advancedMode");
		}));
		
		// Subscribe to events about the module change
		dojo.subscribe("dwb/displayMode/simple", dojo.hitch(this, function () {
			dojo.removeClass(this.domNode, "advancedMode");
			dojo.addClass(this.domNode, "simpleMode");
		}));
	},
	
	startup: function () {
		this.inherited(arguments);
		dojo.query(".dijitTitlePaneContentInner", this.domNode).connect("onclick", dojo.hitch(this, function () {
			target = arguments[0].target;
			// Did they click close? 
			if (dojo.hasClass(target, "rowCloseIcon")) {
				var moduleName = target.parentNode.parentNode.childNodes[0].innerHTML;
				this.onModuleRemove(this.moduleItemLookup[moduleName]);
			// Or did they click the actual label?
			} else if (dojo.hasClass(target, "moduleLabel")) {
				var moduleName = target.parentNode.childNodes[0].innerHTML;
				this.onModuleSelected(this.moduleItemLookup[moduleName]);
			}
		}));
	},
	
	addBuildParameters : function (parameters) {
		this._addOptions(this.crossDomainBuildParam, parameters.cdn);
		this._addOptions(this.optimiseBuildParam, parameters.optimise);
		this._addOptions(this.targetPlatformsParam, parameters.platforms);
		this._addOptions(this.includeThemeParam, parameters.themes);
		this._addOptions(this.cssOptimiseParam, parameters.cssOptimise);
	},
	
	_addOptions : function (item, options) {
		dojo.forEach(options, function (option) {
			item.addOption({"label": option.label, "value": option.value});
		});		
	},
	
	onModuleRemove : function () {
	},
	
	onModuleSelected : function () {
	},
	
	_getConfigurationValuesAttr : function () {
		var values = this.formValues.get("value");
		
		// Convert array of values back to true/false
		for (key in values) {
			var value = values[key];
			values[key] = !!value[0];
		}
		
		return values;
	},
	
	_onOptionChanged : function () {
		dojo.publish("dwb/search/updateFilter");
	},
	
	_setModulesSelectedAttr : function (modulesSelected) {
		var title = dojo.string.substitute("Modules Selected (${0})", [modulesSelected.length]);
		this.modulesSelectedView.set("title", title);
		
		var tableTemplate = "<table>";
		var rowTemplate ="<tr><td class='moduleLabel'>${0}</td><td class='moduleRemoval'><span class='dijitInline dijitTabCloseButton dijitTabCloseIcon rowCloseIcon'>"
			+ "<span class='dijitTabCloseText'>x</span></span></td></tr>";
		
		// Clear module lookup
		this.moduleItemLookup = {};
		
		dojo.forEach(modulesSelected, dojo.hitch(this, function(module) {
			if (module) {
				tableTemplate += dojo.string.substitute(rowTemplate, [module.name]);
				this.moduleItemLookup[module.name] = module;	
			}
		}));
		
		tableTemplate += "</table>";
		
		this.modulesSelectedView.set("content", tableTemplate);
	},
	
	_onLayerTitleChange : function () {
		var layerTitle  = this.layerNameTextBox.get("value");
		dojo.publish("dwb/layer/selected/titleChange", [{title: layerTitle}]);
	},

	_setCurrentLayerTitleAttr : function (title) {
		this.layerNameTextBox.set("value", title);	
	},
	
	_setReadOnlyLayerTitleAttr : function (readonly) {
		this.layerNameTextBox.set("disabled", readonly);
	},
	
	// Set layers store on filter select and reset default value to first one.
	_setLayersStoreAttr : function (layersStore) {
		this.layerNameSelect.set("store", layersStore);
		this.layerNameSelect.set("value", 0);
	},
	
	_getSelectedLayerAttr : function () {
		return this.layerNameSelect.get("item");
	},
	
	_getFormValuesAttr : function () {
		return this.form.getValues();
	},
	
	_getBuildParametersAttr : function () {
		// Find all build parameters, from compiler and theme options.
		var buildParams = dojo.mixin(this.buildParameterForm.getValues(), this.themeParameterForm.getValues());
		return buildParams;
	},
	
	onUpdateLayerTitle : function () {
	},
	
	onLocalFileSelect : function () {
	},
	
	_analyseSourceChange : function (checked) {
		// Ignore onChange event for option that has 
		// been deselected
		if (checked) {
			dojo.publish("dwb/analysis/sourceType", [this.analyseModulesSourceForm.getValues()]);
		}
	},
	
	_onAddModuleSelection : function () {
		// Construct array of items
		var selectedModuleItems = [];
		for (var i in this.moduleItemLookup) {
			selectedModuleItems.push(this.moduleItemLookup[i]);
		}
		
		dojo.publish("dwb/layers/addModules", [{
			layer: this.layerNameSelect.get("item"),
			modules: selectedModuleItems
		}]);
	},
	
	_onAddAnalysisModules : function () {
		dojo.publish("dwb/layers/addAnalysisModules", [{
			layer: this.layerNameSelect.get("item")
		}]);
	},
	
	_onBuild : function () {
		dojo.publish("dwb/build/request");
	},
	
	_titlePaneOpen : function (tp) {
		var classLabel = tp.get("title").replace(/ /g, "") + "Open";
		dojo.addClass(this.domNode, classLabel);
	},
	
	_titlePaneClose : function (tp) {
		var f = dojo.hitch(this, function () {
			var classLabel = tp.get("title").replace(/ /g, "") + "Open";
			dojo.removeClass(this.domNode, classLabel);	
		});
		// Don't expand content until other title pane hide animation has completed,
		// otherwise we get inner scroll bars
		setTimeout(f, dijit.defaultDuration);
	}
});
