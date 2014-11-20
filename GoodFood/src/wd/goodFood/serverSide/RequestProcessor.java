package wd.goodFood.serverSide;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import wd.goodFood.entity.Business;
import wd.goodFood.entity.BusinessData;
import wd.goodFood.entity.Food;
import wd.goodFood.entity.FoodData;
import wd.goodFood.nlp.GoodFoodFinder;
import wd.goodFood.nlp.PostProcessor;
import wd.goodFood.serverSide.CityGridInfoProcessor;
import wd.goodFood.serverSide.InfoProcessor;
import wd.goodFood.utils.Configuration;
import wd.goodFood.utils.EntityProcessor;

public class RequestProcessor {
	private Configuration config;
//	CityGridInfoProcessor cityGridProcessor;
	FourSquareInfoProcessor fourSquareInfoProcessor;
//	SearchEngineInfoProcessor searchEngineInfoProcessor;
//	GoogleInfoProcessor googleInfoProcessor;
	Gson gson = new Gson();
	
	private GoodFoodFinder finder;
	
	class DishAndBusiness{
		List<FoodData> dishes;
		List<BusinessData> businesses;
		
		DishAndBusiness(List<BusinessData> bizDatas){
			dishes = new LinkedList<FoodData>();
			businesses = new LinkedList<BusinessData>();
			for(BusinessData biz : bizDatas){
				for(FoodData food:biz.foods){
					if(food.reviews.size() > 3){//to eliminate food which has too few reviews
						dishes.add(food);
					}
				}
				biz.foods = null;//TODO: write specific method for this
				businesses.add(biz);						
			}
		}
		
//		DishAndBusiness(List<Business> bizs){
//			dishes = new LinkedList<FoodData>();
//			businesses = new LinkedList<BusinessData>();
//			for(Business biz : bizs){
//				businesses.add(biz.getBusinessDataOnly());
//				
//			}
//		}
	}
	
	public RequestProcessor(String pathConfig) throws Exception{
		this.config = new Configuration(pathConfig);
//		this.finder = new GoodFoodFinder(config.getValue("sentSplitterPath"), 
//				config.getValue("tokenizerPath"), 
//				config.getValue("NETaggerPath"));
		this.finder = new GoodFoodFinder(config.getValue("NETaggerPath"));
//		cityGridProcessor = new CityGridInfoProcessor(pathConfig, this.finder);
		fourSquareInfoProcessor = new FourSquareInfoProcessor(pathConfig, this.finder);
//		googleInfoProcessor = new GoogleInfoProcessor(pathConfig, this.finder);
//		searchEngineInfoProcessor = new SearchEngineInfoProcessor(pathConfig, this.finder);
	}

//	@Deprecated
//	public String callProcessors(String lat, String lon){
//		List<InfoProcessor> processors = new ArrayList<InfoProcessor>();		
//		List<List<Business>> bizMatrix = new ArrayList<List<Business>>();//each line is corresponding to a data source
////		processors.add(this.cityGridProcessor);
//		processors.add(this.fourSquareInfoProcessor);
////		processors.add(this.googleInfoProcessor);
//		for(InfoProcessor processor : processors){
//			//call each processor to fetch info
////			processor.clearHistoryData();
//			List<Business> bizsTmp = processor.fetchPlaces(lat, lon);
//			bizMatrix.add(processor.fetchReviews(bizsTmp));
////			bizs.addAll(processor.fetchReviews(bizsTmp));
//		}
//		
//		PostProcessor postProcessor = new PostProcessor();
//		List<Business> bizs = postProcessor.removeDuplicateBiz(bizMatrix);
//		List<BusinessData> bizDatas = new LinkedList<BusinessData>();
//		
//		//remove too general food name
//		for(Iterator<Business> itr = bizs.iterator(); itr.hasNext();){
//			Business biz = itr.next();
//			biz.extractInfoFromReviews();//obtain good foods
//			postProcessor.removeGeneralFood(biz);//remove general food name
//			if(biz.getGoodFoods() == null || biz.getGoodFoods().size() == 0){					
//				itr.remove();//remove biz without reviews
//			}else{				
//				bizDatas.add(EntityProcessor.biz2JsonDataObj(biz));				
//			}
//			
//		}
//		
//		String jsonStr = gson.toJson(bizDatas);
////		this.getDbconnector().closeAll();//need explicitly call it?
//		return "{\"places\":" + jsonStr + "}";
//	}
	
	public String callProcessors(HttpServletRequest req){
		String lat = req.getParameter("lat");
        String lon = req.getParameter("lon");
        String keywords = req.getParameter("keywords");
        Boolean pretty = Boolean.parseBoolean(req.getParameter("pretty"));
        String version = req.getParameter("version");
        
        List<InfoProcessor> processors = new ArrayList<InfoProcessor>();
        List<List<Business>> bizMatrix = new ArrayList<List<Business>>();
//		processors.add(this.cityGridProcessor);
		processors.add(this.fourSquareInfoProcessor);
//		processors.add(this.googleInfoProcessor);
//		processors.add(this.searchEngineInfoProcessor);
		
		for(InfoProcessor processor : processors){
			//call each processor to fetch info
			List<Business> bizsTmp = processor.fetchPlaces(lat, lon);
			processor.fetchReviews(bizsTmp);			
			processor.addDBTableName(bizsTmp);
			bizMatrix.add(bizsTmp);
		}
        
		PostProcessor postProcessor = new PostProcessor();
		List<Business> bizs = postProcessor.removeDuplicateBiz(bizMatrix);
		List<BusinessData> bizDatas = new LinkedList<BusinessData>();
		
		for(Iterator<Business> itr = bizs.iterator(); itr.hasNext();){
			Business biz = itr.next();
			if(keywords == null || keywords.equalsIgnoreCase("")){
				//when no search function is called
				biz.extractInfoFromReviews();
			}else{
				biz.extractInfoFromReviews(keywords);
			}
			
			postProcessor.removeGeneralFood(biz);//remove general food name
			//remove biz without reviews
			if(biz.getGoodFoods() == null || biz.getGoodFoods().size() == 0){					
				itr.remove();
			}else{				
				bizDatas.add(EntityProcessor.biz2JsonDataObj(biz));				
			}
			
		}
		
		String jsonStr = null;
		
		//TODO ugly code, should be changed
		if(version == null || version.equals("")||version.equalsIgnoreCase("1")){
//			System.out.println("version 1 is called");
			//output based on pretty
			if(pretty){
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				jsonStr = gson.toJson(bizDatas);			
			}else{
				Gson gson = new Gson();
				jsonStr = gson.toJson(bizDatas);
			}	
			
		}else if(version.equalsIgnoreCase("2")){
//			System.out.println("version 2 is called!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			//output for version 2: dishes list and restaurants list
			if(pretty){
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				jsonStr = gson.toJson(new DishAndBusiness(bizDatas));			
			}else{
				Gson gson = new Gson();
				jsonStr = gson.toJson(new DishAndBusiness(bizDatas));
			}
			return jsonStr;
		}		
//		System.out.println(jsonStr);
		return "{\"places\":" + jsonStr + "}"; 
	}
	
	//may not be a good practice; what if new version of json request?
	public DishAndBusiness getDishAndBusinessList(List<BusinessData> bizDatas){
		return new DishAndBusiness(bizDatas);
	}
	
//	@Deprecated
//	public String callProcessors(String lat, String lon, String keywords){
//		if(keywords == null || keywords.equalsIgnoreCase("")){
//			//when no search function is called
//			return this.callProcessors(lat, lon);
//		}
//		
//		
//		List<InfoProcessor> processors = new ArrayList<InfoProcessor>();
//		
//		List<List<Business>> bizMatrix = new ArrayList<List<Business>>();//each line is corresponding to a data source
////		processors.add(this.cityGridProcessor);
//		processors.add(this.fourSquareInfoProcessor);
////		processors.add(this.googleInfoProcessor);
//		for(InfoProcessor processor : processors){
//			//call each processor to fetch info
//			List<Business> bizsTmp = processor.fetchPlaces(lat, lon);
//			bizMatrix.add(processor.fetchReviews(bizsTmp));
////			bizs.addAll(processor.fetchReviews(bizsTmp));
//		}
//		
//		PostProcessor postProcessor = new PostProcessor();
//		List<Business> bizs = postProcessor.removeDuplicateBiz(bizMatrix);
//		List<BusinessData> bizDatas = new LinkedList<BusinessData>();
//		
//		//remove too general food name
//		for(Iterator<Business> itr = bizs.iterator(); itr.hasNext();){
//			Business biz = itr.next();
////			biz.extractInfoFromReviews();//obtain good foods
//			biz.extractInfoFromReviews(keywords);//obtain good foods
////			System.out.println(biz.getGoodFoods().size());
//			postProcessor.removeGeneralFood(biz);//remove general food name
//			if(biz.getGoodFoods() == null || biz.getGoodFoods().size() == 0){					
//				itr.remove();//remove biz without reviews
//			}else{				
//				bizDatas.add(EntityProcessor.biz2JsonDataObj(biz));				
//			}
//			
//		}
//		
//		String jsonStr = gson.toJson(bizDatas);
//		return "{\"places\":" + jsonStr + "}";
//	}
	

	
	public final Configuration getConfig() {
		return config;
	}

	public final void setConfig(Configuration config) {
		this.config = config;
	}


	public final Gson getGson() {
		return gson;
	}

	public final void setGson(Gson gson) {
		this.gson = gson;
	}

	public final GoodFoodFinder getFinder() {
		return finder;
	}

	public final void setFinder(GoodFoodFinder finder) {
		this.finder = finder;
	}

	/**
	 * handle request, call processor to get and process info accordingly
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		if(args.length < 1){
			System.out.println("no property file input!!!");
			System.exit(0);
		}
		List<String[]> locations = new ArrayList<String[]>();
		locations.add(new String[]{"37.6141019893", "-122.395812287"});
		locations.add(new String[]{"33.9388100309", "-118.402768344"});
		locations.add(new String[]{"40.6929648399", "-74.1845529215"});
		locations.add(new String[]{"39.1792298757", "-76.6744909797"});
		locations.add(new String[]{"37.6141019893", "-122.395812287"});
		locations.add(new String[]{"33.9388100309", "-118.402768344"});
		locations.add(new String[]{"40.6929648399", "-74.1845529215"});
		locations.add(new String[]{"39.1792298757", "-76.6744909797"});
		locations.add(new String[]{"37.6141019893", "-122.395812287"});
		locations.add(new String[]{"33.9388100309", "-118.402768344"});
		locations.add(new String[]{"40.6929648399", "-74.1845529215"});
		locations.add(new String[]{"39.1792298757", "-76.6744909797"});
		locations.add(new String[]{"37.6141019893", "-122.395812287"});
		locations.add(new String[]{"33.9388100309", "-118.402768344"});
		locations.add(new String[]{"40.6929648399", "-74.1845529215"});
		locations.add(new String[]{"39.1792298757", "-76.6744909797"});
		locations.add(new String[]{"37.6141019893", "-122.395812287"});
		locations.add(new String[]{"33.9388100309", "-118.402768344"});
		locations.add(new String[]{"40.6929648399", "-74.1845529215"});
		locations.add(new String[]{"39.1792298757", "-76.6744909797"});
		locations.add(new String[]{"37.6141019893", "-122.395812287"});
		locations.add(new String[]{"33.9388100309", "-118.402768344"});
		locations.add(new String[]{"40.6929648399", "-74.1845529215"});
		locations.add(new String[]{"39.1792298757", "-76.6744909797"});
		locations.add(new String[]{"37.6141019893", "-122.395812287"});
		locations.add(new String[]{"33.9388100309", "-118.402768344"});
		locations.add(new String[]{"40.6929648399", "-74.1845529215"});
		locations.add(new String[]{"39.1792298757", "-76.6744909797"});
		locations.add(new String[]{"37.6141019893", "-122.395812287"});
		locations.add(new String[]{"33.9388100309", "-118.402768344"});
		locations.add(new String[]{"40.6929648399", "-74.1845529215"});
		locations.add(new String[]{"39.1792298757", "-76.6744909797"});
		RequestProcessor handler = new RequestProcessor(args[0]);
		for(String[] loc : locations){
			long startTime = System.currentTimeMillis();
//			handler.callProcessors(loc[0], loc[1]);
			long endTime = System.currentTimeMillis();
			System.out.println("running time:\t" + ( endTime - startTime));
		}
	}

}
