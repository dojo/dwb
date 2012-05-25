package org.dtk.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dtk.resources.exceptions.IncorrectParameterException;

import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/***
 * Tiny utility class to log all feedback submissions from the front-end
 * application. Handles AJAX submissions of JSON fields. Possible fields 
 * include "name", "email address", "category" & "description". We simply
 * turn all submissions into log lines. 
 * 
 * Also produces an RSS 2.0 feed for all existing log entries, updated as new entries 
 * are received.
 * 
 * @author James Thomas
 */

@Path("/feedback")
public class Feedback {
	
	/** 
	 * RSS 2.0 feed for the Feedback submissions
	 */
	protected FeedbackRssFeed logEntriesRssFeed;
	
	/** 
	 * Simple log formatter, used to construct log entries. 
	 */
	protected static final Formatter logFormatter = new SimpleFormatter();		
	
	/**
	 * Expected feedback form parameters. Only mandatory parameter
	 * is "details", without that feedback is useless. 
	 */	  
	protected static final String EMAIL_ADDRESS = "email";
	protected static final String CONTACT_NAME = "name";
	protected static final String FEEDBACK_CATEGORY = "category";
	protected static final String FEEDBACK_DETAILS = "details";
	
	/**
	 * Regular expression used to differentiate log entries in the 
	 * existing feedback logs. 
	 */
	protected static final Pattern NEW_LOG_ENTRY_PATTERN 
		= Pattern.compile("(.*) org.dtk.util.Feedback submitFeedback$");
	
	/**
	 * Error message when the user has submitted feedback without
	 * the mandatory details parameter. 
	 */
	protected static final String MISSING_FEEDBACK_DETAILS = "Unable to process " +
		"feedback submission, missing mandatory parameter 'details'.";
	
	/**
	 * Log message generated for each feedback submission
	 */
	protected static final String FEEDBACK_MSG_FORMAT = "Feedback submission, category " +
		"%1$s, submitted by %2$s (%3$s): %4$s";
	
	/**
	 * All feedback is sent straight to this logging instance.
	 */
	private static Logger logger = Logger.getLogger(Feedback.class.getName());
	
	/**
	 * On instantiation, read all existing feedback entries and used to
	 * construct the RSS feed for feedback submissions. 
	 */
	public Feedback() {
		initialiseLogFeed();
	}
	
	/**
	 * API call to allow feedback submission, must contain 
	 * mandatory feedback parameter, details. Valid submissions are sent
	 * through to the logging service and added to the RSS feed generated.
	 * 
	 * @param feedbackSubmission - JSON object containing feedback details
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public void submitFeedback(HashMap<String, Object> feedbackSubmission) {
		if (feedbackSubmission.containsKey(FEEDBACK_DETAILS)) {			
			addFeedbackFeedEntry(feedbackSubmission);
			logger.log(Level.INFO, constructLogMessage(feedbackSubmission));
		} else {
			throw new IncorrectParameterException(MISSING_FEEDBACK_DETAILS); 
		} 
	}	
	
	/**
	 * Return feedback submissions formatted into a valid 
	 * RSS 2.0 feed. All submission entries are included as 
	 * individual feed items. 
	 * 
	 * @return RSS 2.0 feed for feedback entries
	 * @throws FeedException - Unable to create output feed
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	public String getFeedbackFeed() throws FeedException {		
        SyndFeedOutput output = new SyndFeedOutput();
        return output.outputString(logEntriesRssFeed);        
	}
	
	/**
	 * Read existing feedback log file, if it exists, turning 
	 * into a series of log entries submitted. Use these to instantiate 
	 * the RSS feed to be served up. 
	 */
	protected void initialiseLogFeed() {
		String existingFeedbackLogs = getExistingFeedbackLogs();		
		List<String> logEntries = parseLogFileIntoEntries(existingFeedbackLogs);		
		logEntriesRssFeed = new FeedbackRssFeed(logEntries);
	}
		
	/**
	 * Add new feedback entry into the currently RSS feed. Format as an existing 
	 * log line and then add directly into the feed stream.  
	 * 
	 * @param feedbackSubmission - JSON object containing feed details.
	 */
	protected void addFeedbackFeedEntry(HashMap<String, Object> feedbackSubmission) {		
		String formattedLogEntry = logFormatter.format(new LogRecord(Level.INFO, constructLogMessage(feedbackSubmission)));
		logEntriesRssFeed.addLogEntry(formattedLogEntry);		
	}
	
	/**
	 * Return contents of the feedback log file used to store submission entries. 
	 * If there is a problem reading the file, return an empty string. 
	 * 
	 * @return Log file contents
	 */
	protected String getExistingFeedbackLogs() {
		String existingFeedbackLog = "";
		String logFile = String.format(ContextListener.LOG_FILE_FORMAT, Feedback.class.getName());
		
		try {		
			File file = new File(logFile);
			if (file.exists()) {
				existingFeedbackLog = FileUtils.readFileToString(file);					
			} else {
				logger.warning("Unable to read existing feedback log file, " + logFile);
			}			
		} catch (IOException e) {
			logger.warning("Unable to read existing feedback log file, " + logFile);
		}
		
		return existingFeedbackLog;		
	}
	
	/**
	 * Parse through existing log file contents, pulling out 
	 * individual log entries as separate strings. Returns a list
	 * of those entries as single strings. 
	 * 
	 * @param logFileContents - Existing log file contents
	 * @return List of log entry strings
	 */
	protected List<String> parseLogFileIntoEntries(String logFileContents) {
		List<String> logEntries = new ArrayList<String>();		
		String[] logFileLines = logFileContents.split("\n");
		StringBuffer logEntryBuffer = new StringBuffer();
		
		for(String line: logFileLines) {			
			Matcher matcher =  NEW_LOG_ENTRY_PATTERN.matcher(line);
			
			if (matcher.find() ) {
				storeAndClearBuffer(logEntryBuffer, logEntries);									
			}
			
			logEntryBuffer.append(line + "\n");			
		}		
	
		storeAndClearBuffer(logEntryBuffer, logEntries);
		
		return logEntries;
	}
	
	/**
	 * Add StringBuffer contents as a new List<String> entry, ignoring 
	 * if the contents are empty, following by reseting the buffer to 
	 * empty afterwards. 
	 * 
	 * @param logEntryBuffer - String buffer  
	 * @param logEntries - List of strings
	 */
	protected void storeAndClearBuffer(StringBuffer logEntryBuffer, List<String> logEntries) {
		if (logEntryBuffer.length() > 0) {
			logEntries.add(logEntryBuffer.toString());
			logEntryBuffer.delete(0, logEntryBuffer.length());
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