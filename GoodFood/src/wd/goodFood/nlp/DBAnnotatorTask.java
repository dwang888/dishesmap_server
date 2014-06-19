package wd.goodFood.nlp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.commons.pool.impl.GenericObjectPool;

import wd.goodFood.entity.Review;

public class DBAnnotatorTask implements Runnable{

	GoodFoodFinder finder;
	Review r;
	String rawText;
	Connection conn;
	PreparedStatement ps;
	int id;
	GenericObjectPool pool;
	/**
	 * @param args
	 */
	public DBAnnotatorTask(GoodFoodFinder finder, String rawText, Connection conn, PreparedStatement ps, int id){
		this.finder = finder;
		this.rawText = rawText;
		this.conn = conn;
		this.ps = ps;
		this.id = id;
	}

	
	
	 
	public DBAnnotatorTask(GenericObjectPool p, String rawText, Connection conn, PreparedStatement ps, int id){
		pool = p;
		try {
			this.finder = (GoodFoodFinder) pool.borrowObject();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.rawText = rawText;
		this.conn = conn;
		this.ps = ps;
		this.id = id;
	}
	 
	@Override
	public void run() {
		r = finder.process(rawText);
//		long end = System.currentTimeMillis();
//		time += end - start;
//		System.out.println(id + "\t" + r.getTaggedStr());
//		System.out.println(id + "\t" + r.getNEStr());
		
		//update
		try {
			ps.setString(1, r.getTaggedStr());
			ps.setString(2, r.getNEStr());
			ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
			ps.setInt(4, id);
			
			ps.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		if(DBAnnotator.numRecordProcessed % 1000 == 0){
			System.out.println(DBAnnotator.numRecordProcessed + "\t reviews processed");
			System.out.println(pool.getNumActive() + "\t active and\t" + pool.getNumIdle() + "\t idle objects in pool");
		}
		DBAnnotator.numRecordProcessed++;
		try {
			pool.returnObject(finder);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	

}
