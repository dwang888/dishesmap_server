package wd.goodFood.unsupervised;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.util.Span;
import wd.goodFood.entity.Review;

public class NERTask implements Runnable{

	NameFinderME tagger;
	Review review;
	/**
	 * @param args
	 */
	public NERTask(){
		
	}
	
	public NERTask(NameFinderME tagger, Review r){
		this.tagger = tagger;
		this.review = r;
	}
	
	@Override
	public void run() {
		this.review.resetReview();//remove info from previous iteration, or duplicated info will be appended, index error.
		double score = 0;
		int numNER = 0;
		double averageScore = 0;
		for(int m = 0; m < review.getAllStrs().size(); m++){
			//process each sentence
			String[] sent = review.getAllStrs().get(m);//tokenized sentences
			synchronized(tagger){
				Span[] spansTmp = tagger.find(sent);
				
				review.getAllNERSpans().add(spansTmp);
				double[] probsTmp = tagger.probs(spansTmp);
				review.getAllNERProbs().add(probsTmp);
				numNER += spansTmp.length;
				for(double d : probsTmp){
					score += d;
				}
			}			
		}
		tagger.clearAdaptiveData();
		if(numNER != 0){
			averageScore = score / numNER;
		}
		review.setScoreNER(averageScore);
//		System.out.println("Average score:\t" + averageScore);
//		for(double[] probs : review.getAllNERProbs()){
//			System.out.println(probs.length);
//		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	

}
