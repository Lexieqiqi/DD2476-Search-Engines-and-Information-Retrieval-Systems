/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score = 0;
    public ArrayList<Integer> offsets;
    public HashMap<Integer, Integer> phaseOffsets;

    public PostingsEntry(int docID) {
      this.docID = docID;
    }


    public PostingsEntry(int docID, int offset) {
      this.docID = docID;
      offsets = new ArrayList<Integer>();
      offsets.add(offset);
    }

    public void addOffset(int offset) {
      offsets.add(offset);
    }


    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     *  0 equal <0 d1<d2 >0 d1>d2
     */
    public int compareTo( PostingsEntry other ) {
       return Double.compare( other.score, score );
    }

    //
    // YOUR CODE HERE
    //


}

