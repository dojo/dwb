dojo.provide("dwb.service.Package");

// Data controller exposing RESTful packages API. 
// Used to load and access meta-data for the packages 
// system. 

dojo.declare("dwb.service.Package", null, {
	serviceEndPoint: dwb.util.Config.get("packagesApi"),

    // Load package meta-data for this endpoint 
    load: function () {
		var d = dojo.xhrGet({
			url: this.serviceEndPoint,
			handleAs: "json"
		});

		d.then(dojo.hitch(this, "_processPackages"), this._loadingError);
    }, 

    // List of packages available returned, find versions 
    // information for each one package.
    _processPackages: function (response) {
        var packages = response.packages, pkge = null;

        // Run through entire package list, breaking when we find
        // the "dojo" package.
        while((pkge = packages.pop()) && pkge.name !== "dojo");

        // Unable to find base Dojo package, something has gone 
        // wrong in the backend service. 
        if (typeof pkge == "undefined") {
            this._loadingError();
            return;
        }

        // Send off request for all package versions, we'll select the 
        // newest version
        var versionReq = {url: pkge.link, handleAs: "json"};

        // Retrieve version information for the dojo package
		dojo.xhrGet(versionReq).then(dojo.hitch(this, "_processPackageVersions"), this._loadingError);

        // Publish default package name and all build options
        dojo.publish("dwb/package/default", [{"package": pkge.name}]);
        dojo.publish("dwb/build/options", [response]);
    },
    
    // Package version meta-data received, find latest version
    // and fire off request for package details.
    _processPackageVersions: function (versions) {
         var newest = versions.sort(function(a,b) {
            return (a.name > b.name) ? 1 : (a.name < b.name) ? -1 : 0;
        }).pop();

        // We should always have module version information.
        if (!newest) {
            this._loadingError();
            return;
        }

        dojo.publish("dwb/package/default", [{version: newest.name}]);

       var d = dojo.xhrGet({
			url: newest.link,
			handleAs: "json"
		});

		d.then(dojo.hitch(this, "_processPackageModules"), dojo.hitch(this, "_loadingError"));
    },

    _processPackageModules : function (response) {
        dojo.publish("dwb/package/modules", [response.modules]);
    },

    // When there's an error accessing or processing service response, 
    // generate error message. Error handling component will take the 
    // appropriate action.
    _loadingError: function () {
        dojo.publish("dwb/error/loading_packages");
    }
});
