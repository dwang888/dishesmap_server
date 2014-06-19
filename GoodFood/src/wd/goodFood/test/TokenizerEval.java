package wd.goodFood.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations.*;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class TokenizerEval {

	public String loadData(String path){
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(path)));
			String line;
			while((line = br.readLine()) != null){
				sb.append(line);
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sb.toString();
	}
	
	public SentenceDetector initiateSentenceDetector(String modelFile){
		SentenceDetectorME sentenceDetector = null;		
		try {
			InputStream modelInSent = new FileInputStream(modelFile);
			SentenceModel model = new SentenceModel(modelInSent);
			sentenceDetector = new SentenceDetectorME(model);
			modelInSent.close();
			}catch (IOException e) {
				e.printStackTrace();
			}		
		return sentenceDetector;
	}
	
	public Tokenizer initiateOpenNLPTokenizer(String modelFile){
		Tokenizer tokenizer = null;		
		try {
			InputStream modelInSent = new FileInputStream(modelFile);
			TokenizerModel model = new TokenizerModel(modelInSent);
			tokenizer = new TokenizerME(model);
			modelInSent.close();
			}catch (IOException e) {
				e.printStackTrace();
			}		
		return tokenizer;
	}
	
	public void initStanfordTokenizer(){
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	}
	
	public void evalMultiChunksSpeed(){
		String path = "data/rawData.txt";
		StringBuilder sb = new StringBuilder();
		SentenceDetector opennlpSentSpliter = initiateSentenceDetector("data/en-sent.bin"); 
		Tokenizer opennlpTokenizer = this.initiateOpenNLPTokenizer("data/en-token.bin");
		String result1 = "";
		long timeSent = 0;
		long timeToken = 0;
		long time = 0;
		long timeWhiteSpace = 0;
		
		long startSent;
		long endSent;
		long startToken;
		long endToken;
		
		
		
		long start = System.currentTimeMillis();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(path)));
			String line;
			while((line = br.readLine()) != null){				
				startSent = System.currentTimeMillis();
				String[] sents = opennlpSentSpliter.sentDetect(line);
				endSent = System.currentTimeMillis();
				timeSent += endSent-startSent;				
				
				for(String sent : sents){
					startToken = System.currentTimeMillis();
					String[] tokensWhite = sent.split(" ");
					endToken = System.currentTimeMillis();
					timeWhiteSpace += endToken - startToken;
					startToken = System.currentTimeMillis();
					String[] tokens = opennlpTokenizer.tokenize(sent);
					endToken = System.currentTimeMillis();
					timeToken += endToken - startToken;					
//					for(int i = 0; i < tokens.length; i++){
//						result1 += tokens[i];
//					}
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		
		System.out.println(result1.length());	
		System.out.println(timeSent);
		System.out.println(timeWhiteSpace);
		System.out.println(timeToken);
		System.out.println(end-start);	
		
		
		
		
		sb = new StringBuilder();
		
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit");
		props.setProperty("tokenize.options","ptb3Escaping=false");
		
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    
	    start = System.currentTimeMillis();
		timeSent = 0;
		timeToken = 0;
		time = 0;
		String result2 = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(path)));
			String line;
			while((line = br.readLine()) != null){
//				startSent = System.currentTimeMillis();
//				System.out.println("\n\n----------->");
//				System.out.println(line);
				Annotation document = new Annotation(line);
				pipeline.annotate(document);
				List<CoreMap> sentences = document.get(SentencesAnnotation.class);
				for(CoreMap sentence: sentences) {
					for (CoreLabel token: sentence.get(TokensAnnotation.class)){
				        // this is the text of the token
				    	String word = token.get(TextAnnotation.class);
//				    	System.out.print(word + " ");
//				    	result2 += word;
				    }
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		end = System.currentTimeMillis();
//		System.out.println(timeSent);
//		System.out.println(timeToken);
		System.out.println(result2.length());
		System.out.println(end-start);
	}
	
	public void evalSingleChunkSpeed(){
		String rawText = this.loadData("data/rawData.txt");
		SentenceDetector opennlpSentSpliter = initiateSentenceDetector("data/en-sent.bin"); 
		Tokenizer opennlpTokenizer = this.initiateOpenNLPTokenizer("data/en-token.bin");
		System.out.println(rawText.length());
		
		long start = System.currentTimeMillis();
		String result1 = "";
		long startSent = System.currentTimeMillis();
		String[] sents = opennlpSentSpliter.sentDetect(rawText);
		long endSent = System.currentTimeMillis();
		long startToken = System.currentTimeMillis();
		for(String sent : sents){
			String[] tokens = opennlpTokenizer.tokenize(sent);
			for(int i = 0; i < tokens.length; i++){
				result1 += tokens[i];
			}
		}
		long endToken = System.currentTimeMillis();
		long end = System.currentTimeMillis();
		System.out.println(result1.length());		
		System.out.println(endSent-startSent);
		System.out.println(endToken-startToken);
		System.out.println(end-start);		
		
		
		
		String result2 = "";
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit");
		props.setProperty("normalizeParentheses", "false");
		props.setProperty("normalizeOtherBrackets", "false");
		
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    
	    start = System.currentTimeMillis();
		Annotation document = new Annotation(rawText);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for(CoreMap sentence: sentences) {
			for (CoreLabel token: sentence.get(TokensAnnotation.class)){
		        // this is the text of the token
		    	String word = token.get(TextAnnotation.class);
		    	result2 += word;
//		        String pos = token.get(PartOfSpeechAnnotation.class);
//		        String ne = token.get(NamedEntityTagAnnotation.class);
		    }

		}		
		end = System.currentTimeMillis();
		System.out.println(result2.length());		
		System.out.println(end-start);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TokenizerEval eval = new TokenizerEval();
		eval.evalMultiChunksSpeed();
	}

}
