package wd.goodFood.utils;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * load in GATE xml files, output opennlp desired format
 * */
public class TrainingXmlConvertor {

	public static class SortedAnnotationList extends Vector {
		/**
		* to store the generated annotation results
		*/
	    public SortedAnnotationList() {
	      super();
	    } // SortedAnnotationList
  
	    public boolean addSorted(Annotation annot) {
		      Annotation currAnot = null;
		      long annotStart = annot.getStartNode().getOffset().longValue();
		      long currStart;
		      // insert
		      for (int i=0; i < size(); ++i) {
		        currAnot = (Annotation) get(i);
		        currStart = currAnot.getStartNode().getOffset().longValue();
		        if(annotStart < currStart) {
		          insertElementAt(annot, i);
		          return true;
		        }
		      }

		      int size = size();
		      insertElementAt(annot, size);
		      return true;
		    }
	    
	  } // SortedAnnotationList
	
	public String generateGateSimpleXML(String dirPath) throws GateException, IOException{
		Gate.init();
		System.out.println("GATE loaded successfully");
		Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), "ANNIE").toURI().toURL());
		SerialAnalyserController annieController = (SerialAnalyserController)Factory.createResource("gate.creole.SerialAnalyserController",
				Factory.newFeatureMap(), Factory.newFeatureMap(), "ANNIE_" + Gate.genSym());

		FeatureMap paramTokenizer = Factory.newFeatureMap();
		ProcessingResource prTokenizer = (ProcessingResource)Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", paramTokenizer);
		annieController.add(prTokenizer);
		
		FeatureMap paramSS = Factory.newFeatureMap();
		ProcessingResource prSS = (ProcessingResource)Factory.createResource("gate.creole.splitter.SentenceSplitter", paramSS);
		annieController.add(prSS);
		
		Corpus corpus = (Corpus) Factory.createResource("gate.corpora.CorpusImpl");
		File dir = new File(dirPath);
		StringBuilder allXMLContent = new StringBuilder();
		
		for(File f : dir.listFiles()){
			//get corpus for GATE
			if(f.getAbsolutePath().endsWith(".xml")){
				FeatureMap params = Factory.newFeatureMap();
				URL u = new URL("file:\\" + f.getAbsolutePath());
				params.put("sourceUrl", u);
				params.put("preserveOriginalContent", new Boolean(true));//to store the original content
				params.put("collectRepositioningInfo", new Boolean(true));//to store the automated annotated content
				gate.Document doc = (gate.Document)Factory.createResource("gate.corpora.DocumentImpl", params);
				corpus.add(doc);
//				System.out.println(f.getAbsolutePath());
			}
		}
		
//		System.exit(0);
		annieController.setCorpus(corpus);		
		annieController.execute();//kick off the pipeline! The generated data will be stored in instance corpus
		Iterator itor = corpus.iterator();//get all the information including original text and new generated info from corpus.
		int count = 0;
		gate.Document docTmp;
		AnnotationSet defaultAnnotSet;
		
		while(itor.hasNext()){
			docTmp = (gate.Document) itor.next();
			defaultAnnotSet = docTmp.getAnnotations();
			Set annotTypesRequired = new HashSet();
		    annotTypesRequired.add("Sentence");
		    annotTypesRequired.add("Food");
		    annotTypesRequired.add("Token");
		    Set<Annotation> annots = new HashSet<Annotation>(defaultAnnotSet.get(annotTypesRequired));
		    ++count;	    	
		    String xmlDocStr = docTmp.toXml(annots);
		    allXMLContent.append("<Article>\n" + xmlDocStr + "\n</Article>\n");
		}
		
		String finalXMLContent = "<ReviewCorpus>\n" + allXMLContent.toString() + "\n</ReviewCorpus>";
//		System.out.println(finalXMLContent);
		return finalXMLContent;
	}	
	
	/**
	 * we get two results file here: one is in order of original xml files; another is shuffled and has no duplicated
	 * */
	public String gateSimpleXML2OpennlpNER(String inputPath, String outputPath) throws Exception, IOException{
		//convert gateXml to OpenNLP NER format
		File xmlFile = new File(inputPath);
//		File outputFile = new File(outputPath);
//		BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath), "utf-8"));
//		BufferedWriter bwNewLabeled = new BufferedWriter(new FileWriter(outputPath+".shuffled"));
		BufferedWriter bwNewLabeled = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath+".shuffled"), "utf-8"));//for shuffled
		BufferedWriter bwDic = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath+".dic"), "utf-8"));//for shuffled
		BufferedWriter bwWordFreq = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath+".wordfreq"), "utf-8"));//for frequent word
		Map<String, Integer> dic = new HashMap<String, Integer>();
		Map<String, Integer> wordFreq = new HashMap<String, Integer>();
		Map<String, Integer> wordFreqTmp = new HashMap<String, Integer>();
		
		DocumentBuilderFactory docbuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docbuilderFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(xmlFile);
		doc.getDocumentElement().normalize();
		StringBuilder outputStr = new StringBuilder();		
		NodeList articles = doc.getElementsByTagName("Article");
		int countLine = 0;
		Set<String> reviewSet = new HashSet<String>();
		int numLike = 0;
		int numDislike = 0;
		int numUnknown = 0;
		int numNER = 0;
		
		for(int m = 0; m < articles.getLength(); m++){
			String sents = "";
			Element articleElem = (Element)articles.item(m);
			NodeList paragraphs = articleElem.getElementsByTagName("paragraph");
			for(int i = 0; i < paragraphs.getLength(); i++){
				Node paragraph = paragraphs.item(i);
				Element paragraphElem = (Element)paragraph;
				NodeList sentences = paragraphElem.getElementsByTagName("Sentence");
				for(int j = 0; j < sentences.getLength(); j++){
					int flag = 0;
					wordFreqTmp.clear();
					Node sentence = sentences.item(j);
					Element sentenceElem = (Element)sentence;
//					NodeList foods = sentenceElem.getElementsByTagName("Food");
					NodeList phrases = sentenceElem.getChildNodes();
					StringBuilder strSent = new StringBuilder();
					for(int k = 0; k < phrases.getLength(); k++){
						Node phrase = phrases.item(k);
						if(phrase.getNodeType() != Node.ELEMENT_NODE){
							continue;
						}
						Element phraseElem = (Element)phrase;
//						Element foodElem = (Element)food;
//						System.out.println(phrase.getNodeName());
						if(phrase.getNodeName().equalsIgnoreCase("#text") || phrase.getNodeName().equalsIgnoreCase("Token")){
							strSent.append(phraseElem.getTextContent() + " ");
							String wordContext = phraseElem.getTextContent();//get context word frequency
							if(!wordFreqTmp.containsKey(wordContext)){
								wordFreqTmp.put(wordContext, 0);
							}
							wordFreqTmp.put(wordContext, wordFreqTmp.get(wordContext)+1);
//							System.out.println("|" + phraseElem.getAttribute("string") + "|");
						}else if(phrase.getNodeName().equalsIgnoreCase("Food")){
							numNER++;
							flag = 1;
							//for all food							
//							strSent.append("<START:");
//							strSent.append(phrase.getNodeName());
//							strSent.append("> ");
//							strSent.append(phrase.getTextContent());
//							strSent.append(" <END> ");
							//generate food name dictionary
							String foodName = phrase.getTextContent();//get food name frequency
							if(!dic.containsKey(foodName)){
								dic.put(foodName, 0);
							}
							dic.put(foodName, dic.get(foodName)+1);
							if(phraseElem.getAttribute("like").equalsIgnoreCase("yes")){
								numLike++;
								strSent.append("<START:");
								strSent.append(phrase.getNodeName() + "_like");
								strSent.append("> ");
								strSent.append(phrase.getTextContent());
								strSent.append(" <END> ");
							}
//							else if(phraseElem.getAttribute("like").equalsIgnoreCase("no")){
//								numDislike++;
//								strSent.append("<START:");
//								strSent.append(phrase.getNodeName() + "_dislike");
//								strSent.append("> ");
//								strSent.append(phrase.getTextContent());
//								strSent.append(" <END> ");
//							}
//							else{
//								numUnknown++;
//								strSent.append("<START:");
//								strSent.append(phrase.getNodeName() + "_unknown");
//								strSent.append("> ");
//								strSent.append(phrase.getTextContent());
//								strSent.append(" <END> ");
//							}
						}
					}
//					System.out.println("final sentence:\t" + strSent.toString());
//					bw.write(strSent.toString() + "\n");
//					System.out.println(countLine++);
//					System.out.println(strSent.toString());
					//harvest word freq in currernt sentences
					if(flag == 1 && !wordFreqTmp.isEmpty()){
						for(String key : wordFreqTmp.keySet()){
							if(!wordFreq.containsKey(key)){
								wordFreq.put(key, 0);
							}
							wordFreq.put(key, wordFreq.get(key)+wordFreqTmp.get(key));
						}
					}					
					
					String s = strSent.toString();
					if(this.hasLegalTagPares(s))
						sents += s + "\n";
					if(this.hasLegalTagPares(s)){
						outputStr.append(s + "\n");
					}else{
						System.out.println("LEGAL tags in this line:\t" + s);
					}					
				}			
			}
			
			reviewSet.add(sents);			
			outputStr.append("\n");
		}		
//		System.out.println(outputStr.toString());
		StringBuilder strBuilder = new StringBuilder();
		List<String> reviewsShuffled = new ArrayList<String>(reviewSet);
		Collections.shuffle(reviewsShuffled);
		
		for(String s : reviewsShuffled){
			strBuilder.append(s);
			strBuilder.append("\n");			
		}
		bwNewLabeled.write(strBuilder.toString());
		
		bw.write(outputStr.toString());
		bw.close();
		bwNewLabeled.close();
		
		//output the dictionary
		List<Entry<String, Integer>> foodSorted = new ArrayList<Entry<String, Integer>>(dic.entrySet());
		Collections.sort(foodSorted, new Comparator<Entry<String, Integer>> (){
			public int compare(Entry<String, Integer> e1, Entry<String, Integer> e2){
				return e2.getValue() - e1.getValue();
			}
		});
		for(Entry<String, Integer> e : foodSorted){
			bwDic.write(e.getKey().trim() + "\n");
			System.out.println(e.getValue() + "\t" + e.getKey());
		}
		bwDic.close();
		
		//output the context word freq
		List<Entry<String, Integer>> wordSorted = new ArrayList<Entry<String, Integer>>(wordFreq.entrySet());
		Collections.sort(wordSorted, new Comparator<Entry<String, Integer>> (){
			public int compare(Entry<String, Integer> e1, Entry<String, Integer> e2){
				return e2.getValue() - e1.getValue();
			}
		});
		for(Entry<String, Integer> e : wordSorted){
			bwWordFreq.write(e.getKey().trim() + "\t" + e.getValue() + "\n");
			System.out.println(e.getValue() + "\t" + e.getKey());
		}
		bwWordFreq.close();
		
		System.out.println("NERs found:\n" +
				"num of like:\t" + numLike + 
				"\nnum of dislike:\t" + numDislike + 
				"\nnum of unknown:\t" + numUnknown + 
				"\nnum of NERs:\t" + numNER);
		return outputStr.toString();
	}
	
	
	
	/**
	 * to make sure current line has equal number of <START> and <END> tags
	 * */
	public boolean hasLegalTagPares(String line){
		if(line.trim().contains("\n") || line.trim().contains("\r")){
			return false;
		}
		
		String start = "<START";
		String end = "<END>";
		Pattern ptnStart = Pattern.compile(start);
		Pattern ptnEnd = Pattern.compile(end);
		Matcher mStart = ptnStart.matcher(line.trim());
		Matcher mEnd = ptnEnd.matcher(line.trim());
		
		
		int numS = 0;
		int numE = 0;
		
		while(mStart.find()){
			numS++;
		}
		
		while(mEnd.find()){
			numE++;
		}
		
		if(numS != numE){
			System.out.println(numS + " " + numE);
			System.out.println(line);
			return false;
		}		
		
		return true;
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, Exception {
		String gateXMLDirPath = args[0];
		String gateSimpleXMLPath = args[1];
		String outputPath = args[2];
		
		
		TrainingXmlConvertor convertor = new TrainingXmlConvertor();
		
		String allXMLStr = convertor.generateGateSimpleXML(gateXMLDirPath);
		Writer gateSimpleXMLWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(gateSimpleXMLPath), "utf-8"));
		gateSimpleXMLWriter.write(allXMLStr);
		gateSimpleXMLWriter.close();		
		convertor.gateSimpleXML2OpennlpNER(gateSimpleXMLPath, outputPath);
		
	}

}
