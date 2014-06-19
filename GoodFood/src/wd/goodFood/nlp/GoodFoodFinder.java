package wd.goodFood.nlp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.swing.event.ListSelectionEvent;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;
import wd.goodFood.entity.Review;

/**
 * use single model of opennlp to find good food only
 * */
public class GoodFoodFinder {

	private SentenceDetector sentSplitter;
	private Tokenizer tokenizer;
	private POSTagger posTagger;
	private NameFinderME NETagger;
	private StanfordCoreNLP stanfordPipeline;
//	private Review currentReview;
	
	public GoodFoodFinder(String sentSplitterPath, String tokenizerPath, String POSPath, String NETaggerPath){		
		try {
			this.setSentSplitter(this.initiateSentenceDetector(sentSplitterPath));
			this.setTokenizer(this.initiateTokenizer(tokenizerPath));
//			this.tokenizer = SimpleTokenizer.INSTANCE;
//			this.tokenizer = WhitespaceTokenizer.INSTANCE;
//			this.setPosTagger(this.initializePOSTagger(POSPath));
			this.NETagger = this.initiateNETagger(NETaggerPath);
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public GoodFoodFinder(String sentSplitterPath, String tokenizerPath, String NETaggerPath){		
		this(sentSplitterPath, tokenizerPath, null, NETaggerPath);
	}
	
	public GoodFoodFinder(String NETaggerPath){		
		try {
			this.stanfordPipeline = this.initStanfordNLP();
			this.NETagger = this.initiateNETagger(NETaggerPath);
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public StanfordCoreNLP initStanfordNLP(){
		//only use sentence splitter, tokenizer here.
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit");
		props.put("tokenize.options","ptb3Escaping=false");
		props.put("tokenize.options","untokenizable=noneKeep");
		
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    return pipeline;
	}
	
	public synchronized Review process(Review r){
		long start = System.currentTimeMillis();
		double score = 0;
		int numNER = 0;
		double averageScore = 0;
		long timeSent = 0;
		long timeToken = 0;
		long timeNE = 0;
		long timeProb = 0;
		long s;
		long e;
//		r.resetReview();//needed?
		if(r.getReviewStr() != null || r.getAllStrs().size() == 0){
			//if just raw text in review, run sent splitter and tokenizer first
			List<String[]> tokenizedSents = new ArrayList<String[]>();
			s = System.currentTimeMillis();
			Annotation document = new Annotation(r.getReviewStr());
			this.stanfordPipeline.annotate(document);
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			for(CoreMap sentence: sentences) {
				List<String> tokens = new ArrayList<String>();
				for (CoreLabel token: sentence.get(TokensAnnotation.class)){
			    	String word = token.get(TextAnnotation.class);
			    	tokens.add(word);
//			    	System.out.print(word + " ");
//			    	result2 += word;
			    }
				tokenizedSents.add(tokens.toArray(new String[tokens.size()]));
			}
			r.setAllStrs(tokenizedSents);
			e = System.currentTimeMillis();
			timeToken = e - s;
		}
		
		for(int m = 0; m < r.getAllStrs().size(); m++){
			//process each sentence
			String[] sent = r.getAllStrs().get(m);// get tokenized sentences
			s = System.currentTimeMillis();
			Span[] spans = NETagger.find(sent);
			e = System.currentTimeMillis();
			timeNE += e-s;
			r.getAllNERSpans().add(spans);
			s = System.currentTimeMillis();
			double[] probsTmp = NETagger.probs(spans);
			e = System.currentTimeMillis();
			timeProb += e-s;
			r.getAllNERProbs().add(probsTmp);
			numNER += spans.length;
			
			for(double d : probsTmp){
				score += d;
			}					
		}
		
		NETagger.clearAdaptiveData();
		r.attachTag2Str();
		if(numNER != 0){
			averageScore = score / numNER;
		}
		r.setScoreNER(averageScore);
//		for(String[] sent : r.getAllNERStrs()){
//			System.out.println(Arrays.toString(sent));
//		}
		long end = System.currentTimeMillis();
//		if((end-start) > 2){
//			System.out.println("+++++++++++++++++");
//			System.out.println(timeToken);
//			System.out.println(timeNE);
//			System.out.println(timeProb);
//			System.out.println(end-start);
//		}
		
		return r;
	}
	
	public synchronized Review process(String reviewStr){
		Review r = new Review(reviewStr);
		this.process(r);		
		return r;
	}

	
	public synchronized Review processOpennlp(Review r){
		long start = System.currentTimeMillis();
		double score = 0;
		int numNER = 0;
		double averageScore = 0;
		long timeSent = 0;
		long timeToken = 0;
		long timeNE = 0;
		long timeProb = 0;
		long s;
		long e;
//		r.resetReview();//needed?
		if(r.getReviewStr() != null || r.getAllStrs().size() == 0){
			//if just raw text in review, run sent splitter and tokenizer first
			s = System.currentTimeMillis();
			String[] sents = this.getSentSplitter().sentDetect(r.getReviewStr());
			e = System.currentTimeMillis();
			timeSent += e-s;
			
			List<String[]> tokenizedSents = new ArrayList<String[]>();
			for(int i = 0; i < sents.length; i++){
				//tokenizing
				s = System.currentTimeMillis();
				String[] tokens = this.getTokenizer().tokenize(sents[i]);
				e = System.currentTimeMillis();
				timeToken += e-s;
//				System.out.println("sentence:----------->\t" + sents[i]);
//				for(int j = 0; j < tokens.length; j++){
//					System.out.println(tokens[j] + " ");
//				}
				tokenizedSents.add(tokens);
			}
			r.setAllStrs(tokenizedSents);
		}
		
		for(int m = 0; m < r.getAllStrs().size(); m++){
			//process each sentence
			String[] sent = r.getAllStrs().get(m);// get tokenized sentences
			s = System.currentTimeMillis();
			Span[] spans = NETagger.find(sent);
			e = System.currentTimeMillis();
			timeNE += e-s;
			r.getAllNERSpans().add(spans);
			s = System.currentTimeMillis();
			double[] probsTmp = NETagger.probs(spans);
			e = System.currentTimeMillis();
			timeProb += e-s;
			r.getAllNERProbs().add(probsTmp);
			numNER += spans.length;
			
			for(double d : probsTmp){
				score += d;
			}					
		}
		
		NETagger.clearAdaptiveData();
		r.attachTag2Str();
		if(numNER != 0){
			averageScore = score / numNER;
		}
		r.setScoreNER(averageScore);
//		for(String[] sent : r.getAllNERStrs()){
//			System.out.println(Arrays.toString(sent));
//		}
		long end = System.currentTimeMillis();
//		if((end-start) > 2){
//			System.out.println("+++++++++++++++++");
//			System.out.println(timeSent);
//			System.out.println(timeToken);
//			System.out.println(timeNE);
//			System.out.println(timeProb);
//			System.out.println(end-start);
//		}
		
		return r;
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
	
	public Tokenizer initiateTokenizer(String modelFile){
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
	
	public POSTagger initializePOSTagger(String modelPath) throws InvalidFormatException, IOException{
		if(modelPath == null){
			return null;
		}
		POSTaggerME tagger = null;
		InputStream modelIn = new FileInputStream(modelPath);
		POSModel model = new POSModel(modelIn);
		modelIn.close();
		tagger = new POSTaggerME(model);
		return tagger;
	}
	
	public NameFinderME initiateNETagger(String modelPath) throws InvalidFormatException, IOException{
		InputStream modelIn = new FileInputStream(modelPath);
		TokenNameFinderModel NEModel = new TokenNameFinderModel(modelIn);
		NameFinderME NETagger = new NameFinderME(NEModel);
		modelIn.close();
		return NETagger;
	}	
	
	public SentenceDetector getSentSplitter() {
		return sentSplitter;
	}

	public void setSentSplitter(SentenceDetector sentSplitter) {
		this.sentSplitter = sentSplitter;
	}

	public Tokenizer getTokenizer() {
		return tokenizer;
	}

	public void setTokenizer(Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}

	public POSTagger getPosTagger() {
		return posTagger;
	}

	public void setPosTagger(POSTagger posTagger) {
		this.posTagger = posTagger;
	}

	public NameFinderME getNETagger() {
		return NETagger;
	}

	public void setNETagger(NameFinderME nETagger) {
		NETagger = nETagger;
	}

//	public Review getCurrentReview() {
//		return currentReview;
//	}
//
//	public void setCurrentReview(Review currentReview) {
//		this.currentReview = currentReview;
//	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
