package org.dtk.resources.dependencies;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class represents the known Dojo versions that are supported by the system. 
 * It also provides a lookup of Dojo versions using a digest of source files.
 * 
 * @author James Thomas
 */

public class DojoScriptVersions {
	/** 
	 * Known Dojo Script Versions, also provides
	 * values to represent unknown scripts, valid dojo scripts
	 * whose version we don't support and invalid dojo scripts.
	 */
	public enum Versions {
		ONE_SIX_ONE("1.6.1"),
		ONE_SIX_ZERO("1.6.0"),
		ONE_FIVE_ZERO("1.5.0"),
		ONE_FOUR_THREE("1.4.3"),
		UNKNOWN(""),
		VALID(""),
		INVALID("");
		
		private String Versionstr;

		Versions(String Versionstr) {
			this.Versionstr = Versionstr;
		}

		public String getValue() {
			return this.Versionstr;
		}
	};
	
	public static final Map<String, Versions> lookup;

	static {
		Map<String, Versions> map = new HashMap<String, Versions>();
		/** Dojo Toolkit 1.6.1 **/
		/** Local, Compressed */
		map.put("56042852b70bc32c64634c7af64f25d3", Versions.ONE_SIX_ONE);		

		/** Dojo Toolkit 1.6.0 **/   	
		/** Local, Source */
		map.put("3a4681ac7cc73ce89c7a29e3027f0195", Versions.ONE_SIX_ZERO);

		/** Local, Compressed - Shrinksafe */
		// TODO:

		/** Cross Domain, Source - Google */ 
		map.put("1995bd4489903f5a4b26c047fc4bb2b7", Versions.ONE_SIX_ZERO);

		/** Cross Domain, Compressed - Google */
		map.put("0e310578c14068dfe712d6bf74a7fc48", Versions.ONE_SIX_ZERO);


		/** Dojo Toolkit 1.5.0 **/
		/** Local, Source */
		map.put("96755520900bfff458dc44a55a21702e", Versions.ONE_FIVE_ZERO);

		/** Local, Compressed - Shrinksafe */
		// TODO:

		/** Cross Domain, Source */ 
		map.put("1fcd329ab67b02a4dec0016d8b1dffa3", Versions.ONE_FIVE_ZERO);

		/** Cross Domain, Compressed */
		map.put("a000e7767ef0c1faa6dd233a06ef1526", Versions.ONE_FIVE_ZERO);

		/** Dojo Toolkit 1.4.3 **/
		/** Local, Source */
		map.put("d93c7f52748f962b4646d7f0e6973e88", Versions.ONE_FOUR_THREE);

		/** Local, Compressed - Shrinksafe */
		// TODO:

		/** Cross Domain, Source */ 
		map.put("17bbc3dd3213cb9f9830fc90125ca664", Versions.ONE_FOUR_THREE);

		/** Cross Domain, Compressed */
		map.put("5dddc7943b6b614642ecf631fbf40ac9", Versions.ONE_FOUR_THREE);

		lookup = Collections.unmodifiableMap(map);
	}
}
