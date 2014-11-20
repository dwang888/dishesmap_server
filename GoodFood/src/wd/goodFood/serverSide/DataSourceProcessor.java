package wd.goodFood.serverSide;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import wd.goodFood.entity.Business;
import wd.goodFood.entity.Review;
import wd.goodFood.utils.DBConnector;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class DataSourceProcessor implements InfoProcessor{

	private String dbTableName = "";
//	/**
//	 * add info from Json to Biz obj
//	 * */
//	public void addInfo2Biz(JsonObject jobj, Business biz){
//		//use .toString() here to capture null; need to use .getAsString()?
//		biz.setBusiness_latitude(jobj.get("latitude").toString().trim());
//		biz.setBusiness_longitude(jobj.get("longitude").toString().trim());
//		biz.setBusiness_id(jobj.get("id").toString().trim());
//		biz.setBusiness_name(jobj.get("name").toString().trim().replace("\"", ""));
////		biz.setBusiness_address(jobj.get("address").toString().trim().replace("\"", ""));
//		biz.setBusiness_address(cleanAddr(jobj.get("address").toString()));		
//		biz.setBusiness_phone(jobj.get("phone_number").toString().trim().replace("\"", ""));
//		biz.setBusiness_merchantMsg(jobj.get("profile").toString().trim());
//		biz.setBusiness_offer(jobj.get("offers").toString().trim());
//		biz.setNumReviews(jobj.get("user_review_count").toString().trim());
////		System.out.println(jobj.get("rating").toString());
//		biz.setRating(jobj.get("rating").toString().trim());
//		biz.setWebsite(jobj.get("website").toString().trim().replace("\"", ""));
//		biz.setLink(jobj.get("profile").toString().trim().replace("\"", ""));
//	}
	
	public boolean isBizInDB(Business biz, Connection conn, PreparedStatement ps){
		Boolean flag = false;
//		Connection conn = null;
//		PreparedStatement ps =null;
		ResultSet rs = null;
		try {
//			conn = GoodFoodServlet.ds.getConnection();
//			ps = this.getConn().prepareStatement(this.SELECT_biz);
//			ps = conn.prepareStatement(this.SELECT_biz);
			ps.setString(1, biz.getBusiness_id());
			ps.setInt(2, biz.getDataSource());//1 means CityGrid
			rs = ps.executeQuery();
			if(rs.isAfterLast() == rs.isBeforeFirst()){
				flag = false;
			}else{
				flag = true;
			}
			rs.close();
//			ps.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			DBConnector.close(rs);
//			close(ps);
//			close(conn);
		}
		
		return flag;
	}
	
	/**
	 * store business info to DB, as cache
	 * */
	public void addBiz2DB(Business biz, Connection conn, PreparedStatement ps){
//		System.out.println("calling this method!!!");
//		Connection conn = null;
//		PreparedStatement ps =null;
		try {
//			conn = GoodFoodServlet.ds.getConnection();
//			ps = this.getConn().prepareStatement(this.INSERT_biz);
//			ps = conn.prepareStatement(this.INSERT_biz);
			ps.setString(1, biz.getBusiness_name());
			ps.setString(2, biz.getBusiness_address());
			ps.setString(3, biz.getBusiness_id());
//			System.out.println(new BigDecimal(biz.getBusiness_latitude()));
//			System.out.println(biz.getBusiness_latitude());
			ps.setBigDecimal(4, new BigDecimal(biz.getLatitude()));
			ps.setBigDecimal(5, new BigDecimal(biz.getLongitude()));
			ps.setString(6, biz.getBusiness_phone());
			ps.setString(7, biz.getBusiness_merchantMsg());
			ps.setString(8, biz.getBusiness_offer());
			ps.setInt(9, 0);//TODO:subject to change to real number
			ps.setString(10, biz.getLink());
			ps.setString(11, biz.getWebsite());
			ps.setInt(12, biz.getDataSource());
			ps.setTimestamp(13, new Timestamp(System.currentTimeMillis()));
			ps .executeUpdate();
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally {
//			close(ps);
//			close(conn);
		}

	}
	
	//based on biz, not review TODO: will change to review
	public boolean isInReviewTable(Business biz, Connection conn, PreparedStatement ps){
		Boolean flag = false;
		ResultSet rs = null;
//		PreparedStatement ps = null;
//		Connection conn = null;
		try {
//			conn = GoodFoodServlet.ds.getConnection();
//			ps = conn.prepareStatement(this.SELECT_reviews);
			ps.setString(1, biz.getBusiness_id());
			ps.setInt(2, biz.getDataSource());
			rs = ps.executeQuery();
			flag = rs.isAfterLast() == rs.isBeforeFirst()? false : true;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBConnector.close(rs);
//			close(ps);
//			close(conn);
		}
		
		return flag;
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
//		Connection conn = null;
		ResultSet rs = null;
//		PreparedStatement ps = null;		
		
		try {
			long startTime = System.currentTimeMillis();
//			conn = GoodFoodServlet.ds.getConnection();
//			ps = conn.prepareStatement(this.SELECT_reviews);
			ps.setString(1, biz.getBusiness_id());
			ps.setInt(2, 1);
			rs = ps.executeQuery();
			while(rs.next()){
				long start = System.currentTimeMillis();
//				rStr = rs.getString("text");
				rStr = rs.getString("text");
				rLink = rs.getString("rLink");
				NEStr = rs.getString("food");
				taggedText = rs.getString("taggedText");				
				Review r = new Review(rStr, taggedText, NEStr);
//				System.out.println("--DB-----:\t" + rStr);						
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
//		System.out.println("calling this method!!!");
//		Connection conn = null;
//		PreparedStatement ps =null;		
		try {
//			conn = GoodFoodServlet.ds.getConnection();
//			ps = conn.prepareStatement(this.INSERT_reviews);
			List<Review> reviews = biz.getReviews();
			for(Review r : reviews){
				ps.setInt(1, 0);//how to get biz id?
				ps.setString(2, biz.getBusiness_id());
				ps.setString(3, r.getReviewStr());
				ps.setString(4, r.getWebLink());
				ps.setString(5, r.getTaggedStr());//later, will be not null
				ps.setString(6, r.getNEStr());//later, will be not null
				ps.setInt(7, r.getDataSource());
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
	
	public List<Business> addDBTableName(List<Business> bizs){
		for(Business biz : bizs){
			biz.setBusiness_id(this.dbTableName + "__" + biz.getBusiness_id());
		}
		return bizs;
	}
	
//	/**
//	 * remove the strang symbols and normalize address string
//	 * */
//	public String cleanAddr(String origAddr){
//		if(origAddr == null || origAddr.length() == 0){
//			return "";
//		}
//		
//		JsonObject jObj= jsonParser.parse(origAddr).getAsJsonObject();
//		StringBuilder sb = new StringBuilder();
//		Set<Entry<String, JsonElement>> entries = jObj.entrySet();
//		int i = 0;
//		for(Entry<String, JsonElement> entry : entries){
//			if(i == 0){
//				sb.append(entry.getValue().getAsString().toString());
//			}else{
//				sb.append(", " + entry.getValue().getAsString().toString());
//			}			
//			i++;
//		}
////		System.out.println(sb.toString());
//		return sb.toString();
//	}
//	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Business> fetchPlaces(String lat, String lon) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Business> fetchReviews(List<Business> bizs) {
		// TODO Auto-generated method stub
		return null;
	}


}
