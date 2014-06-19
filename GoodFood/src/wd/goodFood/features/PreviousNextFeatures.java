package wd.goodFood.features;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.AggregatedFeatureGenerator;
/**
 * almost the same with windows features of opennlp, just ignore current word's features, only use previous and next features
 * notice the previous features' orders: abCde-> bade
 * */
public class PreviousNextFeatures implements AdaptiveFeatureGenerator {
	public static final String PREV_PREFIX = "p";
	  public static final String NEXT_PREFIX = "n";

	  private final AdaptiveFeatureGenerator generator;

	  private final int prevWindowSize;
	  private final int nextWindowSize;

	  /**
	   * Initializes the current instance with the given parameters.
	   *
	   * @param generator Feature generator to apply to the window.
	   * @param prevWindowSize Size of the window to the left of the current token.
	   * @param nextWindowSize Size of the window to the right of the current token.
	   */
	  public PreviousNextFeatures(AdaptiveFeatureGenerator generator, int prevWindowSize,  int nextWindowSize) {
	    this.generator = generator;
	    this.prevWindowSize = prevWindowSize;
	    this.nextWindowSize = nextWindowSize;
	  }
	  
	  /**
	   * Initializes the current instance with the given parameters.
	   * 
	   * @param prevWindowSize
	   * @param nextWindowSize
	   * @param generators
	   */
	  public PreviousNextFeatures(int prevWindowSize, int nextWindowSize, AdaptiveFeatureGenerator... generators) {
	    this(new AggregatedFeatureGenerator(generators), prevWindowSize, nextWindowSize);
	  }
	  
	  /**
	   * Initializes the current instance. The previous and next window size is 5.
	   *
	   * @param generator
	   */
	  public PreviousNextFeatures(AdaptiveFeatureGenerator generator) {
	    this(generator, 5, 5);
	  }
	  
	  /**
	   * Initializes the current isntance with the given parameters.
	   * 
	   * @param generators
	   */
	  public PreviousNextFeatures(AdaptiveFeatureGenerator... generators) {
	    this(new AggregatedFeatureGenerator(generators), 5, 5);
	  }
	  
	  public void createFeatures(List<String> features, String[] tokens, int index, String[] preds) {
	    // current features
//	    generator.createFeatures(features, tokens, index, preds);//the only difference with WindowFeatureGenerator of opennlp

	    // previous features
	    for (int i = 1; i < prevWindowSize + 1; i++) {
	      if (index - i >= 0) {

	        List<String> prevFeatures = new ArrayList<String>();

	        generator.createFeatures(prevFeatures, tokens, index - i, preds);

	        for (Iterator<String> it = prevFeatures.iterator(); it.hasNext();) {
	          features.add(PREV_PREFIX + i + it.next());
	        }
	      }
	    }

	    // next features
	    for (int i = 1; i < nextWindowSize + 1; i++) {
	      if (i + index < tokens.length) {

	        List<String> nextFeatures = new ArrayList<String>();

	        generator.createFeatures(nextFeatures, tokens, index + i, preds);

	        for (Iterator<String> it = nextFeatures.iterator(); it.hasNext();) {
	          features.add(NEXT_PREFIX + i + it.next());
	        }
	      }
	    }
	  }

	  public void updateAdaptiveData(String[] tokens, String[] outcomes) {
	    generator.updateAdaptiveData(tokens, outcomes);
	  }

	  public void clearAdaptiveData() {
	      generator.clearAdaptiveData();
	  }

	  @Override
	  public String toString() {
	    return super.toString()+": Prev windwow size: " + prevWindowSize +", Next window size: " + nextWindowSize;
	  }
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
