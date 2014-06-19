package wd.goodFood.serverSide;

import java.io.BufferedReader;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

public class GoogleInfoProcessor extends DataSourceProcessor{
	private Configuration config;
	private static int numBusiness;//how many business results should be returned per request.
	private String apiPrefixPlace;	
	//for fetcheding detail for each place, but only 3 reviews are returned for each place
	private String apiPrefixReview;	
	private String apiSurfixReview;	
	private JsonParser jsonParser;//should be thread safe
	private GoodFoodFinder finder;
	
	String INSERT_biz = "INSERT INTO goodfoodDB.goodfood_biz_Google "
			+ "(bizName, address, bizSrcID, latitude, longitude, phoneNum, merchantMsg, offer, numReviews, profileLink, bizWebsite, dataSource, updateTime, category, note) VALUES"
			+ "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
	String INSERT_reviews = "INSERT INTO goodfoodDB.goodfood_reviews_Google "
			+ "(bizID, bizSrcID, text, rLink, taggedText, food, dataSource, insertTime, updateTime) VALUES"
			+ "(?,?,?,?,?,?,?,?,?)";
	
	String SELECT_biz = "SELECT * FROM goodfoodDB.goodfood_biz_Google WHERE bizSrcID = ? AND dataSource = ?";
		
	String SELECT_reviews = "SELECT text,rLink,taggedText,food,dataSource FROM goodfoodDB.goodfood_reviews_Google WHERE bizSrcID = ? AND dataSource = ?";

	
	public  GoogleInfoProcessor(String configFile, GoodFoodFinder finder) throws Exception{
		config = new Configuration(configFile);
		this.setFinder(finder);
		this.numBusiness = Integer.parseInt(config.getValue("numBusiness_Google"));
		this.apiPrefixPlace = config.getValue("apiPrefixPlace_Google");
		this.apiPrefixReview = config.getValue("apiPrefixReview_Google");
		this.apiSurfixReview = config.getValue("apiSurfixReview_Google");
		this.setJsonParser(new JsonParser());
	}

	@Deprecated
	public  GoogleInfoProcessor(String configFile) throws Exception{
		config = new Configuration(configFile);
		this.numBusiness = Integer.parseInt(config.getValue("numBusiness_Google"));
		this.apiPrefixPlace = config.getValue("apiPrefixPlace_Google");
		this.apiPrefixReview = config.getValue("apiPrefixReview_Google");
		this.apiSurfixReview = config.getValue("apiSurfixReview_Google");
		this.setJsonParser(new JsonParser());
		this.setFinder(new GoodFoodFinder(config.getValue("sentSplitterPath"), config.getValue("tokenizerPath"), config.getValue("NETaggerPath")));
		
	}	
	
	/**
	 * particularly for Google
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
	 * add info from Json to Biz obj
	 * */
	public void addInfo2Biz(JsonObject jobj, Business biz){
		JsonElement eTmp = null;
		JsonObject oTmp = null;
		
		oTmp = jobj.getAsJsonObject("geometry").getAsJsonObject("location");
		if(oTmp != null){
			biz.setLatitude(oTmp.get("lat").getAsString());
			biz.setLongitude(oTmp.get("lng").getAsString());		
		}else{
			//no lat and lng
			
		}
		
		biz.setBusiness_id(jobj.get("id").getAsString());//don't use this for fetching review
		biz.setBusiness_name(jobj.get("name").getAsString());
		if(jobj.get("formatted_address") != null){
			biz.setBusiness_address(jobj.get("formatted_address").getAsString());
		}else{
			biz.setBusiness_address("undisclosed");
		}
		
		eTmp = jobj.get("rating");
		if(eTmp != null){
			biz.setRating(eTmp.getAsString());
		}
		
		biz.setNote(jobj.get("reference").getAsString());//use this for next review fetching
		biz.setDataSource(4);
		
		
		JsonArray arrayTmp = jobj.getAsJsonArray("types");
		if(arrayTmp != null){
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < arrayTmp.size(); i++){
				sb.append(arrayTmp.get(i).getAsString() + ",");
			}
			biz.setCategory(sb.toString());
		}
		
	}
	
	public List<Business> fetchPlaces(String lat, String lon){
		//use json here; an xml based version may be needed
		long startTime = System.currentTimeMillis();
		Connection dbconn = null;
		PreparedStatement psSelectBiz = null;
		PreparedStatement psInsertBiz = null;
		String apiStr = this.getApiPrefixPlace() + "&location=" + lat + "," + lon;//TODO: sharp the category tree
		List<Business> bizs = new ArrayList<Business>();
//		System.out.println("fetching PLACEs from:\t");
//		System.out.println(apiStr);		
		try {
			dbconn = GoodFoodServlet.DS.getConnection();
			psSelectBiz = dbconn.prepareStatement(this.SELECT_biz);
			psInsertBiz = dbconn.prepareStatement(this.INSERT_biz);
			URL url = new URL(apiStr);
			HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();
			InputStream is = urlconn.getInputStream();	
//			BufferedReader br = new BufferedReader(new InputStreamReader(is));
//			String line;
//			while((line = br.readLine()) != null){
//				System.out.println(line);
//			}
//			System.exit(0);
			
			JsonReader reader = new JsonReader(new InputStreamReader(is));
			JsonObject jobj= jsonParser.parse(reader).getAsJsonObject();
			DBConnector.close(is);
			JsonArray locations = jobj.getAsJsonArray("results");
//			System.out.println("number of locations:\t" + locations.size());
			
			for(JsonElement location : locations){
				JsonObject loc = (JsonObject)location;
				Business biz = new Business();
				this.addInfo2Biz(loc, biz);
				bizs.add(biz);				
			}			
			
			fetchGoogleReviews(bizs);
			
			for(Business biz : bizs){
				if(this.isBizInDB(biz, dbconn, psSelectBiz)){
					//if biz in db, complement
					this.fetchBizFromDB(biz, dbconn, psSelectBiz);					
				}else{
					this.addBiz2DB(biz, dbconn, psInsertBiz);
				}
			}
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBConnector.close(dbconn);
			DBConnector.close(psSelectBiz);
			DBConnector.close(psInsertBiz);
		}		
		
		long endTime = System.currentTimeMillis();
//		System.out.println("time fetch PLACES from API:\t" + (endTime - startTime));
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
			ps.setInt(9, biz.getNumReviews());
			ps.setString(10, biz.getLink());
			ps.setString(11, biz.getWebsite());
			ps.setInt(12, 4);
			ps.setTimestamp(13, new Timestamp(System.currentTimeMillis()));
			ps.setString(14, biz.getCategory());
			ps.setString(15, biz.getNote());//for fetching review
			ps .executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally {
//			close(ps);
//			close(conn);
		}
	}
	
	/**
	 * mainly for complementing phone and restaurant url
	 * */
	public void fetchBizFromDB(Business biz, Connection conn, PreparedStatement ps){

		long time = 0;
		int length = 0;
		ResultSet rs = null;		
		
		try {
			ps.setString(1, biz.getBusiness_id());
			ps.setInt(2, 4);//4 is for Google places. TODO: change to non-hardcode->biz.getDataSource()
			
			rs = ps.executeQuery();
			while(rs.next()){
				String phone = rs.getString("phoneNum");
				String link = rs.getString("profileLink");
				biz.setBusiness_phone(phone);
				biz.setLink(link);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			DBConnector.close(rs);
		}	
		
		
	}
	
	/**
	 * collect all the reviews for each business
	 * */
//	public synchronized List<Business> fetchReviews(List<Business> bizs){
	public List<Business> fetchGoogleReviews(List<Business> bizs){
		Connection dbconn = null;
		PreparedStatement psSelectReview = null;
		PreparedStatement psLookupReview = null;
		PreparedStatement psInsertReview = null;
		long start;
		long end;
		try {
			dbconn = GoodFoodServlet.DS.getConnection();
//			dbconn = null;
			psSelectReview = dbconn.prepareStatement(this.SELECT_reviews);
//			psSelectReview = null;
			psInsertReview = dbconn.prepareStatement(this.INSERT_reviews);
//			psInsertReview = null;
			for(Business biz : bizs){
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
					start = System.currentTimeMillis();
					this.fetchReviewsFromAPI(biz);
					this.addReviews2DB(biz, dbconn, psInsertReview);//add to cache
					end = System.currentTimeMillis();
//					System.out.println("fetch reviews from API:\t" + (end - start));
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
	
	public List<Business> fetchReviews(List<Business> bizs){
		//dummy, since reviews are fetched already when fetching places
		return bizs;
	}
	
	public Business fetchReviewsFromAPI(Business biz){
		long startTime = System.currentTimeMillis();
		String id = biz.getBusiness_id();
		String apiStr = this.getApiPrefixReview() + "&reference=" + biz.getNote();//use reference but id for Google places
		long time = 0;
		int length = 0;
		JsonElement eTmp = null;
		JsonObject oTmp = null;
		try {
			URL url = new URL(apiStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			InputStream is = conn.getInputStream();
//			BufferedReader br = new BufferedReader(new InputStreamReader(is));
//			String line;
//			while((line = br.readLine()) != null){
//				System.out.println(line);
//			}
//			System.exit(0);
			
			JsonReader reader = new JsonReader(new InputStreamReader(is));
			JsonObject jobj= jsonParser.parse(reader).getAsJsonObject();
			is.close();
			
			JsonObject result = jobj.getAsJsonObject("result");
			eTmp = result.get("formatted_phone_number");
			if(eTmp != null){
				biz.setBusiness_phone(eTmp.getAsString());
			}
			eTmp = result.get("url");
			if(eTmp != null){
				biz.setLink(eTmp.getAsString());
			}
			
			JsonArray bizReviews = result.getAsJsonArray("reviews");
			if(bizReviews != null){
//				System.out.println(bizReviews.size() + "\t reviews");
				for(JsonElement jElem : bizReviews){
					JsonObject robj = (JsonObject) jElem;
					String rStr = robj.get("text").toString();
					eTmp = robj.get("author_url");
					String rLink = null;
					if(eTmp != null){
						rLink = eTmp.getAsString();
					}					
					
					long start = System.currentTimeMillis();
//					System.out.println(rStr);
					Review r = this.getFinder().process(rStr);//call NLP tools
					
					long end = System.currentTimeMillis();
//					System.out.println("Generate a review:\t" + ( endTime - startTime));
					r.setWebLink(rLink);
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
		
		try {
			long startTime = System.currentTimeMillis();
			ps.setString(1, biz.getBusiness_id());
			ps.setInt(2, 4);//4 is for Google places. TODO: change to non-hardcode->biz.getDataSource()
			rs = ps.executeQuery();
			while(rs.next()){
				long start = System.currentTimeMillis();
//				rStr = rs.getString("text");
				rStr = rs.getString("text") + " (@ via Google Places)";//subject to change
				rLink = rs.getString("rLink");
				NEStr = rs.getString("food");
				taggedText = rs.getString("taggedText");				
				Review r = new Review(rStr, taggedText, NEStr);
//				System.out.println("--DB-----:\t" + rStr);						
//				System.out.println("Generate a review:\t" + ( endTime - startTime));
				r.setWebLink(rLink);
				int dataSource = rs.getInt("dataSource");
				r.setDataSource(dataSource);
				biz.getReviews().add(r);
				
				long end = System.currentTimeMillis();		
				time += (end-start);
				length += rStr.length();
			}
			long endTime = System.currentTimeMillis();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			DBConnector.close(rs);
//			close(ps);
//			close(conn);
		}
		return biz;
	}
	
	/**
	 * store business reviews to DB, as cache
	 * */
	public void addReviews2DB(Business biz, Connection conn, PreparedStatement ps){
		try {
			List<Review> reviews = biz.getReviews();
			for(Review r : reviews){
				ps.setInt(1, 0);//how to get biz id?
				ps.setString(2, biz.getBusiness_id());
				ps.setString(3, r.getReviewStr());
				ps.setString(4, r.getWebLink());
				ps.setString(5, r.getTaggedStr());//later, will be not null
				ps.setString(6, r.getNEStr());//later, will be not null
				ps.setInt(7, 4);//hardcode here. TODO: use r.getDataSource()
				ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
				ps.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
//				ps.setTimestamp(10, new Timestamp(System.currentTimeMillis()));		
				ps.executeUpdate();
			}			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
//			close(ps);
//			close(conn);
		}		
	}

	public static int getNumBusiness() {
		return numBusiness;
	}

	public static void setNumBusiness(int numBusiness) {
		GoogleInfoProcessor.numBusiness = numBusiness;
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
		GoogleInfoProcessor processor = null;
		try {
			processor = new GoogleInfoProcessor(args[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(processor.apiSurfixReview);
		processor.clearHistoryData();
		String lat = "40.669800";
		String lon = "-73.943849";
		List<Business> bizs = processor.fetchPlaces(lat, lon);
		System.out.println(bizs.size());
//		System.exit(0);
//		bizs = processor.fetchReviews(bizs);
		
		for(Business biz : bizs){
			processor.fetchReviewsFromAPI(biz);
//			System.out.println(biz.getBusiness_address());
//			biz.extractInfoFromReviews();
//			for(Food f : biz.getGoodFoods()){
//				System.out.print(f.getFoodText() + "\t");
//			}
//			System.out.println(biz.getGoodFoods().size());
		}
	}

}
