package org.dtk.analysis.script.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.dtk.analysis.page.LocalWebPageTest;
import org.dtk.analysis.script.exceptions.InvalidLiteralNode;
import org.dtk.analysis.script.node.ObjectLiteral;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

/**
 * Implementation of the ScriptConfigParser interface for discovering Dojo Toolkit
 * configuration parameters from JavaScript source text. Dojo configuration can be 
 * defined by setting either the "djConfig" or "dojoConfig" variable with an object
 * literal containing configuration parameters in a JavaScript file. 
 * 
 * This class parses through all the available AST nodes looking for a declarations 
 * that match those labels and converts any discovered object literal values into the 
 * internal Map lookup. 
 * 
 * @author James Thomas
 */

public class LoaderConfigParser extends BaseScriptConfigParser {

	/**
	 * Internal configuration map, updated from any configuration values 
	 * discovered during script parsing.
	 */
	protected final Map<Node, ObjectLiteral> configurationLiterals = new HashMap<Node, ObjectLiteral>();
	
	/**
	 * Static logging instance.
	 */
	protected static final Logger logger = Logger.getLogger(LoaderConfigParser.class.getName());
	
	/**
	 * Default constructor, starts script parsing.
	 * 
	 * @param scriptSource - JavaScript source to parse for configuration
	 */
	public LoaderConfigParser(String scriptSource) {
		super(scriptSource);
	}

	/**
	 * Does AST node contain a valid configuration declaration? 
	 * Must be a local or global object literal variable declaration
	 * matching valid configuration name.
	 * 
	 * @return Node is a configuration declaration
	 */
	@Override
	protected boolean isConfigurationDefinition(Node node) {		
		return isGlobalDefinition(node) || isLocalDefinition(node);
	}

	/**
	 * For a valid configuration node, retrieve all configuration 
	 * parameters labels defined. 
	 * 
	 * @param node - Configuration node
	 * @return Configuration keys defined, empty list if node isn't
	 * valid or configuration has no parameters.
	 */
	@Override
	protected List<String> retrieveConfigurationKeys(Node node) {		
		List<String> configurationKeys = new ArrayList<String>();

		ObjectLiteral objLiteral = configurationLiterals.get(node);
				
		if (objLiteral != null) {			
			configurationKeys = objLiteral.getKeys();
		}
		
		return configurationKeys;
	}
	
	/**
	 * Retrieve configuration value for a given node and key.
	 * Must have already been confirmed as a valid configuration node
	 * using "isConfigurationDefinition". 
	 * 
	 * @param node - AST node containing configuration declaration 
	 * @param key - Configuration key
	 * @return Configuration value or null if not found
	 */
	@Override
	protected Object retrieveConfigurationValue(Node node, String key) {
		Object configuationValue = null;
		
		ObjectLiteral objLiteral = configurationLiterals.get(node);
		
		if (objLiteral != null) {
			configuationValue = objLiteral.getValue(key);								
		}
			
		return configuationValue;
	}		
	
	/**
	 * Check whether a given AST node is a global configuration 
	 * definition, using non-var declaration.
	 * 
	 * @param node - AST node
	 * @return AST node is global configuration declaration
	 */
	protected boolean isGlobalDefinition(Node node) {
		Node globalNameReference = node.getFirstChild();
		
		if (globalNameReference != null && Token.SETNAME == globalNameReference.getType()) {
			String label = getStringNodeLabel(globalNameReference.getFirstChild());							
			if (matchesLoaderConfigName(label)) {
				addConfigurationLiteral(node, globalNameReference.getLastChild());
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Check whether a given AST node is a local configuration 
	 * definition, using var declaration.
	 * 
	 * @param node - AST node
	 * @return AST node is local configuration declaration
	 */
	protected boolean isLocalDefinition(Node node) {
		Node localNameReference = node.getFirstChild();
		
		if (localNameReference != null && Token.NAME == localNameReference.getType()) {
			String label = getStringNodeLabel(localNameReference);							
			if (matchesLoaderConfigName(label)) {
				addConfigurationLiteral(node, localNameReference.getLastChild());
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Store known configuration declaration locally, wrapping 
	 * within the ObjectLiteral helper class.
	 * 
	 * @param configuration - Configuration declaration node
	 * @param literal - Object literal node
	 */
	protected void addConfigurationLiteral(Node configuration, Node literal) {
		try {
			configurationLiterals.put(configuration, new ObjectLiteral(literal));
		} catch (InvalidLiteralNode e) {
			logger.info("Invalid configuration literal discovered: " + literal.getType());
		}
	}
	
	/**
	 * Does variable label match known Dojo configuration values.
	 * 
	 * @param label - Variable name 
	 * @return Label matches DTK configuration declaration
	 */
	protected boolean matchesLoaderConfigName(String label) {
		return DojoConfigAttrs.LOADER_CONFIG_DOJO_CONFIG.equals(label) || DojoConfigAttrs.LOADER_CONFIG_DJCONFIG.equals(label);
	}
}