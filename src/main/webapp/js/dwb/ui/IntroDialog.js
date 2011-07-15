dojo.provide("dwb.ui.IntroDialog");

dojo.require("dwb.ui.PositionableDialog");
dojo.require("dojo.cookie");
dojo.require("dijit.form.CheckBox");

dojo.declare("dwb.ui.IntroDialog", dwb.ui.PositionableDialog, {
	title: "Dojo Web Builder Introduction",
    style: "width: 464px;",
    content: dojo.cache("dwb.ui.fragments", "HelpDialogContent.html"),
	
    // Use cookie to store show dialog check state.
    constructor: function () {
    	dojo.subscribe('dwb/dialog/shownAtLoad', function (message) {
			if (!message.value) {
				dojo.cookie("dontShowDialogAtLoad", "true", {expires: 1000});
			} else {
				dojo.cookie("dontShowDialogAtLoad", null, {expires: -1});
			}
		});
    }
});
