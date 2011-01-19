dojo.provide("dwb.ui.IntroDialog");

dojo.require("dijit.Dialog");
dojo.require("dojo.cookie");
dojo.require("dijit.form.CheckBox");

dojo.declare("dwb.ui.IntroDialog", dijit.Dialog, {
	title: "Dojo Web Builder Introduction",
    style: "width: 464px;",
    content: dojo.cache("dwb.ui.fragments", "HelpDialogContent.html"),
    leftOffset: null,
	topOffset: null,
	
    constructor: function () {
    	dojo.subscribe('dwb/dialog/shownAtLoad', function (message) {
			if (!message.value) {
				dojo.cookie("dontShowDialogAtLoad", "true");
			} else {
				dojo.cookie("dontShowDialogAtLoad", null, {expires: -1});
			}
		});
    },
    
	_position: function(){
		// summary:
		//		Position modal dialog in the viewport. If no relative offset
		//		in the viewport has been determined (by dragging, for instance),
		//		center the node. Otherwise, use the Dialog's stored relative offset,
		//		and position the node to top: left: values based on the viewport.
		// tags:
		//		private
		if (!dojo.hasClass(dojo.body(),"dojoMove")){
			var node = this.domNode,
				viewport = dojo.window.getBox(),
				p = this._relativePosition,
				bb = p ? null : dojo._getBorderBox(node),
				// If absolute position are already available, don't calculate.
				l = this.leftOffset ? this.leftOffset : Math.floor(viewport.l + (p ? p.x : (viewport.w - bb.w) / 2)),
				t = this.topOffset ? this.topOffset : Math.floor(viewport.t + (p ? p.y : (viewport.h - bb.h) / 2))
			;
			
			dojo.style(node,{
				left: l + "px",
				top: t + "px"
			});
		}
	}
});