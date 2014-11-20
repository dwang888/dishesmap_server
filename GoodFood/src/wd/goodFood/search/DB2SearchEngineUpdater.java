package wd.goodFood.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
import wd.goodFood.utils.Configuration;
import wd.goodFood.utils.DBConnector;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory.*;
import org.elasticsearch.node.Node;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.elasticsearch.node.NodeBuilder.*;
import static org.elasticsearch.common.xcontent.XContentFactory.*;

//update data to Search Engine from DB
public class DB2SearchEngineUpdater {
	
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
	static final String DB_URL = "jdbc:mysql://192.241.173.181:3306";
	String urlES = "http://107.170.18.102:9200/goodfood/goodfood_biz/_search";
	   //  Database credentials
	static final String USER = "lingandcs";
	static final String PASS = "sduonline";
	
	String SELECT_biz = "SELECT * FROM goodfoodDB.goodfood_biz";
	String SELECT_biz_foursquare = "SELECT * FROM goodfoodDB.goodfood_biz";
	
	TransportClient client;
	
	private Configuration config;
	
	public DB2SearchEngineUpdater(){
		config = new Configuration("etc/goodfood.properties");
		Settings settings = ImmutableSettings.settingsBuilder()
		        .put("cluster.name", "ES_cluster_dishesmap")
		        .build();
		this.client = new TransportClient(settings);
        
		client.addTransportAddress(new InetSocketTransportAddress("107.170.18.102", 9300));
	}
	
	public List<Business> fetchAllPlacesFromDB(){
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
				this.fetchPlacesFromES(String.valueOf(latitude), String.valueOf(longitude));
//				IndexResponse response = client.prepareIndex("goodfood", "goodfood_biz", String.valueOf(id+625187))
//				        .setSource(json)
//				        .execute()
//				        .actionGet();
//				System.out.println(response.getIndex() + "\t" + 
//				        response.getId() + "\t" + 
//						response.getType() + "\t" + 
//				        response.getVersion() + 
//						"\t" + response.getHeaders());
//				Thread.sleep(1000);
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
			
		}		
		
		long endTime = System.currentTimeMillis();
		System.out.println("time fetch PLACES from API:\t" + (endTime - startTime));
		return bizs;
	}
	
	//fetch restaurants from search engine
		public List<Business> fetchPlacesFromES(String lat, String lon){
			long startTime = System.currentTimeMillis();
			System.out.println("fetching places from search engine..." + lat + "\t" + lon);
//			String jsonStr = "{\"query\":{\"size\" : 10,\"filtered\":{\"query\":{\"match_all\":{}},\"filter\":{\"geo_distance\":" +
//					"{\"distance\":\"50km\", \"goodfood_biz.location\":{\"lat\":" + lat + "," +
//					"\"lon\":" + lon + "}}}}}}";
			String jsonStr = "{\"size\" : " + config.getValue("sizeOfReturn") + "," +
					"\"query\":{" +
						"\"filtered\":{" +
							"\"query\":{" +
//								"\"field\":{\"dataSource\":2}," + 
								"\"match_all\":{}" +
								"}," +
							"\"filter\":{\"geo_distance\":{" +
//									"\"from\":\"" + config.getValue("fromDistance") + "km\", " +
//									"\"to\":\"" + config.getValue("toDistance") + "km\"," +
									"\"distance\":\"" + config.getValue("toDistance") + "km\"," +
									"\"dataSource\": 1," +
									" \"goodfood_biz.location\":{" +
											"\"lat\":" + lat + "," +
											"\"lon\":" + lon + 
							"}}}}}}";
			
			System.out.println(jsonStr);
			List<Business> bizs = new ArrayList<Business>();
			
			URL url;
			try {
				url = new URL(this.urlES);
				HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();			
				urlconn.setRequestMethod("POST");
				urlconn.setDoOutput(true);
				urlconn.setDoInput(true);
				urlconn.setRequestProperty("Content-Type", "application/json");
				urlconn.connect();
				OutputStreamWriter out = new OutputStreamWriter(urlconn.getOutputStream());
				out.write(jsonStr);
				out.close();
				int HttpResult =urlconn.getResponseCode();
				StringBuilder sb = new StringBuilder();
				if(HttpResult ==HttpURLConnection.HTTP_OK){  
			        BufferedReader br = new BufferedReader(new InputStreamReader(  
			        		urlconn.getInputStream(),"utf-8"));  
			        String line = null;  
			        while ((line = br.readLine()) != null) {  
			            sb.append(line + "\n");  
			        }  
			        br.close();  

			        System.out.println(""+sb.toString());

			        JsonParser jsonParser = new JsonParser();
			        JsonObject jobj= (JsonObject)jsonParser.parse(sb.toString());
					
					JsonArray locations = jobj.getAsJsonObject("hits").getAsJsonArray("hits");
					System.out.println("\nnumber of locations:\t" + locations.size());
					
					for(JsonElement location : locations){
						JsonObject loc = (JsonObject)location;
						Business biz = new Business();
						biz = addInfo2Biz(loc.getAsJsonObject("_source"), biz);
						bizs.add(biz);
						System.out.println(biz);
					}        
			    }else{  
			        System.out.println("error info from ES:\t" + HttpResult);  
			    }
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
//			System.out.println("fetching PLACEs from:\t");
//			System.out.println(apiStr);	
			
			long endTime = System.currentTimeMillis();
//			System.out.println("time fetch PLACES from Search Engine:\t" + (endTime - startTime));
			return bizs;
		}
	
		/**
		 * add info from FourSquare Json response to Biz obj
		 * */
		public Business addInfo2Biz(JsonObject jobj, Business biz){
			JsonElement eTmp = null;
			JsonObject oTmp = null;
//			System.out.println(jobj);
			biz.setBusiness_id(jobj.get("bizSrcID").getAsString().trim());
			biz.setBusiness_name(jobj.get("bizName").getAsString().trim());		
			
			eTmp = jobj.get("phoneNum");
//			System.out.println(eTmp.getClass());
			if(eTmp != null)
				biz.setBusiness_phone(eTmp.toString());
			
			biz.setBusiness_address(jobj.get("address").getAsString().trim());
			
			eTmp = jobj.get("location");
			if(eTmp != null){
				String[] latlon = jobj.get("location").getAsString().split(",");
//				System.out.println(latlon[0]);
//				System.out.println(latlon[1]);
				biz.setLatitude(latlon[0]);
				biz.setLongitude(latlon[1]);
			}		
					
			biz.setNumReviews(Integer.parseInt(jobj.get("numReviews").getAsString().trim()));
//			System.out.println(eTmp.getAsString());		
			
			eTmp = jobj.get("merchantMsg");
			if(eTmp != null)
				biz.setBusiness_merchantMsg(eTmp.toString());
			
			eTmp = jobj.get("bizWebsite");
			if(eTmp != null)
				biz.setWebsite(eTmp.toString());
			
			eTmp = jobj.get("profileLink");
			if(eTmp != null)
				biz.setLink(eTmp.toString());
			
			biz.setDataSource(jobj.get("dataSource").getAsInt());//TODO:hardcode?	
//			System.out.println(biz);
			return biz;
		}
		
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Hello World");
		DB2SearchEngineUpdater tester = new DB2SearchEngineUpdater();
//		tester.getFromES();
//		tester.indexDoc();
		tester.fetchAllPlacesFromDB();
	}

}
