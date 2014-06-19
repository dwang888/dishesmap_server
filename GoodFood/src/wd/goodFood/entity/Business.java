package wd.goodFood.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.stanford.nlp.util.StringUtils;

import opennlp.tools.util.Span;

/**
 * always a restaurant in current app
 * could be some other store, market, bar, any type places, etc
 * */
public class Business {

	private String business_id;
	private int business_dbid;
	private String business_name;
	private String business_address;
	private String business_phone;
	private String business_merchantMsg;
	private String business_offer;
	private String latitude;
	private String longitude;
	private List<Review> reviews;
	private List<Food> goodFoods;
	private int numReviews;//from json, not directly from reviews.size()
	private String rating;
	private String website;
	private String link;//different from website: link is usually profile page from api provider
	private String category;//is this a chinese restaurant? cafe? bar? food truck?
	private String note;//for info that is not sure about where to put
	private int dataSource;//1:cityGrid, 2:FourSquare
	
	public Business(){
		this.reviews = new ArrayList<Review>();
		this.goodFoods = new ArrayList<Food>();
	}
	
	//convert to BusinessData type, without food info. just for json output purpose
	public BusinessData getBusinessDataOnly(){
		
		BusinessData bizData = new BusinessData(getBusiness_name(), getBusiness_id(),
				getLatitude(), getLongitude(), getBusiness_address(), 
				getBusiness_phone(), getBusiness_merchantMsg(), 
				getWebsite(), getBusiness_offer(), getLink(), null);
		return bizData;
	}
	
	/*
	 * organize info based on labeled reviews
	 * only for good food currently
	 * maybe bad food then
	 * **/
	public List<Food> extractInfoFromReviews(){
		Map<String, List<Review>> goodFoodFreq = new HashMap<String, List<Review>>();
		for(Review r : this.getReviews()){
			if(r.getNEStr() == null || r.getNEStr().equalsIgnoreCase("")){
				continue;
			}
			String[] NEs = r.getNEStr().split("\\|\\|");
//			System.out.println("extracting info from review:\t" + StringUtils.join(NEs, "\t"));
			for(int m = 0; m < NEs.length; m++){
				//link good foods to mentioning reviews
				String foodName = NEs[m].toLowerCase();
				if(!goodFoodFreq.containsKey(foodName)){
					goodFoodFreq.put(foodName, new ArrayList<Review>());
				}
				goodFoodFreq.get(foodName).add(r);
			}
			
		}
		List<Map.Entry<String, List<Review>>> list = new LinkedList<Map.Entry<String, List<Review>>>(goodFoodFreq.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, List<Review>>>(){//better solution?
			public int compare(Map.Entry<String, List<Review>> e1, Map.Entry<String, List<Review>> e2){
				return e2.getValue().size() - e1.getValue().size();
			}
		});
		for(Map.Entry<String, List<Review>> e : list){
			Food f = new Food(e.getKey());
			f.setReviewsGood(e.getValue());
			this.getGoodFoods().add(f);
		}
		
		return this.getGoodFoods();
	}
	
	/**
	 * specifically for food name search
	 * */
	public List<Food> extractInfoFromReviews(String keywords){
		if(keywords == null || keywords.equalsIgnoreCase("")){
			//when no search function is called
			this.extractInfoFromReviews();
		}
		
		String[] words = keywords.split("_");//spicy_chicken
		
		Map<String, List<Review>> goodFoodFreq = new HashMap<String, List<Review>>();
		for(Review r : this.getReviews()){
			if(r.getNEStr() == null || r.getNEStr().equalsIgnoreCase("")){
				continue;
			}
			String[] NEs = r.getNEStr().split("\\|\\|");
//			System.out.println("extracting info from review:\t" + StringUtils.join(NEs, "\t"));
			for(int m = 0; m < NEs.length; m++){
				//link good foods to mentioning reviews
				String foodName = NEs[m].toLowerCase();
				//ONLY DIFFERENT: MATCH KEYWORDS
				Boolean flag = true;
				for(int w = 0; w < words.length; w++){
					if(foodName.indexOf(words[w]) == -1){
						flag = false;
						break;
					}
				}
				if(flag == false){
					continue;//food name not matched
				}
				if(!goodFoodFreq.containsKey(foodName)){
					goodFoodFreq.put(foodName, new ArrayList<Review>());
				}
				goodFoodFreq.get(foodName).add(r);
			}
			
		}
		List<Map.Entry<String, List<Review>>> list = new LinkedList<Map.Entry<String, List<Review>>>(goodFoodFreq.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, List<Review>>>(){//better solution?
			public int compare(Map.Entry<String, List<Review>> e1, Map.Entry<String, List<Review>> e2){
				return e2.getValue().size() - e1.getValue().size();
			}
		});
		for(Map.Entry<String, List<Review>> e : list){
			Food f = new Food(e.getKey());
			f.setReviewsGood(e.getValue());
			this.getGoodFoods().add(f);
		}
		
		return this.getGoodFoods();
	}
	
	/*
	 * organize info based on labeled reviews
	 * only for good food currently
	 * maybe bad food then
	 * **/
	@Deprecated
	public List<Food> extractInfoFromReviews2(){
		Map<String, List<Review>> goodFoodFreq = new HashMap<String, List<Review>>();
		for(Review r : this.getReviews()){
			for(int i = 0; i < r.getAllNERSpans().size(); i++){
				Span[] spansInSent = r.getAllNERSpans().get(i);
				if(spansInSent.length == 0){
					continue;
				}
				String[] tokens = r.getAllStrs().get(i);
				String[] NEs = Span.spansToStrings(spansInSent, tokens);
				for(int m = 0; m < NEs.length; m++){
					//link good foods to mentioning reviews
					String foodName = NEs[m].toLowerCase();
					if(!goodFoodFreq.containsKey(foodName)){
						goodFoodFreq.put(foodName, new ArrayList<Review>());
					}
					goodFoodFreq.get(foodName).add(r);
				}
//				System.out.println(Arrays.toString(NEs));
			}
		}
		List<Map.Entry<String, List<Review>>> list = new LinkedList(goodFoodFreq.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, List<Review>>>(){//better solution?
			public int compare(Map.Entry<String, List<Review>> e1, Map.Entry<String, List<Review>> e2){
				return e2.getValue().size() - e1.getValue().size();
			}
		});
		for(Map.Entry<String, List<Review>> e : list){
			//TODO: necessary?
			Food f = new Food(e.getKey());
			f.setReviewsGood(e.getValue());
			this.getGoodFoods().add(f);
		}
		
		return this.getGoodFoods();
	}
	
	//use business name and location to judge
	public boolean isSameWith(Business biz){
		float distance = Math.abs(Float.parseFloat(this.getLatitude()) - Float.parseFloat(biz.getLatitude())) 
				+ Math.abs(Float.parseFloat(this.getLongitude()) - Float.parseFloat(biz.getLongitude()));
		int editDistance = editDistance(this.getBusiness_name(), biz.getBusiness_name());
		if(editDistance < 4 && distance < 0.0004){
			return true;
		}else{
			return false;
		}		
	}
	
	public int editDistance(String s1, String s2){
		if(s1 == null && s2 == null){
			return 0;
		}else if(s1 == null){
			return s2.length();
		}else if(s2 == null){
			return s1.length();
		}
		
		int[][] distance = new int[s1.length()+1][s2.length()+1];
		distance[0][0] = 0;
		
		for(int i = 1; i <= s1.length(); i++){
			distance[i][0] = i;
		}
		for(int j = 1; j <= s2.length(); j++){
			distance[0][j] = j;
		}
		
		for(int i = 1; i <= s1.length(); i++){
			for(int j = 1; j <= s2.length(); j++){
				distance[i][j] = minimum(
						distance[i-1][j] + 1,
						distance[i][j-1] + 1,
						distance[i-1][j-1] + (s1.charAt(i-1) == s2.charAt(j-1) ? 0 : 1)
						);
			}
		}
		
		return distance[s1.length()][s2.length()];
	}
	
	public int minimum(int a, int b, int c){
		return Math.min(Math.min(a, b), c);
	}
	
	public String getBusiness_id() {
		return business_id;
	}



	public void setBusiness_id(String business_id) {
		this.business_id = business_id;
	}



	public String getBusiness_name() {
		return business_name;
	}



	public void setBusiness_name(String business_name) {
		this.business_name = business_name;
	}



	public String getBusiness_address() {
		return business_address;
	}



	public void setBusiness_address(String business_address) {
		this.business_address = business_address;
	}





	public final String getLatitude() {
		return latitude;
	}

	public final void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public final String getLongitude() {
		return longitude;
	}

	public final void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public List<Review> getReviews() {
		return reviews;
	}



	public void setReviews(List<Review> reviews) {
		this.reviews = reviews;
	}



	public List<Food> getGoodFoods() {
		return goodFoods;
	}



	public void setGoodFoods(List<Food> goodFoods) {
		this.goodFoods = goodFoods;
	}



	public int getNumReviews() {
		return numReviews;
	}



	public void setNumReviews(int numReviews) {
		this.numReviews = numReviews;
	}



	public String getRating() {
		return rating;
	}



	public void setRating(String rating) {
		this.rating = rating;
	}



	public String getWebsite() {
		return website;
	}



	public void setWebsite(String website) {
		this.website = website;
	}



	public String getLink() {
		return link;
	}



	public void setLink(String link) {
		this.link = link;
	}



	public String getBusiness_phone() {
		return business_phone;
	}

	public void setBusiness_phone(String business_phone) {
		this.business_phone = business_phone;
	}

	public String getBusiness_merchantMsg() {
		return business_merchantMsg;
	}

	public void setBusiness_merchantMsg(String business_merchantMsg) {
		this.business_merchantMsg = business_merchantMsg;
	}

	public String getBusiness_offer() {
		return business_offer;
	}

	public void setBusiness_offer(String business_offer) {
		this.business_offer = business_offer;
	}

	public final int getBusiness_dbid() {
		return business_dbid;
	}

	public final void setBusiness_dbid(int business_dbid) {
		this.business_dbid = business_dbid;
	}

	public final String getCategory() {
		return category;
	}

	public final void setCategory(String category) {
		this.category = category;
	}

	
	
	public final int getDataSource() {
		return dataSource;
	}

	public final void setDataSource(int dataSource) {
		this.dataSource = dataSource;
	}

	public final String getNote() {
		return note;
	}

	public final void setNote(String note) {
		this.note = note;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Business biz = new Business();
		System.out.println(biz.editDistance("dasi chuan", "dasis chuan"));
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
