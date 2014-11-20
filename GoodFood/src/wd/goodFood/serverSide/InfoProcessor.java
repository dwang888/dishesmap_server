package wd.goodFood.serverSide;

import java.util.List;

import wd.goodFood.entity.Business;

/**
 * to process real time info, e.g. get info via API and process them
 * */
public interface InfoProcessor{

	public List<Business> fetchPlaces(String lat, String lon);
	
//	public List<Business> fetchReviews();//add and process review to business
	
	public List<Business> fetchReviews(List<Business> bizs);
	
//	public void clearHistoryData();

	public List<Business> addDBTableName(List<Business> bizs);
	
}
