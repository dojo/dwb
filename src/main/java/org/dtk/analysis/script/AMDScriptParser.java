package org.dtk.analysis.script;

import java.util.List;

import org.mozilla.javascript.Node;

public class AMDScriptParser extends BaseScriptParser  implements ScriptDependencyParser {

	public AMDScriptParser(String scriptSoure) {
		super(scriptSoure);
	}

	@Override
	public List<String> getModuleDependencies() {
		return null;
	}

	@Override
	protected void parseNode(Node node) {
		// TODO Auto-generated method stub
		
	}

}
