dojo.provide("dwb.ui.FeedbackDialog");

dojo.require("dwb.ui.PositionableDialog");
dojo.require("dijit.form.TextBox");
dojo.require("dijit.form.Button");
dojo.require("dijit.form.Select");
dojo.require("dijit.form.SimpleTextarea");

dojo.declare("dwb.ui.FeedbackDialog", dwb.ui.PositionableDialog, {
	title: "Leave Feedback",
    style: "width: 464px;",
    content: dojo.cache("dwb.ui.fragments", "FeedbackDialogContent.html"),

    onLoad: function () {
        this.inherited(arguments);
        dojo.query("form", this.domNode).forEach(dojo.hitch(this, function(node) {
            var form = dijit.byNode(node);
            this.connect(form, "onSubmit", function (e) {
                dojo.stopEvent(e);
                console.log(form.get("value"));
                // Do the AJAX call here....
                this.hide();
            });
        }));
    }
});
