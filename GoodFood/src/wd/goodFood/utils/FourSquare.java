package wd.goodFood.utils;

import java.util.HashMap;
import java.util.Map;

import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.CompactVenue;
import fi.foyt.foursquare.api.entities.CompleteTip;
import fi.foyt.foursquare.api.entities.VenuesSearchResult;
public class FourSquare {

	public void searchTips() throws FoursquareApiException {
	    // not ideal for GoodFood corpus acquisition
	    FoursquareApi foursquareApi = new FoursquareApi("G3RGK41B5M5QZWCPY23IQKXZDYK3LHT4TA2HDTAGFL4NFZTD", 
	    		"4BF4J3JGWWBKLMEEWVHVZ2P1VFQGJXTBBLRZU0FVSMH4TAHM", 
	    		"");
	    
	    Map<String, String> parametersMap = new HashMap<String, String>();
	    parametersMap.put("ll", "44.3,37.2");
//	    parametersMap.put("near", "flushing");
	    parametersMap.put("query", "sichuan");//key words
	    parametersMap.put("limit", "500");
	    
	    Result<CompleteTip[]> result = foursquareApi.tipsSearch("44.3,37.2", 500, null, null, "I");
	    //tipsSearch(String ll, Integer limit, Integer offset, String filter, String query)
	    if (result.getMeta().getCode() == 200) {
	      // if query was ok we can finally we do something with the data
	    	System.out.println(result.getResult().length + "places found!!!");
	    	for(int i = 0; i < result.getResult().length; i++){
	        // TODO: Do something we the data
	    		CompleteTip tip = result.getResult()[i];
	    	  System.out.println("ID:\t" + tip.getId());
	    	  System.out.println("Text:\t" + tip.getText());
//	    	  System.out.println("tips:\t" + tip.getVenue());	        
	      }
	    } else {
	      System.out.println("Error occured: ");
	      System.out.println("  code: " + result.getMeta().getCode());
	      System.out.println("  type: " + result.getMeta().getErrorType());
	      System.out.println("  detail: " + result.getMeta().getErrorDetail()); 
	    }
	}
	
	public void searchVenues() throws FoursquareApiException {
	    // First we need a initialize FoursquareApi. 
	    FoursquareApi foursquareApi = new FoursquareApi("G3RGK41B5M5QZWCPY23IQKXZDYK3LHT4TA2HDTAGFL4NFZTD", 
	    		"4BF4J3JGWWBKLMEEWVHVZ2P1VFQGJXTBBLRZU0FVSMH4TAHM", 
	    		"");
	    
	    Map<String, String> parametersMap = new HashMap<String, String>();
	    parametersMap.put("ll", "44.3,37.2");
//	    parametersMap.put("near", "flushing");
	    
	    parametersMap.put("radius", "100000");
	    parametersMap.put("categoryId", "4d4b7105d754a06374d81259");
	    parametersMap.put("query", "sichuan");//key words
	    parametersMap.put("limit", "50");
	    parametersMap.put("intent", "global");
	    parametersMap.put("ll", "44.3,37.2");
	    
	    Result<VenuesSearchResult> result = foursquareApi.venuesSearch(parametersMap);
	    if (result.getMeta().getCode() == 200) {
	      // if query was ok we can finally we do something with the data
	    	System.out.println(result.getResult().getVenues().length + "places found!!!");
	      for (CompactVenue venue : result.getResult().getVenues()) {
	        // TODO: Do something we the data
	    	  System.out.println("ID:\t" + venue.getId());
	    	  System.out.println("Name:\t" + venue.getName());
	    	  System.out.println("tips:\t" + venue.getTips());	        
	      }
	    } else {
	      System.out.println("Error occured: ");
	      System.out.println("  code: " + result.getMeta().getCode());
	      System.out.println("  type: " + result.getMeta().getErrorType());
	      System.out.println("  detail: " + result.getMeta().getErrorDetail()); 
	    }
	}
	/**
	 * @param args
	 * @throws FoursquareApiException 
	 */
	public static void main(String[] args) throws FoursquareApiException {
		// TODO Auto-generated method stub
		FourSquare fs = new FourSquare();
		fs.searchVenues();
//		fs.searchTips();
	}

}
