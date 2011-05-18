package org.dtk.resources.build;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.dtk.resources.Build;
import org.dtk.resources.Dependencies;
import org.dtk.resources.build.manager.BuildState;
import org.dtk.resources.build.manager.BuildStatusManager;
import org.dtk.resources.packages.PackageRepository;
import org.dtk.util.FileUtil;
import org.mozilla.javascript.ContextFactory;

/**
 * Asynchronous build thread used to convert build details into 
 * compiled JS layers. Build result cache is checked for pre-existing
 * build, based upon parameters requested, and compilation started if 
 * resource is missing.
 *  
 * @author James Thomas
 */

public class BuildRequestProcessor implements Runnable {
	/** Build request details to process during compilation */
	BuildRequest buildRequest;
	
	/** Handle to build status manager */
	BuildStatusManager buildStatusManager;

	/** Logging class for build errors, use global builder log rather than individual
	 *  log for this class */
	protected static Logger logger = Logger.getLogger(Build.class.getName());
	
	/** Log message when the build process throws an exception **/
	protected static final String fatalBuildErrorLogMsg = "Fatal error returned by build process, root exception: %1$s";
	
	/** Log message when build has successfully completed **/
	protected static final String finishedBuildLogMsg = "Successfully processed build request (%1$s), caching result at %2$s";
	
    public BuildRequestProcessor(BuildRequest buildRequest) {
    	this.buildRequest = buildRequest;
    	this.buildStatusManager = BuildStatusManager.getInstance();
    }
	
    /**
     * Process build request details, running compilation stage if a cached 
     * version of the request is not already available. Build state is set
     * accordingly based upon success of build request.
     */
	@Override
	public void run() {
		BuildState finishState = BuildState.FAILED;

		String buildResultPath = buildRequest.getBuildResultPath();
		
		// Check if a version of the toolkit has already been built with the same
		// parameters. If so, we can just use this cached version rather than rebuilding. 
		File resultFile = new File(buildResultPath);
		if (!resultFile.exists()) {
			finishState = generateRequestedBuild();
		} else {
			// Cached version exists, no need to build just update status.
			finishState = BuildState.COMPLETED;
		}
		
		// Set result path in the build status instance when build is available.
		if (finishState == BuildState.COMPLETED) {
			buildStatusManager.setBuildResultPath(buildRequest.getBuildReference(), buildResultPath);
		}
		
		// Update state......
		buildStatusManager.changeBuildState(buildRequest.getBuildReference(), finishState);
	}

	/**
	 * 
	 * 
	 * @return State of the build after completion 
	 */
	protected BuildState generateRequestedBuild() {
		
		BuildState finishedState = BuildState.FAILED;
		
		//String parentPath = ; 
		
		// Should ask build status manager for AMD module loader 
		String moduleLoader = "/Users/james/Code/DTK/dojotoolkit/dojo/dojo.js";
				
		try {
			// Write profile file to disk for use by the build system 
			String profileFile = createPermanentBuildProfile();
			
			ProfileBuilder profileBuilder = new ProfileBuilder(profileFile, buildRequest.getBuildResultDir(), moduleLoader);
			if (profileBuilder.executeBuild()) {
				// Add files to zip archive 
				createBuildArchive();
				finishedState = BuildState.COMPLETED;	
			} else {
				
			}
			
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return finishedState;
	}
	
	public String createPermanentBuildProfile() throws JsonParseException, JsonMappingException, IOException {
		String profileFilename = "build.profile.js";
		String buildResultDir = buildRequest.getBuildResultDir();
		File profileFile = new File(buildResultDir, profileFilename);
		
	    FileUtil.writeToFile(profileFile.getAbsolutePath(), buildRequest.getProfileText(), null, false);
	    
	    return profileFile.getAbsolutePath();
	}
	
	public void createBuildArchive() throws IOException {
		String buildArchivePath = buildRequest.getBuildResultPath();
		
		List<File> layerFiles = buildRequest.getGeneratedLayerFiles();
		Iterator<File> layerFilesIter = layerFiles.iterator();
		
		Map<String, String> archiveContents = new HashMap<String, String>();
		
		while(layerFilesIter.hasNext()) {
			File layerFile = layerFilesIter.next();			
			String layerContent = FileUtil.readFromFile(layerFile.getAbsolutePath(), null);
			archiveContents.put(layerFile.getName(), layerContent);			
		}
		
		FileUtil.writeToZipFile(buildArchivePath, archiveContents);
	}
	
	/**
	 * Start build process for current build request, writing 
	 * results to path parameter.  
	 * 
	 * @param resultPath - Build result path to write to.
	 * @return State of build process completion
	 
	protected BuildState requestCompilation(String resultPath) {
		String scriptPath = buildStatusManager.getBuildScriptPath(),
			buildScriptsDir = buildStatusManager.getBuildScriptsDir();
		
		// Create new Rhino-based JS compilation script
		BuilderContextAction contextAction = new BuilderContextAction(scriptPath, buildScriptsDir, buildRequest);
		
		BuildState finishState = BuildState.FAILED;
		String reference = buildRequest.getBuildReference();
		try { 
			// Execute the compilation script with build request
	        ContextFactory.getGlobal().call(contextAction);
	        Exception exception = contextAction.getException();
	
	        // Check script executed without any errors
	        if (exception != null) {
	        	buildStatusManager.addNewBuildLog(reference, exception.getMessage());
	        	logger.log(Level.SEVERE, String.format(fatalBuildErrorLogMsg, exception.getMessage()));
	        } else {
	        	// Retrieve the result of JS build function and write to file path given.
	            Map<String, String> builtLayers = contextAction.getResult();
	            FileUtil.writeToZipFile(resultPath, builtLayers);
	            finishState = BuildState.COMPLETED;
	            logger.log(Level.INFO, String.format(finishedBuildLogMsg, reference, resultPath));
	        }
	    // Log any script failures
		} catch (Exception e) {
			buildStatusManager.addNewBuildLog(reference, e.getMessage());
			logger.log(Level.SEVERE, String.format(fatalBuildErrorLogMsg, e.getMessage()));
        }
		
		return finishState;
	}*/
}
