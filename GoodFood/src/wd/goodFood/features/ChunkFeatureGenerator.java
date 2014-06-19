package wd.goodFood.features;

import java.util.List;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.util.featuregen.FeatureGeneratorAdapter;

/**
 * generate POS tag for current token,
 * POS tagger is introduced as argument, currently use opennlp default one.
 * */
public class ChunkFeatureGenerator extends FeatureGeneratorAdapter {

	ChunkerME chunker;
	POSTagger posTagger;
	
	public ChunkFeatureGenerator(ChunkerME chunker, POSTagger tagger){
		this.chunker = chunker;
		this.posTagger = tagger;
	}
	
	public void createFeatures(List<String> features, String[] tokens, int index, String[] previousOutcomes) {
		String[] posTags = this.posTagger.tag(tokens);
		String[] chunkTags = this.chunker.chunk(tokens, posTags);
		String chunkTag = chunkTags[index];
		features.add("chunk=" + chunkTag);
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
