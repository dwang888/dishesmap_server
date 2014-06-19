package wd.goodFood.nlp;

import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * a pool with GoodfoodFinders
 * based on Apache object pool
 * */
public class GoodfoodFinderPool {
	GenericObjectPool pool;
	
	public GoodfoodFinderPool(){
		pool = new GenericObjectPool();
		pool.setFactory(new GoodFoodFinderFactory());
		
		
		pool.setMinEvictableIdleTimeMillis(1000);
	    pool.setTimeBetweenEvictionRunsMillis(600);
	    
	    System.err.println("Number of employees in pool: " + pool.getNumIdle());
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
