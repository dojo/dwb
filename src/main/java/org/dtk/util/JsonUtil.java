package org.dtk.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class JsonUtil {

	/** Format when encoding JSON in HTML, required when sending data via Dojo io.iframe. */
	private static final String htmlEncodedJsonFormat 
		= "<html><body><textarea>%1$s</textarea></body></html>";  
	
	/** JSON object mapper */
	private static final ObjectMapper mapper = new ObjectMapper(new JsonFactory()); 
	
	/** Type conversion reference for JavaScript mapper */
	private static final TypeReference<HashMap<String,Object>> typeRef 
		= new TypeReference<HashMap<String,Object>>() {};        
	
	/**
	 * Enforce noninstantiability of utility class. 
	 */
	private JsonUtil() {
		throw new AssertionError();
	}

	/**
	 * Read a file containing JSON content and convert to a generic
	 * map collection. 
	 * 
	 * @param JSONFile - File path with JSON contents.
	 * @return Generic map corresponding to JSON Object contents
	 */
	public static HashMap<String, Object> genericJSONMapper(File JSONFile) 
		throws JsonParseException, JsonMappingException, IOException {
		return mapper.readValue(JSONFile, typeRef);
	}
	
	/**
	 * Read a string containing JSON content and convert to a generic
	 * map collection. 
	 * 
	 * @param JSONFile - File path with JSON contents.
	 * @return Generic map corresponding to JSON Object contents
	 */
	public static HashMap<String, Object> genericJSONMapper(String JSONStr) 
		throws JsonParseException, JsonMappingException, IOException {
		return mapper.readValue(JSONStr, typeRef);
	}
	
	/**
	 * Read an input stream containing JSON content and convert to a generic
	 * map collection. 
	 * 
	 * @param JSONFile - File path with JSON contents.
	 * @return Generic map corresponding to JSON Object contents
	 */
	public static HashMap<String, Object> genericJSONMapper(InputStream JSONStr) 
		throws JsonParseException, JsonMappingException, IOException {
		return mapper.readValue(JSONStr, typeRef);
	}
	
	/** 
	 * Utility method to convert single Java object to 
	 * JSON string equivalent. 
	 * 
	 * @param genericJsonObject - Java instance to convert
	 * @return JSON String represntation of parameter
	 * @throws JsonParseException - Error creating JSON for parameter 
	 * @throws JsonMappingException - Error mapping between Java and JSON
	 * @throws IOException - IO Error
	 */
	public static String writeJavaToJson(Object genericJavaObject) 
	throws JsonParseException, JsonMappingException, IOException {
		JsonFactory factory = new JsonFactory(); 
		ObjectMapper om = new ObjectMapper(factory);
		
		return om.writeValueAsString(genericJavaObject);
	}
	
	/** 
	 * Utility method to convert single Java object to 
	 * JSON string equivalent and enclose with HTML page.
	 * 
	 * @param genericJsonObject - Java instance to convert
	 * @return JSON String represntation of parameter
	 * @throws JsonParseException - Error creating JSON for parameter 
	 * @throws JsonMappingException - Error mapping between Java and JSON
	 * @throws IOException - IO Error
	 */
	public static String writeJavaToHtmlEncodedJson(Object genericJavaObject) 
	throws JsonParseException, JsonMappingException, IOException {
		return String.format(htmlEncodedJsonFormat, writeJavaToJson(genericJavaObject));
	}
}
