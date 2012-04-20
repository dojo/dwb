package org.dtk.analysis.script;

import java.util.List;

public class NonAMDScriptParser extends BaseScriptParser implements ScriptDependencyParser {

	public NonAMDScriptParser(String scriptSoure) {
		super(scriptSoure);
	}

	@Override
	public List<String> getModuleDependencies() {
		// TODO Auto-generated method stub
		return null;
	}

}
