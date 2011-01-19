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
		[ "dwb", "/root/dwb/build/js/dwb" ]
	]
}
