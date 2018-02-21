package main.core;

/**
 * Dependencies
 * ws4j: https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/ws4j/ws4j-1.0.1.jar
 * jawjaw: https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/jawjaw/jawjaw-1.0.2.jar
 */

import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

//use WordSimilarity.getSim(word1, word2) to get the similarity score of 2 words
public class WordSimilarity {
	private static ILexicalDatabase db = new NictWordNet();
	private static RelatednessCalculator rc =  new WuPalmer(db);
	
	//can calculate other similarities, here we use WUP as the paper suggested	
	private static double getWUP( String word1, String word2 ) {
		word1 = word1.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
		word2 = word2.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
		
		//it should return the same value as the demo: http://ws4jdemo.appspot.com
        //they may have updated in the demo...
        if (word1.equals(word2)){
            return 1.0;
        }
        WS4JConfiguration.getInstance().setMFS(true);
        List<POS[]> posPairs = rc.getPOSPairs();
        double maxScore = -1D;
        
        for(POS[] posPair: posPairs) {
            List<Concept> synsets1 = (List<Concept>)db.getAllConcepts(word1, posPair[0].toString());
            List<Concept> synsets2 = (List<Concept>)db.getAllConcepts(word2, posPair[1].toString());
            
            for(Concept synset1: synsets1) {
                for (Concept synset2: synsets2) {
                    Relatedness relatedness = rc.calcRelatednessOfSynset(synset1, synset2);
                    double score = relatedness.getScore();
                    if (score > maxScore) {
                        maxScore = score;
                    }
                }
            }
        }
        
        if (maxScore == -1D) {
            maxScore = 0.0;
        }
        return maxScore>1.0?0.0:maxScore; //can return number>1...may be a bug in ws4j
	}
	
	//Jaccard coefficient
	private static double getJaccard(String word1, String word2){
		word1 = word1.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
		word2 = word2.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
		Set<Character> charSet1 = new HashSet<>();
		Set<Character> charSet2 = new HashSet<>();
		
		for (char c : word1.toCharArray()){charSet1.add(c);}
		for (char c : word2.toCharArray()){charSet2.add(c);}
		
		Set<Character> intersection = new HashSet<Character>(charSet1);
		intersection.retainAll(charSet2);
		Set<Character> union = new HashSet<Character>(charSet1);
		union.addAll(charSet2);
		
		double jaccard = (1.0 * intersection.size())/(1.0 * union.size());
		jaccard = Math.sqrt(jaccard);
		
		return jaccard;
		
	}
	
	//max of WUP and Jaccard
	public static double getSim (String word1, String word2){
		return Math.max(getWUP(word1, word2), getJaccard(word1,word2));
	}
	
	public static void main(String[] args) throws Exception {
		String word1 = "book";
		String word2 = "dog";
		System.out.printf("WUP similarity between %s and %s is: %f\n", word1, word2, WordSimilarity.getWUP(word1, word2));
		System.out.printf("Jaccard coefficient between %s and %s is: %f\n", word1, word2, WordSimilarity.getJaccard(word1, word2));
		System.out.printf("Final similarity between %s and %s is: %f\n", word1, word2, WordSimilarity.getSim(word1, word2));
	}
}

