package org.dtk.analysis.script.loader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Abstract class that implements a module path resolving for the Dojo Toolkit 
 * loader. Module identifiers are matched against the most specific first when 
 * resolving modules to file paths. Base document location is used against the 
 * module loader to generate the absolute URL references for files.
 * 
 * @author James Thomas
 */

abstract public class AbstractModulePathResolver implements ModulePathResolver {
	/**
	 * Base URL for the module loader.
	 */
	protected URL baseUrl; 
	
	/**
	 * Tree-structure which holds the matching module paths 
	 * to resolve. Walking the tree reveals any matching configuration
	 * paths for module identifiers.
	 */
	protected ModulePath root = new ModulePath();
	
	/**
	 * Regular expression used to convert module identifiers to named parts
	 */
	protected Pattern pathElementPattern;
	
	/**
	 * Class logger for this instance.
	 */
	protected static Logger logger = Logger.getLogger(AbstractModulePathResolver.class.getName());
		
	/**
	 * Default constructor, passing in loader location and configuration paths for 
	 * modules. 
	 * 
	 * @param baseUrl - Loader location directory
	 * @param modulePaths - Lookup for module paths to locations
	 */
	public AbstractModulePathResolver(URL baseUrl, Map<String, String> modulePaths) {
		this.baseUrl = baseUrl;
		setUpModulePathLookup(modulePaths);
	}
	
	/**
	 * Return local file path reference relative to the module loader. 
	 *  
	 * @param moduleIdentifier - Absolute module identifier
	 * @return Relative module path
	 */
	@Override
	public String getRelativePath(String moduleIdentifier) {
		return resolveModulePath(root, getPathElements(moduleIdentifier));
	}
	/**
	 * Return absolute file path for a resolved module identifier. 
	 *  
	 * @param moduleIdentifier - Absolute module identifier
	 * @return Absolute module location
	 */
	@Override
	public URL getAbsolutePath(String moduleIdentifier) {
		URL absolutePath = null;
		String relativePath = getRelativePath(moduleIdentifier);
		
		try {
			absolutePath = new URL(baseUrl, relativePath);
		} catch (MalformedURLException e) {
			logger.warning("Invalid relative encountered while constructing module path: " + relativePath);
		}
		
		return absolutePath;
	}
	
	
	/**
	 * Iterate through the configuration values for the specified: 
	 * 		module paths -> locations
	 * lookup and convert module path to a linked list of tree nodes, with each path
	 * component being an edge. 
	 * Nodes should contain the file location for the 
	 * walked series of paths nodes. 
	 * So the identifier "a/b/c" -> "../dir/" should become....
	 *      root 
	 *	    /
	 *     a
	 *    /
	 *   b
	 *  /
	 * c ( value = "../dir/")
	 * 
	 * @param modulePaths - Lookup for identifiers to locations
	 */
	protected void setUpModulePathLookup(Map<String, String> modulePaths) {
		Iterator<Entry<String, String>> pathIter = modulePaths.entrySet().iterator();
		ModulePath currentChild;
		
		while(pathIter.hasNext()) {
			Entry<String, String> pathEntry = pathIter.next();
			String pathMatcher = pathEntry.getKey(), pathValue = pathEntry.getValue();
			
			currentChild = root; 
			
			for(String pathElement: getPathElements(pathMatcher)) {				
				currentChild = createDescendentPath(currentChild, pathElement);
			}
			
			currentChild.setValue(pathValue);
		}
	}
	
	/**
	 * Create a module path identifier as a child node of the modulePath
	 * parent. If a node already exists, just return the current modulePath
	 * child.
	 * 
	 * @param modulePath - Parent path element
	 * @param path - Module path identifier
	 * @return Module path 
	 */
	protected ModulePath createDescendentPath(ModulePath modulePath, String path) {
		ModulePath descendentChild = modulePath.getDescedentPath(path);
		if (descendentChild == null) {
			descendentChild = modulePath.createDescendentPath(path);
		}
		return descendentChild;
	}
	
	/**
	 * Convert module path section into a series of path fragment, 
	 * splitting on module format separator. 
	 * 
	 * @param pathMatcher - Module path matcher section
	 * @return List of path fragments
	 */
	protected List<String> getPathElements(String pathMatcher) {
		if (pathElementPattern == null) {
			pathElementPattern = Pattern.compile(Character.toString(getModulePathSeparator()), Pattern.LITERAL);
		}
		
		return new ArrayList<String>(Arrays.asList(pathElementPattern.split(pathMatcher)));
	}
	
	/**
	 * Walk module paths tree searching for a matching series of path fragments. 
	 * Will return node value at deepest tree match discovered by recursively 
	 * traversing the child nodes. 
	 * 
	 * @param currentNode - Module path tree node
	 * @param pathElements - List of path fragments to search against
	 * @return Matching module path
	 */
	protected String resolveModulePath(ModulePath currentNode, List<String> pathElements) {
		// If we have no next matching path to search or check, just return the current node value....
		if (pathElements.isEmpty() || !hasNextPathMatch(currentNode, pathElements)) {
			return constructResolvedPath(currentNode.getValue(), pathElements);
		}
		
		// Otherwise, grab the list head and keep searching...
		String nextPathMatch = pathElements.remove(0);		
		return resolveModulePath(currentNode.getDescedentPath(nextPathMatch), pathElements);
	}
	
	/**
	 * Convert a series of remaining path fragments into a single path string
	 * along with a matching path location.
	 * 
	 * @param pathPrefix - Path location
	 * @param pathParts - Unmatched path fragments 
	 * @return Resolved path 
	 */
	protected String constructResolvedPath(final String pathPrefix, final List<String> pathParts) {	
		return String.format("%s/%s.js", pathPrefix, StringUtils.join(pathParts, "/"));		
	}	
	
	/**
	 * Check whether we have a descendant path to search.
	 * 
	 * @param modulePath - Module path node
	 * @param pathElements - Path fragments to match
	 * @return Node has path
	 */
	protected boolean hasNextPathMatch(ModulePath modulePath, List<String> pathElements) {
		return modulePath.hasDescendantPath(pathElements.get(0));
	}
	
	/**
	 * Implementation-specific module path fragment separator
	 * 
	 * @return Path separator
	 */
	abstract protected char getModulePathSeparator();
	
	/**
	 * Inline class to provide a tree-like structure, 
	 * representing a series of string fragments to match against
	 * module paths. Nodes contain the file location that matches the 
	 * descendent paths values.
	 */
    private class ModulePath {
        private String value = "..";
        private Map<String, ModulePath> paths = new HashMap<String, ModulePath>();        
        
        public String getValue() {
        	return value;
        }
        
        public void setValue(String value) {
        	this.value = value;
        }
        
        public ModulePath getDescedentPath(String pathIdentifier) {        	
        	return paths.get(pathIdentifier);
        }
        
        public ModulePath createDescendentPath(String pathIdentifier) {
        	ModulePath modulePath = new ModulePath();
        	paths.put(pathIdentifier, modulePath);        
        	return modulePath;
        }  
        
        public boolean hasDescendantPath(String pathIdentifier) {
        	return paths.containsKey(pathIdentifier);
        }
    }		
}