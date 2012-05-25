package org.dtk.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

/**
 * Unit tests for RSS feed generation from sample log files. 
 * 
 * @author James Thomas
 */

public class FeedbackRssFeedTest {	
	private static final DateFormat LOG_DATE_PARSER = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss aa");
	
	@Test 
	public void mustReturnDefaultEmptyFeedWithoutAnyLogs() {
		SyndFeed feed = new FeedbackRssFeed();
		
		assertEquals(FeedbackRssFeed.FEED_FORMAT, feed.getFeedType());		
        assertEquals(FeedbackRssFeed.FEED_TITLE, feed.getTitle());
        assertEquals(FeedbackRssFeed.FEED_LINK, feed.getLink());
        assertEquals(FeedbackRssFeed.FEED_DESCRIPTION, feed.getDescription());
        
        assertEquals(Collections.EMPTY_LIST, feed.getEntries());
	}
	
	@Test
	public void mustAddNewLogLinesToTheExistingFeed() throws IOException, ParseException {
		FeedbackRssFeed feed = new FeedbackRssFeed();
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample_logs/feedback/single_entry_single_line_log.txt");
		String formattedLogEntry = IOUtils.toString(is);
		
		feed.addLogEntry(formattedLogEntry);
		
		List<SyndEntry> entries = feed.getEntries();		
		assertEquals(1, entries.size());
		
		SyndEntry entry = entries.get(0);
		
		verifyLogEntry(entry, "Jan 01, 2012 1:23:45 PM", 
				"Feedback submission, category comment, submitted by James Thomas (jthomas.uk@gmail.com)", 
				"Testing Feedback Submission");		
	}
	
	@Test
	public void mustAddNewMultilineLogLinesToTheExistingFeed() throws IOException, ParseException {
		FeedbackRssFeed feed = new FeedbackRssFeed();
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample_logs/feedback/single_entry_multi_line_log.txt");
		String formattedLogEntry = IOUtils.toString(is);
		
		feed.addLogEntry(formattedLogEntry);
		
		List<SyndEntry> entries = feed.getEntries();		
		assertEquals(1, entries.size());
		
		SyndEntry entry = entries.get(0);
		
		verifyLogEntry(entry, "Jan 01, 2012 1:23:45 PM", 
			"Feedback submission, category comment, submitted by James Thomas (jthomas.uk@gmail.com)", 
			"Testing Feedback Submission\nTesting Feedback Submission\nTesting Feedback Submission");		
	}
	
	@Test
	public void mustAddMultipleNewLogLinesToTheExistingFeed() throws IOException, ParseException {
		FeedbackRssFeed feed = new FeedbackRssFeed();
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample_logs/feedback/single_entry_single_line_log.txt");
		String formattedLogEntry = IOUtils.toString(is);
		
		int entries = 10;
		
		for(int i = 0; i < entries; i++) {
			feed.addLogEntry(formattedLogEntry);	
		}
		
		List<SyndEntry> logEntries = feed.getEntries();		
		assertEquals(entries, logEntries.size());
		
		for(SyndEntry entry: logEntries) {
			verifyLogEntry(entry, "Jan 01, 2012 1:23:45 PM", 
					"Feedback submission, category comment, submitted by James Thomas (jthomas.uk@gmail.com)", 
					"Testing Feedback Submission");	
		}				
	}
	
	@Test 
	public void canInitialiseFeedWithExistingLogEntries() throws IOException, ParseException {
		InputStream is = getClass().getClassLoader().getResourceAsStream("sample_logs/feedback/single_entry_single_line_log.txt");
		String simpleLogEntry = IOUtils.toString(is);
		is = getClass().getClassLoader().getResourceAsStream("sample_logs/feedback/single_entry_multi_line_log.txt");
		String multiLogEntry = IOUtils.toString(is);
		
		List<String> logEntryLines = Arrays.asList(simpleLogEntry, multiLogEntry, simpleLogEntry);
		
		FeedbackRssFeed feed = new FeedbackRssFeed(logEntryLines);
		
		assertEquals(FeedbackRssFeed.FEED_FORMAT, feed.getFeedType());		
        assertEquals(FeedbackRssFeed.FEED_TITLE, feed.getTitle());
        assertEquals(FeedbackRssFeed.FEED_LINK, feed.getLink());
        assertEquals(FeedbackRssFeed.FEED_DESCRIPTION, feed.getDescription());
		
		List<SyndEntry> logEntries = feed.getEntries();		
		assertEquals(3, logEntries.size());
		
		verifyLogEntry(logEntries.get(0), "Jan 01, 2012 1:23:45 PM", 
			"Feedback submission, category comment, submitted by James Thomas (jthomas.uk@gmail.com)", 
			"Testing Feedback Submission");	
		verifyLogEntry(logEntries.get(1), "Jan 01, 2012 1:23:45 PM", 
				"Feedback submission, category comment, submitted by James Thomas (jthomas.uk@gmail.com)", 
				"Testing Feedback Submission\nTesting Feedback Submission\nTesting Feedback Submission");
		verifyLogEntry(logEntries.get(2), "Jan 01, 2012 1:23:45 PM", 
				"Feedback submission, category comment, submitted by James Thomas (jthomas.uk@gmail.com)", 
				"Testing Feedback Submission");
	}
	
	protected void verifyLogEntry(SyndEntry entry, String dateTime, String title, String description) throws ParseException {
		assertEquals(LOG_DATE_PARSER.parse(dateTime), entry.getPublishedDate());
		assertEquals(title, entry.getTitle());
		SyndContent content = entry.getDescription();
		
		assertEquals("text/plain", content.getType());
		assertEquals(description, content.getValue());
	}
}
