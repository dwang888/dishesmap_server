package wd.goodFood.search;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;

import wd.goodFood.entity.Business;
import wd.goodFood.utils.DBConnector;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory.*;
import org.elasticsearch.node.Node;

import static org.elasticsearch.node.NodeBuilder.*;
import static org.elasticsearch.common.xcontent.XContentFactory.*;

public class DataImporter {
	
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
	static final String DB_URL = "jdbc:mysql://192.241.173.181:3306";

	   //  Database credentials
	static final String USER = "lingandcs";
	static final String PASS = "sduonline";
	
	String SELECT_biz = "SELECT * FROM goodfoodDB.goodfood_biz_Google";
	TransportClient client;
	
	public DataImporter(){
		Settings settings = ImmutableSettings.settingsBuilder()
		        .put("cluster.name", "elasticsearch")
		        .build();
		this.client = new TransportClient(settings);
        
		client.addTransportAddress(new InetSocketTransportAddress("107.170.18.102", 9300));
	}
	
	public List<Business> fetchAllPlacesFromDB2SearchEngine(){
		//use json here; an xml based version may be needed
//		System.out.println(lat);
		long startTime = System.currentTimeMillis();
		Connection dbconn = null;
		PreparedStatement psSelectBiz = null;
		PreparedStatement psInsertBiz = null;
		String today = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());

		
		List<Business> bizs = new ArrayList<Business>();
//		System.out.println("fetching PLACEs from:\t");
//		System.out.println(apiStr);		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			dbconn = DriverManager.getConnection(DB_URL,USER,PASS);
			Statement stmt = dbconn.createStatement();
			
//			dbconn = DriverManager.getConnection("mysql:\\192.241.173.181:3306", "lingandcs", "sduonline");
//			dbconn = GoodFoodServlet.DS.getConnection();
//			psSelectBiz = dbconn.prepareStatement(this.SELECT_biz);
//			ResultSet result = psSelectBiz.executeQuery();
			System.out.println("querying...");
			ResultSet rs = stmt.executeQuery(this.SELECT_biz);
			int count = 0;
			
			while(rs.next()){
//				System.out.println("a record");
				int id = rs.getInt("id");
				String bizName = rs.getString("bizName");
				String address = rs.getString("address");
				String bizSrcID = rs.getString("bizSrcID");
				Double latitude = rs.getDouble("latitude");
				Double longitude = rs.getDouble("longitude");
				String phoneNum = rs.getString("phoneNum");
				String merchantMsg = rs.getString("merchantMsg");
				String offer = rs.getString("offer");
				int numReviews = rs.getInt("numReviews");
				String profileLink = rs.getString("profileLink");
				String bizWebsite = rs.getString("bizWebsite");
				int dataSource = rs.getInt("dataSource");
				Timestamp updateTime = rs.getTimestamp("updateTime");
				String updateTimeFormatted = new SimpleDateFormat("yyyy-MM-dd").format(updateTime);
//				System.out.println(updateTime);
//				System.out.println(new SimpleDateFormat("yyyy-MM-dd").format(updateTime));
				
				
				XContentBuilder builder = jsonBuilder()
					    .startObject()
					        .field("id", id + 625187)
					        .field("address", address)			        
					        .field("bizName", bizName)				      
					        .field("bizSrcID", bizSrcID)
					        .field("bizWebsite", bizWebsite)
					        .field("dataSource", dataSource)
					        .field("location", latitude + "," + longitude)
					        .field("merchantMsg", merchantMsg)
					        .field("numReviews", numReviews)
					        .field("offer", offer)
					        .field("phoneNum", phoneNum)
					        .field("profileLink", profileLink)
					        .field("updateTime", updateTimeFormatted)				        
					    .endObject();
				String json = builder.string();
				System.out.println(json);				
				IndexResponse response = client.prepareIndex("goodfood", "goodfood_biz", String.valueOf(id+625187))
				        .setSource(json)
				        .execute()
				        .actionGet();
//				System.out.println(response.getIndex() + "\t" + 
//				        response.getId() + "\t" + 
//						response.getType() + "\t" + 
//				        response.getVersion() + 
//						"\t" + response.getHeaders());
//				Thread.sleep(1000);
				count++;
				if(count%1000 == 0){
					System.out.println(count + "\t records loaded to ES");
				}
			}
			

			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			DBConnector.close(dbconn);
			DBConnector.close(psSelectBiz);
			DBConnector.close(psInsertBiz);
			
			client.close();
		}		
		
		long endTime = System.currentTimeMillis();
		System.out.println("time fetch PLACES from API:\t" + (endTime - startTime));
		return bizs;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Hello World");
		DataImporter tester = new DataImporter();
//		tester.getFromES();
//		tester.indexDoc();
		tester.fetchAllPlacesFromDB2SearchEngine();
	}

}
