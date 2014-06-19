package wd.goodFood.utils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;



public class DBConnector {

	private Connection conn = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private PreparedStatement psSelect_biz = null;
	private PreparedStatement psInsert_biz = null;
	private PreparedStatement psUpdate_biz = null;
	private PreparedStatement psIsInDB_biz = null;
	
	private PreparedStatement psSelect_reviews = null;
	private PreparedStatement psInsert_reviews = null;
	private PreparedStatement psUpdate_reviews = null;
	private PreparedStatement psIsInDB_review = null;
	
	private ResultSet resultSet = null;

	
	public DBConnector(Connection connection){
//		System.out.println(connection.toString());
		this.conn = connection;
		this.setupPS();
	}
	
	public void setupPS(){
		try {
			String strInsert_biz = "INSERT INTO goodfoodDB.goodfood_biz "
					+ "(bizName, address, bizSrcID, latitude, longitude, phoneNum, merchantMsg, offer, numReviews, profileLink, bizWebsite, dataSource, updateTime) VALUES"
					+ "(?,?,?,?,?,?,?,?,?,?,?,?,?)";
			this.psInsert_biz = this.conn.prepareStatement(strInsert_biz);
			
			String strInsert_reviews = "INSERT INTO goodfoodDB.goodfood_reviews "
					+ "(bizID, bizSrcID, text, rLink, taggedText, food, dataSource, insertTime, updateTime) VALUES"
					+ "(?,?,?,?,?,?,?,?,?)";
			this.psInsert_reviews = this.conn.prepareStatement(strInsert_reviews);
			
			String strSelect_biz = "SELECT * FROM goodfoodDB.goodfood_biz WHERE bizSrcID = ? AND dataSource = ?";
			this.psSelect_biz = this.conn.prepareStatement(strSelect_biz);
			
			String strSelect_reviews = "SELECT * FROM goodfoodDB.goodfood_reviews WHERE bizSrcID = ? AND dataSource = ?";
			this.psSelect_reviews = this.conn.prepareStatement(strSelect_reviews);
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void testPreparedStatment(){
		String psStr = "INSERT INTO goodfoodDB.goodfood_biz "
				+ "(bizName, address, bizSrcID, latitude, longitude, phoneNum, merchantMsg, offer, numReviews, profileLink, bizWebsite, dataSource, updateTime) VALUES"
				+ "(?,?,?,?,?,?,?,?,?,?,?,?,?)";
		try {
			preparedStatement = this.conn.prepareStatement(psStr);
			preparedStatement.setString(1, "laobeifang");
			preparedStatement.setString(2, "Broadway Ave");
			preparedStatement.setString(3, "97698768");
			preparedStatement.setBigDecimal(4, new BigDecimal(34.3462435));
			preparedStatement.setBigDecimal(5, new BigDecimal(123.45687089));
			preparedStatement.setString(6, "206-724-7389");
			preparedStatement.setString(7, "no merchant");
			preparedStatement.setString(8, "buy one get on free");
			preparedStatement.setInt(9, 0);
			preparedStatement.setString(10, "http://www.yelp.com");
			preparedStatement.setString(11, "http://citygrid");
			preparedStatement.setInt(12, 1);
			preparedStatement.setTimestamp(13, new Timestamp(System.currentTimeMillis()));
			
			preparedStatement .executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		

	}
	
	
	public void closeAll() {
		try{
			if (resultSet != null) {
				resultSet.close();
		    }
			if(this.psInsert_biz != null){
				this.psInsert_biz.close();
			}
			if(this.psInsert_reviews != null){
				this.psInsert_reviews.close();
			}
			if(this.psSelect_biz != null){
				this.psSelect_biz.close();
			}
			if(this.psSelect_reviews != null){
				this.psSelect_reviews.close();
			}
			if (statement != null) {
				statement.close();
		    }
			if (this.conn != null) {
				this.conn.close();
		    }
		} catch (Exception e) {
			
		} finally{
			
		}
	}
	
	
	
	public Connection getConn() {
		return conn;
	}

	public void setConn(Connection conn) {
		this.conn = conn;
	}

	public Statement getStatement() {
		return statement;
	}

	public void setStatement(Statement statement) {
		this.statement = statement;
	}

	public PreparedStatement getPreparedStatement() {
		return preparedStatement;
	}

	public void setPreparedStatement(PreparedStatement preparedStatement) {
		this.preparedStatement = preparedStatement;
	}

	public PreparedStatement getPsSelect_biz() {
		return psSelect_biz;
	}

	public void setPsSelect_biz(PreparedStatement psSelect_biz) {
		this.psSelect_biz = psSelect_biz;
	}

	public PreparedStatement getPsInsert_biz() {
		return psInsert_biz;
	}

	public void setPsInsert_biz(PreparedStatement psInsert_biz) {
		this.psInsert_biz = psInsert_biz;
	}

	public PreparedStatement getPsUpdate_biz() {
		return psUpdate_biz;
	}

	public void setPsUpdate_biz(PreparedStatement psUpdate_biz) {
		this.psUpdate_biz = psUpdate_biz;
	}

	public PreparedStatement getPsSelect_reviews() {
		return psSelect_reviews;
	}

	public void setPsSelect_reviews(PreparedStatement psSelect_reviews) {
		this.psSelect_reviews = psSelect_reviews;
	}

	public PreparedStatement getPsInsert_reviews() {
		return psInsert_reviews;
	}

	public void setPsInsert_reviews(PreparedStatement psInsert_reviews) {
		this.psInsert_reviews = psInsert_reviews;
	}

	public PreparedStatement getPsUpdate_reviews() {
		return psUpdate_reviews;
	}

	public void setPsUpdate_reviews(PreparedStatement psUpdate_reviews) {
		this.psUpdate_reviews = psUpdate_reviews;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public void setResultSet(ResultSet resultSet) {
		this.resultSet = resultSet;
	}

	public static void close(PreparedStatement ps){
		try {			
			if(ps != null){
				ps.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void close(ResultSet rs){
		try {			
			if(rs != null){
				rs.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void close(Connection conn){
		try {			
			if(conn != null){
				conn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void close(HttpURLConnection conn){
		if(conn != null){
			conn.disconnect();
		}
	}
	
	public static void close(InputStream is){
		if(is != null) {
			try {
				is.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
//		DBConnector goodfoodDB = new DBConnector("199.168.136.80:3306", "lingandcs", "sduonline", "goodfoodDB");
//		goodfoodDB.testPreparedStatment();
//		System.out.println("test is done!");
	}

}
