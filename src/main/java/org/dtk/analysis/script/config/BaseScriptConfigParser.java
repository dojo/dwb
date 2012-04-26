package org.dtk.analysis.script.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.dtk.analysis.script.BaseScriptParser;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

/**
 * Abstract class which contains basic implementation of the ScriptDependencyParser 
 * interface for analysing loader configuration values defined in a JavaScript source file.
 * 
 * Each AST node is checked to see if it is a object literal declaration, with the variable name
 * corresponding to a valid configuration label. All defined values are extracted and stored internally,
 * for access through the ScriptConfigParser API.
 * 
 * @author James Thomas
 */
public abstract class BaseScriptConfigParser extends BaseScriptParser implements ScriptConfigParser {

	/**
	 * Parsed script configuration, updated by values encountered during 
	 * configuration parsing.  
	 */
	Map<String, Object> scriptConfiguration;

	/**
	 * Logging instance for this class
	 */
	protected static Logger logger = Logger.getLogger(BaseScriptConfigParser.class.getName());
	
	/**
	 * Default constructor, store script source reference.
	 * 
	 * @param scriptSource - JavaScript source text
	 */
	public BaseScriptConfigParser(String scriptSource) {
		super(scriptSource);
	}

	/**
	 * Abstract method implementation, executed each time a new AST node
	 * is discovered during parsing. If node is a variable declaration for an object
	 * literal, check if it's defining a configuration variable name. List of valid 
	 * configuration value keys are extracted and the values update internal configuration.  
	 * 
	 * @param node - AST node 
	 */
	@Override
	protected void parseNode(Node node) {
		switch(node.getType()) {
		case Token.VAR: 
			if (isConfigurationDefinition(node)) {
				List<String> configurationKeys = retrieveConfigurationKeys(node);

				for(String configKey: configurationKeys) {
					scriptConfiguration.put(configKey, retrieveConfigurationValue(node, configKey));					
				}
			}								
		}
	}
	
	/**
	 * Retrieve composite loader configuration values discovered
	 * during parsing. If configuration values aren't available, trigger 
	 * parsing before returning results.
	 * 
	 * @return Discovered loader configuration values
	 */
	@Override
	public Map<String, Object> getScriptConfig() {
		if (scriptConfiguration == null) {
			initAndParseScriptConfiguration();
		}
		return scriptConfiguration;
	}
	
	/**
	 * Initialise and start configuration parsing from 
	 * available source text. Any errors during parsing 
	 * will be handled silently. 
	 */
	protected void initAndParseScriptConfiguration() {
		scriptConfiguration = new HashMap<String, Object>();
		try {
			parse();
		} catch (EvaluatorException e) {
			logger.warning("Unable to parse script source: " + scriptSource);
		}
	}
	
	/**
	 * Does the variable declaration correspond to a configuration 
	 * object literal? Implementation specific based upon
	 * supported loader configuration format.
	 * 
	 * @param node - AST declaration node
	 * @return AST node is a valid configuration declaration
	 */
	protected abstract boolean isConfigurationDefinition(Node node);
	
	/**
	 * Retrieve a list of valid configuration keys discovered
	 * in this configuration declaration. 
	 * 
	 * @param node - Configuration declaration node
	 * @return Supported configuration keys present
	 */
	protected abstract List<String> retrieveConfigurationKeys(Node node);
	
	/**
	 * Retrieve a configuration value for the given key in 
	 * the defined configuration. If value isn't present, 
	 * return a null. 
	 * 
	 * @param node - Configuration declaration node
	 * @param key - Configuration key
	 * @return Configuration value for key or null
	 */
	protected abstract List<String> retrieveConfigurationValue(Node node, String key);
}
