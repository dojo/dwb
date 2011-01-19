dependencies = {
	layers: [
        {
			// Custom modules 
			name: "dwb.js",
			dependencies: [
				"dwb.Main"
			]
		}
	],

	prefixes: [
		[ "dijit", "../dijit" ],
		[ "dojox", "../dojox" ],
		[ "dwb", "@dwb@/js/dwb" ]
	]
}
