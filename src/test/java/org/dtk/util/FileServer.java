package org.dtk.util;

import java.io.File;
import java.net.URL;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

/**
 * Simple HTTP file server used for testing. 
 * 
 * @author James Thomas
 */

public class FileServer {

	Server server;
	
	public FileServer(int port, String basePath) {
		server = new Server(port);
        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setResourceBase(basePath);        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resource_handler, new DefaultHandler() });
        server.setHandler(handlers);        
	}
	
	public void start() throws Exception {
		server.start();
	}
	
	public void stop() throws Exception {
		server.stop();		
	}	
	
	public void getURL() {
		Connector[] connectors = server.getConnectors();
		System.out.println(connectors[0].getHost());
	}
	
	static public FileServer spawn(int port, String directory) throws Exception {
		URL url = FileServer.class.getClassLoader().getResource(directory);			
		File file = new File(url.toURI());
		
		FileServer fileServer = new FileServer(port, file.toString());
		fileServer.start();
		
		return fileServer;
    }
	
	public static void main(String[] args) throws Exception {
		URL url = FileServer.class.getClassLoader().getResource(".");	
		
		File file = new File(url.toURI());
		
		FileServer fs = new FileServer(9080, file.toString());
		System.out.println("...");
		fs.start();
		fs.getURL();
		System.out.println("...");
		fs.stop();
	}
}