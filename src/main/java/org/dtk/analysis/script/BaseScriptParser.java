package org.dtk.analysis.script;

public abstract class BaseScriptParser {
	protected String scriptSource;
	
	public BaseScriptParser(String scriptSource) {
		this.scriptSource = scriptSource;
	}
}
