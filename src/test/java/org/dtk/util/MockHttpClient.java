package org.dtk.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

/**
 * Mock implementation of the HTTP client, will return responses from 
 * a file system rather than a remote host.
 * 
 * @author James Thomas
 */
public class MockHttpClient implements HttpClient {
	
	public String hostPrefix, appDir;
	
	@Override
	public HttpResponse execute(HttpUriRequest request) throws IOException,
		ClientProtocolException {
		
		String url = request.getURI().toString();
		
		if (!url.startsWith(hostPrefix)) {
			throw new IOException("Trying to read non-local file");
		}
		
		url = appDir + url.substring(hostPrefix.length());
		String responseText = getResourceAsString(url);
		
		StatusLine statusLine;			
		HttpResponse response;
		
		if (responseText != null) {
			statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), HttpStatus.SC_OK, null);			
			response = new BasicHttpResponse(statusLine);				
			response.setEntity(new StringEntity(responseText));
		} else {
			statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 2, 0), HttpStatus.SC_NOT_FOUND, null);			
			response = new BasicHttpResponse(statusLine);
		}
		
		return response;
	}
	
	@Override
	public HttpParams getParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ClientConnectionManager getConnectionManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpResponse execute(HttpUriRequest request, HttpContext context)
			throws IOException, ClientProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpResponse execute(HttpHost target, HttpRequest request)
			throws IOException, ClientProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpResponse execute(HttpHost target, HttpRequest request,
			HttpContext context) throws IOException,
			ClientProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T execute(HttpUriRequest request,
			ResponseHandler<? extends T> responseHandler)
			throws IOException, ClientProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T execute(HttpUriRequest request,
			ResponseHandler<? extends T> responseHandler,
			HttpContext context) throws IOException,
			ClientProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T execute(HttpHost target, HttpRequest request,
			ResponseHandler<? extends T> responseHandler)
			throws IOException, ClientProtocolException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T execute(HttpHost target, HttpRequest request,
			ResponseHandler<? extends T> responseHandler,
			HttpContext context) throws IOException,
			ClientProtocolException {
		// TODO Auto-generated method stub
		return null;
	}
	
	private String getResourceAsString(String filePath) throws IOException {
		InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);		
		if (is == null) {
			return null;
		}
		return IOUtils.toString(is);
	}
}