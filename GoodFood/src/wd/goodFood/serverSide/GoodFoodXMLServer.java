package wd.goodFood.serverSide;

import java.util.logging.Logger;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.plus.jndi.*;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.xml.XmlConfiguration;

import wd.goodFood.utils.Configuration;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GoodFoodXMLServer {
	private Configuration config;
	private Server server;
	Resource server_xml;
	XmlConfiguration configuration;
	public static final Logger logger = Logger.getLogger(GoodFoodXMLServer.class.getName());
	public static String pathServletConfig;
	// http://127.0.0.1:8080/where?lat=40.7056310&lon=-73.9780030&callback=?
	//http://api.citygridmedia.com/content/places/v2/search/latlon?what=restaurant&publisher=test&radius=5&sort=dist&format=json&rpp=50&lat=40.7056310&lon=-73.9780030
	public GoodFoodXMLServer(String pathXml) throws Exception{        
		server_xml = Resource.newResource(pathXml);		
        configuration = new XmlConfiguration(server_xml.getInputStream());
        server = (Server)configuration.configure();
	}
	
	public GoodFoodXMLServer(String[] args) throws Exception{        
		server_xml = Resource.newResource(args[0]);
		pathServletConfig = args[1];
        configuration = new XmlConfiguration(server_xml.getInputStream());
        server = (Server)configuration.configure();
	}
	
	public void runServer(){
		try {
			server.start();
			server.join();
		} catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage());
			e.printStackTrace();
		}        
	}
	
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		if(args.length < 2){
			System.out.println("not enough property file input!!!");
			System.exit(0);
		}
		//dummy
//		GoodFoodXMLServer server = new GoodFoodXMLServer(args[0]);
		GoodFoodXMLServer server = new GoodFoodXMLServer(args);
//		GoodFoodXMLServer server = new GoodFoodXMLServer();

        server.runServer();
        
	}

}
