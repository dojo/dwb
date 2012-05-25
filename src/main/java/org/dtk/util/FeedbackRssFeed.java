package org.dtk.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeedImpl;

/**
 * Extension of the SyndFeedImpl class to support generating an 
 * RSS 2.0 feed for feedback submissions. Provides the ability to construct
 * the feed based upon existing log entry strings and add new entries. 
 * 
 * Expects logs entries strings to be in the "SimpleLogFormatter" entry 
 * and messages should be formatted as specified in the Feedback.FEEDBACK_MSG_FORMAT
 * structure. 
 * 
 * Feed entries have the date-time set as the log creation time, title as the user details
 * and description as the message contents. 
 * 
 * @author James Thomas
 */

public class FeedbackRssFeed extends SyndFeedImpl {
	
	/**
	 * Default feed details, RSS 2.0 feed type. 
	 */
	protected static final String FEED_FORMAT = "rss_2.0";
	protected static final String FEED_TITLE = "Dojo Web Builder Feedback";
	protected static final String FEED_LINK = "http://build.dojotookit.org";
	protected static final String FEED_DESCRIPTION 
		= "This feed contains details of user-submitted feedback for the Dojo Web Builder tool";	
	protected static final String ENTRY_FORMAT = "text/plain";
	
	/**
	 * Datetime parser for log entries, used to set feed entry published times. 
	 */
	protected static final DateFormat DATE_PARSER = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss aa");
	
	/**
	 * Logging instance for this class.
	 */
	private static Logger logger = Logger.getLogger(FeedbackRssFeed.class.getName());
	
	/**
	 * Construct the default feedback feed instance, 
	 * will be an RSS 2.0 XML feed.
	 */
	public FeedbackRssFeed() {
		super();
		this.setFeedType(FEED_FORMAT);
		this.setTitle(FEED_TITLE);
		this.setLink(FEED_LINK);
		this.setDescription(FEED_DESCRIPTION);
	}
	
	/**
	 * Construct the default feedback RSS 2.0 feed instance,
	 * pre-populating entry list with existing log entries. 
	 * 
	 * @param formattedLogEntries - Existing formatted log entries.
	 */
	public FeedbackRssFeed(List<String> formattedLogEntries) {
		this();
		
		for(String formattedLogEntry: formattedLogEntries) {
			addLogEntry(formattedLogEntry);
		}
	}
	
 	/**
 	 * Add a new feed entry based upon a pre-formatted log entry line 
 	 * for a feedback submission. Log string will be parsed to extract publish
 	 * date, title and description before adding a new entry to the feed. 
 	 * 
 	 * Log formatted line expected to be in the following format: 
 	 * Jan 01, 1970 01:01:01 AM org.dtk.util.Feedback submitFeedback
	 * INFO: Feedback submission, category <xxx>, submitted by xxx (xxx): <message contents>
 	 * 
 	 * Messages formatted differently will be ignored.
 	 * 
 	 * @param formattedLogEntry - Formatted feedback log entry
 	 */
	public void addLogEntry(String formattedLogEntry) {		
		List<String> logEntryLines = new ArrayList<String>(Arrays.asList(formattedLogEntry.split("\n")));
		List<SyndEntry> entries = getEntries();
		
		if (logEntryLines.size() < 2) {
			logger.warning("Unable to parse existing log entry, invalid format: " + formattedLogEntry);
			return;
		}
		
		String entryDateWithLoggerName = logEntryLines.remove(0),
			entryLevelWithCustomPrefix = logEntryLines.remove(0);
		
		SyndEntry entry = new SyndEntryImpl();
		SyndContent	description = new SyndContentImpl();
		
		try {
			entry.setPublishedDate(parseLogEntryDate(entryDateWithLoggerName));
			entry.setTitle(parseLogEntryTitle(entryLevelWithCustomPrefix));		
			
			logEntryLines.add(0, parseLogMessage(entryLevelWithCustomPrefix));
			
	        description.setType(ENTRY_FORMAT);
	        description.setValue(StringUtils.join(logEntryLines, '\n'));
	        
	        entry.setDescription(description);
	        
	        entries.add(entry);	        
		} catch (ParseException e) {
			logger.warning("Unable to parse existing log entry, invalid date format: " + formattedLogEntry);
		}
	}
	
	/**
	 * Parse date-time entry from log line. 
	 * 
	 * @param logEntryDateLine - Log entry line
	 * @return Date log was constructed
	 * @throws ParseException - Unable to parse date string format 
	 */
	protected Date parseLogEntryDate(String logEntryDateLine) throws ParseException {
		String[] entryParts = logEntryDateLine.split(Feedback.class.getName());		
		return DATE_PARSER.parse(entryParts[0].trim());
	}
	
	/**
	 * Return user details from submission log. 
	 * 
	 * @param logEntryMessageLine - Log entry
	 * @return User details
	 */
	protected String parseLogEntryTitle(String logEntryMessageLine) {
		return logEntryMessageLine.split(":")[1].trim();
	}
	
	/**
	 * Return user message from submission log. 
	 * 
	 * @param logEntryMessageLine - Log entry
	 * @return User message
	 */
	protected String parseLogMessage(String logEntryMessageLine) {
		return logEntryMessageLine.split(":")[2].trim();
	}
}