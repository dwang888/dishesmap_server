package wd.goodFood.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * a bundle of tools for file processing: clean, convert, .....
 * */
public class FileTools {
	public String removeEmpty(String s){
		return s.replaceAll("\\s", "");
	}
	
	/**
	 * remove all the file A's sentences from file B
	 * @throws IOException 
	 * */
	public void removeFileFromFile(String APath, String BPath, String resultPath) throws IOException{
//		BufferedReader brA = new BufferedReader(new FileReader(APath));
//		BufferedReader brB = new BufferedReader(new FileReader(BPath));
		BufferedReader brA = new BufferedReader(new InputStreamReader(new FileInputStream(APath), "utf-8"));
		BufferedReader brB = new BufferedReader(new InputStreamReader(new FileInputStream(BPath), "utf-8"));
//		BufferedWriter bwResult = new BufferedWriter(new FileWriter(resultPath));
		BufferedWriter bwResult  = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultPath), "utf-8"));
		
		System.out.println("processing file...");
		
		String line;
		String strTmp = "";
		Map<String, String> allStrs = new HashMap<String, String>();
//		List<String> labeled = new ArrayList<String>();
		Set<String> labeled = new HashSet<String>();
		Set<String> legalUnlabeled = new HashSet<String>();
		
		int numRemoved = 0;
		int numLegalUnLabeled = 0;
		int numAllUnlabeled = 0;
		int flag = 0;
		
		while((line = brA.readLine()) != null){
			//load labelled data
			if(line.trim().equalsIgnoreCase("")){
				if(!strTmp.matches("^\\s*$")){
					labeled.add(this.removeEmpty(strTmp));
				}				
				strTmp = "";
			}else{
				strTmp += line.trim() + "\n";
			}
		}
		
		strTmp = "";
		System.out.println(labeled.size() + "\t labeled reviews loaded");
		
		while((line = brB.readLine()) != null){
			//process unlabelled data
			if(line.trim().equalsIgnoreCase("------")){
				numAllUnlabeled++;
				flag = 0;
				if(!strTmp.matches("^\\s*$")){
					String denseUnlabeled = this.removeEmpty(strTmp);
					for(String labeledStr : labeled){
						if(denseUnlabeled.indexOf(labeledStr) >= 0 && Math.abs(denseUnlabeled.length()-labeledStr.length()) < 10){							
							flag = 1;
//							System.out.println("labeled:\t" + labeledStr);
//							System.out.println("unlabeled:\t" + strTmp);
							break;
						}
					}
					if(flag == 0){
						numLegalUnLabeled++;
						legalUnlabeled.add(strTmp);
//						bwResult.write(strTmp + "------\n");
					}
				}				
				strTmp = "";
			}else{
				strTmp += line.trim() + "\n";
			}
		}
		
		
		
		for(String s : legalUnlabeled){
			bwResult.write(s);
			bwResult.write("------\n");
			
		}
		
		
		
		System.out.println(numAllUnlabeled + "\t unlabeled reviews loaded");		
		System.out.println(numLegalUnLabeled + "\t unlabeled reviews left now");
		System.out.println((numAllUnlabeled - numLegalUnLabeled) + "\t unlabeled reviews removed");
		System.out.println(legalUnlabeled.size() + "\t non duplicated unlabeled reviews left");
		System.out.println("all done!");
		
		brA.close();
		brB.close();
		bwResult.close();
	}
	
	/**
	 * create review file to be annotated,
	 * @throws IOException 
	 * */
	public void generateUnlabeledDate(String intputPath, String outputDirPath) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(intputPath));
		String line;
		String strTmp = "";
		Set<String> reviews = new HashSet<String>();
		
		while((line = br.readLine()) != null){
			if(line.trim().equalsIgnoreCase("------")){
				if(!strTmp.matches("^\\s*$")){
					reviews.add(strTmp);
				}				
				strTmp = "";
			}else{
				strTmp += line.trim() + "\n";
			}
		}
		
		String[] strs = reviews.toArray(new String[0]);
		int topN = 3900;
		for(int i = 1; i <= topN; i+=1){
			String fileName = outputDirPath+File.separator+(1100+i)+".txt";
			BufferedWriter bw  = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
			bw.write(strs[i]);
			bw.close();
		}
		
		BufferedWriter bwUnlabelRemain  = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(intputPath + ".remain"), "UTF-8"));
		for(int i = topN+1; i < strs.length; i++){
			bwUnlabelRemain.write(strs[i].trim());
			bwUnlabelRemain.write("\n------\n");
		}
		
		bwUnlabelRemain.close();
		br.close();
		System.out.println("unlabelled files created!");
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String inputFilePath = args[0];
		String outputDirPath = args[1];
		FileTools tools = new FileTools();
		tools.generateUnlabeledDate(inputFilePath, outputDirPath);
		
//		String labeledPath = args[0];
//		String unlabeledPath = args[1];
//		String resultPath = args[2];
//		
//		System.out.println("processing");
//		tools.removeFileFromFile(labeledPath, unlabeledPath, resultPath);
		
	}

}
