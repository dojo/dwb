package org.dtk.resources.build.manager;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class is used to represent the state of a build process
 * and hold all associated build logs. Access to the build state
 * to controlled with ReadWrite lock. Stores final result path
 * when build has finished.
 * 
 * @author James Thomas
 */

public class BuildStatus {
	/** Lock used to control access to the build state for this instance */
	ReadWriteLock stateLock; 
	
	/** Internal state used to represent state of associated build process */
	BuildState buildState;
	
	/** Used buffer containing build logs, StringBuffer is a thread-safe class. */
	StringBuffer buildLogs;
	
	/** Path to result of the build process, set when build is available */
	String buildResultPath; 

	/**
	 * Create new build status, process hasn't been started. 
	 */
	public BuildStatus() {
		this.stateLock = new ReentrantReadWriteLock(); 
		this.buildState = BuildState.NOT_STARTED;
		this.buildLogs = new StringBuffer();
	}
	
	/**
	 * Retrieve read lock for build state
	 * @return Read lock
	 */
	public Lock getStateReadLock() {
		return stateLock.readLock();
	}
	
	/**
	 * Retrieve write lock for build state
	 * @return Write lock
	 */
	public Lock getStateWriteLock() {
		return stateLock.writeLock();
	}
	
	/**
	 * Set the build state 
	 * @param buildState - New build state 
	 */
	public void setBuildState(BuildState buildState) {
		this.buildState = buildState;
	}
	
	/**
	 * Retrieve current build state 
	 * @return Build state
	 */
	public BuildState getBuildState() {
		return this.buildState;
	}
	
	/**
	 * Add a new build log line.
	 * @param buildLog - Log line
	 */
	public void addBuildLog(String buildLog) {
		buildLogs.append(buildLog);
	}
	
	/**
	 * Clear all build logs
	 */
	public void clearBuildLogs() {
		buildLogs.setLength(0);
	}
	
	/**
	 * Return build logs as a single line.
	 * @return
	 */
	public String getAllBuildLogs() {
		return buildLogs.toString();
	}
	
	/**
	 * Get the path for the build result
	 * 
	 * @return Build result path
	 */
	public String getBuildResultPath() {
		return buildResultPath;
	}

	/**
	 * Set the path for the build result
	 * 
	 * @param buildResultPath - Build result path
	 */
	public void setBuildResultPath(String buildResultPath) {
		this.buildResultPath = buildResultPath;
	}
}
