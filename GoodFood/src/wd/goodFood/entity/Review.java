package wd.goodFood.entity;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import opennlp.tools.util.Span;

/**
 * base class for review
 * */
public class Review {

	String reviewStr;
	String taggedStr;
	String NEStr;
	List<String[]> allStrs = new ArrayList<String[]>();//to store tokenized sentences
	List<String[]> allTaggedSents = new ArrayList<String[]>();//add '<NER>' tag for each NER in allStrs, following opennlp NER training data format
	List<String[]> allPOSs = new ArrayList<String[]>();
	List<String[]> allNEs = new ArrayList<String[]>();//store all the NEs sent by sent
	List<Span[]> allNERSpans = new ArrayList<Span[]>();
	List<String[]> allChunks = new ArrayList<String[]>();
	List<double[]> allNERProbs = new ArrayList<double[]>();
	double scoreNER;//average score of all NERs in all snetences of this review
	private String business_id;
	private String webLink;//url to web source; used to display
	int dataSource;
	
	public Review(List<String[]> strArrays){
		this.setAllStrs(strArrays);
	}
	
	public Review(String rStr){
		this.setReviewStr(rStr);
	}
	
	/**
	 * a constructor to generate instance from DB tagged record
	 * */
	public Review(String rStr, String taggedText, String NEStr){
		this.reviewStr = rStr;
		this.taggedStr = taggedText;
		this.NEStr = NEStr;
	}
	
	/**
	 * attach "<NER>" tag to text
	 * attach all NEs
	 * merge all NEs with seperator ||
	 * */
	public void attachTag2Str(){
		StringBuilder sb = new StringBuilder();
		StringBuilder sbNEStr = new StringBuilder();
		if(this.getAllTaggedSents() == null){
			this.setAllTaggedSents(new ArrayList<String[]>());
		}
		if(this.getAllNEs() == null){
			this.setAllNERs(new ArrayList<String[]>());
		}
		for(int i = 0; i < this.getAllNERSpans().size(); i++){
			//process NERs in each sentence
			Span[] spans = this.getAllNERSpans().get(i);
			String[] sent = this.getAllStrs().get(i);
			String[] NERStrsTmp = sent.clone();//TODO: necessary?
			String[] NEs = Span.spansToStrings(spans, sent);
			for(int m = 0; m < spans.length; m++){
				//attach NE tag
				Span span = spans[m];
				int start = span.getStart();
				int end = span.getEnd()-1;
				NERStrsTmp[start] = "<START:" + span.getType() + "> " + NERStrsTmp[start];
				NERStrsTmp[end] = NERStrsTmp[end] + " <END>";
			}
//			this.getAllTaggedSents().add(NERStrsTmp);
			sb.append(StringUtils.join(NERStrsTmp, " "));
			sb.append(" ");
			this.getAllNEs().add(NEs);
			if(NEs != null && NEs.length != 0){
				sbNEStr.append(StringUtils.join(NEs, "||"));
				sbNEStr.append("||");
			}
		}
		this.setTaggedStr(sb.toString());
		this.NEStr = sbNEStr.toString();
//		System.out.println("original NE Str:\t" + NEStr);
	}
	
	/**
	 * attach "<NER>" tag to each NE
	 * */
	public void attachNERTag(){
		if(this.getAllTaggedSents() == null){
			this.setAllTaggedSents(new ArrayList<String[]>());
		}
		for(int i = 0; i < this.getAllNERSpans().size(); i++){
			//process NERs in each sentence
			Span[] spans = this.getAllNERSpans().get(i);
			String[] sent = this.getAllStrs().get(i);
			String[] NERStrsTmp = sent.clone();//TODO: necessary?
			
			for(int m = 0; m < spans.length; m++){
				//attach NE tag
				Span span = spans[m];
				int start = span.getStart();
				int end = span.getEnd()-1;
				NERStrsTmp[start] = "<START:" + span.getType() + "> " + NERStrsTmp[start];
				NERStrsTmp[end] = NERStrsTmp[end] + " <END>";
			}
			this.getAllTaggedSents().add(NERStrsTmp);
		}
	}
	
	/**
	 * clean some field for next iteration
	 * */
	public void resetReview(){
		this.setAllTaggedSents(new ArrayList<String[]>());
		this.setAllNERs(new ArrayList<String[]>());
		this.setAllNERSpans(new ArrayList<Span[]>());
		this.setAllNERProbs(new ArrayList<double[]>());
		this.setScoreNER(0);
		this.setNEStr(null);
	}
	
	public String getReviewStr() {
		return reviewStr;
	}

	public void setReviewStr(String reviewStr) {
		this.reviewStr = reviewStr;
	}

	public List<String[]> getAllStrs() {
		return allStrs;
	}

	public void setAllStrs(List<String[]> allStrs) {
		this.allStrs = allStrs;
	}

	public List<String[]> getAllPOSs() {
		return allPOSs;
	}

	public void setAllPOSs(List<String[]> allPOSs) {
		this.allPOSs = allPOSs;
	}

	public List<String[]> getAllNEs() {
		return allNEs;
	}

	public void setAllNERs(List<String[]> allNEs) {
		this.allNEs = allNEs;
	}

	public List<String[]> getAllChunks() {
		return allChunks;
	}

	public void setAllChunks(List<String[]> allChunks) {
		this.allChunks = allChunks;
	}

	public double getScoreNER() {
		return scoreNER;
	}

	public void setScoreNER(double scoreNER) {
		this.scoreNER = scoreNER;
	}

	public List<Span[]> getAllNERSpans() {
		return allNERSpans;
	}

	public void setAllNERSpans(List<Span[]> allNERSpans) {
		this.allNERSpans = allNERSpans;
	}

	public List<double[]> getAllNERProbs() {
		return allNERProbs;
	}

	public void setAllNERProbs(List<double[]> allNERProbs) {
		this.allNERProbs = allNERProbs;
	}

	public final String getTaggedStr() {
		return taggedStr;
	}

	public final void setTaggedStr(String taggedStr) {
		this.taggedStr = taggedStr;
	}

	public final List<String[]> getAllTaggedSents() {
		return allTaggedSents;
	}

	public final void setAllTaggedSents(List<String[]> allTaggedSents) {
		this.allTaggedSents = allTaggedSents;
	}

	public String getBusiness_id() {
		return business_id;
	}

	public void setBusiness_id(String business_id) {
		this.business_id = business_id;
	}

	public String getWebLink() {
		return webLink;
	}

	public void setWebLink(String webLink) {
		this.webLink = webLink;
	}
	
	

	public final int getDataSource() {
		return dataSource;
	}

	public final void setDataSource(int dataSource) {
		this.dataSource = dataSource;
	}

	public final String getNEStr() {
		return NEStr;
	}

	public final void setNEStr(String nEStr) {
		NEStr = nEStr;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
