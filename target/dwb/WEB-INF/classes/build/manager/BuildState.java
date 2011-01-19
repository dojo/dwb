package org.dtk.resources.build.manager;

/**
 * Represents possible states of a build process.
 * 
 * @author James Thomas
 */

public enum BuildState {
	// Build hasn't started
	NOT_STARTED,
	// Build has begun
	BUILDING,
	// Build finished successfully
	COMPLETED,
	// Build failed!
	FAILED,
}
