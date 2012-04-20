package org.dtk.analysis.script;

import java.util.List;

public class AMDScriptParser extends BaseScriptParser  implements ScriptDependencyParser {

	public AMDScriptParser(String scriptSoure) {
		super(scriptSoure);
	}

	@Override
	public List<String> getModuleDependencies() {
		return null;
	}

}
