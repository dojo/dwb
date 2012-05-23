package org.dtk.analysis.script;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.dtk.resources.Dependencies;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

/**
 * Simple error reporter to log all script parsing errors in the 
 * global dependencies log. 
 * 
 * @author James Thomas
 */

public class ScriptParserErrorReporter implements ErrorReporter {

	/** Log format for error messages */
	String logMessageFormat = "Error encountered parsing source, '%1$s', at " +
		"offset %2$s of line %3$s (full line source: ' %4$s'). Full error message trapped: ' %5$s'";
	
	/** Access to the dependencies logger */
	protected static Logger logger = Logger.getLogger(Dependencies.class.getName());
	
	/** Whenever we encounter a parsing error, log the details to the dependecies log and carry on. */
	@Override
	public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
		logError(message, sourceName, line, lineSource, lineOffset);
	}

	@Override
	public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
		logError(message, sourceName, line, lineSource, lineOffset);
		return new EvaluatorException(message);
	}

	@Override
	public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
		logError(message, sourceName, line, lineSource, lineOffset);
	}
	
	/** Log the parsing error to the dependencies log. */
	protected void logError(String message, String sourceName, int line, String lineSource, int lineOffset) {
		String logMessage = String.format(logMessageFormat, sourceName, lineOffset, line, lineSource, message);
		logger.log(Level.WARNING, logMessage);
	}
}
