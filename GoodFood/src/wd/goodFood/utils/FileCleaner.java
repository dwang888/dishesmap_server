package wd.goodFood.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileCleaner {

	public void clean(String filePath, String outputPath) throws IOException{
		Map<String, Integer> entityFreq = new HashMap<String, Integer>();
		List<Pattern> ptns = new ArrayList<Pattern>();
		
		String regexStr = "\t  \t[ \\w']+ \t";
		Pattern ptn = Pattern.compile(regexStr);
		ptns.add(ptn);
		String regexStr2 = "[A-Z]\\s+(\\w+)";
		Pattern ptn2 = Pattern.compile(regexStr2);
		ptns.add(ptn2);
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputPath)));
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String line;
		
		while((line = br.readLine()) != null){
//			System.out.println(line.trim());
			for(int i = 0; i < ptns.size(); i++){
				Matcher m = ptns.get(i).matcher(line);
				if(m.find()){
					String s = m.group().trim();
					if(i == 1){
						s = m.group(1).trim();
//						System.out.println(m.group(1));
					}
					if(s != null && s != "" && s != " " && !entityFreq.containsKey(s)){
						bw.write(s.trim() + "\n");
						entityFreq.put(s, 1);
					}
//					System.out.println(m.group().trim());
				}else{
//					System.out.println(line);
				}
			}
			
		}
		
		bw.close();
		br.close();
	}
	
	
	public static void main(String[] args) throws IOException {
		String filePath = "D:\\projects\\FoodSearch\\data\\misc\\accessdata.fda.gov.txt";
		String outputPath = "D:\\projects\\FoodSearch\\data\\misc\\seafoodList.txt";
		FileCleaner fc = new FileCleaner();
		fc.clean(filePath, outputPath);
	}

}
