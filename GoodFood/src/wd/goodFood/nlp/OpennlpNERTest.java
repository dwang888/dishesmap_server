package wd.goodFood.nlp;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;


import opennlp.maxent.GISModel;
import opennlp.maxent.io.PooledGISModelReader;
import opennlp.model.MaxentModel;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinderCrossValidator;
import opennlp.tools.namefind.TokenNameFinderEvaluator;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.eval.FMeasure;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.BigramNameFeatureGenerator;
import opennlp.tools.util.featuregen.CachedFeatureGenerator;
import opennlp.tools.util.featuregen.OutcomePriorFeatureGenerator;
import opennlp.tools.util.featuregen.PreviousMapFeatureGenerator;
import opennlp.tools.util.featuregen.SentenceFeatureGenerator;
import opennlp.tools.util.featuregen.WindowFeatureGenerator;
import opennlp.tools.util.featuregen.TokenFeatureGenerator;
import opennlp.tools.util.featuregen.TokenClassFeatureGenerator;
import opennlp.tools.util.featuregen.DictionaryFeatureGenerator;
import wd.goodFood.features.ChunkFeatureGenerator;
import wd.goodFood.features.MyDicFeatureGenerator;
import wd.goodFood.features.NgramFeatureGenerator;
import wd.goodFood.features.NgramTokenFeatureGenerator;
import wd.goodFood.features.POSFeatureGenerator;
import wd.goodFood.features.PreviousNextFeatures;
import wd.goodFood.features.PureTokenFeatureGenerator;

/**
 * train a NER model with opennlp API
 * */
public class OpennlpNERTest {

	public AdaptiveFeatureGenerator createFeatureGenerator(Dictionary dict, POSTagger posTagger, ChunkerME chunker){
		AdaptiveFeatureGenerator featureGenerator = new CachedFeatureGenerator(
		         new AdaptiveFeatureGenerator[]{
//		           new WindowFeatureGenerator(new TokenFeatureGenerator(), 3, 3),
//		           new WindowFeatureGenerator(new NgramTokenFeatureGenerator(true, 2, 2), 1, 1),//order is: current, previous m, next n
		           new WindowFeatureGenerator(new TokenFeatureGenerator(true), 10, 10),//false->upper case; true->lower
//		           new WindowFeatureGenerator(new MyDicFeatureGenerator("D", dict), 5, 5),
//		           new OutcomePriorFeatureGenerator(),
//		           new PreviousMapFeatureGenerator(),
//		           new BigramNameFeatureGenerator(),
//		           new SentenceFeatureGenerator(false, false),//ignore begin and end of a sentence
		           //------------------below are my own feawtures
//		           new NgramFeatureGenerator(new MyDicFeatureGenerator("D", dict), 2, 2),
//		           new NgramTokenFeatureGenerator(true, 2, 2),
//		           new TokenFeatureGenerator(true),
//		           new TokenClassFeatureGenerator(false),//false->don't use token, only use class feature
//		           new DictionaryFeatureGenerator("Dic=",  dict),
		           new MyDicFeatureGenerator("D", dict),
//		           new POSFeatureGenerator(posTagger),
//		           new PreviousNextFeatures(new TokenFeatureGenerator(), 1, 1),
//		           new WindowFeatureGenerator(new POSFeatureGenerator(posTagger), 1, 1),
//		           new WindowFeatureGenerator(new DictionaryFeatureGenerator(dict), 8, 8),		           
		           });
		
		return featureGenerator;
	}
	
	public void trainNER(String trainingPath, String modelFilePath, Dictionary dict, POSTagger posTagger, ChunkerME chunker) throws IOException{
		AdaptiveFeatureGenerator featureGenerator = this.createFeatureGenerator(dict, posTagger, chunker);		
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
	}
	
	public void evaluateNER(String testPath, String modelPath) throws InvalidFormatException, IOException{
		InputStream modelIn = new FileInputStream(modelPath);
		TokenNameFinderModel NEModel = new TokenNameFinderModel(modelIn);
		TokenNameFinderEvaluator evaluator = new TokenNameFinderEvaluator(new NameFinderME(NEModel));
		Charset charset = Charset.forName("UTF-8");
		ObjectStream<String> lineStream = new PlainTextByLineStream(new FileInputStream(testPath), charset);
		ObjectStream<NameSample> testStream = new NameSampleDataStream(lineStream);
		evaluator.evaluate(testStream);
		FMeasure result = evaluator.getFMeasure();
		System.out.println(result.toString());
	}
	
	public void nFoldEvaluate(String trainingPath, int numFolds, Dictionary dict, POSTagger posTagger, ChunkerME chunker) throws IOException{
		FileInputStream sampleDataIn = new FileInputStream(trainingPath);
		ObjectStream<String> lineStream = new PlainTextByLineStream(sampleDataIn.getChannel(), "UTF-8");
		ObjectStream<NameSample> sampleStream = new NameSampleDataStream(lineStream); 
		
//		TokenNameFinderCrossValidator evaluator = new TokenNameFinderCrossValidator("en", 5, 100);//original one
		AdaptiveFeatureGenerator featureGenerator = this.createFeatureGenerator(dict, posTagger, chunker);
		InHouseNERCrossValidator evaluator = new InHouseNERCrossValidator("en", 5, 100, featureGenerator);//use my customized features
		
		evaluator.evaluate(sampleStream, numFolds);
		FMeasure result = evaluator.getFMeasure();

		System.out.println(result.toString());
	}
	
	public Dictionary getDict(String dictPath) throws InvalidFormatException, IOException{
		InputStream is = new FileInputStream(dictPath);
		Dictionary dict = new Dictionary(is);
		return dict;
	}
	
	public POSTagger initializePOSTagger(String modelPath) throws InvalidFormatException, IOException{
		InputStream modelIn = new FileInputStream(modelPath);
		POSModel model = new POSModel(modelIn);
		modelIn.close();
		POSTaggerME tagger = new POSTaggerME(model);
		return tagger;
	}
	
	public ChunkerME initializeChunker(String modelPath) throws InvalidFormatException, IOException{
		InputStream modelIn = new FileInputStream(modelPath);
		ChunkerModel model = new ChunkerModel(modelIn);
		modelIn.close();
		ChunkerME chunker = new ChunkerME(model);
		return chunker;
	}
	
	public static void main(String[] args) throws IOException {
		String trainingPath = args[0];
		String modelFilePath = args[1];
		String testPath = args[2];
		String dictPath = args[3];
		String allPath = args[4];
		
		String posModelPath = "D:\\NLP_TOOLS\\ML\\opennlp\\apache-opennlp-1.5.2-incubating\\models\\en-pos-maxent.bin";
		String chunkerModelPath = "D:\\NLP_TOOLS\\ML\\opennlp\\apache-opennlp-1.5.2-incubating\\models\\en-chunker.bin";		
		
		OpennlpNERTest tester = new OpennlpNERTest();
		Dictionary dictNER = tester.getDict(dictPath);
		POSTagger posTagger = tester.initializePOSTagger(posModelPath);
		ChunkerME chunker = tester.initializeChunker(chunkerModelPath);
		
//		tester.trainNER(trainingPath, modelFilePath);
//		tester.trainNER(trainingPath, modelFilePath, dictNER, posTagger);
//		tester.evaluateNER(testPath, modelFilePath);
		tester.nFoldEvaluate(allPath, 10, dictNER, posTagger, chunker);
	}

}
