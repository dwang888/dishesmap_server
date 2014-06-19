package wd.goodFood.features;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import opennlp.tools.postag.POSTagger;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorAdapter;

/**
 * generate POS tag for current token,
 * POS tagger is introduced as argument, currently use opennlp default one.
 * */
public class NgramFeatureGenerator extends FeatureGeneratorAdapter {
	private static final String NGRAM_PREFIX = "ng";
	private int nLeft;
	private int nRight;
	private final AdaptiveFeatureGenerator generator;
	
	public NgramFeatureGenerator(AdaptiveFeatureGenerator generator, int ngramLeft, int ngramRight){
		this.generator = generator;
		this.nLeft = ngramLeft;
		this.nRight = ngramRight;
	}
	
	public void createFeatures(List<String> features, String[] tokens, int index, String[] preds) {
		int left = index - nLeft + 1;
		left = Math.max(left, 0);
		int right = index + nRight - 1;
		right = Math.min(right, tokens.length-1);
		StringBuilder strTmp = new StringBuilder();
		strTmp.append(NGRAM_PREFIX + "=");
		for(int i = left; i <= right; i++){
			List<String> featuresTmp = new ArrayList<String>();
			generator.createFeatures(featuresTmp, tokens, i, preds);
			for(Iterator<String> itr = featuresTmp.iterator(); itr.hasNext();){
				strTmp.append(itr.next() + "_");
			}			
		}
		features.add(strTmp.toString());
//		System.out.println("\t |-" + tokens[index] + "->" + strTmp);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}
