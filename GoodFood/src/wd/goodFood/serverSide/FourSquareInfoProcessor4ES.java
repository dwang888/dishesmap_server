package wd.goodFood.serverSide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;

import wd.goodFood.entity.Business;
import wd.goodFood.entity.Food;
import wd.goodFood.entity.Review;
import wd.goodFood.nlp.GoodFoodFinder;
import wd.goodFood.utils.Configuration;
import wd.goodFood.utils.DBConnector;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class FourSquareInfoProcessor4ES extends DataSourceProcessor{
	private Configuration config;
	private static int numBusiness;//how many business results should be returned per request.
	private String apiPrefixPlace;	
	//for fetcheding detail for each place, but only 3 reviews are returned for each place
	private String apiPrefixReview;	
	private String apiSurfixReview;	
	private JsonParser jsonParser;//should be thread safe
	private GoodFoodFinder finder;
	
	String INSERT_biz = "INSERT INTO goodfoodDB.goodfood_biz_FourSquare "
			+ "(bizName, address, bizSrcID, latitude, longitude, phoneNum, merchantMsg, offer, numReviews, profileLink, bizWebsite, dataSource, updateTime, category) VALUES"
			+ "(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
	String INSERT_reviews = "INSERT INTO goodfoodDB.goodfood_reviews_FourSquare "
			+ "(bizID, bizSrcID, text, rLink, taggedText, food, dataSource, insertTime, updateTime) VALUES"
			+ "(?,?,?,?,?,?,?,?,?)";
	
	String SELECT_biz = "SELECT * FROM goodfoodDB.goodfood_biz_FourSquare WHERE bizSrcID = ? AND dataSource = ?";
		
	String SELECT_reviews = "SELECT text,rLink,taggedText,food,dataSource,insertTime FROM goodfoodDB.goodfood_reviews_FourSquare WHERE bizSrcID = ? AND dataSource = ?";
	String DELETE_reviews = "DELETE FROM goodfoodDB.goodfood_reviews_FourSquare WHERE bizSrcID = ? AND dataSource = ?";
	
	String urlES = "http://107.170.18.102:9200/goodfood/goodfood_biz/_search";
	
	public  FourSquareInfoProcessor4ES(String configFile, GoodFoodFinder finder) throws Exception{
		config = new Configuration(configFile);
		this.setFinder(finder);
		this.numBusiness = Integer.parseInt(config.getValue("numBusiness_FourSquare"));
		this.apiPrefixPlace = config.getValue("apiPrefixPlace_FourSquare");
		this.apiPrefixReview = config.getValue("apiPrefixReview_FourSquare");
		this.apiSurfixReview = config.getValue("apiSurfixReview_FourSquare");
		this.setJsonParser(new JsonParser());
	}

	@Deprecated
	public  FourSquareInfoProcessor4ES(String configFile) throws Exception{
		config = new Configuration(configFile);
		this.numBusiness = Integer.parseInt(config.getValue("numBusiness_FourSquare"));
		this.apiPrefixPlace = config.getValue("apiPrefixPlace_FourSquare");
		this.apiPrefixReview = config.getValue("apiPrefixReview_FourSquare");
		this.apiSurfixReview = config.getValue("apiSurfixReview_FourSquare");
		this.setJsonParser(new JsonParser());
		this.setFinder(new GoodFoodFinder(config.getValue("sentSplitterPath"), config.getValue("tokenizerPath"), config.getValue("NETaggerPath")));
		
	}	
	
	/**
	 * particularly for FourSquare
	 * */
	public String extractAddress(JsonObject location){
		JsonElement eTmp = null;
		StringBuilder sb = new StringBuilder();
		eTmp = location.get("address");
		if(eTmp != null){
			sb.append(eTmp.getAsString() + ", ");
		}
		eTmp = location.get("city");
		if(eTmp != null){
			sb.append(eTmp.getAsString() + ", ");
		}
		eTmp = location.get("state");
		if(eTmp != null){
			sb.append(eTmp.getAsString());
		}
		return sb.toString();
	}
	
	/**
	 * add info from FourSquare Json response to Biz obj
	 * */
	public Business addInfo2Biz(JsonObject jobj, Business biz){
		JsonElement eTmp = null;
		JsonObject oTmp = null;
//		System.out.println(jobj);
		biz.setBusiness_id(jobj.get("bizSrcID").getAsString().trim());
		biz.setBusiness_name(jobj.get("bizName").getAsString().trim());		
		
		eTmp = jobj.get("phoneNum");
//		System.out.println(eTmp.getClass());
		if(eTmp != null)
			biz.setBusiness_phone(eTmp.toString());
		
		biz.setBusiness_address(jobj.get("address").getAsString().trim());
		
		eTmp = jobj.get("location");
		if(eTmp != null){
			String[] latlon = jobj.get("location").getAsString().split(",");
//			System.out.println(latlon[0]);
//			System.out.println(latlon[1]);
			biz.setLatitude(latlon[0]);
			biz.setLongitude(latlon[1]);
		}		
				
		biz.setNumReviews(Integer.parseInt(jobj.get("numReviews").getAsString().trim()));
//		System.out.println(eTmp.getAsString());		
		
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
//		System.out.println(biz);
		return biz;
	}
	
	//fetch restaurants from search engine
	public List<Business> fetchPlaces(String lat, String lon){
		long startTime = System.currentTimeMillis();
//		System.out.println("fetching places from search engine..." + lat + "\t" + lon);
//		String jsonStr = "{\"query\":{\"size\" : 10,\"filtered\":{\"query\":{\"match_all\":{}},\"filter\":{\"geo_distance\":" +
//				"{\"distance\":\"50km\", \"goodfood_biz.location\":{\"lat\":" + lat + "," +
//				"\"lon\":" + lon + "}}}}}}";
		String jsonStr = "{\"size\" : " + config.getValue("sizeOfReturn") + ",\"query\":{\"filtered\":{\"query\":{\"match_all\":{}},\"filter\":{\"geo_distance_range\":" +
				"{\"from\":\"" + config.getValue("fromDistance") + "km\", \"to\":\"" + config.getValue("toDistance") + "km\", \"goodfood_biz.location\":{\"lat\":" + lat + "," +
				"\"lon\":" + lon + "}}}}}}";
		
		List<Business> bizs = new ArrayList<Business>();
		
		URL url;
		try {
			url = new URL(this.urlES);
			HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();			
			urlconn.setRequestMethod("GET");
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

//		        System.out.println(""+sb.toString());

		        JsonParser jsonParser = new JsonParser();
		        JsonObject jobj= (JsonObject)jsonParser.parse(sb.toString());
				
				JsonArray locations = jobj.getAsJsonObject("hits").getAsJsonArray("hits");
//				System.out.println("\nnumber of locations:\t" + locations.size());
				int numCityGrid = 0;
				int numFourSquare = 0;
				
				for(JsonElement location : locations){
					JsonObject loc = (JsonObject)location;
					Business biz = new Business();
					biz = addInfo2Biz(loc.getAsJsonObject("_source"), biz);
					bizs.add(biz);
//					System.out.println(biz);
//					System.out.println(biz);
					if(biz.getDataSource() == 1){
						numCityGrid += 1;
					}else if(biz.getDataSource() == 2){
						numFourSquare += 1;
					}
					System.out.println("CityGrid and FourSquare:\t" + numCityGrid + "\t" + numFourSquare);
				}        
		    }else{  
		        System.out.println("error info from ES:\t" + HttpResult);  
		    }
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		System.out.println("fetching PLACEs from:\t");
//		System.out.println(apiStr);	
		
		long endTime = System.currentTimeMillis();
//		System.out.println("time fetch PLACES from Search Engine:\t" + (endTime - startTime));
		return bizs;
	}
	
	/**
	 * store business info to DB, as cache
	 * */
	public void addBiz2DB(Business biz, Connection conn, PreparedStatement ps){
		try {
			ps.setString(1, biz.getBusiness_name());
			ps.setString(2, biz.getBusiness_address());
			ps.setString(3, biz.getBusiness_id());
			ps.setBigDecimal(4, new BigDecimal(biz.getLatitude()));
			ps.setBigDecimal(5, new BigDecimal(biz.getLongitude()));
			ps.setString(6, biz.getBusiness_phone());
			ps.setString(7, biz.getBusiness_merchantMsg());
			ps.setString(8, biz.getBusiness_offer());
			ps.setInt(9, 0);//TODO:subject to change to real number; number of reviews
			ps.setString(10, biz.getLink());
			ps.setString(11, biz.getWebsite());
			ps.setInt(12, biz.getDataSource());//data source; here it should be 2 for FourSquare
			ps.setTimestamp(13, new Timestamp(System.currentTimeMillis()));
			ps.setString(14, biz.getCategory());
			ps .executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally {
//			close(ps);
//			close(conn);
		}

	}
	
	//and check if 60 days ago, only for FourSquare
	@Override
	public boolean isInReviewTable(Business biz, Connection conn, PreparedStatement ps){
		Boolean inTable = false;
		ResultSet rs = null;		
//		PreparedStatement ps = null;
//		Connection conn = null;
		try {			
//			conn = GoodFoodServlet.ds.getConnection();
//			ps = conn.prepareStatement(this.SELECT_reviews);
			ps.setString(1, biz.getBusiness_id());
			ps.setInt(2, biz.getDataSource());
			rs = ps.executeQuery();
			inTable = rs.isAfterLast() == rs.isBeforeFirst()? false : true;
			if(true == inTable){//check if 30 days ago				
				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.MONTH, -100);
				Timestamp nowNdays = new Timestamp(calendar.getTimeInMillis());	
				rs.next();
				Timestamp insertTime = rs.getTimestamp("insertTime");
//				System.out.println(insertTime);
				if(!insertTime.after(nowNdays)){//TODO: hardcoded column name
					//reviews 30 days ago
					inTable = false;
					PreparedStatement psDeleteReview = conn.prepareStatement(this.DELETE_reviews);
					this.deleteReviewsFromDB(biz, conn, psDeleteReview);//TODO: remove from here?
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBConnector.close(rs);
//			close(ps);
//			close(conn);			
		}
		return inTable;
	}
	
	/**
	 * collect all the reviews for each business
	 * */
//	public synchronized List<Business> fetchReviews(List<Business> bizs){
	public List<Business> fetchReviews(List<Business> bizs){
//		System.out.println(bizs.size() + "\t bizs fetched!");
		
		
		Connection dbconn = null;
		PreparedStatement psSelectReview = null;
		PreparedStatement psLookupReview = null;
		PreparedStatement psInsertReview = null;
//		PreparedStatement psDeleteReview = null;
		long start;
		long end;
		try {
			dbconn = GoodFoodServlet.DS.getConnection();
			psSelectReview = dbconn.prepareStatement(this.SELECT_reviews);
//			psLookupReview = dbconn.prepareStatement(this.Lookup_reviews);
			psInsertReview = dbconn.prepareStatement(this.INSERT_reviews);
//			psDeleteReview = dbconn.prepareStatement(this.DELETE_reviews);
			
			for(Business biz : bizs){
//				System.out.println("\n\n" + biz);
//				System.out.println(biz.getBusiness_id());
//				System.out.println(biz.getDataSource());
				
				start = System.currentTimeMillis();
				boolean flag = this.isInReviewTable(biz, dbconn, psSelectReview);
				end = System.currentTimeMillis();
				if(flag == true){
					start = System.currentTimeMillis();
					fetchReviewsFromDB(biz, dbconn, psSelectReview);//read cache
					end = System.currentTimeMillis();
//					System.out.println("fetch reviews from DB:\t" + (end - start));
				}else{
					//no records in DB
					if(biz.getDataSource() == 2){
						//only consider FourSquare cases
						start = System.currentTimeMillis();
//						System.out.println(biz.getBusiness_id());
//						System.out.println(biz.getDataSource());
						this.fetchReviewsFromAPI(biz);
						this.addReviews2DB(biz, dbconn, psInsertReview);//add to cache
						end = System.currentTimeMillis();
//						System.out.println("fetch reviews from API:\t" + (end - start));
					}
					
				}			
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}  finally {
			DBConnector.close(dbconn);
			DBConnector.close(psSelectReview);
			DBConnector.close(psInsertReview);
			DBConnector.close(psLookupReview);
		}		
		
		return bizs;
	}
	
	public Business fetchReviewsFromAPI(Business biz){
		long startTime = System.currentTimeMillis();
		String id = biz.getBusiness_id();
		String apiStr = this.getApiPrefixReview() + id + this.getApiSurfixReview();
//		String apiStr = this.getApiPrefixReview() + id + this.getApiSurfixReview() + "&v=20130518";
		String today = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
		apiStr += "&v=" + today;
//		System.out.println(apiStr);
		long time = 0;
		int length = 0;
		try {
			URL url = new URL(apiStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			InputStream is = conn.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			String line;
			while((line = br.readLine()) != null){
				sb.append(line);
			}
			String jsonStr = sb.toString();
			jsonStr = jsonStr.replaceAll("(?<![\"}\\]\\d,(true)(false)]),\"", "\",\"");
			jsonStr = jsonStr.replaceAll("\",\",\"", "\",\"");
//			System.out.println(jsonStr);
			JsonObject jobj= (JsonObject)jsonParser.parse(jsonStr);
//			JsonReader reader = new JsonReader(new InputStreamReader(is));
//			JsonObject jobj= jsonParser.parse(reader).getAsJsonObject();
			is.close();
			br.close();
			JsonObject tips = jobj.getAsJsonObject("response").getAsJsonObject("tips");
			if(tips != null){
				JsonArray bizReviews = tips.getAsJsonArray("items");
//				System.out.println(bizReviews.size() + "\t reviews");
				for(JsonElement jElem : bizReviews){
					JsonObject robj = (JsonObject) jElem;
//					String rStr = robj.get("text").toString().trim() + " @ via FourSquare";
					String rStr = robj.get("text").toString().trim();
					String rLink = robj.get("canonicalUrl").getAsString().trim();
					
					long start = System.currentTimeMillis();
					Review r = this.getFinder().process(rStr);//call NLP tools
					long end = System.currentTimeMillis();
					r.setWebLink(rLink);
					r.setDataSource(biz.getDataSource());//TODO: change hardcode 2?
					biz.getReviews().add(r);
					time += (end-start);
					length += rStr.length();
//					System.out.println(biz.getReviews().size());
				}
//				System.out.println(biz.getReviews().size());
			}
//			
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return biz;
		} catch (IOException e) {
			e.printStackTrace();
			return biz;
		}
		long endTime = System.currentTimeMillis();
//		System.out.println("---------------->");
//		System.out.println("length of review:\t" + length);
//		System.out.println("NER from API:\t" + time);
//		System.out.println("fetch reviews from API:\t" + (endTime - startTime));
		return biz;
	}	
	
	/**
	 * delete expired reviews; now 30 days
	 * */
	public void deleteReviewsFromDB(Business biz, Connection conn, PreparedStatement ps){
		String bizSrcID = biz.getBusiness_id();
		try {
			ps.setString(1, bizSrcID);
			ps.setInt(2, 2);//TODO: hardcode?
			ps.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	/**
	 * no ML or NLP operation here
	 * only DB IO
	 * */
	public Business fetchReviewsFromDB(Business biz, Connection conn, PreparedStatement ps){
		String rStr;
		String rLink;
		String NEStr;
		String taggedText;
		long time = 0;
		int length = 0;
		ResultSet rs = null;		
//		System.out.println(biz.getBusiness_name());
		try {
			long startTime = System.currentTimeMillis();
			ps.setString(1, biz.getBusiness_id());
			ps.setInt(2, biz.getDataSource());//data source TODO: hardcode?
			rs = ps.executeQuery();
			while(rs.next()){
				long start = System.currentTimeMillis();
				rStr = rs.getString("text");
//				if(rs.getString("text").toLowerCase().indexOf("foursquare") == -1)
//					rStr = rs.getString("text") + " (@ via FourSquare)";//subject to change
//				System.out.println("--DB-----:\t" + rStr);
				rLink = rs.getString("rLink");
				NEStr = rs.getString("food");
				taggedText = rs.getString("taggedText");				
				Review r = new Review(rStr, taggedText, NEStr);
								
//				System.out.println("Generate a review:\t" + ( endTime - startTime));
				r.setWebLink(rLink);
				r.setDataSource(rs.getInt("dataSource"));
				biz.getReviews().add(r);
				long end = System.currentTimeMillis();		
				time += (end-start);
				length += rStr.length();
			}
			long endTime = System.currentTimeMillis();
//			System.out.println("------------------------------------------------>");
//			System.out.println("length of review:\t" + length);
//			System.out.println("NER from DB time:\t" + time);
//			System.out.println("fetch reviews from DB:\t" + (endTime - startTime));
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBConnector.close(rs);
//			close(ps);
//			close(conn);
		}
		return biz;
	}

	public static int getNumBusiness() {
		return numBusiness;
	}

	public static void setNumBusiness(int numBusiness) {
		FourSquareInfoProcessor4ES.numBusiness = numBusiness;
	}


	public String getApiPrefixPlace() {
		return apiPrefixPlace;
	}

	public void setApiPrefixPlace(String apiPrefixPlace) {
		this.apiPrefixPlace = apiPrefixPlace;
	}

	public JsonParser getJsonParser() {
		return jsonParser;
	}

	public void setJsonParser(JsonParser jsonParser) {
		this.jsonParser = jsonParser;
	}


	public GoodFoodFinder getFinder() {
		return finder;
	}

	public void setFinder(GoodFoodFinder finder) {
		this.finder = finder;
	}

	
	
	public void clearHistoryData(){
		
	}
	
	public Configuration getConfig() {
		return config;
	}

	public void setConfig(Configuration config) {
		this.config = config;
	}

	

	public final String getApiPrefixReview() {
		return apiPrefixReview;
	}

	public final void setApiPrefixReview(String apiPrefixReview) {
		this.apiPrefixReview = apiPrefixReview;
	}

	public final String getApiSurfixReview() {
		return apiSurfixReview;
	}

	public final void setApiSurfixReview(String apiSurfixReview) {
		this.apiSurfixReview = apiSurfixReview;
	}

	public static void main(String[] args){
		if(args.length < 1){
			System.out.println("no property file input!!!");
			System.exit(0);
		}
		FourSquareInfoProcessor4ES processor = null;
		try {
			processor = new FourSquareInfoProcessor4ES(args[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(processor.apiSurfixReview);
		processor.clearHistoryData();
		String lat = "40.669800";
		String lon = "-73.943849";
		List<Business> bizs = processor.fetchPlaces(lat, lon);
		
		bizs = processor.fetchReviews(bizs);
		System.exit(0);
		for(Business biz : bizs){
//			System.out.println(biz.getBusiness_address());
			biz.extractInfoFromReviews();
			for(Food f : biz.getGoodFoods()){
				System.out.print(f.getFoodText() + "\t");
			}
//			System.out.println(biz.getGoodFoods().size());
		}
	}

}
