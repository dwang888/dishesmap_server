package wd.goodFood.features;

import java.util.List;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.namefind.DictionaryNameFinder;
import opennlp.tools.util.featuregen.FeatureGeneratorAdapter;
import opennlp.tools.util.featuregen.InSpanGenerator;


public class MyDicFeatureGenerator extends FeatureGeneratorAdapter {
	
  private MyInSpanGenerator isg;
  
  public MyDicFeatureGenerator(Dictionary dict) {
    this("",dict);
  }
  public MyDicFeatureGenerator(String p, Dictionary dict) {
    setDictionary(p,dict);
  }
  
  public void setDictionary(Dictionary dict) {
    setDictionary("",dict);
  }
  
  public void setDictionary(String name, Dictionary dict) {
    isg = new MyInSpanGenerator(name, new MyDictionaryNameFinder(dict));
  }
  
  public void createFeatures(List<String> features, String[] tokens, int index, String[] previousOutcomes) {
    isg.createFeatures(features, tokens, index, previousOutcomes);
//    System.out.println(tokens[index] + "--->\t" + features);
  }
  
}
