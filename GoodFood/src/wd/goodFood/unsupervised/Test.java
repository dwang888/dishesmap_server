package wd.goodFood.unsupervised;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinderEvaluator;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.eval.FMeasure;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.BigramNameFeatureGenerator;
import opennlp.tools.util.featuregen.CachedFeatureGenerator;
import opennlp.tools.util.featuregen.OutcomePriorFeatureGenerator;
import opennlp.tools.util.featuregen.PreviousMapFeatureGenerator;
import opennlp.tools.util.featuregen.SentenceFeatureGenerator;
import opennlp.tools.util.featuregen.TokenClassFeatureGenerator;
import opennlp.tools.util.featuregen.TokenFeatureGenerator;
import opennlp.tools.util.featuregen.WindowFeatureGenerator;
import wd.goodFood.entity.Review;
import wd.goodFood.utils.Configuration;
import java.util.concurrent.*;

/**
 * class for bootstrap; based on MaxEnt in opennlp
 * */
public class Test {

	String modelSentenceDetectorPath;
	String modelTokenizerPath;
	String modelNERTaggerPath;
	String unlabeledPath;
	int thresholdIter = 100;
	int numTopNReviews;
	String dictPath;
	String trainingOld;
	String devPath;
	double F = 0;//need to be initialized
	String logPath;
	Writer logger;
	
	private ExecutorService executor = Executors.newFixedThreadPool(10);
	
	public void initializeSys(Configuration parameters){
		//set parameters
		this.modelSentenceDetectorPath = (String) parameters.getValue("modelSentenceDetectorPath");
		this.modelTokenizerPath = (String) parameters.getValue("modelTokenizerPath");
		this.modelNERTaggerPath = (String) parameters.getValue("modelNERTaggerPath");
		this.unlabeledPath = (String) parameters.getValue("unlabeledPath");
		this.thresholdIter = Integer.parseInt(parameters.getValue("thresholdIter"));
		this.numTopNReviews = Integer.parseInt(parameters.getValue("numTopNReviews"));
		this.dictPath = (String) parameters.getValue("dictPath");
		this.trainingOld = (String) parameters.getValue("trainingOld");
		this.devPath = (String) parameters.getValue("devPath");
		this.logPath = (String) parameters.getValue("logPath");
		try {
			this.logger  = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.logPath, false), "utf-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void multithreadTest() throws InvalidFormatException, IOException{
		Long startTime = System.currentTimeMillis();
		NameFinderME NERTagger0 = this.initiateNERTagger(this.modelNERTaggerPath);
		NameFinderME NERTagger1 = this.initiateNERTagger(this.modelNERTaggerPath);
		NameFinderME NERTagger2 = this.initiateNERTagger(this.modelNERTaggerPath);
		List<Review> unlabeledReviews = this.loadReviews(unlabeledPath);
		NameFinderME tagger = null;		
		
		for(int i = 0; i < unlabeledReviews.size(); i++){
			//process each review
			if(i%3 == 0){
				tagger = NERTagger0;
			}else if(i%3 == 1){
				tagger = NERTagger1;
			}else if(i%3 == 2){
				tagger = NERTagger2;
			}		
			
			System.out.println(i);
			Runnable task = new NERTask(tagger, unlabeledReviews.get(i));
//			Thread t = new Thread(task);
			this.executor.execute(task);
		}
		this.executor.shutdown();
		
		Long endTime = System.currentTimeMillis();
		System.out.println("Totle time is\t" + (endTime - startTime) + "\t milliseconds");
	}
	
	public void Run() throws IOException{
		
		List<Review> unlabeledReviews = this.loadReviews(unlabeledPath);
		int numIter = 0;
				
		//initialize taggerOld and trainingNew, trainingOld, if not existed
		NameFinderME NERTaggerOld = this.initiateNERTagger(this.modelNERTaggerPath);
//		String trainingNew = this.duplicateFile(this.trainingOld, this.trainingOld+".new");
		String modelNERTaggerNewPath = this.modelNERTaggerPath + ".new";
		this.F = this.evaluateNER(this.devPath, this.modelNERTaggerPath);
		
		
		
		while(unlabeledReviews.size() > this.numTopNReviews && numIter < this.thresholdIter){	
			//log and output some info
			System.out.println("-------------- a new iteration:\n unlabeled data size:\t" + unlabeledReviews.size());
			System.out.println("ITERATION:\t" + numIter);
			logger.write("------------------\nIteration times:\t" + numIter + "\n");
			logger.write("unlabeled data size:\t" + unlabeledReviews.size() + "\n\n\n");
			
			//tag the unlabeled			
			unlabeledReviews = this.tagUnlabel(NERTaggerOld, unlabeledReviews);
			
			//pick top N reviews; remove from unlabeled
//			List<Review> topUnlabeledReviews = this.getTopN(this.numTopNReviews, unlabeledReviews);
			unlabeledReviews = this.getTopN(this.numTopNReviews, unlabeledReviews);
			List<Review> topUnlabeledReviews = unlabeledReviews.subList(0, this.numTopNReviews);
			unlabeledReviews = unlabeledReviews.subList(this.numTopNReviews, unlabeledReviews.size());
//			if(numIter >= 1){
//				System.exit(0);
//			}
			
//			for(Review r : topUnlabeledReviews){
//				for(int m = 0; m < r.getAllStrs().size(); m++){
//					System.out.println(Arrays.asList(r.getAllStrs().get(m)));
//					System.out.println(r.getAllNERs());
//				}				
//			}
//			System.exit(0);

			//merge N reviews to training data and create seperate trainingNew
			String trainingNew = this.duplicateFile(this.trainingOld, this.trainingOld+".new");
			trainingNew = this.appendReviews2Training(trainingNew, topUnlabeledReviews);
			
			//retrain a new taggerNew
			modelNERTaggerNewPath = this.trainNER(trainingNew, modelNERTaggerNewPath);
			NameFinderME NERTaggerNew = this.initiateNERTagger(modelNERTaggerNewPath);
			
			//evaluate
			double FNew = this.evaluateNER(this.devPath, modelNERTaggerNewPath);
			
			//judge if topN reviews are helpful
			if(FNew > F){
				System.out.println("GOOD JOB!!! This iteration improves system :) :) :) :) :)");
				//update training data and NER tagger
				F = FNew;
				NERTaggerOld = NERTaggerNew;
				FileUtils.deleteQuietly(new File(trainingOld));
				trainingOld = this.duplicateFile(trainingNew, trainingOld);//now old and new are the same
				FileUtils.deleteQuietly(new File(this.modelNERTaggerPath));
				this.modelNERTaggerPath = this.duplicateFile(modelNERTaggerNewPath, this.modelNERTaggerPath);//now old and new are the same
				
			}else{
				//keep it
				System.out.println("fine... let's try next iteration :( :( :( :(");

			}			
			
			numIter++;
			
		}
		this.logger.close();
	}
	
	public String duplicateFile(String oldPath, String newPath) throws IOException{
		FileUtils.copyFile(new File(oldPath), new File(newPath));
		return newPath;	}
	
	public String trainNER(String trainingPath, String modelFilePath) throws IOException{
		AdaptiveFeatureGenerator featureGenerator = this.createFeatureGenerator();		
		Charset charset = Charset.forName("UTF-8");
		ObjectStream<String> lineStream = new PlainTextByLineStream(new FileInputStream(trainingPath), charset);
		ObjectStream<NameSample> sampleStream = new NameSampleDataStream(lineStream);
		TokenNameFinderModel model;

		try {
		  model = NameFinderME.train("en", "Food", sampleStream, featureGenerator, Collections.<String, Object>emptyMap(), 100, 5);
		}finally {
		  sampleStream.close();
		}

		BufferedOutputStream modelOut = null;
		try {
			modelOut = new BufferedOutputStream(new FileOutputStream(modelFilePath));
			model.serialize(modelOut);
		} finally {
			if(modelOut != null){
				modelOut.close();
			}		           
		}
		return modelFilePath;
	}
	
	public double evaluateNER(String testPath, String modelPath) throws InvalidFormatException, IOException{
		InputStream modelIn = new FileInputStream(modelPath);
		TokenNameFinderModel NEModel = new TokenNameFinderModel(modelIn);
		TokenNameFinderEvaluator evaluator = new TokenNameFinderEvaluator(new NameFinderME(NEModel));
		Charset charset = Charset.forName("UTF-8");
		ObjectStream<String> lineStream = new PlainTextByLineStream(new FileInputStream(testPath), charset);
		ObjectStream<NameSample> testStream = new NameSampleDataStream(lineStream);
		evaluator.evaluate(testStream);
		FMeasure result = evaluator.getFMeasure();
		System.out.println("System initialized, evaluation starts from:");
		System.out.println(result.toString());
		this.logger.write("current performance:\n" + result.toString());
		modelIn.close();
		return result.getFMeasure();
	}
	
	public AdaptiveFeatureGenerator createFeatureGenerator(){
		AdaptiveFeatureGenerator featureGenerator = new CachedFeatureGenerator(
		         new AdaptiveFeatureGenerator[]{
		           new WindowFeatureGenerator(new TokenFeatureGenerator(), 2, 2),
		           new WindowFeatureGenerator(new TokenClassFeatureGenerator(true), 2, 2),//order is: current, previous m, next n
		           new OutcomePriorFeatureGenerator(),
		           new PreviousMapFeatureGenerator(),
		           new BigramNameFeatureGenerator(),
		           new SentenceFeatureGenerator(true, false)
//		           new TokenFeatureGenerator(),
//		           new TokenClassFeatureGenerator(true),
//		           new DictionaryFeatureGenerator(this.dictPath),
//		           new POSFeatureGenerator(posTagger),
//		           new PreviousNextFeatures(new POSFeatureGenerator(posTagger), 2, 2),
		           });
		
		return featureGenerator;
	}
	
	public String appendReviews2Training(String filePath, List<Review> reviews) throws IOException{
		BufferedWriter writer  = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, true), "utf-8"));
		this.appendReviews2Training(writer, reviews);
		return filePath;
	}
	
	/**
	 * append reviews to training data
	 * format is following opennlp format
	 * @throws IOException 
	 * */
	public void appendReviews2Training(Writer writer, List<Review> reviews) throws IOException{
		for(Review review : reviews){
			List<String[]> allNERStrs = review.getAllNEs();
			for(int i = 0; i < allNERStrs.size(); i++){
				String[] sentWithTag = allNERStrs.get(i);
				for(int m = 0; m < sentWithTag.length; m++){
					writer.write(sentWithTag[m] + " ");
				}
				writer.write("\n");
			}
			writer.write("\n");			
		}		
		
		writer.close();
	}
	
	public List<Review> tagUnlabel(NameFinderME tagger, List<Review> unlabeled){
		
		for(int i = 0; i < unlabeled.size(); i++){
			//process each review
			Review r = unlabeled.get(i);
			r.resetReview();//remove info from previous iteration, or duplicated info will be appended, index error.
			double score = 0;
			int numNER = 0;
			for(int m = 0; m < r.getAllStrs().size(); m++){
				//process each sentence
				String[] sent = r.getAllStrs().get(m);//tokenized sentences
				Span[] spansTmp = tagger.find(sent);
				unlabeled.get(i).getAllNERSpans().add(spansTmp);
				double[] probsTmp = tagger.probs(spansTmp);
				unlabeled.get(i).getAllNERProbs().add(probsTmp);
				numNER += spansTmp.length;
				for(double d : probsTmp){
					score += d;
				}
			}
			double averageScore = score / numNER;
			unlabeled.get(i).setScoreNER(averageScore);
		}
		
		//TODO:sort all reviews and find top N
		
		return unlabeled;
	}
	
	/**
	 * get top N reviews with highest scores
	 * */
	public List<Review> getTopN(int N, List<Review> unlabeled){
		class ReviewComparator implements Comparator<Review>{
		    public int compare(Review r1, Review r2) {
		        return r1.getScoreNER() > r1.getScoreNER() ? 1 : -1;
		    }
		}
		
		Collections.sort(unlabeled, new ReviewComparator());
		List<Review> topN = unlabeled.subList(0, N);
//		for(Review r : topN){
//			//generate formatted data for creating training data
//			r.attachNERTag();
//		}
		for(int i = 0; i < N; i++){
			unlabeled.get(i).attachNERTag();
		}
		
//		Iterator<Review> itr = unlabeled.iterator();//remove top N
//		int num = 0;
//		while(itr.hasNext()){
//		    if(num < N){
//		    	Review r = itr.next();
//		    	itr.remove();		    	
//		    }
//		    num++;
//		}
//		unlabeled.removeAll(unlabeled.subList(0, N));
//		for(int i = 0; i < N; i++){
//			unlabeled.remove(0);
//		}
//		unlabeled = unlabeled.subList(N, unlabeled.size());//here, closure is broken
		return unlabeled;
	}
	
	public List<Review> loadReviews(String unlabeledPath) throws IOException{
		List<Review> reviews = new ArrayList<Review>();
		List<List<String[]>> reviewStrs = this.initializeUnlabeled(unlabeledPath);
		
		for(int i = 0; i < reviewStrs.size(); i++){
			Review r = new Review(reviewStrs.get(i));
			reviews.add(r);
		}
		
//		System.out.println(reviews.size() + "\t unlabeled reviews loaded");
		
		return reviews;
	}
	
	/**
	 * convert '------' segmented unlabeled data into strings
	 * 
	 * */
	public List<List<String[]>> initializeUnlabeled(String unlabeledPath) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(unlabeledPath));
		String line;
		String strTmp = "";
		List<String> unlabeled = new ArrayList<String>();
		SentenceDetector sentDetector = this.initiateSentenceDetector(this.modelSentenceDetectorPath);
		Tokenizer tokenizer = this.initiateTokenizer(this.modelTokenizerPath);
		List<List<String[]>> reviews = new ArrayList<List<String[]>>();
		
		while((line = br.readLine()) != null){
			if(line.trim().equalsIgnoreCase("------")){
				if(!strTmp.matches("^\\s*$")){
					unlabeled.add(strTmp.trim());
				}				
				strTmp = "";
			}else{
				strTmp += line.trim() + "\n";
			}
		}
		
		for(int i = 0; i < unlabeled.size(); i++){
			String review = unlabeled.get(i);
			List<String[]> reviewTmp = new ArrayList<String[]>();
//			System.out.println(review);
			String sentences[] = sentDetector.sentDetect(review);
			for(int m = 0; m < sentences.length; m++){
				String sent = sentences[m];
				String tokens[] = tokenizer.tokenize(sent);
				reviewTmp.add(tokens);
//				System.out.println(Arrays.asList(tokens));
			}
			reviews.add(reviewTmp);
		}
		
		System.out.println(reviews.size() + "\t unlabeled reviews loaded");
		br.close();
		
		return reviews;
	}
	
	public SentenceDetector initiateSentenceDetector(String modelFile){
		SentenceDetectorME sentenceDetector = null;
		//hardcode path of model file
		
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
		//hardcode path of model file
		
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
		InputStream modelIn = new FileInputStream(modelPath);
		POSModel model = new POSModel(modelIn);
		modelIn.close();
		POSTaggerME tagger = new POSTaggerME(model);
		return tagger;
	}
	
	public NameFinderME initiateNERTagger(String modelPath) throws InvalidFormatException, IOException{
		InputStream modelIn = new FileInputStream(modelPath);
		TokenNameFinderModel NEModel = new TokenNameFinderModel(modelIn);
		NameFinderME NERTagger = new NameFinderME(NEModel);
		modelIn.close();
		return NERTagger;
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
//		Test tester = new Test();
//		String configPath = args[0];
//		Configuration bootstrapConfig = new Configuration(configPath);
//		System.out.println(bootstrapConfig.getValue("devPath"));
//		tester.initializeSys(bootstrapConfig);
////		strapper.Run();
//		tester.multithreadTest();
		for(int i = 9; i <= 9; i++){
			System.out.println(i);
		}
	}

}
