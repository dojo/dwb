dojo.provide("dwb.util.Config");

// Simple class to manage application configuration
dwb.util.Config = {
	// Sync load of config file 
	_configJson: dojo.cache("dwb.config", "config.json"),
	_config: null,	
	
	get: function (name) {
		// Convert json string to JS object, first time.
		if (this._config === null) {
			this._config = dojo.fromJson(this._configJson);
		}		
		
		return this._config[name];
	}
};