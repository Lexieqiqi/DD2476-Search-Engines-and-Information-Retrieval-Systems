/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.nio.charset.*;
import java.io.*;
import java.util.*;


/**
 *  A class for representing a query as a list of words, each of which has
 *  an associated weight.
 */
public class Query {

    /**
     *  Help class to represent one query term, with its associated weight. 
     */
    class QueryTerm {
        String term;
        double weight;
        QueryTerm( String t, double w ) {
            term = t;
            weight = w;
        }
    }

    /** 
     *  Representation of the query as a list of terms with associated weights.
     *  In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();
    public HashMap<String, Integer> queryts = new HashMap<String, Integer>();

    /**  
     *  Relevance feedback constant alpha (= weight of original query terms). 
     *  Should be between 0 and 1.
     *  (only used in assignment 3).
     */
    double alpha = 0.2;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    double beta = 1 - alpha;
    
    
    /**
     *  Creates a new empty Query 
     */
    public Query() {
    }
    
    
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
        StringTokenizer tok = new StringTokenizer( queryString );
        while ( tok.hasMoreTokens() ) {
            String token = tok.nextToken();
            queryterm.add( new QueryTerm(token, 1.0) );
            queryts.put(token, queryterm.size()-1);
        }    
    }
    
    public void addToQueryTerm (String query) {
        if(!queryts.containsKey(query)) {
            queryterm.add( new QueryTerm(query, 1.0) );
            queryts.put(query, queryterm.size()-1);
        }
    }
    /**
     *  Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }
    
    
    /**
     *  Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for ( QueryTerm t : queryterm ) {
            len += t.weight; 
        }
        return len;
    }
    
    
    /**
     *  Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for ( QueryTerm t : queryterm ) {
            queryCopy.queryterm.add( new QueryTerm(t.term, t.weight) );
        }
        return queryCopy;
    }
    
    
    /**
     *  Expands the Query using Relevance Feedback
     *
     *  @param results The results of the previous query.
     *  @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     *  @param engine The search engine object
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Engine engine ) {
        //
        //  YOUR CODE HERE
        //
        // get each doc from the results. 
        ArrayList<QueryTerm> original_qt = new ArrayList<QueryTerm>();
        for (int k = 0; k<queryterm.size();k++){
            original_qt.add(new QueryTerm(queryterm.get(k).term, queryterm.get(k).weight));
        }
        // calculate the weight of each term in the doc.. * beta
        int N = engine.index.docNames.size();
        for (int i=0; i< docIsRelevant.length; i++) {
            if(docIsRelevant[i]) {
                int docId = results.get(i).docID;
                // read the relevant file again
                File doc = new File( engine.index.docNames.get( docId ));
                ArrayList<String> tokens = new ArrayList<String>();
                HashMap<String, Integer> toccr = new HashMap<String, Integer>();
                try {
                    Reader reader = new InputStreamReader( new FileInputStream(doc), StandardCharsets.UTF_8 );
                    Tokenizer tok = new Tokenizer( reader, true, false, true, "/Volumes/Yuqi/uni/DD2476/Assignment_3/assignment3/patterns.txt" );
                    while ( tok.hasMoreTokens() ) {
                        String token = tok.nextToken();
                        if (!tokens.contains(token)) {
                            tokens.add(token);
                            toccr.put(token, 1);
                        } else {
                            int occ = toccr.get(token);
                            occ++;
                            toccr.put(token, occ);
                        }
                    }
                } catch ( IOException e ) {
                    System.err.println( "Warning: IOException during indexing." );
                }

                double docEucLen = 0.0;
                docEucLen = engine.index.docEucLens.get(docId);
                for (int j=0; j<tokens.size(); j++) {
                    String token = tokens.get(j);
                    int df_t = engine.index.getPostings(token).size();
                    double idf = Math.log( N / (double)df_t);
                    addToQueryTerm(token);
                    QueryTerm qt = queryterm.get(queryts.get(token));
                    int tf_d =  toccr.get(token);
                    double tf_idf = idf * tf_d / docEucLen;
                    if (original_qt.contains(qt)){
                        qt.weight += alpha * tf_idf;
                    } else {
                        qt.weight += beta * tf_idf;
                    }
                }
            }

        }
    }
}


