package wd.goodFood.features;

import java.util.List;

import opennlp.tools.postag.POSTagger;
import opennlp.tools.util.featuregen.FeatureGeneratorAdapter;

/**
 * generate POS tag for current token,
 * POS tagger is introduced as argument, currently use opennlp default one.
 * */
public class POSFeatureGenerator extends FeatureGeneratorAdapter {
	private static final String POS_PREFIX = "pos";
	POSTagger posTagger;
	
	public POSFeatureGenerator(POSTagger tagger){
		this.posTagger = tagger;
	}
	
	public void createFeatures(List<String> features, String[] tokens, int index, String[] previousOutcomes) {
		String[] tags = this.posTagger.tag(tokens);
		String pos = tags[index];
		features.add(POS_PREFIX + "=" + pos);
//		for(int i = 0; i < tokens.length; i++){
//			System.out.print("\t" + tokens[i]);
//		}
//		
//		System.out.println("\t |-" + tokens[index] + "->" + pos);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}
