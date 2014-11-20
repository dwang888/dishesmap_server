package wd.goodFood.serverSide;

import java.io.IOException;
import java.io.PrintWriter;
//import java.sql.Connection;
//import java.sql.SQLException;
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

public class GoodFoodServlet extends HttpServlet {
	private Configuration config;
	private RequestProcessor processor;
//	private String pathConfig = "etc/goodfood.properties";
	private String pathConfig = GoodFoodXMLServer.pathServletConfig;
    private static final long serialVersionUID = 4L;
    public static final Logger logger = Logger.getLogger(GoodFoodServlet.class.getName());
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
			this.processor = new RequestProcessor(pathConfig);		
		} catch (NamingException e) {
			logger.info("Can't find the naming resource -> DB pooling!!!");
			e.printStackTrace();
		} catch (ServletException e) {
			logger.info("Servlet initializing failed!!!");
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException{
    	long startTime = System.currentTimeMillis();
//    	Connection conn = null;
    	BasicDataSource bds = null;
    	try {
			bds = (BasicDataSource)this.IC.lookup("jdbc/DSDB");
//			System.out.println(bds.getNumActive());
//			System.out.println(bds.getNumIdle());
		} catch (NamingException e1) {
			e1.printStackTrace();
		}
    	
//		try {			
//			conn = ds.getConnection();
//			processor.setConn(conn);
//		} catch (SQLException e) {
//			logger.info("SQL exception!!!");
//			e.printStackTrace();
//		} catch (Exception e) {
//			logger.info("exception about DB");
//			e.printStackTrace();
//		}
		
//		System.out.println(GoodFoodServlet.ds.getNumActive());
//		System.out.println(GoodFoodServlet.ds.getNumIdle());
		
    	String lat = req.getParameter("lat");
        String lon = req.getParameter("lon");
//        String keywords = req.getParameter("keywords");
//        Boolean pretty = Boolean.parseBoolean(req.getParameter("pretty"));
        String callBackFunctionName = req.getParameter("callback");
//        System.out.println("location:\t" + lat + "\t" + lon);
//        System.out.println("keywords:\t" + keywords);
        logger.info("Get;\t" + lat + "\t" + lon + "\t" + new Timestamp(System.currentTimeMillis()));
//        String jsonStr = this.processor.callProcessors(lat, lon);
//        String jsonStr = this.processor.callProcessors(lat, lon, keywords);
        String jsonStr = this.processor.callProcessors(req);
//        String responseStr = callBackFunctionName + "(" + jsonStr + ");";//here we use jsonp to handle cross-domain issue
        String responseStr = jsonStr;//here we use jsonp to handle cross-domain issue
    	response.setContentType("text/javascript");
    	PrintWriter out = response.getWriter();
        response.setStatus(HttpServletResponse.SC_OK);
        long endTime = System.currentTimeMillis();
        logger.info("doGet method costs time:\t" + (endTime - startTime));
        out.print(responseStr);
//        System.out.println(GoodFoodServlet.ds.getNumActive());
//		System.out.println(GoodFoodServlet.ds.getNumIdle());
//		
//        if(conn != null){
//        	try {
//				conn.close();
//			} catch (SQLException e) {
//				e.printStackTrace();
//			}
//        }
//        if(processor != null){
//        	processor.getDbconnector().closeAll();
//        }
//        System.out.println(GoodFoodServlet.ds.getNumActive());
//		System.out.println(GoodFoodServlet.ds.getNumIdle());
    }
    
    protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
    	String lat = req.getParameter("lat");
        String lon = req.getParameter("lon");
        String callBackFunctionName = req.getParameter("callback");
//        System.out.println("location:\t" + lat + "\t" + lon);
//        System.out.println("Request type: POST");
        logger.info("Post;\t" + lat + "\t" + lon + "\t" + new Timestamp(System.currentTimeMillis()));
//        String jsonStr = this.processor.callProcessors(lat, lon);
        String jsonStr = this.processor.callProcessors(req);
//        String responseStr = callBackFunctionName + "(" + jsonStr + ");";
        String responseStr = jsonStr;
    	response.setContentType("text/javascript");
    	PrintWriter out = response.getWriter();
        response.setStatus(HttpServletResponse.SC_OK);
        out.print(responseStr);
    }
    
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
