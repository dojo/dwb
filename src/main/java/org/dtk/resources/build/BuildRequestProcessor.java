package org.dtk.resources.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.dtk.resources.Build;
import org.dtk.resources.build.manager.BuildState;
import org.dtk.resources.build.manager.BuildStatusManager;
import org.dtk.util.FileUtil;

/**
 * Asynchronous build thread used to convert build details into 
 * compiled JS layers. Build result cache is checked for pre-existing
 * build, based upon parameters requested, and compilation started if 
 * resource is missing. When the build has finished, required artifacts
 * are added to the compressed archive. 
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
			finishState = executeBuildProcess();
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
	 * Set off the build process for the current request. Build system will be run 
	 * against the generated profile and resulting artifacts compiled into build
	 * archive, ready for access by the user. 
	 * 
	 * @return Status of the build after completion 
	 */
	protected BuildState executeBuildProcess() {		
		BuildState finishedState = BuildState.FAILED;				
		try {			
			ProfileBuilder profileBuilder = setupProfileBuilder();
			
			// Execute the build scripts for this request and, if successful, create the archive file
			// with relevant build artifacts
			if (profileBuilder.executeBuild()) { 
				createBuildArchive();
				
				// Build completed successfully, set state accordingly.
				finishedState = BuildState.COMPLETED;
				logger.log(Level.INFO, String.format(finishedBuildLogMsg, buildRequest.getBuildReference(), buildRequest.getBuildResultPath()));
			} else {
				Exception buildError = profileBuilder.getBuildError();
				logger.log(Level.SEVERE, String.format(fatalBuildErrorLogMsg, buildError.getMessage()));
				buildStatusManager.addNewBuildLog(buildRequest.getBuildReference(), buildError.getMessage());
			}			
		} catch (Exception e) {
			logger.log(Level.SEVERE, String.format(fatalBuildErrorLogMsg, e.getMessage()));				
		}
		
		return finishedState;
	}
	
	/**
	 * Instantiate new instance of profile builder for this request. Ensure profile file 
	 * exists for this request and properties are passed in. 
	 * 
	 * @return Profile builder ready to build this request.
	 * @throws JsonParseException - Unable to generate profile file
	 * @throws JsonMappingException - Unable to generate profile file
	 * @throws IOException - Unable to persist profile file text
	 */
	protected ProfileBuilder setupProfileBuilder() throws JsonParseException, JsonMappingException, IOException {
		// Create file path to profile text  
		String profileFile = getPermanentBuildProfile(),
			amdLoaderPath = buildStatusManager.getLoaderModulePath(),
			buildPackageLocation = buildStatusManager.getBuildModulePath();
		
		File dojoLocation = new File(buildRequest.getDojoLocation(), "dojo");
		
		ProfileBuilder profileBuilder = new ProfileBuilder(profileFile, buildRequest.getBuildResultDir(), 
			amdLoaderPath, dojoLocation.getAbsolutePath(), buildPackageLocation, buildRequest.getBuildReference());
		
		return profileBuilder;
	}
	
	/**
	 * Generate the build profile for this build request and persist the 
	 * text to a file in the build directory for this unique build. 
	 * 
	 * @return Path to persistent build profile 
	 * @throws JsonParseException - Unable to generate JSON profile
	 * @throws JsonMappingException - Unable to convert Java POJOs to JSON 
	 * @throws IOException - Unable to write build profile to disk
	 */
	protected String getPermanentBuildProfile() throws JsonParseException, JsonMappingException, IOException {
		String profileFilename = "build.profile.js";
		String buildResultDir = buildRequest.getBuildResultDir();
		File profileFile = new File(buildResultDir, profileFilename);
		
		if (!profileFile.exists()) {
			FileUtil.writeToFile(profileFile.getAbsolutePath(), buildRequest.getProfileText(), null, false);
		}
	    
	    return profileFile.getAbsolutePath();
	}
	
	/**
	 * Create a new build archive from the artifacts generated 
	 * during the build process. The resulting archive will be
	 * written to disk in the cache directory. 
	 * 
	 * @throws IOException - Unable to read build artifacts or write 
	 * final archive file. 
	 */
	protected void createBuildArchive() throws IOException {
		String buildArchivePath = buildRequest.getBuildResultPath();
				
		Map<String, String> archiveContents = new HashMap<String, String>();
		Iterator<File> artifactFilesIter = extractBuildArtifactFiles().iterator();
		
		while(artifactFilesIter.hasNext()) {
			File artifactFile = artifactFilesIter.next();			
			String layerContent = FileUtil.readFromFile(artifactFile.getAbsolutePath(), null);
			// Path must be relative to base directory.....
			archiveContents.put(artifactArchivePath(artifactFile), layerContent);			
		}
		
		FileUtil.writeToZipFile(buildArchivePath, archiveContents);
	}
	
	/**
	 * Convert artifact file path to relative archive path.
	 * 
	 * @param artifactFile - Artifact file 
	 * @return Relative path in archive for this artifact
	 */
	protected String artifactArchivePath(File artifactFile) {
		// Default archive name is just last path entry
		String artifactArchivePath = artifactFile.getName();
		
		// Non JS files are theme files, index those from the start of
		// the themes directory. 
		if (!artifactArchivePath.endsWith(".js")) {
			String absPath = artifactFile.getAbsolutePath();
			artifactArchivePath = absPath.substring(absPath.lastIndexOf("themes"));
		}
		
		return artifactArchivePath; 
	}
	
	/**
	 * Return the list of all built artifacts we want to compile into the
	 * build archive. This will contain all the layer files and, if 
	 * selected, theme files needed. 
	 * 
	 * @return List of files to archive
	 */
	protected Collection<File> extractBuildArtifactFiles() {		
		Collection<File> artifactFiles = buildRequest.getBuildArtifactFiles(), 
			themeFiles = new ArrayList<File>();
		
		// If we find a CSS artifact to be included in the build result, user
		// has requested a theme and we need to pull in all related theme artifacts.
		Iterator<File> iter = artifactFiles.iterator();
		
		while(iter.hasNext()) {
			String artifactPath = (iter.next()).getAbsolutePath();
			if (artifactPath.endsWith("css")) {
				themeFiles = findAllThemeArtifacts(artifactPath);
				break;
			}
		}

		artifactFiles.addAll(themeFiles);		
		return artifactFiles;
	}
	
	/**
	 * Find all theme artifacts related to the a desired theme.
	 * Currently this consists all a series of image files referenced
	 * by the compacted theme's CSS file.  
	 * 
	 * @param path - Theme CSS file 
	 * @return File paths that are associated with this theme
	 */
	protected Collection<File> findAllThemeArtifacts(String path) {		
		// Search for all image artifacts under the parent theme
		// directory
		File themeParentFile = (new File(path)).getParentFile();		
		String[] themeArtifactExtensions = new String[]{ "png", "gif" };
		
		return FileUtils.listFiles(themeParentFile, themeArtifactExtensions, true);
	}
}
