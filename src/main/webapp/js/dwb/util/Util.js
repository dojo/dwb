dojo.provide("dwb.util.Util");

// Utility class is just a collection of helper methods. 
// Create class as singleton, don't need an instance per usage.
dwb.util.Util = {
	// Count of created module tabs, used when generating random
	// tab titles.
	_moduleTabCount : 0,	
		
	_moduleTabTitleFormat: "custom_layer${index}.js",
	
	// Matching query template for text search
	_wordClauseTemplate: "${field}:*${word}*",

	_moduleFilterPrefix: "not baseModule:",

	_queryConjunction: " AND ",
	
	// Generate random identifiers for layers. Acts at unique 
	// key in data store. Layers cannot have the same name.
	generateModuleTabTitle : function () {
		var params = {
			"index": ""
		};
		
		// First title doesn't need integer count, 
		// the rest should have _X format. 
		if (dwb.util.Util._moduleTabCount > 0) {			
			params.index = "_" + dwb.util.Util._moduleTabCount;
		}

		// Increment number of generated titles
		dwb.util.Util._moduleTabCount++;

		return dojo.string.substitute(dwb.util.Util._moduleTabTitleFormat, params);
	},
	
	// Given a list of modules, attempt to return a details instance 
	// for each. This contains the module name and description, with
	// a placeholder for missing descriptions.
	// We use asynchronous fetch operations and return a Deferred List to allow
	// callee to know when operations are complete.
	_populateModuleDetails : function (store, modules) {
		var dfl = dojo.map(modules, function (module) {
			var df = new dojo.Deferred();
			
			// Search for module based upon name.
			store.fetchItemByIdentity({
				identity:module,
				onItem:function (item) {
					// Generate module store item format from module and possible details.
					var moduleDetails = {
						"name": module,
						"desc": (!!item ? store.getValue(item, "desc") : "Unable to find a description for this module.")
					};			
					
					// Indicate completion of inner async. operation. 
					df.callback(moduleDetails);
				}
			});
			
			return df;
		});

		// Wait until all asynchronous fetch operations are complete before
		// proceeding to show modules found.
		return new dojo.DeferredList(dfl);
	},
	
	// Return module query for module grid from free text search value and 
	// configuration panels. Sentence is matched against name and description fields. 
	// Ability to also exclude all modules from certain prefixes.
	constructModuleFilterQuery : function (textSearchValue, currentSelection) {
		var queryClauses = [], freeTextClauses = [];

		// User must have entered some text to search...
		if (textSearchValue) {
			// Add clause for fields we want to search against
			dojo.forEach(["name", "desc"], function (id) {
				if (currentSelection[id]) {
					freeTextClauses.push(dwb.util.Util._constructWordMatchingClause(textSearchValue, id));	
				}
			});

			if (freeTextClauses.length > 0) {
				queryClauses.push("(" + freeTextClauses.join(" OR ") + ")");
			};	
		} 

		// Check for module packages to exclude
		dojo.forEach(["dojo", "dijit", "dojox"], function(moduleName) {
			if (currentSelection[moduleName]) {
				queryClauses.push(dwb.util.Util._moduleFilterPrefix + moduleName);
			}
		});

		// Filter out any empty clauses and create complete query from individual clauses.
		var query = queryClauses.join(dwb.util.Util._queryConjunction);

		return query;
	},

	// Parse a sentence into words and create a query clause for each words. These
	// words will be matched against an individual field. 
	// Return the conjunction of all the sub-clauses.
	_constructWordMatchingClause : function (/*String*/ sentence, /*String*/ field) {
		var words = dwb.util.Util.sanitise(sentence.split(" "));

		var sub = function (word) {
			return dojo.string.substitute(dwb.util.Util._wordClauseTemplate, {field:field, word:word});
		};

		return (dojo.map(words, sub).join(dwb.util.Util._queryConjunction));		
	},
	
	// Filter out any empty strings from array.
	sanitise : function (items) {
		return dojo.filter(items, function (item) {
			return item !== "";
		});
	}
};