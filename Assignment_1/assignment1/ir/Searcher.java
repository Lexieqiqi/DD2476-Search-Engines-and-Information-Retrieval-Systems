/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.lang.Math;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    public ArrayList<String> sortTerms(ArrayList<String> terms, HashMap<String, PostingsList> termsPostingsList) {

      for(int i = 0; i < terms.size() - 1; i++) {
          for(int j = 0; j < terms.size() - 1 - i; j++) {
              if(termsPostingsList.get(terms.get(j)).size() > termsPostingsList.get(terms.get(j+1)).size()) {     
                  String temp = terms.get(j+1);       
                  terms.set(j+1, terms.get(j));
                  terms.set(j, temp);
              }
          }
      }
      return terms;
    }

    public PostingsList intersect(PostingsList p1, PostingsList p2) {
      PostingsList answer = new PostingsList();
      int size1 = p1.size();
      int size2 = p2.size();
      int i = 0;
      int j = 0;
      int docID1 = 0;
      int docID2 = 0;
      do {
        docID1 = p1.get(i).docID;
        docID2 = p2.get(j).docID;
        if( docID1 == docID2 ) {
          answer.addToPostingsList(docID1);
          ++i;
          ++j;
        }
        else if ( docID1 < docID2 ) {
          ++i;
        }
        else ++j;
      } while (i<size1 && j<size2);
      return answer;
    }

    public PostingsList phrase(PostingsList p1, PostingsList p2) {
      PostingsList answer = new PostingsList();
      int size1 = p1.size();
      int size2 = p2.size();
      int i = 0;
      int j = 0;
      int docID1 = 0;
      int docID2 = 0;
      
      do {
        docID1 = p1.get(i).docID;
        docID2 = p2.get(j).docID;
        if( docID1 == docID2 ) {
          ArrayList<Integer> pp1 = p1.getOffsetsByDocID(docID1).offsets;
          ArrayList<Integer> pp2 = p2.getOffsetsByDocID(docID2).offsets;
          while(!pp2.isEmpty() && !pp1.isEmpty()) {
              if (pp2.get(0)-pp1.get(0)==1) {
                answer.addToPostingsList(docID1, pp2.get(0));
                pp1.remove(0);
                pp2.remove(0);
              }
              else if (pp2.get(0)>pp1.get(0)) pp1.remove(0);
              else pp2.remove(0);
          }
          ++i;
          ++j;
        }
        else if ( docID1 < docID2 ) {
          ++i;
        }
        else ++j;
      } while (i<size1 && j<size2);
      return answer;
    }

    public PostingsList phraseQuery(ArrayList<String> terms, HashMap<String, PostingsList> termsPostingsList) {
      PostingsList result = termsPostingsList.get(terms.get(0));
      terms.remove(0);
      do{
        result = phrase(result, termsPostingsList.get(terms.get(0)));
        terms.remove(0);
      } while (!terms.isEmpty() && result!=null);
      return result;
    }


    public PostingsList intersectQuery(ArrayList<String> terms, HashMap<String, PostingsList> termsPostingsList) {
      terms = sortTerms(terms, termsPostingsList);
      PostingsList result = termsPostingsList.get(terms.get(0));
      terms.remove(0);
      do{
        result = intersect(result, termsPostingsList.get(terms.get(0)));
        terms.remove(0);
      } while (!terms.isEmpty() && result!=null);
      return result;
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType ) { 
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        ArrayList<String> terms = new ArrayList<String>();
        HashMap<String, PostingsList> termsPostingsList = new HashMap<String, PostingsList>();
        for (int i=0; i<query.size(); i++){
          terms.add(query.queryterm.get(i).term);
          termsPostingsList.put(query.queryterm.get(i).term, index.getPostings(query.queryterm.get(i).term));
        }
        switch (queryType) {
          case INTERSECTION_QUERY:
            return intersectQuery(terms, termsPostingsList);
          case PHRASE_QUERY:
            return phraseQuery(terms, termsPostingsList);
        }
        return null;
    }

}