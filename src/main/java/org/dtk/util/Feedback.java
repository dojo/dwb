package org.dtk.util;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.dtk.resources.exceptions.IncorrectParameterException;

/***
 * Tiny utility class to log all feedback submissions from the front-end
 * application. Handles AJAX submissions of JSON fields. Possible fields 
 * include "name", "email address", "category" & "description". We simply
 * turn all submissions into log lines. 
 * 
 * @author James Thomas
 */

@Path("/feedback")
public class Feedback {
	
	// Expected feedback form parameters. Only mandatory parameter 
	// is "details", without that feedback is useless. 
	protected static final String EMAIL_ADDRESS = "email";
	protected static final String CONTACT_NAME = "name";
	protected static final String FEEDBACK_CATEGORY = "category";
	protected static final String FEEDBACK_DETAILS = "details";
	
	// Error message when the user has submitted feedback without 
	// the mandatory details parameter.
	protected static final String MISSING_FEEDBACK_DETAILS = "Unable to process " +
		"feedback submission, missing mandatory parameter 'details'.";
	
	// Log message generated for each feedback submission
	protected static final String FEEDBACK_MSG_FORMAT = "Feedback submission, category " +
		"%1$s, submitted by %2$s (%3$s): %4$s";
	
	// All feedback is sent straight to logging instance.
	private static Logger logger = Logger.getLogger(Feedback.class.getName());
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void submitFeedback(HashMap<String, Object> feedbackSubmission) {
		// Check for mandatory feedback parameter, throwing 400 if this parameter
		// is missing.
		if (feedbackSubmission.containsKey(FEEDBACK_DETAILS)) {
			// Log all valid feedback submissions.
			logger.log(Level.INFO, constructLogMessage(feedbackSubmission));
		} else {
			throw new IncorrectParameterException(MISSING_FEEDBACK_DETAILS); 
		} 
	}
	
	/**
	 * Turn feedback submission into a human-readable log string containing all
	 * the parameter values.  
	 * 
	 * @param feedbackSubmission - Feedback parameters submitted
	 * @return Human readable log message string
	 */
	protected String constructLogMessage(HashMap<String, Object> feedbackSubmission) {
		// Access all parameters, optional parameters default to "unknown" if missing
		String details = getFeedbackParam(feedbackSubmission, FEEDBACK_DETAILS), 
			contactName = getFeedbackParam(feedbackSubmission, CONTACT_NAME), 
			emailAddress = getFeedbackParam(feedbackSubmission, EMAIL_ADDRESS),
			feedbackCategory = getFeedbackParam(feedbackSubmission, FEEDBACK_CATEGORY);
		
		return String.format(FEEDBACK_MSG_FORMAT, feedbackCategory, contactName, emailAddress, details);
	}
	
	/**
	 * Extract feedback parameter from user submission, default to "unknown"
	 * when no key is found. 
	 * 
	 * @param feedbackSubmission - JSON details with feedback parameters
	 * @param parameterKey - Feedback parameter we want
	 * @return Feedback parameter value
	 */
	protected String getFeedbackParam(HashMap<String, Object> feedbackSubmission, String parameterKey) {
		String parameterValue = "Unknown";
		
		if (feedbackSubmission.containsKey(parameterKey)) {
			parameterValue = (String) feedbackSubmission.get(parameterKey);
		}
		
		return parameterValue;
	}
}
