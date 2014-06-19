package wd.goodFood.nlp;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.TokenNameFinderCrossValidator;
import opennlp.tools.namefind.TokenNameFinderEvaluationMonitor;
import opennlp.tools.namefind.TokenNameFinderEvaluator;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.eval.CrossValidationPartitioner;
import opennlp.tools.util.eval.FMeasure;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.model.ModelUtil;
/**
 * main change: add customized feature generator class, instead of xml configuration file
 * */
public class InHouseNERCrossValidator{
	String languageCode;
	TokenNameFinderEvaluationMonitor[] listeners;
	AdaptiveFeatureGenerator featureGenerator;
	int iterations;
	int cutoff;
	FMeasure fmeasure = new FMeasure();
	
	public InHouseNERCrossValidator(String languageCode, int cutoff, int iterations, AdaptiveFeatureGenerator featureGenerator){
		this.languageCode = languageCode;
		this.featureGenerator = featureGenerator;
		this.iterations = iterations;
		this.cutoff = cutoff;
	}
	

	public void evaluate(ObjectStream<NameSample> samples, int nFolds) throws IOException {
		CrossValidationPartitioner<NameSample> partitioner = new CrossValidationPartitioner<NameSample>(samples, nFolds);
		while (partitioner.hasNext()) {
			CrossValidationPartitioner.TrainingSampleStream<NameSample> trainingSampleStream = partitioner.next();
			TokenNameFinderModel model  = opennlp.tools.namefind.NameFinderME.train("en", "Food", trainingSampleStream, 
					this.featureGenerator, Collections.<String, Object>emptyMap(), this.iterations, this.cutoff);
		    TokenNameFinderEvaluator evaluator = new TokenNameFinderEvaluator(new NameFinderME(model), listeners);
		    evaluator.evaluate(trainingSampleStream.getTestSampleStream());
		    fmeasure.mergeInto(evaluator.getFMeasure());
		}
	}
	
	public FMeasure getFMeasure() {
		return fmeasure;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
