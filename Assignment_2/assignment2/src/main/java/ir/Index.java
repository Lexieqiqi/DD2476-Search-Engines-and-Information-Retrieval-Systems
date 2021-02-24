/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.HashMap;
import java.util.Iterator;

/**
 *  Defines some common data structures and methods that all types of
 *  index should implement.
 */
public interface Index {

    /** Mapping from document identifiers to document names. */
    public HashMap<Integer,String> docNames = new HashMap<Integer,String>();

    public HashMap<Integer,String> docTitles = new HashMap<Integer,String>();
    public HashMap<String,Integer> titleDocid = new HashMap<String,Integer>();
    public HashMap<String,Boolean> docT = new HashMap<String,Boolean>();

    /** Mapping from document identifier to document length. */
    public HashMap<Integer,Integer> docLengths = new HashMap<Integer,Integer>();

    public HashMap<Integer,Double> docEucLens = new HashMap<Integer, Double>();

    /** Inserts a token into the index. */
    public void insert( String token, int docID, int offset );

    /** Returns the postings for a given term. */
    public PostingsList getPostings( String token );

    /** This method is called on exit. */
    public void cleanup();

}
