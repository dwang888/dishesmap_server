package wd.goodFood.utils;

import java.util.LinkedList;
import java.util.List;

import wd.goodFood.entity.Business;
import wd.goodFood.entity.BusinessData;
import wd.goodFood.entity.Food;
import wd.goodFood.entity.FoodData;
import wd.goodFood.entity.Review;
import wd.goodFood.entity.ReviewData;

/**
 * a bundle of methods
 * */
public class EntityProcessor {

	/**
	 * convert business obj to a json obj format
	 * */
	public static BusinessData biz2JsonDataObj(Business biz){
		List<FoodData> foods = new LinkedList<FoodData>();
		for(Food f : biz.getGoodFoods()){
			List<Review> rws = f.getReviewsGood();
			List<ReviewData> rwsData = new LinkedList<ReviewData>();
			for(Review rw : rws){
//				rwsData.add(new ReviewData(rw.getWebLink(), rw.getReviewStr()));
				rwsData.add(new ReviewData(rw.getReviewStr()));
			}
			FoodData fData = new FoodData(f.getFoodText(), f.getReviewsGood().size(), rwsData, biz.getBusiness_id());
			foods.add(fData);
		}
		BusinessData bizData = new BusinessData(biz.getBusiness_name(), biz.getBusiness_id(),
				biz.getLatitude(), biz.getLongitude(), biz.getBusiness_address(), 
				biz.getBusiness_phone(), biz.getBusiness_merchantMsg(), 
				biz.getWebsite(), biz.getBusiness_offer(), biz.getLink(), foods);
		return bizData;
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
