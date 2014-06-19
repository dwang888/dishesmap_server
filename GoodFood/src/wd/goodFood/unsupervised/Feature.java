package wd.goodFood.unsupervised;

import java.util.List;


//abstract class for features that is used in extraction
public abstract class Feature {


	public String label;
	int countXAndY;
	int countX;
	List<String> words;
	List<String> BIOs;
	
	
	
	public String getLabel() {
		return label;
	}



	public void setLabel(String label) {
		this.label = label;
	}



	public int getCountXAndY() {
		return countXAndY;
	}



	public void setCountXAndY(int countXAndY) {
		this.countXAndY = countXAndY;
	}



	public int getCountX() {
		return countX;
	}



	public void setCountX(int countX) {
		this.countX = countX;
	}



	public List<String> getWords() {
		return words;
	}



	public void setWords(List<String> words) {
		this.words = words;
	}



	public List<String> getBIOs() {
		return BIOs;
	}



	public void setBIOs(List<String> bIOs) {
		BIOs = bIOs;
	}



	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
