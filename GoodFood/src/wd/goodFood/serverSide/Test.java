package wd.goodFood.serverSide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.jetty.util.ajax.JSON;

import wd.goodFood.entity.Business;
import wd.goodFood.utils.DBConnector;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;


public class Test {
	JsonParser jsonParser = new JsonParser();
	
	public void testResponse() throws IOException{
		
		URL url = new URL("http://199.168.136.80:8080/fk?lat=37.6141019893&lon=-122.395812287");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		
		InputStream is = conn.getInputStream();			
		JsonReader reader = new JsonReader(new InputStreamReader(is));
		JsonParser jsonParser = new JsonParser();
		JsonObject jobj= jsonParser.parse(reader).getAsJsonObject();
		is.close();
		JsonArray places = jobj.getAsJsonArray("places");
		for(int i = 0; i < places.size(); i++){
			JsonObject j = places.get(i).getAsJsonObject();
			System.out.println(j.get("name"));
		}
		
	}
	
	public String cleanAddr(String origAddr){
		StringBuilder sb = new StringBuilder();		
		origAddr = origAddr.trim().replace("\"", "");
		origAddr = origAddr.trim().replace("{", "");
		origAddr = origAddr.trim().replace("}", "");
		String[] strSplits = origAddr.split(",");
		for(int i = 0; i < strSplits.length; i++){
			if(i == 0){
				sb.append(strSplits[i].split(":")[1]);
			}else{
				sb.append(", " + strSplits[i].split(":")[1]);
			}			
		}
		return sb.toString();
	}
	
	public void testJson2() throws IOException{
		String apiStr = "https://api.foursquare.com/v2/venues/search?client_id=G3RGK41B5M5QZWCPY23IQKXZDYK3LHT4TA2HDTAGFL4NFZTD&client_secret=4BF4J3JGWWBKLMEEWVHVZ2P1VFQGJXTBBLRZU0FVSMH4TAHM&limit=50&llAcc=10&" +
				"radius=541&ll=40.714352999999996,-74.005973&categoryId=4d4b7105d754a06374d81259";
		String today = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
		apiStr += "&v=" + today;
		URL url;
		
			url = new URL(apiStr);
			HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();
			InputStream is = urlconn.getInputStream();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			String line;
			while((line = br.readLine()) != null){
//				System.out.println(line);
				sb.append(line);
			}
			String jsonStr = sb.toString();
//			jsonStr = "{\"formattedPhone\":\"(212) 406-5310\"}";
			System.out.println(jsonStr);
			//exclude " } ] number true false
			jsonStr = jsonStr.replaceAll("(?<![\"}\\]\\d(true)(false)]),\"", "\",\"");
			System.out.println(jsonStr);
			
			System.out.println(today);
			JsonObject jobj= (JsonObject)jsonParser.parse(jsonStr);
			
//			JsonReader reader = new JsonReader(new InputStreamReader(is));
//			JsonObject jobj= jsonParser.parse(reader).getAsJsonObject();
			DBConnector.close(is);
			
			JsonArray groups = jobj.getAsJsonObject("response").getAsJsonArray("venues");
			System.out.println(groups.size());
			for(JsonElement location : groups){
				JsonObject loc = (JsonObject)location;
				Business biz = new Business();
				this.addInfo2Biz(loc, biz);
//				if(!this.isBizInDB(biz, dbconn, psSelectBiz)){
//					this.addBiz2DB(biz, dbconn, psInsertBiz);
//				}
//				bizs.add(biz);
			}
//			JsonArray locations = groups.get(0).getAsJsonObject().getAsJsonArray("items");
//			System.out.println("number of locations:\t" + locations.size());
		
	
	}
	
	public void addInfo2Biz(JsonObject jobj, Business biz){
		JsonElement eTmp = null;
		JsonObject oTmp = null;
			
		biz.setBusiness_id(jobj.get("id").getAsString().trim());
		biz.setBusiness_name(jobj.get("name").getAsString().trim());
		
		eTmp = jobj.getAsJsonObject("contact").get("phone");
		if(eTmp != null){
			biz.setBusiness_phone(eTmp.getAsString());
		}

		oTmp = jobj.getAsJsonObject("location");
		eTmp = oTmp.get("address");
		biz.setLatitude(oTmp.get("lat").getAsString());
		biz.setLongitude(oTmp.get("lng").getAsString());		
		biz.setBusiness_address(this.extractAddress(oTmp));
		
		oTmp = jobj.getAsJsonObject("stats");
		eTmp = oTmp.get("tipCount");
		biz.setNumReviews(Integer.parseInt(eTmp.getAsString()));
//		System.out.println(eTmp.getAsString());
		
		oTmp = jobj.getAsJsonObject("description");
		if(oTmp != null){
			biz.setBusiness_merchantMsg(oTmp.getAsString());
		}
		
		biz.setWebsite(jobj.get("url").getAsString());
		biz.setLink(jobj.get("url").getAsString());
		//no such rating in new version foursquare api since 201309
//		if(jobj.get("rating") != null){
//			biz.setRating(jobj.get("rating").getAsString());
//		}
				
		oTmp = jobj.getAsJsonObject("categories");
		JsonArray arrayTmp = jobj.getAsJsonArray("categories");//only consider the first category
		if(arrayTmp != null && arrayTmp.get(0) != null){
			String catID = arrayTmp.get(0).getAsJsonObject().get("id").getAsString();
			biz.setCategory(catID);
		}
		
		biz.setDataSource(2);//TODO:hardcode?		
	}
	
	public String extractAddress(JsonObject location){
		JsonElement eTmp = null;
		StringBuilder sb = new StringBuilder();
		eTmp = location.get("address");
		if(eTmp != null){
			sb.append(eTmp.getAsString() + ", ");
		}
		eTmp = location.get("city");
		if(eTmp != null){
			sb.append(eTmp.getAsString() + ", ");
		}
		eTmp = location.get("state");
		if(eTmp != null){
			sb.append(eTmp.getAsString());
		}
		return sb.toString();
	}
	
	public void testJson(){
		String apiStr = "https://api.foursquare.com/v2/venues/search?client_id=G3RGK41B5M5QZWCPY23IQKXZDYK3LHT4TA2HDTAGFL4NFZTD&client_secret=4BF4J3JGWWBKLMEEWVHVZ2P1VFQGJXTBBLRZU0FVSMH4TAHM&limit=50&llAcc=10&radius=800&v=20130929&ll=40.72,-74.1&categoryId=4d4b7105d754a06374d81259";
		URL url;
		Gson gson = new Gson();
		JsonReader rdr;
		
		try {
			url = new URL(apiStr);
			InputStream is = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			String jsonStr = "";
			String line;
			while((line = br.readLine()) != null){
//				System.out.println(line);
				sb.append(line);
				jsonStr += line;
			}
			System.out.println(jsonStr);
			String objStr = sb.toString();
			jsonStr = StringEscapeUtils.escapeJavaScript(jsonStr);
			System.out.println(jsonStr);
			JSONObject json = (JSONObject) JSONSerializer.toJSON( objStr );    
//			JsonReader jreader = new JsonReader(new StringReader(jsonStr));
//			JsonObject jobj= jsonParser.parse(jreader).getAsJsonObject();
			
			JsonElement root = new JsonParser().parse(jsonStr);
			String value = root.getAsJsonObject().get("meta").getAsString();
			JsonObject jobj= (JsonObject)jsonParser.parse(objStr);
//			JsonObject jobj= new JsonObject(objStr);
			
			
//			JsonReader reader = new JsonReader(new InputStreamReader(is));
			DBConnector.close(is);
//			reader.setLenient(true);
//			JsonObject jobj= jsonParser.parse(reader).getAsJsonObject();
			

			
			JsonArray groups = jobj.getAsJsonObject("response").getAsJsonArray("groups");
			JsonArray locations = groups.get(0).getAsJsonObject().getAsJsonArray("items");
			System.out.println("number of locations:\t" + locations.size());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Test t = new Test();
//		t.testResponse();
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, -1);
		Timestamp pastTime = new Timestamp(calendar.getTimeInMillis());
		
		Timestamp curTime = new Timestamp(System.currentTimeMillis());
		
//		System.out.println(pastTime.before(curTime));
		t.testJson2();
	}

}
