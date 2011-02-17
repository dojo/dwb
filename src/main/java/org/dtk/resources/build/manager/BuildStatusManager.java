package org.dtk.resources.build.manager;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

import org.dtk.resources.build.BuildProcess;
import org.dtk.resources.build.BuildRequest;
import org.dtk.resources.exceptions.MissingResourceException;

/**
 * The build status manager is responsible for creation of new build processes,
 * maintaining the state of all running builds and ensuring that only a single build
 * process for the same parameters is running at once. 
 * 
 * @author James Thomas
 */

public final class BuildStatusManager {
	/** Store build status look up table, keyed against unique build resource identifiers. 
	 * Use concurrent map to allow putIfAbsent method to create initial build status for a
	 * new resource without locking entire table. */
	protected final ConcurrentMap<String, BuildStatus> buildStateLookup 
		= new ConcurrentHashMap<String, BuildStatus>();
	
	/** Unable to find requested resource error message */
	protected static final String missingResourceErrorText 
		= "Unable to find build status for requested resource.";
	
	/** Single instance of build status manager */
	protected static final BuildStatusManager INSTANCE = new BuildStatusManager();

	/** File name for the main build script */
	protected final String buildScriptName = "build.js";
	
	/** Directory containing cached build results */
	protected String buildResultCachePath;
	
	/** Full path for main build script */
	protected String buildScriptPath;
	
	/** Directory containing local build scripts */
	protected String buildScriptsDir;
	
	/**
	 * Private constructor to enforce singleton pattern.
	 */
	private BuildStatusManager() {
		if (INSTANCE != null) {
			throw new IllegalStateException("Already instantiated");
		}
	}
	
	/**
	 * Return static instance of BuildStatusManger
	 * 
	 * @return Build status manager instance.
	 */
	public static BuildStatusManager getInstance() {
		return INSTANCE;
	}
	
	/**
	 * Request scheduling of a new build process for the given parameters. The internal
	 * build status cache checks whether this is the first time a build has been requested
	 * for this combination of parameters. If the build request is new or the previous build
	 * attempt failed, a new build process will be start in the background. If a previous build
	 * completed or is still in progress, no action will be taken.
	 * 
	 * @param buildRequest - Build request to schedule
	 */
	public void scheduleBuildRequest(BuildRequest buildRequest) {
		// Retrieve unique identifier for this build request, 
		// just a digest of the parameters
		String reference = buildRequest.getBuildReference();
		
		// Holds next build state for the current request
		
		
		// Does a build need scheduling for the current request?
		boolean scheduleBuild = false;
		
		// Use thread safe operation to create new build status instance
		// the first time this particular build is requested.
		buildStateLookup.putIfAbsent(reference, new BuildStatus());
		
		// Now access reference to current build state for the unique 
		// build identifier. This will either be pre-existing or the new
		// instance we created.
		BuildStatus buildStatus = buildStateLookup.get(reference);
		
		// Retrieve next logical build state and given the current state. 
		// Use read lock when checking actual build state. Multiple readers
		// accessing same state is allowed, we use write lock to enforce synchronisation
		// when modifying the build state.
		BuildState nextState = getNextBuildState(retrieveBuildState(buildStatus));
		
		// If the build status is ready to begin, i.e. build hasn't started or previously failed, 
		// then we can schedule that background build process for the current build request.
		if (nextState == BuildState.BUILDING) {
			// Modify the build state value to indicate build process has been started. Use write 
			// lock to block all readers and other threads don't try to start the same build process.
			// We must re-check build state after acquiring write lock in case another thread has 
			// already started the build process after we released our read lock.
			Lock wlock = buildStatus.getStateWriteLock();
			wlock.lock();
			try {
				// Re-check build status hasn't been updated in between 
				// lock releasing and re-acquiring. 
				nextState = getNextBuildState(buildStatus.getBuildState());
				if (nextState == BuildState.BUILDING) {
					// Set state to BUILDING and release blocking write lock
					// immediately. No other thread will try to start off the 
					// build process and we can do that after. Clear out any
					// previous build logs.
					buildStatus.setBuildState(nextState);	
					resetBuildLogs(reference);
					scheduleBuild = true;
				}
			} finally {
				wlock.unlock();
			}		
			
			if (scheduleBuild) {
				// Kick off asynchronous build thread, this process will
				// change the state to FINISHED or FAILED.
				scheduleNewBuild(buildRequest);
			}
		}
	}
	
	/**
	 * Schedule the asynchronous build process to generate compressed
	 * Dojo layers from parameters object. New thread is spawned to 
	 * run the build process in the background.   
	 * 
	 * @param buildRequest - Build request to schedule
	 */
	protected void scheduleNewBuild(BuildRequest buildRequest) {
		// Code will go here to kick off new build thread, running
		// in the background. When thread completes, it'll change
		// the state to FINISHED or FAILED.
		BuildProcess t = new BuildProcess(buildRequest);
		new Thread(t).start();
	}
	
	/**
	 * Find the next typical build state given the current state. 
	 * Usual path: NOT_STARTED -> BUILDING -> COMPLETED. If build 
	 * fails, state will be FAILED, which should revert to BUILDING
	 * when the build is initiated again. 
	 * 
	 * @param buildState - Current build state
	 * @return Next logical build state
	 */
	protected BuildState getNextBuildState(BuildState buildState) {
		BuildState nextState = BuildState.NOT_STARTED; 
		
		switch(buildState) {
		// Build hasn't started, ready to kick off a build
		case NOT_STARTED:
			nextState = BuildState.BUILDING;
			break;
		// Build is in progress
		case BUILDING:
			nextState = BuildState.COMPLETED;
			break;
		// Build has finished successfully
		case COMPLETED:
			nextState = BuildState.COMPLETED;
			break;
		// Build process failed, ready to restart
		case FAILED:
			nextState = BuildState.BUILDING;
			break;
		}
		
		return nextState;
	}
	
	/**
	 * Access current build status for a given build. Read lock
	 * used to enforce thread safety in case writers may be 
	 * currently changing build status.
	 * 
	 * @param reference - Unique identifier for build 
	 */
	public BuildState retrieveBuildState(String reference)
	throws MissingResourceException {
		// Use read lock to ensure synchronicity between readers/writers
		// when accessing the build state.
		BuildStatus buildStatus = retrieveBuildStatus(reference);
		return retrieveBuildState(buildStatus);
	}
	
	/**
	 * Retrieve build status for a status instance. Use read lock
	 * to enforce synchronisation when some has write lock already. 
	 * Multiple readers is allowed.
	 * 
	 * @param buildStatus - Build status instance
	 * @return Current build state at time of check.
	 */
	public BuildState retrieveBuildState(BuildStatus buildStatus) {
		BuildState currentState;
		// Acquire reader lock, change state and release. 
		Lock rlock = buildStatus.getStateReadLock();
		rlock.lock();
		try {
			currentState = buildStatus.getBuildState();
		} finally {
			rlock.unlock();
		}
		
		return currentState;
	}
	
	/**
	 * Modify the build state for a given build. Write lock
	 * used to enforce thread safety between readers/writers.
	 * This method is called by the background build process
	 * when the build has either failed or completed, setting state
	 * accordingly. 
	 * 
	 * @param reference - Unique identifier for build 
	 * @param newState - New build state
	 */
	public void changeBuildState(String reference, BuildState newState)
	throws MissingResourceException {
		// Use write lock to ensure synchronicity between readers/writers
		// when modifying the build status.
		BuildStatus buildStatus = retrieveBuildStatus(reference);
		
		// Acquire write lock, change state and release. 
		Lock wlock = buildStatus.getStateWriteLock();
		wlock.lock();
		try {
			buildStatus.setBuildState(newState);
		} finally {
			wlock.unlock();
		}
	}
	
	/**
	 * Retrieve complete build log for a given build.
	 * 
	 * @param reference - Unique build reference
	 * @return Complete build log for a given build
	 */
	public String getCompleteBuildLog(String reference) {
		BuildStatus buildStatus = retrieveBuildStatus(reference);
		return buildStatus.getAllBuildLogs();
	}
	
	/**
	 * Add a new build log line to build status.
	 * 
	 * @param reference - Unique build reference
	 * @param buildLog - Log line to add
	 */
	public void addNewBuildLog(String reference, String buildLog) {
		BuildStatus buildStatus = retrieveBuildStatus(reference);
		buildStatus.addBuildLog(buildLog);
	}
	
	/**
	 * Remove all build log content for a given 
	 * build process.
	 * 
	 * @param reference - Unique build reference
	 */
	public void resetBuildLogs(String reference) {
		BuildStatus buildStatus = retrieveBuildStatus(reference);
		buildStatus.clearBuildLogs();
	}
	
	/**
	 * Set the build result path for a build reference.
	 * 
	 * @param reference - Build reference identifier
	 * @param buildResultPath - File path for build result
	 */
	public void setBuildResultPath(String reference, String buildResultPath) {
		BuildStatus buildStatus = retrieveBuildStatus(reference);
		buildStatus.setBuildResultPath(buildResultPath);
	}
	
	/**
	 * Get the build result path for a build reference.
	 * 
	 * @param reference - Build reference identifier
	 * @param buildResultPath - File path for build result
	 */
	public String getBuildResultPath(String reference) {
		BuildStatus buildStatus = retrieveBuildStatus(reference);
		return buildStatus.getBuildResultPath();
	}
	
	/**
	 * 
	 * Set directory path containing cached build results.
	 * 
	 * @param buildResultCachePath - Directory path
	 */
	public void setBuildResultCachePath(String buildResultCachePath) {
		this.buildResultCachePath = buildResultCachePath;
	}
	
	/**
	 * Get directory path containing cached build results.
	 * 
	 * @return Directory path
	 */
	public String getBuildResultCachePath() {
		return this.buildResultCachePath;
	}
	
	/**
	 * Get the absolute file path for the build script.
	 * 
	 * @return Build script path
	 */
	public String getBuildScriptPath() {
		return this.buildScriptPath;
	}
	
	/**
	 * Get the full path for the directory containing
	 * the build scripts.
	 * 
	 * @return Build scripts directory path
	 */
	public String getBuildScriptsDir() {
		return this.buildScriptsDir;
	}
	
	/**
	 * 
	 * @param buildScriptsDir
	 */
	public void setBuildScriptsDir(String buildScriptsDir) {
		this.buildScriptsDir = buildScriptsDir;
		// Now derive the full path for the build script
		// from the absolute scripts path and build file name.
		File buildScript = (new File(this.buildScriptsDir, this.buildScriptName));
		this.buildScriptPath = buildScript.getAbsolutePath();
	}
	
	/**
	 * Access the build status for the unique reference. If the 
	 * reference doesn't exist, throw an exception that automatically
	 * propagates correct HTTP status code as response.
	 * 
	 * @param reference - Unique build reference
	 * @return Referenced build status instance
	 * @throws MissingResourceException - Could not find that reference
	 */
	protected BuildStatus retrieveBuildStatus(String reference) throws MissingResourceException {
		// Verify that this resource exists 
		if (!buildStateLookup.containsKey(reference)) {
			throw new MissingResourceException(missingResourceErrorText);
		}
		
		return buildStateLookup.get(reference);
	}
}
