package wd.goodFood.features;

import java.util.LinkedList;
import java.util.List;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.dictionary.Index;
import opennlp.tools.namefind.TokenNameFinder;
import opennlp.tools.util.Span;
import opennlp.tools.util.StringList;

/**
 * This is a dictionary based name finder, it scans text
 * for names inside a dictionary.
 */
public class MyDictionaryNameFinder implements TokenNameFinder {

  private Dictionary mDictionary;

  private Index mMetaDictionary;

  /**
   * Initializes the current instance.
   *
   * @param dictionary
   */
  public MyDictionaryNameFinder(Dictionary dictionary) {
    mDictionary = dictionary;
    mMetaDictionary = new Index(dictionary.iterator());
  }

  public Span[] find(String[] tokenStrings) {
    List<Span> foundNames = new LinkedList<Span>();

    for (int startToken = 0; startToken < tokenStrings.length; startToken++) {

      Span foundName = null;

      String  tokens[] = new String[]{};

      for (int endToken = startToken; endToken < tokenStrings.length; endToken++) {

        String token = tokenStrings[endToken];
//        System.out.println(token);
        // TODO: improve performance here
        String newTokens[] = new String[tokens.length + 1];
        System.arraycopy(tokens, 0, newTokens, 0, tokens.length);
        newTokens[newTokens.length - 1] = token;
        tokens = newTokens;

        if (mMetaDictionary.contains(token)) {
//        	System.out.println(token);
          StringList tokenList = new StringList(tokens);

          if (mDictionary.contains(tokenList)) {
            foundName = new Span(startToken, endToken + 1);
          }
        }
        else {
          break;
        }
      }

      if (foundName != null) {
        foundNames.add(foundName);
      }
    }

    return foundNames.toArray(new Span[foundNames.size()]);
  }
  
  public void clearAdaptiveData() {
    // nothing to clear
  }
}
