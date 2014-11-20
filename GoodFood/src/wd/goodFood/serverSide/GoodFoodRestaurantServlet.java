package wd.goodFood.serverSide;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;
import wd.goodFood.utils.Configuration;
//import wd.goodFood.utils.DBConnector;

//handle API for restaurant info
public class GoodFoodRestaurantServlet extends HttpServlet {
	private Configuration config;
	private RequestRestaurantProcessor processor;
//	private String pathConfig = "etc/goodfood.properties";
	private String pathConfig = GoodFoodXMLServer.pathServletConfig;
    private static final long serialVersionUID = 4L;
    public static final Logger logger = Logger.getLogger(GoodFoodRestaurantServlet.class.getName());
    public static InitialContext IC;
    public static BasicDataSource DS;
    
//	public GoodFoodServlet() throws Exception{		
//		this.config = new Configuration(pathConfig);
//		handler = new RequestProcessor(pathConfig);
//	}
//
//	public GoodFoodServlet(String pathConfig) throws Exception{
//		this.pathConfig = pathConfig;
//		this.config = new Configuration(pathConfig);
//		handler = new RequestProcessor(pathConfig);
//		
//	}
    
	@Override
	public void init(){
		try {
			super.init();
			this.config = new Configuration(pathConfig);
			IC = new InitialContext();
			DS = (BasicDataSource)IC.lookup("jdbc/DSDB");
			this.processor = new RequestRestaurantProcessor(pathConfig);		
		} catch (NamingException e) {
			logger.info("Can't find the naming resource -> DB pooling!!!");
			e.printStackTrace();
		} catch (ServletException e) {
			logger.info("Servlet initializing failed!!!");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException{
    	String jsonStr = this.processor.callProcessors(req);
    	PrintWriter out = response.getWriter();
        response.setStatus(HttpServletResponse.SC_OK);
        System.out.println(req.getPathInfo());
        out.print(jsonStr);

    }
    
    protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
    	PrintWriter out = response.getWriter();
        response.setStatus(HttpServletResponse.SC_OK);
        System.out.println(req.getPathInfo());
        out.print("Just a test string");
    }
    
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
