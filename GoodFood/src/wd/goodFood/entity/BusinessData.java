package wd.goodFood.entity;

import java.util.List;

public class BusinessData {

	public String name;
	public String lat;
	public String lon;
	public String address;
	public String phone;
	public String merMsg;
	public String hp;//website of business
	public String offer;
	public String link;//link to citygrid
	public List<FoodData> foods;
	public String bizID;
	
	public BusinessData (String n, String id, String lat, String lon, String address, 
			String phone, String merMsg, 
			String homepage, String offer, 
			String link, List<FoodData> fs){
		this.name = n;
		this.lat = lat;
		this.lon = lon;
		this.address = address;
		this.phone = phone;
		this.merMsg = merMsg;
		this.hp = homepage;
		this.offer = offer;
		this.link = link;
		this.foods = fs;
		this.bizID = id;
	}
	
	//mostly for dishes and restaurant list
//	public BusinessData(Business biz){
//		this.name = biz.getBusiness_name();
//		this.lat = biz.get;
//		this.lon = lon;
//		this.address = address;
//		this.phone = phone;
//		this.merMsg = merMsg;
//		this.hp = homepage;
//		this.offer = offer;
//		this.link = link;
//		this.foods = fs;
//	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
