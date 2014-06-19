package wd.goodFood.nlp;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.util.Span;
import wd.goodFood.entity.Review;

public class ReviewTask implements Runnable{

	GoodFoodFinder tagger;
	Review review;
	/**
	 * @param args
	 */
	public ReviewTask(){
		
	}
	
	public ReviewTask(Review r, GoodFoodFinder tagger){
		this.tagger = tagger;
		this.review = r;
	}
	
	@Override
	public void run() {
//		this.review.resetReview();//remove info from previous iteration, or duplicated info will be appended, index error.
		tagger.process(review);		
//		System.out.println("Average score:\t" + averageScore);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	

}
