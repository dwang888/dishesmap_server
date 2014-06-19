package wd.goodFood.utils;

import java.util.HashMap;
import java.util.Map;

import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.CompactVenue;
import fi.foyt.foursquare.api.entities.VenuesSearchResult;

/**
 * Basic search example
 * @TODO - more examples please :)
 * @author rmangi
 *
 */
public class BasicExample {

	  public static void main(String[] args) {
	    String ll = args.length > 0 ? args[0] : "44.3,37.2";
	    try {
	      (new BasicExample()).searchVenues(ll);
	    } catch (FoursquareApiException e) {
	      // TODO: Error handling
	    }
	  }

	  public void searchVenues(String ll) throws FoursquareApiException {
	    // First we need a initialize FoursquareApi. 
	    FoursquareApi foursquareApi = new FoursquareApi("G3RGK41B5M5QZWCPY23IQKXZDYK3LHT4TA2HDTAGFL4NFZTD", 
	    		"4BF4J3JGWWBKLMEEWVHVZ2P1VFQGJXTBBLRZU0FVSMH4TAHM", 
	    		"");
	    
	    Map<String, String> parametersMap = new HashMap<String, String>();
	    parametersMap.put("ll", "44.3,37.2");
	    parametersMap.put("radius", "100000");
	    parametersMap.put("categoryId", "4d4b7105d754a06374d81259");
	    
	    
	    // After client has been initialized we can make queries.
//	    Result<VenuesSearchResult> result = foursquareApi.venuesSearch(ll, null, null, null, null, null, null, null, null, null, null);
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
	      // TODO: Proper error handling
//	      System.out.println("Error occured: ");
//	      System.out.println("  code: " + result.getMeta().getCode());
//	      System.out.println("  type: " + result.getMeta().getErrorType());
//	      System.out.println("  detail: " + result.getMeta().getErrorDetail()); 
	    }
	  }
}
