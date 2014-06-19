package wd.goodFood.nlp;

import java.util.List;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.namefind.DictionaryNameFinder;
import opennlp.tools.util.featuregen.FeatureGeneratorAdapter;
import opennlp.tools.util.featuregen.InSpanGenerator;

/**
 * just for test
 * */
public class DummyFeatureGenerator extends FeatureGeneratorAdapter {

	private InSpanGenerator isg;
	  
	  public DummyFeatureGenerator(Dictionary dict) {
	    this("",dict);
	  }
	  public DummyFeatureGenerator(String prefix, Dictionary dict) {
	    setDictionary(prefix,dict);
	  }
	  
	  public void setDictionary(Dictionary dict) {
	    setDictionary("",dict);
	  }
	  
	  public void setDictionary(String name, Dictionary dict) {
	    isg = new InSpanGenerator(name, new DictionaryNameFinder(dict));
	  }
	  
	  public void createFeatures(List<String> features, String[] tokens, int index, String[] previousOutcomes) {
	    isg.createFeatures(features, tokens, index, previousOutcomes);

	        features.add("Dummy" + ":w=dic");
	        features.add("Dummy" + ":w=dic=" + tokens[index]);
	  }
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
