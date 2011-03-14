dojo.provide("dwb.ui.FeedbackDialog");

dojo.require("dwb.ui.PositionableDialog");
dojo.require("dojo.string");
dojo.require("dijit.form.TextBox");
dojo.require("dijit.form.Button");
dojo.require("dijit.form.Select");
dojo.require("dijit.form.SimpleTextarea");

dojo.declare("dwb.ui.FeedbackDialog", dwb.ui.PositionableDialog, {
	title: "Leave Feedback",
    style: "width: 464px;",
    content: dojo.cache("dwb.ui.fragments", "FeedbackDialogContent.html"),

    feedbackForm: null,
    feedbackContent: null,
    submitBtn: null,

    onLoad: function () {
        this.inherited(arguments);

        // Once content has rendered, set up event handlers for form submission
        // and button enabling.
        dojo.query("form", this.domNode).forEach(dojo.hitch(this, function(node) {
            this.feedbackForm = dijit.byNode(node);
            this.connect(this.feedbackForm, "onSubmit", "_onSubmission");

            // Find submit button 
            dojo.query(".dijitButton", node).forEach(dojo.hitch(this, function (btn) {
                this.submitBtn = dijit.byNode(btn);
            }));

            // Submit button enabled when user has typed some actual content
            dojo.query("textarea", node).forEach(dojo.hitch(this, function(textarea) {
                this.feedbackContent = dijit.byNode(textarea);
                this.connect(this.feedbackContent, "onKeyUp", "_onFeedbackContentChanged");
            }));
        }));
    },

    _onSubmission: function (e) {
        dojo.stopEvent(e);
        // Fire and forget the feedback to utility api 
        // to log all the submissions. 
        
        var fields = this.feedbackForm.get("value");
        dojo.xhrPost({
            url:"/api/feedback", 
            handleAs:"json", 
            headers:{"Content-Type": "application/json"},
            postData:dojo.toJson(fields)
        });
        
        // Hide the dialog and overlay.
        this.hide();
    }, 

    _onFeedbackContentChanged : function () {
        // We want non-empty content string from textarea
        var content = dojo.string.trim(this.feedbackContent.get("value"));
        // Enable button when we have some content!
        this.submitBtn.set("disabled", (content.length > 0) ? false : true);
    }
});
