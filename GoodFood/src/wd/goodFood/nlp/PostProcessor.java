package wd.goodFood.nlp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import wd.goodFood.entity.Business;
import wd.goodFood.entity.Food;
import wd.goodFood.serverSide.GoodFoodServlet;
import wd.goodFood.utils.DBConnector;

/**
 * do post processing work in data
 * e.g. remove general words: bread, burger, beer, wine
 * */
public class PostProcessor {

	public static String[] FOODS_NAMES = new String[] { 
//		"bread", "breads", 
//		"pizza", "pizzas", "cake", "rice", "soup", "dessert", "beef", "noodle", "crab",
//		"beer", "beers", "wine", "coffee", "fries", "seafood", "roll", "eggs", "egg", "mac", "bacon",
//		"steak", "steaks", "sushi", "chicken", "cheese", "burgers", "burger", "pasta",
//		"sandwiches", "fish", "lobster", "desserts", "duck", "strawberry", "fried",
//		"fruit", "salad", "filet", "cookies", "onion", "cheeses", "sandwich", "chips", "rolls"
		};
	public static Set<String> GENERAL_FOODS_DIC;
	public static String SELECT_reviews = "SELECT words FROM goodfoodDB.goodfood_generalFood";
	
	static{
		Connection dbconn = null;
		PreparedStatement psSelectGeneralFood = null;
		ResultSet rs = null;
		try {			
			dbconn = GoodFoodServlet.DS.getConnection();
			psSelectGeneralFood = dbconn.prepareStatement(SELECT_reviews);
			rs = psSelectGeneralFood.executeQuery();
			if(GENERAL_FOODS_DIC == null){
				GENERAL_FOODS_DIC = new HashSet<String>();
			}
			while(rs.next()){
				String foodName = rs.getString("words");
				GENERAL_FOODS_DIC.add(foodName);
			}			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			GENERAL_FOODS_DIC = new HashSet<String>(Arrays.asList(FOODS_NAMES));
		} finally{
			DBConnector.close(rs);
			DBConnector.close(psSelectGeneralFood);
			DBConnector.close(dbconn);
		}		
	}
	
	/**
	 * remove general words: bread, burger, beer, wine
	 * */
	public void removeGeneralFood(List<Business> bizs){
//		System.out.println("|||||||||||||||||| running postprocessing!!!");
		for(Business biz : bizs){
			if(biz.getGoodFoods().size() < 3){
				//if too few foods, ignore
				continue;
			}
			for(Iterator<Food> itr = biz.getGoodFoods().iterator(); itr.hasNext();){
				Food f = itr.next();
//				System.out.println(f.getFoodText());
				if(GENERAL_FOODS_DIC.contains(f.getFoodText())){					
					itr.remove();
				}
			}
		}
	}
	
	public void removeGeneralFood(Business biz){		
		if(biz.getGoodFoods().size() > 2){
			//if too few foods, ignore
			for(Iterator<Food> itr = biz.getGoodFoods().iterator(); itr.hasNext();){
				Food f = itr.next();
//				System.out.println(f.getFoodText());
				if(GENERAL_FOODS_DIC.contains(f.getFoodText())){					
					itr.remove();
				}
			}
		}		
	}
	
	public List<Business> removeDuplicateBiz(List<List<Business>> bizsGroup){
		if(bizsGroup == null || bizsGroup.size() == 0){
			return null;
		}
		if(bizsGroup.size() == 1){
			return bizsGroup.get(0);
		}
		
		List<Business> uniqueBizs = new ArrayList<Business>();
		List<Business> bizsTmp = new ArrayList<Business>();//to store intermediate bizs
		
		int longest = 0;
		//find out longest biz list
		int indexLongest = 0;
		
		for(int i = 0; i < bizsGroup.size(); i++){
			if(bizsGroup.get(i).size() > longest){
				longest = bizsGroup.get(i).size();
				indexLongest = i;
			}
		}
		uniqueBizs.addAll(bizsGroup.get(indexLongest));//base bizs
		boolean flag = false;//found duplicated biz or not
		//check other bizs
		for(int i = 0; i < bizsGroup.size(); i++){
			if(i == indexLongest){
				continue;
			}
			bizsTmp.clear();//must be cleared
			for(Business biz : bizsGroup.get(i)){
				//check each biz from other data source
				for(int m = 0; m < uniqueBizs.size(); m++){
					//match with all other bizs
					if(biz.isSameWith(uniqueBizs.get(m))){
						mergeBiz(biz, uniqueBizs.get(m));
						flag = true;
						break;
					}
				}
				if(flag == false){
					bizsTmp.add(biz);
				}
				flag = false;//must reset here
			}
			uniqueBizs.addAll(bizsTmp);			
		}	
		
		return uniqueBizs;
	}
	
	public Business mergeBiz(Business from, Business to){
		//currently only goodfoods with reviews
		if(from.getGoodFoods() != null && from.getGoodFoods().size() > 0){
			to.getGoodFoods().addAll(from.getGoodFoods());
		}
		if(from.getReviews() != null && from.getReviews().size() > 0){
			if(to.getReviews() == null){
				to.setReviews(from.getReviews());
			}else{
				to.getReviews().addAll(from.getReviews());
			}
		}
		
		if(to.getBusiness_address() == null || to.getBusiness_address().equals("")){
			to.setBusiness_address(from.getBusiness_address());
		}		
		
		return from;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
