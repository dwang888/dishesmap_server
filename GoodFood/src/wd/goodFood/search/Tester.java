package wd.goodFood.search;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.FilterBuilders.*;
import org.elasticsearch.index.query.QueryBuilders.*;
import org.elasticsearch.search.SearchHit;

import wd.goodFood.entity.Business;
import wd.goodFood.utils.DBConnector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class Tester {

	public void searchES(){
		TransportClient client;
		
		
		Settings settings = ImmutableSettings.settingsBuilder()
			        .put("cluster.name", "elasticsearch")
			        .build();
		client = new TransportClient(settings);
//		client = new TransportClient();
		System.out.println("adding hostname and port");
		client.addTransportAddress(new InetSocketTransportAddress("107.170.18.102", 9300));
		
		
		
		System.out.println("client is " +  client.settings().toString());
		SearchResponse response = client.prepareSearch("goodfood")
		        .setTypes("goodfood_biz")
		        .setSearchType(SearchType.QUERY_AND_FETCH)
//		        .setQuery(QueryBuilders.termQuery("bizName", "laobeifang"))             // Query
		        .setQuery(QueryBuilders.termQuery("user", "kimchy"))             // Query
//		        .setPostFilter(FilterBuilders.rangeFilter("age").from(12).to(18))   // Filter
//		        .setFrom(0).setSize(60).setExplain(true)
		        .execute()
		        .actionGet();
		SearchHit[] results = response.getHits().getHits();
		System.out.println("size is " +  results.length); 
        client.close();
//		System.out.println(response.getHeaders());
//		for (SearchHit hit : results) {
//			System.out.println("------------------------------");
//			Map<String,Object> result = hit.getSource();
//			System.out.println(result);
//			}
	}
	
	public void searchViaJson(){
		StringBuilder sb = new StringBuilder();  

		String urlStr = "http://107.170.18.102:9200/goodfood/goodfood_biz/_search";
		System.out.println(urlStr);
		XContentBuilder builder;
		try {
			builder = jsonBuilder()
				    .startObject()
//			        .field("id", )
				        .field("bizName", "laobeifang")			        
				    .endObject();
			String jsonStr = builder.string();
			
			jsonStr = "{\"query\":{\"filtered\":{\"query\":{\"match_all\":{}},\"filter\":{\"geo_distance\":" +
					"{\"distance\":\"10km\", \"goodfood_biz.location\":{\"lat\":41.435465997926," +
					"\"lon\":-76.928028553348}}}}}}";
			System.out.println(jsonStr);
			
			URL url = new URL(urlStr);
			HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();
			
			urlconn.setRequestMethod("GET");
			urlconn.setDoOutput(true);
			urlconn.setDoInput(true);
			urlconn.setRequestProperty("Content-Type", "application/json");
			urlconn.connect();
			OutputStreamWriter out = new OutputStreamWriter(urlconn.getOutputStream());
			out.write(jsonStr);
			out.close();
			
			int HttpResult =urlconn.getResponseCode();
			if(HttpResult ==HttpURLConnection.HTTP_OK){  
		        BufferedReader br = new BufferedReader(new InputStreamReader(  
		        		urlconn.getInputStream(),"utf-8"));  
		        String line = null;  
		        while ((line = br.readLine()) != null) {  
		            sb.append(line + "\n");  
		        }  
		        br.close();  

//		        System.out.println(""+sb.toString());  

		        JsonParser jsonParser = new JsonParser();
		        JsonObject jobj= (JsonObject)jsonParser.parse(sb.toString());
				
//				Gson gson = new Gson();
//				Phone fooFromJson = gson.f.fromJson(jsonString, Phone.class);

//				JsonArray groups = jobj.getAsJsonObject("response").getAsJsonArray("groups");
//				JsonArray locations = groups.get(0).getAsJsonObject().getAsJsonArray("items");
				JsonArray locations = jobj.getAsJsonObject("hits").getAsJsonArray("hits");
				System.out.println("\nnumber of locations:\t" + locations.size());
				
				for(JsonElement location : locations){
					JsonObject loc = (JsonObject)location;
					Business biz = new Business();
					biz = addInfo2Biz(loc.getAsJsonObject("_source"), biz);
//					System.out.println(biz);
//					System.out.println(biz);
				}			
		        
		        
		    }else{  
		        System.out.println(urlconn.getResponseMessage());  
		    }
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * add info from FourSquare Json response to Biz obj
	 * */
	public Business addInfo2Biz(JsonObject jobj, Business biz){
		JsonElement eTmp = null;
		JsonObject oTmp = null;
		System.out.println(jobj);
		biz.setBusiness_id(jobj.get("id").getAsString().trim());
		biz.setBusiness_name(jobj.get("bizName").getAsString().trim());		
		
		eTmp = jobj.get("phoneNum");
//		System.out.println(eTmp.getClass());
		if(eTmp != null)
			biz.setBusiness_phone(eTmp.toString());
		
		biz.setBusiness_address(jobj.get("address").getAsString().trim());
		
		eTmp = jobj.get("location");
		if(eTmp != null){
			String[] latlon = jobj.get("location").getAsString().split(",");
			System.out.println(latlon[0]);
			System.out.println(latlon[1]);
			biz.setLatitude(latlon[0]);
			biz.setLongitude(latlon[1]);
		}		
				
		biz.setNumReviews(Integer.parseInt(jobj.get("numReviews").getAsString().trim()));
//		System.out.println(eTmp.getAsString());		
		
		eTmp = jobj.get("merchantMsg");
		if(eTmp != null)
			biz.setBusiness_merchantMsg(eTmp.toString());
		
		eTmp = jobj.get("bizWebsite");
		if(eTmp != null)
			biz.setWebsite(eTmp.toString());
		
		eTmp = jobj.get("profileLink");
		if(eTmp != null)
			biz.setLink(eTmp.toString());
		
		biz.setDataSource(jobj.get("dataSource").getAsInt());//TODO:hardcode?	
		System.out.println(biz);
		return biz;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Tester tester = new Tester();
		System.out.println("Hello world" + tester.getClass());
		tester.searchES();
//		tester.searchViaJson();
	}

}
