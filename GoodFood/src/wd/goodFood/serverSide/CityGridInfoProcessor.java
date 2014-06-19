package wd.goodFood.serverSide;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import wd.goodFood.entity.Business;
import wd.goodFood.entity.Food;
import wd.goodFood.entity.Review;
import wd.goodFood.nlp.GoodFoodFinder;
import wd.goodFood.utils.Configuration;
import wd.goodFood.utils.DBConnector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class CityGridInfoProcessor extends DataSourceProcessor{
	private Configuration config;
	private static int numBusiness;//how many business results should be returned per request.
//	private static String apiPrefix;
	private String apiPrefixPlace;	
	//for fetcheding detail for each place, but only 3 reviews are returned for each place
	private String apiPrefixPlaceDetail;	
	//for fetcheding reviews for each location; 50 reviews are returned; could get input of latitude longitude, or listing_id
	private String apiPrefixReviewDetail;	
	private JsonParser jsonParser;
	private GoodFoodFinder finder;
//	private Connection conn;
	
	String INSERT_biz = "INSERT INTO goodfoodDB.goodfood_biz "
			+ "(bizName, address, bizSrcID, latitude, longitude, phoneNum, merchantMsg, offer, numReviews, profileLink, bizWebsite, dataSource, updateTime) VALUES"
			+ "(?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
	String INSERT_reviews = "INSERT INTO goodfoodDB.goodfood_reviews "
			+ "(bizID, bizSrcID, text, rLink, taggedText, food, dataSource, insertTime, updateTime) VALUES"
			+ "(?,?,?,?,?,?,?,?,?)";
	
	String SELECT_biz = "SELECT * FROM goodfoodDB.goodfood_biz WHERE bizSrcID = ? AND dataSource = ?";
		
	String SELECT_reviews = "SELECT text,rLink,taggedText,food,dataSource FROM goodfoodDB.goodfood_reviews WHERE bizSrcID = ? AND dataSource = ?";
	String Lookup_reviews = "SELECT id FROM goodfoodDB.goodfood_reviews WHERE bizSrcID = ? AND dataSource = ?";
	
	@Deprecated
	public  CityGridInfoProcessor(String configFile) throws Exception{
		config = new Configuration(configFile);
		this.numBusiness = Integer.parseInt(config.getValue("numBusiness"));
//		this.apiPrefix = config.getValue("apiPrefix");
		this.apiPrefixPlace = config.getValue("apiPrefixPlace") + this.numBusiness;
		this.apiPrefixPlaceDetail = config.getValue("apiPrefixPlaceDetail");
		this.apiPrefixReviewDetail = config.getValue("apiPrefixReviewDetail");
		this.setJsonParser(new JsonParser());
//		this.setBizs(new ArrayList<Business>());
		this.setFinder(new GoodFoodFinder(config.getValue("sentSplitterPath"), config.getValue("tokenizerPath"), config.getValue("NETaggerPath")));
		
	}
	
	public  CityGridInfoProcessor(String configFile, GoodFoodFinder finder) throws Exception{
		config = new Configuration(configFile);
		this.setFinder(finder);
		this.numBusiness = Integer.parseInt(config.getValue("numBusiness"));
//		this.apiPrefix = config.getValue("apiPrefix");
		this.apiPrefixPlace = config.getValue("apiPrefixPlace") + this.numBusiness;
		this.apiPrefixPlaceDetail = config.getValue("apiPrefixPlaceDetail");
		this.apiPrefixReviewDetail = config.getValue("apiPrefixReviewDetail");
		this.setJsonParser(new JsonParser());
//		this.setBizs(new ArrayList<Business>());		
	}
	
//	public CityGridInfoProcessor(String configFile, GoodFoodFinder finder, DBConnector dbconnector){
//		config = new Configuration(configFile);
//		this.setFinder(finder);
//		this.numBusiness = Integer.parseInt(config.getValue("numBusiness"));
//		this.apiPrefix = config.getValue("apiPrefix");
//		this.apiPrefixPlace = config.getValue("apiPrefixPlace") + this.numBusiness;
//		this.apiPrefixPlaceDetail = config.getValue("apiPrefixPlaceDetail");
//		this.apiPrefixReviewDetail = config.getValue("apiPrefixReviewDetail");
//		this.setJsonParser(new JsonParser());
//		this.setBizs(new ArrayList<Business>());	
//		this.dbconnector = dbconnector;
//	}
	
	/**
	 * add info from Json to Biz obj
	 * */
	public void addInfo2Biz(JsonObject jobj, Business biz){
		//use .toString() here to capture null; need to use .getAsString()?
		biz.setLatitude(jobj.get("latitude").toString().trim());
		biz.setLongitude(jobj.get("longitude").toString().trim());
		biz.setBusiness_id(jobj.get("id").toString().trim());
		biz.setBusiness_name(jobj.get("name").toString().trim().replace("\"", ""));
//		biz.setBusiness_address(jobj.get("address").toString().trim().replace("\"", ""));
		biz.setBusiness_address(cleanAddr(jobj.get("address").toString()));		
		biz.setBusiness_phone(jobj.get("phone_number").toString().trim().replace("\"", ""));
		biz.setBusiness_merchantMsg(jobj.get("profile").toString().trim());
		biz.setBusiness_offer(jobj.get("offers").toString().trim());
		if(jobj.get("user_review_count") != null){
			biz.setNumReviews(jobj.get("user_review_count").getAsInt());
		}		
//		System.out.println(jobj.get("rating").toString());
		biz.setRating(jobj.get("rating").toString().trim());
		biz.setWebsite(jobj.get("website").toString().trim().replace("\"", ""));
		biz.setLink(jobj.get("profile").toString().trim().replace("\"", ""));
		biz.setDataSource(1);
	}
	
	public synchronized List<Business> fetchPlaces(String lat, String lon){
//	public List<Business> fetchPlaces(String lat, String lon){
		//use java URL instead of HtmlUnit to save memory
		//use json here; an xml based version may be needed
		long startTime = System.currentTimeMillis();
		Connection dbconn = null;
		PreparedStatement psSelectBiz = null;
		PreparedStatement psInsertBiz = null;
		String apiStr = this.getApiPrefixPlace() + "&lat=" + lat + "&lon=" + lon;
		List<Business> bizs = new ArrayList<Business>();
		System.out.println("fetching CityGrid PLACEs from:\t");
		System.out.println(apiStr);		
		try {
			dbconn = GoodFoodServlet.DS.getConnection();
			psSelectBiz = dbconn.prepareStatement(this.SELECT_biz);
			psInsertBiz = dbconn.prepareStatement(this.INSERT_biz);
			URL url = new URL(apiStr);
			HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();
			InputStream is = urlconn.getInputStream();			
			JsonReader reader = new JsonReader(new InputStreamReader(is));
			JsonObject jobj= jsonParser.parse(reader).getAsJsonObject();
			DBConnector.close(is);
			JsonArray locations = jobj.getAsJsonObject("results").getAsJsonArray("locations");
//			System.out.println(locations.size());
			
			for(JsonElement location : locations){
				JsonObject loc = (JsonObject)location;
				Business biz = new Business();
				this.addInfo2Biz(loc, biz);
//				if(this.getConn() != null && !this.isBizInDB(biz)){
				if(!this.isBizInDB(biz, dbconn, psSelectBiz)){
					this.addBiz2DB(biz, dbconn, psInsertBiz);
				}				
//				System.out.println(loc.get("latitude").getAsString());
				bizs.add(biz);
			}			
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			DBConnector.close(dbconn);
			DBConnector.close(psSelectBiz);
			DBConnector.close(psInsertBiz);
			
		}
		
		
		long endTime = System.currentTimeMillis();
		System.out.println("PLACES fetching cost:\t" + (endTime - startTime));
//		this.setBizs(bizs);
		return bizs;
	}
	
//	public boolean isBizInDB(Business biz, Connection conn, PreparedStatement ps){
//		Boolean flag = false;
////		Connection conn = null;
////		PreparedStatement ps =null;
//		ResultSet rs = null;
//		try {
////			conn = GoodFoodServlet.ds.getConnection();
////			ps = this.getConn().prepareStatement(this.SELECT_biz);
////			ps = conn.prepareStatement(this.SELECT_biz);
//			ps.setString(1, biz.getBusiness_id());
//			ps.setInt(2, 1);//1 means CityGrid
//			rs = ps.executeQuery();
//			if(rs.isAfterLast() == rs.isBeforeFirst()){
//				flag = false;
//			}else{
//				flag = true;
//			}
//			rs.close();
////			ps.close();
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} finally {
//			DBConnector.close(rs);
////			close(ps);
////			close(conn);
//		}
//		
//		return flag;
//	}
	
//	/**
//	 * store business info to DB, as cache
//	 * */
//	public void addBiz2DB(Business biz, Connection conn, PreparedStatement ps){
////		System.out.println("calling this method!!!");
////		Connection conn = null;
////		PreparedStatement ps =null;
//		try {
////			conn = GoodFoodServlet.ds.getConnection();
////			ps = this.getConn().prepareStatement(this.INSERT_biz);
////			ps = conn.prepareStatement(this.INSERT_biz);
//			ps.setString(1, biz.getBusiness_name());
//			ps.setString(2, biz.getBusiness_address());
//			ps.setString(3, biz.getBusiness_id());
////			System.out.println(new BigDecimal(biz.getBusiness_latitude()));
////			System.out.println(biz.getBusiness_latitude());
//			ps.setBigDecimal(4, new BigDecimal(biz.getBusiness_latitude()));
//			ps.setBigDecimal(5, new BigDecimal(biz.getBusiness_longitude()));
//			ps.setString(6, biz.getBusiness_phone());
//			ps.setString(7, biz.getBusiness_merchantMsg());
//			ps.setString(8, biz.getBusiness_offer());
//			ps.setInt(9, 0);//TODO:subject to change to real number
//			ps.setString(10, biz.getLink());
//			ps.setString(11, biz.getWebsite());
//			ps.setInt(12, 1);
//			ps.setTimestamp(13, new Timestamp(System.currentTimeMillis()));			
//			ps .executeUpdate();
//		} catch (SQLException e1) {
//			e1.printStackTrace();
//		} finally {
////			close(ps);
////			close(conn);
//		}
//
//	}
		

	
	/**
	 * collect all the reviews for each business
	 * */
//	public synchronized List<Business> fetchReviews(List<Business> bizs){
	public List<Business> fetchReviews(List<Business> bizs){
		Connection dbconn = null;
		PreparedStatement psSelectReview = null;
		PreparedStatement psLookupReview = null;
		PreparedStatement psInsertReview = null;
		long start;
		long end;
		try {
			dbconn = GoodFoodServlet.DS.getConnection();
			psSelectReview = dbconn.prepareStatement(this.SELECT_reviews);
//			psLookupReview = dbconn.prepareStatement(this.Lookup_reviews);
			psInsertReview = dbconn.prepareStatement(this.INSERT_reviews);
			for(Business biz : bizs){
				start = System.currentTimeMillis();
//				boolean flag = this.isInReviewTable(biz, dbconn, psLookupReview);
				boolean flag = this.isInReviewTable(biz, dbconn, psSelectReview);
				end = System.currentTimeMillis();
//				System.out.println("------------------------------------------------>");
//				System.out.println("time for judeg based on review table:\t" + (end - start));
				if(flag == true){
					start = System.currentTimeMillis();
					fetchReviewsFromDB(biz, dbconn, psSelectReview);//read cache
					end = System.currentTimeMillis();
					System.out.println("fetch reviews from DB:\t" + (end - start));
				}else{
					//no records in DB
					start = System.currentTimeMillis();
					this.fetchReviewsFromAPI(biz);
					this.addReviews2DB(biz, dbconn, psInsertReview);//add to cache
					end = System.currentTimeMillis();
					System.out.println("fetch reviews from API:\t" + (end - start));
				}			
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
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
		String apiStr = this.getApiPrefixReviewDetail() + id;
		long time = 0;
		int length = 0;
//		System.out.println("fetching reviews from:\n" + apiStr);
		try {
			URL url = new URL(apiStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			InputStream is = conn.getInputStream();			
			JsonReader reader = new JsonReader(new InputStreamReader(is));
			JsonObject jobj= jsonParser.parse(reader).getAsJsonObject();
			is.close();
			JsonObject results = jobj.getAsJsonObject("results");
			JsonArray bizReviews = results.getAsJsonArray("reviews");
//			System.out.println(bizReviews.toString());
			for(JsonElement rJelem : bizReviews){
				JsonObject robj = (JsonObject) rJelem;
				String rStr = robj.get("review_text").toString().trim();
				String rLink = robj.get("review_url").toString().trim().replace("\"", "");					
				long start = System.currentTimeMillis();
				Review r = this.getFinder().process(rStr);//call NLP tools
//				System.out.println("++API+++++:\t" + rStr);
				long end = System.currentTimeMillis();
//				System.out.println("Generate a review:\t" + ( endTime - startTime));
				r.setWebLink(rLink);
				r.setDataSource(1);//hardcode
				biz.getReviews().add(r);
				time += (end-start);
				length += rStr.length();
//				System.out.println(biz.getReviews().size());
			}
//			System.out.println(biz.getReviews().size());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
//		System.out.println("---------------->");
//		System.out.println("length of review:\t" + length);
//		System.out.println("NER from API:\t" + time);
//		System.out.println("fetch reviews from API:\t" + (endTime - startTime));
		return biz;
	}
	
//	/**
//	 * no ML or NLP operation here
//	 * only DB IO
//	 * */
//	public Business fetchReviewsFromDB(Business biz, Connection conn, PreparedStatement ps){
//		String rStr;
//		String rLink;
//		String NEStr;
//		String taggedText;
//		long time = 0;
//		int length = 0;
////		Connection conn = null;
//		ResultSet rs = null;
////		PreparedStatement ps = null;		
//		
//		try {
//			long startTime = System.currentTimeMillis();
////			conn = GoodFoodServlet.ds.getConnection();
////			ps = conn.prepareStatement(this.SELECT_reviews);
//			ps.setString(1, biz.getBusiness_id());
//			ps.setInt(2, 1);
//			rs = ps.executeQuery();
//			while(rs.next()){
//				long start = System.currentTimeMillis();
//				rStr = rs.getString("text");
//				rLink = rs.getString("rLink");
//				NEStr = rs.getString("food");
//				taggedText = rs.getString("taggedText");				
//				Review r = new Review(rStr, taggedText, NEStr);
////				System.out.println("--DB-----:\t" + rStr);						
////				System.out.println("Generate a review:\t" + ( endTime - startTime));
//				r.setWebLink(rLink);
//				biz.getReviews().add(r);
//				long end = System.currentTimeMillis();		
//				time += (end-start);
//				length += rStr.length();
//			}
//			long endTime = System.currentTimeMillis();
////			System.out.println("------------------------------------------------>");
////			System.out.println("length of review:\t" + length);
////			System.out.println("NER from DB time:\t" + time);
////			System.out.println("fetch reviews from DB:\t" + (endTime - startTime));
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} finally {
//			DBConnector.close(rs);
////			close(ps);
////			close(conn);
//		}
//		return biz;
//	}
	
//	@Deprecated
//	public Business fetchReviewsFromDB2(Business biz, Connection conn, PreparedStatement ps){
//		String rStr;
//		String rLink;
//		long time = 0;
//		int length = 0;
//		ResultSet rs = null;		
//		
//		try {
//			long startTime = System.currentTimeMillis();
//			ps.setString(1, biz.getBusiness_id());
//			ps.setInt(2, 1);
//			rs = ps.executeQuery();
//			while(rs.next()){
//				rStr = rs.getString("text");
//				rLink = rs.getString("rLink");
//				long start = System.currentTimeMillis();
//				Review r = this.getFinder().process(rStr);
//				long end = System.currentTimeMillis();
//				r.setWebLink(rLink);
//				biz.getReviews().add(r);
//				time += (end-start);
//				length += rStr.length();
//			}
//			long endTime = System.currentTimeMillis();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		} finally {
//			DBConnector.close(rs);
//		}
//		return biz;
//	}
	
//	/**
//	 * store business reviews to DB, as cache
//	 * */
//	public void addReviews2DB(Business biz, Connection conn, PreparedStatement ps){
////		System.out.println("calling this method!!!");
////		Connection conn = null;
////		PreparedStatement ps =null;		
//		try {
////			conn = GoodFoodServlet.ds.getConnection();
////			ps = conn.prepareStatement(this.INSERT_reviews);
//			List<Review> reviews = biz.getReviews();
//			for(Review r : reviews){
//				ps.setInt(1, 0);//how to get biz id?
//				ps.setString(2, biz.getBusiness_id());
//				ps.setString(3, r.getReviewStr());
//				ps.setString(4, r.getWebLink());
//				ps.setString(5, r.getTaggedStr());//later, will be not null
//				ps.setString(6, r.getNEStr());//later, will be not null
//				ps.setInt(7, 1);
//				ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
//				ps.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
////				ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));		
//				ps.executeUpdate();
//			}			
//		} catch (SQLException e) {
//			e.printStackTrace();
//		} finally{
////			close(ps);
////			close(conn);
//		}		
//	}
	

	
//	public boolean isInReviewTable(Business biz, Connection conn, PreparedStatement ps){
//		Boolean flag = false;
//		ResultSet rs = null;
////		PreparedStatement ps = null;
////		Connection conn = null;
//		try {
////			conn = GoodFoodServlet.ds.getConnection();
////			ps = conn.prepareStatement(this.SELECT_reviews);
//			ps.setString(1, biz.getBusiness_id());
//			ps.setInt(2, 1);
//			rs = ps.executeQuery();
//			flag = rs.isAfterLast() == rs.isBeforeFirst()? false : true;
//		} catch (SQLException e) {
//			e.printStackTrace();
//		} finally {
//			DBConnector.close(rs);
////			close(ps);
////			close(conn);
//		}
//		
//		return flag;
//	}
	

	public static int getNumBusiness() {
		return numBusiness;
	}

	public static void setNumBusiness(int numBusiness) {
		CityGridInfoProcessor.numBusiness = numBusiness;
	}

//	public static String getApiPrefix() {
//		return apiPrefix;
//	}

//	public static void setApiPrefix(String apiPrefix) {
//		CityGridInfoProcessor.apiPrefix = apiPrefix;
//	}

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


	public String getApiPrefixPlaceDetail() {
		return apiPrefixPlaceDetail;
	}

	public void setApiPrefixPlaceDetail(String apiPrefixPlaceDetail) {
		this.apiPrefixPlaceDetail = apiPrefixPlaceDetail;
	}


	public String getApiPrefixReviewDetail() {
		return apiPrefixReviewDetail;
	}

	public void setApiPrefixReviewDetail(String apiPrefixReviewDetail) {
		this.apiPrefixReviewDetail = apiPrefixReviewDetail;
	}

	public GoodFoodFinder getFinder() {
		return finder;
	}

	public void setFinder(GoodFoodFinder finder) {
		this.finder = finder;
	}
	
//	public final DBConnector getDbconnector() {
//		return dbconnector;
//	}

//	public final void setDbconnector(DBConnector dbconnector) {
//		this.dbconnector = dbconnector;
//	}

//	public final Connection getConn() {
//		return conn;
//	}
//
//	public final void setConn(Connection conn) {
//		this.conn = conn;
//	}

	/**
	 * remove the strang symbols and normalize address string
	 * */
	public String cleanAddr(String origAddr){
		if(origAddr == null || origAddr.length() == 0){
			return "";
		}
		
		JsonObject jObj= jsonParser.parse(origAddr).getAsJsonObject();
		StringBuilder sb = new StringBuilder();
		Set<Entry<String, JsonElement>> entries = jObj.entrySet();
		int i = 0;
		for(Entry<String, JsonElement> entry : entries){
			if(i == 0){
				sb.append(entry.getValue().getAsString().toString());
			}else{
				sb.append(", " + entry.getValue().getAsString().toString());
			}			
			i++;
		}
//		System.out.println(sb.toString());
		return sb.toString();
	}
	
	@Deprecated
	public String cleanAddr2(String origAddr){
		
		if(origAddr == null || origAddr.length() == 0){
			return "";
		}
		StringBuilder sb = new StringBuilder();		
		origAddr = origAddr.trim().replace("\"", "");
		origAddr = origAddr.trim().replace("{", "");
		origAddr = origAddr.trim().replace("}", "");
		String[] strSplits = origAddr.split(",");
		for(int i = 0; i < strSplits.length; i++){
			String[] splitTmp = strSplits[i].split(":");
			String realAddr;
			if(splitTmp.length == 0){
				realAddr = splitTmp[0];
			}else{
				realAddr = splitTmp[1];
			}
			if(i == 0){
				sb.append(realAddr);
			}else{
				sb.append(", " + realAddr);
			}			
		}
		return sb.toString();
	}
	
	public void clearHistoryData(){
		
	}
	
	public Configuration getConfig() {
		return config;
	}

	public void setConfig(Configuration config) {
		this.config = config;
	}


	public static void main(String[] args){
		if(args.length < 1){
			System.out.println("no property file input!!!");
			System.exit(0);
		}
		CityGridInfoProcessor processor = null;
		try {
			processor = new CityGridInfoProcessor(args[0]);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		processor.clearHistoryData();
		String lat = "34.10652";
		String lon = "-118.411509";
		List<Business> bizsTmp = processor.fetchPlaces(lat, lon);
		List<Business> bizs = processor.fetchReviews(bizsTmp);
		for(Business biz : bizs){
			biz.extractInfoFromReviews();
			for(Food f : biz.getGoodFoods()){
				System.out.print(f.getFoodText() + "\t");
			}
//			System.out.println(biz.getGoodFoods().size());
		}
	}

}
