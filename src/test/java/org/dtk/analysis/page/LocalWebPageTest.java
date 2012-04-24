package org.dtk.analysis.page;

import static org.junit.Assert.*;

import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.Test;

public class LocalWebPageTest {
	@Test
	public void detectDojoScriptFromLocalFilePath() {
		LocalWebPage localWebPage = new LocalWebPage("");
		
		Attributes attrs = new Attributes();
		attrs.put("src", "dojo.js");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		assertTrue(localWebPage.isDojoScript(dojoScript));
	}
	
	@Test
	public void detectDojoScriptFromFullFilePath() {
		LocalWebPage localWebPage = new LocalWebPage("");
		
		Attributes attrs = new Attributes();
		attrs.put("src", "/some/folders/go/here/dojo.js");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		assertTrue(localWebPage.isDojoScript(dojoScript));
	}
	
	@Test
	public void detectDojoScriptWhenPathHasQueryParameters() {
		LocalWebPage localWebPage = new LocalWebPage("");
		
		Attributes attrs = new Attributes();
		attrs.put("src", "dojo.js?cachebust=true");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		assertTrue(localWebPage.isDojoScript(dojoScript));
	}
	
	@Test
	public void detectDojoScriptWithExtraPathChars() {
		LocalWebPage localWebPage = new LocalWebPage("");
		
		Attributes attrs = new Attributes();
		attrs.put("src", "dojo.js.uncompressed.js");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);		
		assertTrue(localWebPage.isDojoScript(dojoScript));
	}
	
	@Test
	public void dontDetectNonDojoScript() {
		LocalWebPage localWebPage = new LocalWebPage("");
		
		Attributes attrs = new Attributes();
		attrs.put("src", "another_script.js");		
		
		Element dojoScript = new Element(Tag.valueOf("script"), "", attrs);
		
		assertFalse(localWebPage.isDojoScript(dojoScript));
	}
}
