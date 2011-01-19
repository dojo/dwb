package org.dtk.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.wink.common.internal.MultivaluedMapImpl;
import org.apache.wink.common.model.multipart.BufferedInMultiPart;
import org.apache.wink.common.model.multipart.InPart;

public class HttpUtil {
	/** Content disposition HTTP header */
	public static final String contentDisposition = "Content-disposition";
	
	/** Filename for build result */
	public static final String contentDispositionAttachment = "attachment; filename=dojo.zip";

	/** Regex pattern to match name attributes in content disposition header */
	protected static final String namePatternStr = "name=\"(.+?)\"";

	/** Regex pattern */
	protected static final Pattern namePattern = Pattern.compile(namePatternStr);

	/** Resource path format, "context_path/servet_path/resource_path" */
	protected static final String resourcePathFormat = "%1$s/%2$s";
	
	/**
	 * Convert multipart form upload into a generic map structure. All 
	 * form part bodies will be stored as chosen type. 
	 * 
	 * @param multiPartForm - Form data
	 * @Param klass - Value type to reader
	 * @return MultiPart form fields, converted to a map.
	 */
	public static MultivaluedMap<String, Object> retrieveMultiPartFormValues(BufferedInMultiPart multiPartForm,
		Class klass) {
		MultivaluedMap<String, Object> formFields = new MultivaluedMapImpl<String, Object>(); 
		Iterator<InPart> partIter = multiPartForm.getParts().iterator();		
		
		while(partIter.hasNext()) {
			InPart formPart = partIter.next();
			String formPartName = getFormPartName(formPart);

			if (formPartName != null) {
				try {
					// Extract part body and store in response map
					Object formPartValue = formPart.getBody(klass, null);

					if (formPartValue != null) {
						formFields.add(formPartName, formPartValue);	
					}	
				} catch (IOException e) {
					// Swallow errors, we aren't expected anything that won't be a string.
					e.printStackTrace();
				}							
			}
		}

		return formFields;		
	}

	/**
	 * Search for a name given a multipartform part.
	 * 
	 * @param formPart - MultiPart form part
	 * @return Name for associated multipartform part, null if not given. 
	 */
	protected static String getFormPartName(InPart formPart) {
		String formPartName = null;  

		// Get Content-Disposition header from form part
		String fieldContentDisposition = formPart.getHeaders().getFirst(contentDisposition);	
		Matcher matcher = namePattern.matcher(fieldContentDisposition);

		if (matcher.find()) {
			// First group in the regex is the name.
			formPartName = matcher.group(1);
		}

		return formPartName; 
	}
	
	/**
	 * Return an absolute URL for the relative resource path, given the 
	 * passed context.
	 * 
	 * @param request - HTTP Request, needed to extract context and servlet path
	 * @param localPath - Relative path to resource
	 * @return Absolute resource path for local resource
	 */
	public static String constructFullURLPath(HttpServletRequest request, String localPath) throws MalformedURLException {
		String resourcePath = request.getContextPath() + request.getServletPath() + localPath;
		String resourceURLPath = "";

		URL reconstructedURL = new URL(request.getScheme(),
				request.getServerName(),
				request.getServerPort(),
				resourcePath);
		resourceURLPath = reconstructedURL.toString();

		return resourceURLPath;
	}
}
