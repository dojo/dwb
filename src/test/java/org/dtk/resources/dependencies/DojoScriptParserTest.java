package org.dtk.resources.dependencies;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

public class DojoScriptParserTest {

	// Dump this into an enum......
	protected static final Map<String, String> dojoVersionScriptLocations;
    static {
    	dojoVersionScriptLocations = new HashMap<String, String>();
    	dojoVersionScriptLocations.put("http://ajax.googleapis.com/ajax/libs/dojo/1.6.0/dojo/dojo.xd.js", "1.6.0");
    }
	
	@Test
	public void test_RetrieveKnownDojoVersions() {
		Iterator<Entry<String, String>> iter = dojoVersionScriptLocations.entrySet().iterator();
		
		while(iter.hasNext()) {
			Entry<String, String> knownDojoScript = iter.next();
			DojoScriptParser parser = new DojoScriptParser(knownDojoScript.getKey());
			assertEquals(knownDojoScript.getValue(), parser.getDojoVersion());
		}
	}

}
