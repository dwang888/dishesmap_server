package wd.goodFood.utils;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

//import com.gargoylesoftware.htmlunit.BrowserVersion;
//import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
//import com.gargoylesoftware.htmlunit.WebClient;
//import com.gargoylesoftware.htmlunit.xml.XmlPage;
//
//
public class CityGrid {
//
//	WebClient webClient = null;
//	String reviewAPIStr = "http://api.citygridmedia.com/content/reviews/v2/search/where?where=10016&publisher=test&rpp=50&tag=1722&review_type=user_review";
//	
//	/**
//	 * readin the zip codes in csv file
//	 * use supercsv lib
//	 * @throws IOException 
//	 * */
//	@SuppressWarnings("finally")
//	public List<Map<String, String>> loadZip(String filePath){
//		String regexZip = "\\d{5}";
//		Pattern ptn = Pattern.compile(regexZip);
//		
//		ICsvMapReader reader;
//		try {
//			reader = new CsvMapReader(new FileReader(filePath), CsvPreference.EXCEL_PREFERENCE);
//			String[] header = reader.getHeader(true);
//			List<Map<String, String>> csvList = new ArrayList<Map<String, String>>();
//			Map<String, String> hashTmp;
//			int numZip = 1;
//			
//			while((hashTmp = reader.read(header)) != null){
//				numZip++;
//				if(ptn.matcher(hashTmp.get("zip code")).find()){
//					csvList.add(hashTmp);
//				}
//			}
//			
//			System.out.println(csvList.size() + "\t valid zips loaded from\t" + numZip + "\t zips");		
//			return csvList;
//			
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} 
//
//		return null;
//	}
//	
//	protected WebClient createWebClient(){
//		//following the scraper style
//		webClient = new WebClient(BrowserVersion.FIREFOX_3_6); //(BrowserVersion.INTERNET_EXPLORER_8); //
//
//		webClient.setTimeout(180000); //3 min, used twice, first for connection, second for retrieval
//		try {
//			webClient.setUseInsecureSSL(true);
//			webClient.setRedirectEnabled(true); 
//			webClient.setCssEnabled(false);
//		} catch (GeneralSecurityException ex) {
//			//ignore it, continue
//		}		
//		webClient.setJavaScriptEnabled(false); //by default, it is enabled
//		webClient.setJavaScriptTimeout(30000); //timeout for executing java script
//		webClient.setThrowExceptionOnScriptError(false); //if throw exception when script occures
//		return webClient;
//	}
//	
//	public List<String> fetchReviews(Map<String, String> params){
//		WebClient browser = this.createWebClient();		
////		HtmlPage page = browser.getPage(this.reviewAPIStr);
//		String reviewAPIStr = "http://api.citygridmedia.com/content/reviews/v2/search/where?";
//		List<String> reviews = new ArrayList<String>();
//		
//		for(String s : params.keySet()){
//			//add all params from hash
//			reviewAPIStr += s + "=" + params.get(s) + "&";
//		}
//		
//		XmlPage xmlPage;
//		try {
//			xmlPage = (XmlPage) browser.getPage(reviewAPIStr);
//			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();		
//			Document doc = xmlPage.getXmlDocument();
//			doc.getDocumentElement().normalize();
//			NodeList nodes = doc.getElementsByTagName("review_text");
//			for(int i = 0; i < nodes.getLength(); i++){
//				Node node = nodes.item(i);
//				reviews.add(node.getTextContent());
//			}
//		} catch (FailingHttpStatusCodeException e) {
//			e.printStackTrace();
//		} catch (MalformedURLException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (ParserConfigurationException e) {
//			e.printStackTrace();
//		}
//		
//		return reviews;
//	}
//	
//	public void collectReviews(String zipFilePath, String outputPath) throws FailingHttpStatusCodeException, MalformedURLException, IOException, ParserConfigurationException, SAXException, InterruptedException{
//		Map<String, String> params = new HashMap<String, String>();
//		params.put("publisher", "test");
//		params.put("rpp", "50");
//		params.put("tag", "1722");
//		params.put("review_type", "user_review");
//		
//		BufferedWriter bw  = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath), "UTF-8"));
//		List<String> reviews = new ArrayList<String>();
//		int numReviews = 0;
//		int numTh = 1;
//		List<Map<String, String>> zipInfos = this.loadZip(zipFilePath);
//		
//		for(Map<String, String> zipInfo : zipInfos){
//			numTh++;
//			String zip = zipInfo.get("zip code");
//			params.put("where", zip);
//			reviews = this.fetchReviews(params);
//			Thread.sleep(100);
//			if(null != reviews){
//				numReviews += reviews.size();
//				System.out.println("fetching\t" + numTh + "th" + "\treviews from zip code:\t" + zipInfo.toString());
//				for(String review : reviews){
//					bw.write(review + "\n------\n");
//				}
//			}else{
//				System.out.println("get no reviews in this area:\t" + zipInfo.toString());
//			}
//		}
//		
//		bw.close();
//		System.out.println(numReviews + "\t reviews collected!");
//	}
//	
	public static void main(String[] args) throws Exception, IOException {
//		// TODO Auto-generated method stub
//		CityGrid cg = new CityGrid();
//		
//		
////		cg.getReviews();
//		String zipPath = "D:\\projects\\FoodSearch\\data\\resources\\zips.csv";
//		String reviewOutputPath = "D:\\projects\\FoodSearch\\data\\resources\\allReviews.txt";
//		cg.collectReviews(zipPath, reviewOutputPath);
	}
//
}
