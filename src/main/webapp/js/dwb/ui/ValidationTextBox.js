dojo.provide("dwb.ui.ValidationTextBox");

dojo.require("dijit.form.ValidationTextBox");

/* Just override the template to include a character '!', rather than using an image */
dojo.declare("dwb.ui.ValidationTextBox", dijit.form.ValidationTextBox, {
	templateString: dojo.cache("dwb.ui", "templates/ValidationTextBox.html")
});
