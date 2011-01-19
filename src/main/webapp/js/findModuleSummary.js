load("../jslib/fileUtil.js");

function findSummary(fileContents) {
  // Find first line match "summary:", match groups consisting of 
  // any text after the summary prefix and any additional lines with
  // two tabs. This will match the following summary formats:
  // summary: some summary text (single line)
  // & 
  // summary:
  // 		some summary text (multiple lines)
  var summaryRegex = new RegExp("//(\\s)*summary:(.*\n)((.*//\t\t.*\n)*)");
  var summary = null;

  matches = fileContents.match(summaryRegex);

//  return matches;
  if (matches != null) {
    if (matches[2] && matches[2].replace(/(\s|\n)/g, "") !== "") {
      // Remove newline from single line summary
      summary = matches[2].replace(/(\n|^ )/g,"");
    } else {
      // Remove tabs and double-backslashes, replace newlines with a space.
      summary = matches[3].replace(/(\t|\/\/)/g,"").replace(/\n/g, " ");
    }
  } else {
    summary = "Could not find summary for this module";
  }

  return summary.replace(/\"/g, '\'');
}


function testing() {
  var files = [
    "../../../dojo/parser.js"
  ];


  for (var i = 0; i < files.length; i++) {
    var fileContents = new fileUtil.readFile(files[i]);
    results = findSummary(fileContents);
    print(results);
  }
}

//testing();
