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

public class RequestRestaurantProcessor {
	private Configuration config;
//	CityGridInfoProcessor cityGridProcessor;
	FourSquareInfoProcessor fourSquareInfoProcessor;
//	SearchEngineInfoProcessor searchEngineInfoProcessor;
//	GoogleInfoProcessor googleInfoProcessor;
	BizDBProcessor bizDBProcessor;
	
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
					dishes.add(food);
				}
				biz.foods = null;//TODO: write specific method for this
				businesses.add(biz);						
			}
		}

	}
	
	public RequestRestaurantProcessor(String pathConfig) throws Exception{
		this.config = new Configuration(pathConfig);
//		this.finder = new GoodFoodFinder(config.getValue("sentSplitterPath"), 
//				config.getValue("tokenizerPath"), 
//				config.getValue("NETaggerPath"));
		this.finder = new GoodFoodFinder(config.getValue("NETaggerPath"));
//		cityGridProcessor = new CityGridInfoProcessor(pathConfig, this.finder);
//		fourSquareInfoProcessor = new FourSquareInfoProcessor(pathConfig, this.finder);
		bizDBProcessor = new BizDBProcessor();
	}

	
	public String callProcessors(HttpServletRequest req){
		String id = req.getParameter("id");
		if(id == null || id.equalsIgnoreCase("")){
			System.out.println("an invalid id");
		}
		String[] items = id.split("__");
		if(items == null || items.length != 2){
			System.out.println("invalid id partition");
		}
		String dbName = items[0];
		String bizID = items[1];
		
		System.out.println("restaurant id:\t" + id);
		System.out.println(dbName + "||" + bizID);
//		String lat = req.getParameter("lat");
//        String lon = req.getParameter("lon");
//        String keywords = req.getParameter("keywords");
        Boolean pretty = Boolean.parseBoolean(req.getParameter("pretty"));
//        String version = req.getParameter("version");
        
        List<InfoProcessor> processors = new ArrayList<InfoProcessor>();
        List<List<Business>> bizMatrix = new ArrayList<List<Business>>();
//		processors.add(this.cityGridProcessor);
//		processors.add(this.fourSquareInfoProcessor);
//		processors.add(this.googleInfoProcessor);
//		processors.add(this.searchEngineInfoProcessor);
		processors.add(this.bizDBProcessor);
		List<Business> bizs = new ArrayList<Business>();
		
		
//		for(InfoProcessor processor : processors){
//			//call each processor to fetch info
//			List<Business> bizsTmp = processor.fetchPlaces(dbName, bizID);
//			System.out.println(bizsTmp);
//			bizs.addAll(bizsTmp);
//		}
		
		List<Business> bizsTmp = this.bizDBProcessor.fetchPlacesFromDB(dbName, bizID);
		bizs.addAll(bizsTmp);
		
        
		List<BusinessData> bizDatas = new LinkedList<BusinessData>();
		bizDatas.add(EntityProcessor.biz2JsonDataObj(bizs.get(0)));
//		for(Iterator<Business> itr = bizs.iterator(); itr.hasNext();){
//			Business biz = itr.next();
//			//remove biz without reviews
//			if(biz.getGoodFoods() == null || biz.getGoodFoods().size() == 0){					
//				itr.remove();
//			}else{				
//				bizDatas.add(EntityProcessor.biz2JsonDataObj(biz));				
//			}
//			
//		}
		
		String jsonStr = null;
		
		//TODO ugly code, should be changed
		if(true){
//			System.out.println("version 1 is called");
			//output based on pretty
			if(pretty){
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				jsonStr = gson.toJson(bizDatas.get(0));			
			}else{
				Gson gson = new Gson();
				jsonStr = gson.toJson(bizDatas.get(0));
			}	
			
		}
//		System.out.println(jsonStr);
		return jsonStr; 
	}
	
	//may not be a good practice; what if new version of json request?
	public DishAndBusiness getDishAndBusinessList(List<BusinessData> bizDatas){
		return new DishAndBusiness(bizDatas);
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
		RequestRestaurantProcessor handler = new RequestRestaurantProcessor(args[0]);
		for(String[] loc : locations){
			long startTime = System.currentTimeMillis();
//			handler.callProcessors(loc[0], loc[1]);
			long endTime = System.currentTimeMillis();
			System.out.println("running time:\t" + ( endTime - startTime));
		}
	}

}
