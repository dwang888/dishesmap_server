package wd.goodFood.serverSide;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.google.gson.JsonObject;

/**
 * a wrapper of testers
 * */
public class Tester {
	public Tester(){
		
		
		
		
		
		
	}
	class Location{
		double lat;
		double lon;
		Location(double lat, double lon){			
			this.lat = lat;
			this.lon = lon;
		}
	}
	
	List<Location> cityCenters = new ArrayList<Location>();
	
	List<List<double[]>> locGroup = new ArrayList<List<double[]>>();
	
	public List<Location[]> getLocations(){
		List<Location[]> locGroup = new ArrayList<Location[]>();
		Location[] locs = new Location[2];
		locs[0] = new Location(40.898982, -73.999786);
		locs[1] = new Location(40.723323, -73.841858);
		locGroup.add(locs);

		
		
		return locGroup;
	}

	public double[] generateRandom(double[] upLeft, double[] downRight){
		Random r = new Random();
		double[] loc = new double[2];
		loc[0] = r.nextFloat() * (upLeft[0]-downRight[0]) + downRight[0];
		loc[1] = r.nextFloat() * (downRight[1]-upLeft[1]) + upLeft[1];
//		System.out.println(loc[0] + "\n" + loc[1]);
		return loc;
	}
	
	public Location generateRandom(Location upLeft, Location downRight){
		Random r = new Random();
		double lat = r.nextFloat() * (upLeft.lat - downRight.lat) + downRight.lat;
		double lon = r.nextFloat() * (downRight.lon-upLeft.lon) + upLeft.lon;
		
		return new Location(lat, lon);
	}
	
	public Location generateRandom(Location center){
//		return this.generateRandom(new Location(center.lat+0.005, center.lon-0.005), new Location(center.lat-0.005, center.lon+0.005));
		return this.generateRandom(new Location(center.lat+0.15, center.lon-0.15), new Location(center.lat-0.15, center.lon+0.15));
	}

	
	public String sendRequest(Location loc){
		String u = "http://192.241.173.181:8080/where?lat=" + loc.lat + "&lon=" + loc.lon + "&callback=?";
//		String u = "http://127.0.0.1:8080/where?lat=" + loc.lat + "&lon=" + loc.lon + "&callback=?";
		
		URL url;
		StringBuilder sb = new StringBuilder();
		JsonObject results = null;
		try {
			url = new URL(u);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			InputStream is = conn.getInputStream();
			BufferedReader bf = new BufferedReader(new InputStreamReader(is));
			String line;
			while((line = bf.readLine()) != null){
				sb.append(line);
			}			
			bf.close();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(sb.toString().length() > 1000){
			System.out.println(u);
			System.out.println("long response:\t" + sb.toString().length());
		}
		
		return sb.toString();		
	}
	
	public void runByCityCenter(){
		List<Location> locations = null;
		try {
//			locations = this.loadLatLonList("data\\latlon.txt");
			locations = this.loadBigCityLatLon("data\\bigCities.txt");
		} catch (NumberFormatException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println(locations.size() + "\tlocations got!!!");
		int j = 1;
		for(Location center : locations){			
			System.out.println(j + "\t locations processed");
			for(int i = 0; i < 20; i++){
				this.sendRequest(this.generateRandom(center));
				Random r = new Random();
				try {
					Thread.sleep((long) (r.nextFloat()*5000));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			j++;
		}
		
	}
	
	public List<Location> loadLatLonList(String filePath) throws NumberFormatException, IOException{
		List<Location> locations = new LinkedList<Location>();
		BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
		String line;
		
		while((line = br.readLine()) != null){
			String[] split = line.trim().split(" ");
			if(split.length < 2){
				continue;
			}
			String latStr = split[split.length-2].trim();
			String lonStr = split[split.length-1].trim();

//			System.out.println(latStr.length() + "\t" + lonStr.length());

			if(latStr.length() != 9 || lonStr.length() != 10){
				continue;
			}
			double lat = Integer.valueOf(latStr.substring(1)) / 1000000.0;
			double lon = Integer.valueOf(lonStr.substring(1)) / 1000000.0;
			
			if(latStr.charAt(0) == '-'){
				lat = -1 * lat;
			}
			
			if(lonStr.charAt(0) == '-'){
				lon = -1 * lon;
			}
			
			locations.add(new Location(lat, lon));
//			System.out.println(lat + "\t" + lon);
		}
		
		return locations;
	}
	
	public List<Location> loadBigCityLatLon(String filePath) throws NumberFormatException, IOException{
		List<Location> locations = new LinkedList<Location>();
		BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
		String line;
		
		while((line = br.readLine()) != null){
			String[] split = line.trim().split(" ");
			if(split.length != 2){
				continue;
			}
			String latStr = split[0].trim();
			String lonStr = split[1].trim();

//			System.out.println(latStr.length() + "\t" + lonStr.length());
			double lat = Float.valueOf(latStr);
			double lon = Float.valueOf(lonStr) * -1;
						
			locations.add(new Location(lat, lon));
			System.out.println(lat + "\t" + lon);
		}
		
		return locations;
	}
	
	public void run(){
		List<Location[]> locGroup = this.getLocations();
		for(int i = 0; i < 100; i++){					
//			System.out.println(this.generateRandom(loc1, loc2));
			for(Location[] city : locGroup){
				this.sendRequest(this.generateRandom(city[0], city[1]));
				Random r = new Random();
				try {
					Thread.sleep((long) (r.nextFloat()*1500));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
//		Location loc1 = new Location(37.6141019893, -122.395812287);
//		Location loc2 = new Location(39.6141019893, -121.395812287);		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Tester tester = new Tester();
//		double[] upLeft = {-34.35745687f, 87.3463456f};
//		double[] downRight = {94.254f, 128f};
//		tester.generateRandom(upLeft, downRight);
		tester.runByCityCenter();
		
		
	}

}
