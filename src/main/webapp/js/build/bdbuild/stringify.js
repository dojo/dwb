///
// \amd-mid uild/stringify
//
define(["dojo"], function(dojo) {
var
	spaces= "					 ",
	indentFactor= 2,

	setIndentFactor= function(factor) {
		indentFactor= factor;
	},

	indent= function(n, factor) {
		n= n * (factor || indentFactor);
		while (spaces.length<n) spaces+= spaces;
		return spaces.substring(0, n);
	},

	propName= function(name) {
		return /^[\w\$]+$/.test(name) ?
			name + ":" :
			"'" + name + "':";
	},

	text,
	unsolved,

	split= function(
			text //(string) Text to split into lines.
		) {
			///
			// Split text into a vector lines as given by new-line indicators contained in text
			///
			// Three-step algorithm:
			//	 * turn CR-LF or LF-CR into simple LF
			//	 * turn lone CR into LF
			//	 * then split on LF
			//
			// This should be platform-independent
			return text.replace(/(\r\n)|(\n\r)/g, "\n").replace(/\r/, "\n").split("\n");
		},

	stringify= function(it, level) {
		if (!level) {
			text= "";
			unsolved= false;
			level= 1;
		} else {
			level++;
		}
		var temp, space, p, i;
		switch (typeof it) {
			case "undefined":
				text+= "undefined";
				break;

			case "boolean":
				text+= (it ? "true" : "false");
				break;

			case "number":
				text+= it.toString();
				break;

			case "string":
				text+= dojo.toJson(it);
				break;

			case "object":
				if (it===null) {
					text+= "null";
				} else if (it instanceof RegExp) {
					text+= RegExp.toString();
				} else if (it instanceof Array) {
					if (it.length>1) {
						text+= "[\n";
						for (i= 0; i<it.length-1; i++) {
							text+= indent(level);
							stringify(it[i], level);
							text+= ",\n";
						}
						text+= indent(level);
						stringify(it[i], level);
						text+= "\n" + indent(level-1) + "]";
					} else if (it.length) {
						text+= "[";
						stringify(it[0], level);
						text+= "]";
					} else {
						text+= "[]";
					}
				} else {
					temp= [];
					for (p in it) temp.push(p);
					temp.sort();
					if (temp.length>1) {
						text+= "{\n";
						for (i= 0; i<temp.length-1; i++) {
							text+= indent(level) + propName(temp[i]);
							stringify(it[temp[i]], level);
							text+= ",\n";
						}
						text+= indent(level) + propName(temp[i]);
						stringify(it[temp[i]], level);
						text+= "\n";
						text+= indent(level-1) + "}";
					} else if (temp.length) {
						text+= "{" + propName(temp[0]);
						stringify(it[temp[0]], level);
						text+= "}";
					} else {
						text+= "{}";
					}
				}
				break;

			case "function":
				space= indent(level);
				// the V8 engine seems to strip the leading space from the first line of the function?!?
				var
					functionText= split(it.toString()),
					firstLine= functionText.shift(),
					minSpaces= Number.MAX_VALUE;
				functionText.forEach(function(line) {
					// ignoring lines that have no non-space chars
					var match= line.match(/(\s*)\S/);
					if (match) minSpaces= Math.min(minSpaces, match[1].length);
				});
				if (minSpaces==Number.MAX_VALUE) {
					//every line started without any spaces and we never got a match
					minSpaces= 0;
				}
				functionText.unshift(indent(minSpaces, 1) + firstLine);
				//every line has at least minSpaces indentation...
				text+= "\n" + functionText.map(function(line) { return space + line.substring(minSpaces); }).join("\n");
				break;

			default:
				text+= "undefined /* unsolved */";
				unsolved= true;
		}
		text.unsolved= unsolved;
		return text;
	};

stringify.setIndentFactor= setIndentFactor;
stringify.split= split;
return stringify;

});
