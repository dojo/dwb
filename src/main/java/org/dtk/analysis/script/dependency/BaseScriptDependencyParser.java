package org.dtk.analysis.script.dependency;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.dtk.analysis.script.BaseScriptParser;
import org.dtk.resources.Dependencies;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;

/**
 * Abstract class which contains basic implementation of the ScriptDependencyParser 
 * interface for analysing module dependencies from a JavaScript source file.
 * 
 * Each AST node is checked to see if it is a function call and is the module-specific
 * dependency call. Module dependencies are extracted and stored internally, for access
 * through the ScriptDependencyParser API.
 * 
 * @author James Thomas
 */
public abstract class BaseScriptDependencyParser extends BaseScriptParser implements ScriptDependencyParser {

	/**
	 * Unique list of module dependencies discovered during analysis. 
	 */
	List<String> moduleDependencies;

	/**
	 * Class logger for this instance.
	 */
	protected static Logger logger = Logger.getLogger(BaseScriptDependencyParser.class.getName());
	
	/**
	 * Default constructor, store script source reference.
	 * 
	 * @param scriptSource - JavaScript source text
	 */
	public BaseScriptDependencyParser(String scriptSource) {
		super(scriptSource);
	}

	/**
	 * Abstract method implementation, executed each time a new AST node
	 * is discovered during parsing. If node is a function call, check 
	 * if it's calling the module dependency function. Any module dependencies
	 * specified are extracted are stored internally. 
	 * 
	 * @param node - AST node 
	 */
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
	
	/**
	 * Retrieve module dependencies discovered during parsing. 
	 * If parsing results aren't available, trigger parsing 
	 * before returning results.
	 * 
	 * @return Discovered module dependencies
	 */
	@Override
	public List<String> getModuleDependencies() {
		if (moduleDependencies == null) {
			initAndParseModuleDependencies();
		}
		return moduleDependencies;
	}
	
	/**
	 * Initialise and start dependency parsing from 
	 * available source text. Any errors during parsing 
	 * will be handled silently. 
	 */
	protected void initAndParseModuleDependencies() {
		moduleDependencies = new ArrayList<String>();
		try {
			parse();
		} catch (EvaluatorException e) {
			logger.warning("Unable to parse script source: " + scriptSource);
		}
	}
	
	/**
	 * Does the function call node correspond to a module
	 * dependency method? Implementation specific based upon
	 * supported module format.
	 * 
	 * @param node - Function call node
	 * @return Function call is module dependency method
	 */
	protected abstract boolean isModuleDependencyCall(Node node);
	
	/**
	 * Return list of module dependencies from function 
	 * call.
	 * 
	 * @param node - Module dependency call
	 * @return List of module identifiers passed to dependency function
	 */
	protected abstract List<String> retrieveDependencyArguments(Node node);
}
