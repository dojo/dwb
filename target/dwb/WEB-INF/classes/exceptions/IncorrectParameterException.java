package org.dtk.resources.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;

/**
 * Custom exception used to indicate that the client has sent an invalid request. 
 * This will occur when mandatory request values are missing, invalid or in the 
 * wrong format. 
 * 
 * @author James Thomas
 */

public class IncorrectParameterException extends WebApplicationException {
	
	public IncorrectParameterException(String message) {
		// Wrap message within JSON error object
		super(Response.status(HttpStatus.SC_BAD_REQUEST).entity("{\"error\":\""+message+"\"}").build());
    }
}
