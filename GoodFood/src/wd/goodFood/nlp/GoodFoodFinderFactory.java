package wd.goodFood.nlp;
import org.apache.commons.pool.BasePoolableObjectFactory;



public class GoodFoodFinderFactory extends BasePoolableObjectFactory {

	@Override
	public Object makeObject() throws Exception {
		return new GoodFoodFinder("data/opennlpNER.model");
	}
	
//	public boolean validateObject(Object obj) {
//	    if(obj instanceof GoodFoodFinder) {
//	      if(((GoodFoodFinder)obj).getName() == null)
//	        return true;
//	    }
//	    return false;
//	  }
//
//	  public void passivateObject(Object obj) throws Exception {
//	    if(obj instanceof GoodFoodFinder) {
//	      ((GoodFoodFinder)obj).setName(null);
//	    } else throw new Exception("Unknown object");
//	  }
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	

}
