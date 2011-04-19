dojo.provide("dwb.service.Build");

dojo.require("dwb.util.Config");

// Data controller exposing build server accessed over 
// a RESTful interface.
// Used to submit new build jobs and get notification 
// of completion. 

dojo.declare("dwb.service.Build", null, {
    service: dwb.util.Config.get("buildApi"),
    
    // Deferred object representing current in-flight 
    // build result polling XHR request. 
    _inflight: null,

    // Schedule a new build request using the profile parameter
    // containing module layers and build options. XHR requests
    // creates a new service request and polls for response.
    schedule: function (request) {
        this.onBuildStarted();

        // POST new build request, response will contain status link.
        this._inflight = dojo.xhrPost({
            url: this.service,
            handleAs: "json",
            headers: {"Content-Type":"application/json"},
            postData: dojo.toJson(request)
        });
			
        // Service response contains link to poll for status...
		this._inflight.then(dojo.hitch(this, function (response) {
            this._inflight = dojo.xhrGet({
                url: response.buildStatusLink,
                handleAs: "json"
            });
                
           this._inflight.then(dojo.hitch(this, "_pollBuildStatus", response.buildStatusLink), this.onBuildFailed);
        }), this.onBuildFailed);
    }, 

    // Cancel the current build request
    cancel: function () {
        if (this._inflight !== null) {
            // No more in-flight build requests.
            this._inflight.cancel();
            this._inflight = null;
        }
        this.onBuildCancelled();
    },

    // Iteratively poll the build service until the build 
    // request has completed. Publish the uploaded status logs
    // to all listeners.
    _pollBuildStatus: function (statusUrl, response) {
        this.onBuildStatusUpdate(response.logs.split("\n"));

		// If the build has completed, publish location of the resulting build.
		if (response.state === "COMPLETED") {
			this.onBuildFinished(response.result);
        // Otherwise, keep polling for log changes.
		} else if (response.state === "BUILDING" || response.state === "NOT_STARTED") {
			setTimeout(dojo.hitch(this, function () {
                // Check user hasn't tried to cancel build 
                // during the time we were asleep....
                if (this._inflight) {
				    this._inflight = dojo.xhrGet({
                        url: statusUrl,
                        handleAs: "json"
                    });

                    this._inflight.then(dojo.hitch(this, "_pollBuildStatus", statusUrl), this.onBuildFailed);	
                }
			}), 500);
		// An error occurred, indicate this.
		} else {
			this.onBuildFailed();
		} 
    },

    // Event handlers to signal build state change
    // using custom events and published events
    onBuildStatusUpdate: function (logs) {
        dojo.publish("dwb/build/status", [logs]);
    }, 

    onBuildFinished: function (link) {
        dojo.publish("dwb/build/finished", [link]);
    },

    onBuildCancelled: function () {
        dojo.publish("dwb/build/cancelled");
    },

    onBuildStarted: function () {
        dojo.publish("dwb/build/started");
    },

    onBuildFailed: function () {
        this._inflight = null;
        dojo.publish("dwb/build/failed");
    }
});
