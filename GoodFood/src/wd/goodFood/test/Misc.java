package wd.goodFood.test;

import java.util.ArrayList;
import java.util.List;

public class Misc {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		List<String> a = new ArrayList<String>();
		List<String> b = new ArrayList<String>();
		String s1 = "testStr1";
		String s2 = "testStr2";
		
		a.add(s1);
		a.add(s2);
		b.add(s1);
		System.out.println(a.size() + "\t" + b.size());
		
	}

}
