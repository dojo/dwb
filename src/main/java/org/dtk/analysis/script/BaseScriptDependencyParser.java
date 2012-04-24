package org.dtk.analysis.script;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.dtk.resources.Dependencies;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

public abstract class BaseScriptDependencyParser extends BaseScriptParser implements ScriptDependencyParser {

	List<String> moduleDependencies;

	protected static Logger logger = Logger.getLogger(BaseScriptDependencyParser.class.getName());
	
	public BaseScriptDependencyParser(String scriptSource) {
		super(scriptSource);
	}

	@Override
	protected void parseNode(Node node) {
		switch(node.getType()) {
		case Token.CALL: 
			if (isModuleDependencyCall(node)) {
				List<String> dependencies = retrieveDependencyArguments(node);

				for(String dependency: dependencies) {
					if (!moduleDependencies.contains(dependency)) {
						moduleDependencies.add(dependency);
					}
				}
			}								
		}
	}
	
	@Override
	public List<String> getModuleDependencies() {
		if (moduleDependencies == null) {
			initAndParseModuleDependencies();
		}
		return moduleDependencies;
	}
	
	protected void initAndParseModuleDependencies() {
		moduleDependencies = new ArrayList<String>();
		try {
			parse();
		} catch (EvaluatorException e) {
			logger.warning("Unable to parse script source: " + scriptSource);
		}
	}
	
	abstract boolean isModuleDependencyCall(Node node);
	
	abstract List<String> retrieveDependencyArguments(Node node);
}
