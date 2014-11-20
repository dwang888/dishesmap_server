package wd.goodFood.entity;

import java.util.LinkedList;
import java.util.UUID;
import java.util.List;

public class Food {

	private int sentiment;//1 good; 0 unknown; -1 bad;
	private String foodText;
	private List<Review> reviewsGood;
	private List<Review> reviewsBad;
	private List<Review> reviewsUnknown;
//	private String id;//a unique id; TODO: universal or local unique?
	
	public Food(){
		this.setReviewsGood(new LinkedList<Review>());
//		this.id = UUID.randomUUID().toString().substring(0, 8);
	}
	
	public Food(String text){
		this.setFoodText(text);
		this.setReviewsGood(new LinkedList<Review>());
//		this.id = UUID.randomUUID().toString().substring(0, 8);
	}
	
//	public Food(String text, List<Review> reviews){
//		this.setFoodText(text);
//		this.setReviewsGood(reviews);
//	}
	
	public int getSentiment() {
		return sentiment;
	}


	public void setSentiment(int sentiment) {
		this.sentiment = sentiment;
	}


	public String getFoodText() {
		return foodText;
	}


	public void setFoodText(String foodText) {
		this.foodText = foodText;
	}


	public List<Review> getReviewsGood() {
		return reviewsGood;
	}


	public void setReviewsGood(List<Review> reviewsGood) {
		this.reviewsGood = reviewsGood;
	}


	public List<Review> getReviewsBad() {
		return reviewsBad;
	}


	public void setReviewsBad(List<Review> reviewsBad) {
		this.reviewsBad = reviewsBad;
	}


	public List<Review> getReviewsUnknown() {
		return reviewsUnknown;
	}


	public void setReviewsUnknown(List<Review> reviewsUnknown) {
		this.reviewsUnknown = reviewsUnknown;
	}



	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
