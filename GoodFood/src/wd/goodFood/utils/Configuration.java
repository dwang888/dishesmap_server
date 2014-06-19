package wd.goodFood.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Configuration {

	private Properties properties;
	private FileInputStream inputFile;
	private FileOutputStream outputFile;
	
	public Configuration(){
		properties = new Properties();
	}
	
	public Configuration(String filePath){
		properties = new Properties();
		try{
			inputFile = new FileInputStream(filePath);
			properties.load(inputFile);
			inputFile.close();
		}catch(FileNotFoundException e){
			System.out.println(e + "file " + filePath + " not found!");
			e.printStackTrace();
		}catch(IOException e){
			System.out.println("loading file " + filePath + " failed!");
			e.printStackTrace();
		}
	}	

	/**
	 * override the method 
	 * @param key
	 * @return value of key
	 * */
	public String getValue(String key){
		if(properties.containsKey(key)){
			return properties.getProperty(key);
		}else{
			return "";
		}
	}
	
	public void setValue(String key, String value){
		properties.setProperty(key, value);
	}
	
	public void saveFile(String fileName, String description){
		try{
			outputFile = new FileOutputStream(fileName);
			properties.store(outputFile, description);
			outputFile.close();
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		//Configuration test = new Configuration("conll2008.properties");

		
	}

}
