/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.HashMap;

public class PostingsList {
    
    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();
    private HashMap<Integer, Integer> listDoc = new HashMap<Integer, Integer>();

    public PostingsList() {
      //
    }

    public PostingsList(int docID, int offset) {
      list.add(new PostingsEntry(docID, offset));
      listDoc.put(docID, list.size()-1);
    }

    public void addToPostingsList(int docID) {
      if (!listDoc.containsKey(docID)) {
        list.add(new PostingsEntry(docID));
        listDoc.put(docID, list.size()-1);
      }
    }

    public void addToPostingsList(int docID, int offset) {
      if (!listDoc.containsKey(docID)) {
        list.add(new PostingsEntry(docID, offset));
        listDoc.put(docID, list.size()-1);
      } else {
        list.get(listDoc.get(docID)).addOffset(offset);
      }
    }


    /** Number of postings in this list. */
    public int size() {
      return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
      return list.get( i );
    }

    public PostingsEntry getOffsetsByDocID(int docID){
      // the PostingEntry which has the docID
      return list.get(listDoc.get(docID));
    }

    /** Add  */
}

