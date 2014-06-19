package wd.goodFood.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import wd.goodFood.entity.Business;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

public class Test {

	private static String apiPrefixPlaces = "https://api.foursquare.com/v2/venues/search?" +
			"client_id=G3RGK41B5M5QZWCPY23IQKXZDYK3LHT4TA2HDTAGFL4NFZTD" +
			"&client_secret=4BF4J3JGWWBKLMEEWVHVZ2P1VFQGJXTBBLRZU0FVSMH4TAHM" +
			"&limit=50" +
			"&llAcc=10" +
			"&radius=1500" +
			"&ll=40.7,-74" +
			"&v=20130518";
	
	private static String apiPrefixReviews = "https://api.foursquare.com/v2/venues/40a55d80f964a52020f31ee3/tips?" +
			"sort=recent" +
			"&client_id=G3RGK41B5M5QZWCPY23IQKXZDYK3LHT4TA2HDTAGFL4NFZTD" +
			"&client_secret=4BF4J3JGWWBKLMEEWVHVZ2P1VFQGJXTBBLRZU0FVSMH4TAHM" +
			"&v=20130518";

	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
		System.out.println(date);
		URL url = new URL(apiPrefixPlaces);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		InputStream is = conn.getInputStream();			
//		JsonReader reader = new JsonReader(new InputStreamReader(is));
//		JsonObject jobj= jsonParser.parse(reader).getAsJsonObject();
//		is.close();
//		JsonObject results = jobj.getAsJsonObject("results");
//		JsonArray bizReviews = results.getAsJsonArray("reviews");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		while((line = br.readLine()) != null){
			System.out.println(line);
		}
	}

}
