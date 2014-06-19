package wd.goodFood.entity;

public class ReviewData {
	
	String weblink;
	String text;

	public ReviewData(String t){
		this.text = t;
	}
	
	public ReviewData(String link, String t){
		this.weblink = link;
		this.text = t;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
