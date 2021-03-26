/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.*;

public class PostingsList {

    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();
    private HashMap<Integer, Integer> listDoc = new HashMap<Integer, Integer>();

    public PostingsList() {
        //
    }

    public PostingsList(ArrayList<PostingsEntry> list) {
        this.list = list;
    }

    public PostingsList(int docID, int offset) {
        list.add(new PostingsEntry(docID, offset));
        listDoc.put(docID, list.size() - 1);
    }

    public void addToPostingsList(int docID) {
        if (!listDoc.containsKey(docID)) {
            list.add(new PostingsEntry(docID));
            listDoc.put(docID, list.size() - 1);
        }
    }

    public void addToPostingsList(int docID, int offset) {
        if (!listDoc.containsKey(docID)) {
            list.add(new PostingsEntry(docID, offset));
            listDoc.put(docID, list.size() - 1);
        } else {
            list.get(listDoc.get(docID)).addOffset(offset);
        }
    }

    public void addToPostingsList(int docID, double score) {
        if (!listDoc.containsKey(docID)) {
            list.add(new PostingsEntry(docID, score));
            listDoc.put(docID, list.size() - 1);
        } else {
            list.get(listDoc.get(docID)).score += score;
        }
    }

    public void sortByDocId() {
        list.sort(new Comparator<PostingsEntry>() {
            @Override
            public int compare(PostingsEntry pe1, PostingsEntry pe2) {
                return pe1.compareToDocId(pe2);
            }
        });
    }

    public void sort() {
        list.sort(new Comparator<PostingsEntry>() {
            @Override
            public int compare(PostingsEntry pe1, PostingsEntry pe2) {
                return pe1.compareTo(pe2);
            }
        });

    }

    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get(int i) {
        return list.get(i);
    }

    public int getTf(int i) {
        return list.get(i).offsets.size();
    }

    public int getTfByDocId(int i) {
        return list.get(listDoc.get(i)).offsets.size();
    }

    public PostingsEntry getOffsetsByDocID(int docID) {
        // the PostingEntry which has the docID
        for (PostingsEntry pet : list) {
            if (pet.docID == docID)
                return pet;
        }
        return null;
    }

    public String getString(String token) {
        String pString = "";
        for (int i = 0; i < list.size(); i++) {
            pString = pString + list.get(i).getString();
        }
        return token + "囧" + pString + "\n";
    }

    public static PostingsList stringToPL(String pl) {
        String[] et = pl.split(";");
        PostingsList p = new PostingsList();
        for (int i = 0; i < et.length - 1; i++) {
            int doc = Integer.parseInt(et[i].split(":")[0]);
            String[] stringOffsets = et[i].split(":")[1].split(",");
            for (int j = 0; j < stringOffsets.length; j++) {
                p.addToPostingsList(doc, Integer.parseInt(stringOffsets[j]));
            }
        }
        return p;
    }

    public void mergePL(PostingsList new_pl) {
        for (int i = 0; i < new_pl.size(); i++) {
            this.addToPostingsList(new_pl.get(i).docID);
        }
    }

    public void mergePLByOffsets(PostingsList new_pl) {
        if (new_pl != null) {
            for (int i = 0; i < new_pl.size(); i++) {
                final int index = i;
                new_pl.get(i).offsets.stream()
                        .forEach(offset -> this.addToPostingsList(new_pl.get(index).docID, offset));
            }
        }
    }

    public static String mergePL(PostingsList pl1, PostingsList pl2, String term) {
        for (int i = 0; i < pl2.size(); i++) {
            pl1.list.add(pl2.get(i));
        }
        return pl1.getString(term);
    }
}
