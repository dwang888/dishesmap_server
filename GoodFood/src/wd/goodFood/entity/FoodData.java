package wd.goodFood.entity;

import java.util.List;
import java.util.UUID;

/**
 * only for json response
 * */
public class FoodData {

	public String foodText;
	public int freq;
	public List<ReviewData> reviews;
	public String bizID;//to link to restaurant it belongs to
	public String id;//to link to food

	public FoodData(String fText, int freq, List<ReviewData> rs, String bizID){
		this.foodText = fText;
		this.freq = freq;
		this.reviews = rs;
		this.bizID = bizID;
		this.id = UUID.randomUUID().toString().substring(0,8);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
