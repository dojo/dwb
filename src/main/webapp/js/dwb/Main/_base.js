dojo.provide("dwb.Main._base");

// Custom modules
dojo.require("dwb.ui.ModuleTab");
dojo.require("dwb.ui.BuildConfigPanel");
dojo.require("dwb.ui.AutoAnalysisModuleTab");
dojo.require("dwb.ui.IntroDialog");
dojo.require("dwb.ui.FeedbackDialog");
dojo.require("dwb.ui.ModuleGrid");
dojo.require("dwb.service.Package");
dojo.require("dwb.service.Build");
dojo.require("dwb.util.Config");

// Dojo Toolkit Modules
dojo.require("dojo.parser");
dojo.require("dojo.io.iframe");
dojo.require("dojo.data.ItemFileReadStore");
dojo.require("dojo.data.ItemFileWriteStore");
dojo.require("dijit.form.Form");
dojo.require("dijit.layout.BorderContainer");
dojo.require("dijit.layout.ContentPane");
dojo.require("dijit.layout.TabContainer");
dojo.require("dijit.form.Select");
dojo.require("dijit.TitlePane");
dojo.require("dijit.form.CheckBox");
dojo.require("dijit.form.RadioButton");
dojo.require("dijit.form.FilteringSelect");
dojo.require("dijit.ProgressBar");
dojo.require("dijit.Dialog");
dojo.require("dijit.Menu");
dojo.require("dijit.CheckedMenuItem");
dojo.require("dijit.form.DropDownButton");
dojo.require("dijit.TooltipDialog");
dojo.require("dojox.data.AndOrWriteStore");
dojo.require("dojox.form.BusyButton");
dojo.require("dojox.grid.EnhancedGrid");
dojo.require("dojox.grid.enhanced.plugins.IndirectSelection");

dojo.declare("dwb.Main._base", [dijit._Widget, dijit._Templated], {
});


// Extend title pane to fire an event before toggle content
dojo.extend(dijit.TitlePane, {
	onPreToggleOpen : function () {
	},
	onPreToggleClose : function () {
	},
	_onTitleClick: function () {
		// Fire event before opening, want to shrink
		// module overview panel before expanding this
		// title pane
		if (!this.open) {
			this.onPreToggleOpen(this);
		} else {
			this.onPreToggleClose(this);
		}
		
		if(this.toggleable){
			this.toggle();
		}				
	}
});
