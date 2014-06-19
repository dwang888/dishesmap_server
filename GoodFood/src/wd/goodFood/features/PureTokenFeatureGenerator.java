package wd.goodFood.features;

import java.util.List;

import opennlp.tools.postag.POSTagger;
import opennlp.tools.util.featuregen.FeatureGeneratorAdapter;

/**
 * generate token tag for current token,
 * purpose: just don't use feature prefix like "word=", to save space
 * */


public class PureTokenFeatureGenerator extends FeatureGeneratorAdapter {

  private static final String WORD_PREFIX = "w=";
  private boolean lowercase;

  public PureTokenFeatureGenerator(boolean lowercase) {
    this.lowercase = lowercase;
  }

  public PureTokenFeatureGenerator() {
    this(true);
  }

  public void createFeatures(List<String> features, String[] tokens, int index, String[] preds) {
	  StringBuilder strTmp = new StringBuilder();
	  strTmp.append(WORD_PREFIX);
	  if (lowercase) {
		  strTmp.append(tokens[index].toLowerCase());
	  }else {
		  strTmp.append(tokens[index]);
	  }
	  features.add(strTmp.toString());
	}
}
