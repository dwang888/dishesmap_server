package wd.goodFood.serverSide;

import java.util.logging.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import wd.goodFood.utils.Configuration;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GoodFoodServer {
	private Configuration config;
	private Server server;
	public static final Logger logger = Logger.getLogger(GoodFoodServer.class.getName());
	
	public GoodFoodServer(String pathConfig) throws Exception{
		this.config = new Configuration(pathConfig);				
		this.server = new Server();

		ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);	
		contextHandler.setContextPath("/");
		server.setHandler(contextHandler);
		
		contextHandler.addServlet(new ServletHolder(new GoodFoodServlet()),"/*");	
        
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setUseDirectBuffers(false);
        connector.setHost(config.getValue("host"));
        connector.setPort(Integer.parseInt(config.getValue("port")));
        connector.setThreadPool(new QueuedThreadPool(Integer.parseInt(config.getValue("numThreads"))));        
        server.addConnector(connector);
        
        
	}
	
	public void runServer(){
		try {
			server.start();
			logger.info("Server started! at\t" + config.getValue("host") + ":" + config.getValue("port"));
	        logger.info("Number of threads:\t" + config.getValue("numThreads"));
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
		if(args.length < 1){
			System.out.println("no property file input!!!");
			System.exit(0);
		}
		
		GoodFoodServer server = new GoodFoodServer(args[0]);        
        server.runServer();
        
	}

}
