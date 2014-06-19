package wd.goodFood.nlp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.pool.impl.GenericObjectPool;

import wd.goodFood.entity.Review;

/**
 * to annotate data from DB
 * */
public class DBAnnotator {

	/**
	 * select review text from DB and label; update back to DB
	 * */
	
	public static GenericObjectPool pool;
	public static ExecutorService threadPool;
	public static int numRecordProcessed = 0;
	
	public DBAnnotator(){
		threadPool = Executors.newFixedThreadPool(10);
		
		pool = new GenericObjectPool();
		pool.setFactory(new GoodFoodFinderFactory());		
		
		for(int i = 0; i < 30; i++){
	    	try {
				pool.addObject();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
		
//		pool.setMaxActive(50);
//		pool.setMaxActive(50);
		pool.setMinEvictableIdleTimeMillis(10000);
	    pool.setTimeBetweenEvictionRunsMillis(-1);
	    
	    System.err.println("Number of employees in pool: " + pool.getNumIdle());
	}
	
	public void labelGoodfood_multithreading(){
		Connection conn = null;
		PreparedStatement psSelect_reviews = null;
		PreparedStatement psUpdate_reviews = null;
		List<Future> futures = new ArrayList<Future>();
		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager
			          .getConnection("jdbc:mysql://199.168.136.80:3306/goodfoodDB?", "lingandcs", "sduonline");
			String strSelect_reviews = "SELECT id, text FROM goodfoodDB.goodfood_reviews WHERE taggedText IS NULL";
			psSelect_reviews = conn.prepareStatement(strSelect_reviews);
			String strUpdate_reviews = "UPDATE goodfoodDB.goodfood_reviews SET taggedText=?, food=?, updateTime=? WHERE id=?";
			psUpdate_reviews = conn.prepareStatement(strUpdate_reviews);
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("DB connection, NLP initialization finished!!!");
		
		ResultSet rs = null;
		long time = 0;
		
		try {
			rs = psSelect_reviews.executeQuery();
			while(rs.next()){
				int id = rs.getInt("id");
				String text = rs.getString("text");
//				GoodFoodFinder finder = (GoodFoodFinder) pool.borrowObject();				
				
//				DBAnnotatorTask task = new DBAnnotatorTask(finder, text, conn, psUpdate_reviews, id);
				DBAnnotatorTask task = new DBAnnotatorTask(pool, text, conn, psUpdate_reviews, id);
				
//				threadPool.execute(task);
				futures.add(threadPool.submit(task));
				
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(Future f : futures){
			try {
				f.get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.println("multithreading finished");
		
	}
	
	public void labelGoodfood(){
		Connection conn = null;
		PreparedStatement psSelect_reviews = null;
		PreparedStatement psUpdate_reviews = null;
		GoodFoodFinder finder = new GoodFoodFinder("data/opennlpNER.model");
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager
			          .getConnection("jdbc:mysql://199.168.136.80:3306/goodfoodDB?", "lingandcs", "sduonline");
			String strSelect_reviews = "SELECT id, text FROM goodfoodDB.goodfood_reviews WHERE taggedText IS NULL";
			psSelect_reviews = conn.prepareStatement(strSelect_reviews);
			String strUpdate_reviews = "UPDATE goodfoodDB.goodfood_reviews SET taggedText=?, food=?, updateTime=? WHERE id=?";
			psUpdate_reviews = conn.prepareStatement(strUpdate_reviews);
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("DB connection, NLP initialization finished!!!");
		
		ResultSet rs = null;
		int i = 0;
		long time = 0;
		
		try {
			rs = psSelect_reviews.executeQuery();
			while(rs.next()){
				int id = rs.getInt("id");
				String text = rs.getString("text");
				
//				long start = System.currentTimeMillis();
				Review r = finder.process(text);
//				long end = System.currentTimeMillis();
//				time += end - start;
//				System.out.println(id + "\t" + r.getTaggedStr());
//				System.out.println(id + "\t" + r.getNEStr());
				
				//update
				psUpdate_reviews.setString(1, r.getTaggedStr());
				psUpdate_reviews.setString(2, r.getNEStr());
				psUpdate_reviews.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
				psUpdate_reviews.setInt(4, id);
				psUpdate_reviews.executeUpdate();
				
				if(i%1000 == 0){
					System.out.println(i + "\t reviews processed");
//					System.out.println("time consumed:\t" + (time*1.0/i));
				}
				i++;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(i + "\t reviews processed finally!!!");
		
		try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DBAnnotator annotator = new DBAnnotator();
		annotator.labelGoodfood();
//		annotator.labelGoodfood_multithreading();
	}

}
