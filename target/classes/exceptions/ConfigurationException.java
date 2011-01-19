package org.dtk.resources.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;

/**
 * Custom exception used to indicate that part of the 
 * application's configuration has been incorrectly 
 * set up. Fatal error that causes halt of current API
 * request and gives brief details in the response.
 * 
 * @author James Thomas
 */

public class ConfigurationException extends WebApplicationException {
	
	public ConfigurationException(String message) {
		// Wrap message within JSON error object
		super(Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity("{\"error\":\""+message+"\"}").build());
    }
}
