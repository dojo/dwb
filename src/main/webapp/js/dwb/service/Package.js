dojo.provide("dwb.service.Package");

// Data controller exposing RESTful packages API. 
// Used to load and access meta-data for the packages 
// system. 

dojo.declare("dwb.service.Package", null, {
	serviceEndPoint: dwb.util.Config.get("packagesApi"),

    // Start loading of the package meta-data for this endpoint 
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
        var packages = response.packages;

        // Service hasn't returned any package information, 
        // something has gone wrong! 
        if (!packages){
            this._loadingError();
            return;
        }

        // Load list of modules for the latest version of each package
        // available. Requires multiple XHR requests to load versions for 
        // each package and then modules for package version.
        var module_requests = dojo.map(packages, dojo.hitch(this, "_retrieveLatestPackageModules"));
        var package_modules = new dojo.DeferredList(module_requests);

        // Once latest module data for latest package versions has been
        // loaded, pull out deferred results and publish to any listeners
        package_modules.then(dojo.hitch(this, function (modules) {
            var resolved_details = dojo.map(modules, function(resolved) {
                return resolved[1];
            });

            this.packagesAndModulesAvailable(resolved_details);
        }), this._loadingError);

        // Publish full build options contained within package response
        dojo.publish("dwb/build/options", [response]);
    },
    
    // For a given package, resolve the versions available
    // and use the latest to find all available modules. 
    _retrieveLatestPackageModules: function (pkge) {
        var latestPackageModules = new dojo.Deferred();
        // Send off request for all package versions, we'll select the 
        // newest version
        var versions_request = {url: pkge.link, handleAs: "json"};

		dojo.xhrGet(versions_request).then(dojo.hitch(this, function (versions) {
            // We want modules available for the latest package version
            var latest = this._findLatestPackageVersion(versions);
            dojo.xhrGet({
                url: latest.link,
                handleAs: "json"
            }).then(function (data) {
                // Compose meta-package object from name, version
                // and module list. 
                var packageInfo = {
                    "name": pkge.name,
                    "version": latest.name,
                    "modules": data.modules
                };
                //... finally resolve deferred
                latestPackageModules.callback(packageInfo);
            }, this.loadingError);
        }), this._loadingError);

        return latestPackageModules;
    },

    // Given a list of package versions, use lexical comparison
    // to find the most recent version. 
    _findLatestPackageVersion: function(versions) {
         var newest = versions.sort(function(a,b) {
            return (a.name > b.name) ? 1 : (a.name < b.name) ? -1 : 0;
        }).pop();

        return newest;
    },

    // Custom event to signal that all package and module metadata 
    // has been loaded from the backend service. 
    packagesAndModulesAvailable: function (response) {
        dojo.publish("dwb/package/modules", [response]);
    },

    // When there's an error accessing or processing service response, 
    // generate error message. Error handling component will take the 
    // appropriate action.
    _loadingError: function () {
        dojo.publish("dwb/error/loading_packages");
    }
});
