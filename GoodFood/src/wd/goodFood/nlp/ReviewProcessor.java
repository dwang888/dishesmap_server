package wd.goodFood.nlp;

import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import wd.goodFood.entity.Review;

/**
 * a threadPool of finder
 * */
public class ReviewProcessor {
	static volatile ExecutorService executorService;
	static volatile Queue<GoodFoodFinder> queue;
	int numThreads;
	int numFinders;
	String modelSentenceDetectorPath;
	String modelTokenizerPath;
	String modelNETaggerPath;
	private ReviewProcessor instance;
	
	public ReviewProcessor(int numThreads, int numFinders, String modelSentenceDetectorPath, String modelTokenizerPath, String modelNETaggerPath){
		this.numThreads = numThreads;
		this.numFinders = numFinders;
		this.modelSentenceDetectorPath = modelSentenceDetectorPath;
		this.modelTokenizerPath = modelTokenizerPath;
		this.modelNETaggerPath = modelNETaggerPath;
	}	
	
//	public static ReviewProcessor getInstance(){
//		if(instance == null){
//			instance = new ReviewProcessor(numThreads, numFinders, modelSentenceDetectorPath, modelTokenizerPath, modelNETaggerPath);
//		}
//		return instance;
//	}
	
	public void init(){		
		executorService = Executors.newFixedThreadPool(numThreads);
		queue = new LinkedBlockingQueue<GoodFoodFinder>();
		for(int i = 0; i < numFinders; i++){
			GoodFoodFinder finder = new GoodFoodFinder(modelSentenceDetectorPath, modelTokenizerPath, modelNETaggerPath);
			queue.offer(finder);
		}
	}
	
	public void process(Review r){
		while(this.getNumFreeFinders() == 0){
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		GoodFoodFinder finder = this.queue.poll();
		Runnable task = new ReviewTask(r, finder);
		
		try {
			Future future = this.executorService.submit(task);
//			future.get(5000, TimeUnit.MILLISECONDS);//necessary?
		} 
//		catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ExecutionException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (TimeoutException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} 
		finally{
			this.queue.add(finder);
		}
	}
	
	public void destroy(){
		this.executorService.shutdown();
		try {
			this.executorService.awaitTermination(300000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int getNumFreeFinders(){
		return this.queue.size();
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
