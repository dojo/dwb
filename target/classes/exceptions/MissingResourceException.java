package org.dtk.resources.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;

/**
 * Custom exception used to indicate that the resource requested
 * was not found. The resource identifier passed by the client does
 * not correspond to any resources available.
 * 
 * @author James Thomas
 */

public class MissingResourceException extends WebApplicationException {
	
	public MissingResourceException(String message) {
		// Wrap message within JSON error object
		super(Response.status(HttpStatus.SC_NOT_FOUND).entity("{\"error\":\""+message+"\"}").build());
    }
}
