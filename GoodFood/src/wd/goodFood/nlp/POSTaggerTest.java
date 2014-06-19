package wd.goodFood.nlp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.InvalidFormatException;

public class POSTaggerTest {

	public POSTagger initializeTagger(String modelPath) throws InvalidFormatException, IOException{
		InputStream modelIn = new FileInputStream(modelPath);
		POSModel model = new POSModel(modelIn);
		modelIn.close();
		POSTaggerME tagger = new POSTaggerME(model);
		return tagger;
	}

	public void runTagger(POSTagger tagger){
		String[] sent = new String[]{"Most", "large", "cities", "in", "the", "US", "had", "morning", "and", "afternoon", "newspapers", "."};		  
		String[] tags = tagger.tag(sent);
		for(int i = 0; i < sent.length; i++){
			System.out.println(sent[i] + "_" + tags[i]);
		}
	}
	
	public static void main(String[] args) throws InvalidFormatException, IOException {
		// TODO Auto-generated method stub
		String modelPath = "D:\\NLP_TOOLS\\ML\\opennlp\\apache-opennlp-1.5.2-incubating\\models\\en-pos-maxent.bin";
		POSTaggerTest taggerTest = new POSTaggerTest();
		POSTagger tagger = taggerTest.initializeTagger(modelPath);
		taggerTest.runTagger(tagger);
	}

}
