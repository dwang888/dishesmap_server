//package wd.goodFood.serverSide;
//
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.sql.Timestamp;
//import java.util.logging.Logger;
//
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.eclipse.jetty.server.Server;
//
//import wd.goodFood.utils.Configuration;
//
//public class CopyOfGoodFoodServlet extends HttpServlet {
//	private Configuration config;
//	private RequestProcessor handler;
//	private String pathConfig = "etc/goodfood.properties";
//    private static final long serialVersionUID = 4L;
//    public static final Logger logger = Logger.getLogger(CopyOfGoodFoodServlet.class.getName());
//    
//	public CopyOfGoodFoodServlet() throws Exception{		
//		this.config = new Configuration(pathConfig);
//		handler = new RequestProcessor(pathConfig);
//	}
//
//	public CopyOfGoodFoodServlet(String pathConfig) throws Exception{
//		this.pathConfig = pathConfig;
//		this.config = new Configuration(pathConfig);
//		handler = new RequestProcessor(pathConfig);
//	}
//    
//	@Override
//	public void init(){
//		
//	}
//	
//    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException{
//    	long startTime = System.currentTimeMillis();
//    	String lat = req.getParameter("lat");
//        String lon = req.getParameter("lon");
//        String callBackFunctionName = req.getParameter("callback");
////        System.out.println("location:\t" + lat + "\t" + lon);
////        System.out.println("Request type: GET");
//        logger.info("Get;\t" + lat + "\t" + lon + "\t" + new Timestamp(System.currentTimeMillis()));
//        String jsonStr = this.handler.callProcessors(lat, lon);
//        String responseStr = callBackFunctionName + "(" + jsonStr + ");";//here we use jsonp to handle cross-domain issue
//        
//    	response.setContentType("text/javascript");
//    	PrintWriter out = response.getWriter();
//        response.setStatus(HttpServletResponse.SC_OK);
//        long endTime = System.currentTimeMillis();
//        logger.info("doGet method costs time:\t" + (endTime - startTime));
//        out.print(responseStr);
//    }
//    
//    protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
//    	String lat = req.getParameter("lat");
//        String lon = req.getParameter("lon");
//        String callBackFunctionName = req.getParameter("callback");
////        System.out.println("location:\t" + lat + "\t" + lon);
////        System.out.println("Request type: POST");
//        logger.info("Post;\t" + lat + "\t" + lon + "\t" + new Timestamp(System.currentTimeMillis()));
//        String jsonStr = this.handler.callProcessors(lat, lon);
//        String responseStr = callBackFunctionName + "(" + jsonStr + ");";
//        
//    	response.setContentType("text/javascript");
//    	PrintWriter out = response.getWriter();
//        response.setStatus(HttpServletResponse.SC_OK);
//        out.print(responseStr);
//    }
//    
//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//
//	}
//
//}
