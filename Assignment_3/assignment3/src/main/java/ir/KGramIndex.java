/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class KGramIndex {

    /** Mapping from term ids to actual term strings */
    HashMap<Integer,String> id2term = new HashMap<Integer,String>();

    /** Mapping from term strings to term ids */
    HashMap<String,Integer> term2id = new HashMap<String,Integer>();

    /** Index from k-grams to list of term ids that contain the k-gram */
    HashMap<String,List<KGramPostingsEntry>> index = new HashMap<String,List<KGramPostingsEntry>>();

    HashMap<String, HashMap<String, Integer>> intersectKg = new HashMap<String, HashMap<String, Integer>>();

    /** The ID of the last processed term */
    int lastTermID = -1;

    /** Number of symbols to form a K-gram */
    int K = 3;

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /** Generate the ID for an unknown term */
    private int generateTermID() {
        return ++lastTermID;
    }

    public int getK() {
        return K;
    }


    /**
     *  Get intersection of two postings lists
     */
    private List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {
        // 
        // YOUR CODE HERE
        //
        if (p1 == null || p2 == null) {
    		return null;
        }
        
        List<KGramPostingsEntry> list = new ArrayList<KGramPostingsEntry>();

        int i=0;
        int j=0;
        while (i<p1.size() && j<p2.size()) {
            if (p1.get(i).tokenID == p2.get(j).tokenID) {
                list.add(p1.get(i));
                i++;
                j++;
            } else if(p1.get(i).tokenID < p2.get(j).tokenID) {
                i++;
            } else {
                j++;
            }
        }
    	
        return list;
    }


    public List<String> getKGrams(String token) {
        ArrayList<String> kgrams = new ArrayList<String>();
        String tokenRex = "^" + token + "$";
        for (int i=0; i<token.length()+3-K; i++) {
            String kgram = tokenRex.substring(i, i+K);
            kgrams.add(kgram);
        }
        return kgrams;
    }

    public List<String> getKGramPostings(String token) {

        ArrayList<String> kterms = new ArrayList<String>();
        String tokenRex = "^" + token + "$";
        // do an intersection search for ... in kgram index
        for (int i=0; i<token.length()+3-K; i++) {
            String kgram = tokenRex.substring(i, i+K);
            if(kgram.contains("*")) {
                continue;
            } else {
                getPostings(kgram).stream().forEach(pe -> {
                    if(!kterms.contains(id2term.get(pe.tokenID))) kterms.add(id2term.get(pe.tokenID));
                });
            }
        }

        if (token.contains("*")) {
            // post-process the results using the regex library
            ArrayList<String> results = new ArrayList<String>();
           //System.err.println(token.split("*"));
            System.err.println(tokenRex.split("\\*")[0]);
            System.err.println(tokenRex.split("\\*")[1]);
            String regex = String.format("%s(.*)%s", tokenRex.split("\\*")[0], tokenRex.split("\\*")[1]);
            //List<String> kg1 = getKGrams(token);
            for (String kterm:kterms) {
                if (kterm.matches(regex)) {
                    // List<String> ktermgram = getKGrams(kterm);
                    // int intersectNum = 0;
                    // for (String kg : kg1) {
                    //     if (ktermgram.contains(kg))
                    //         intersectNum++;
                    // }
                    // intersectKg.put(token, new HashMap<String,Integer>().put(kterm, intersectNum));
                    results.add(kterm);
                }
            }
            return results;
        } else {
            return kterms;
        }
    }

    /** Inserts all k-grams from a token into the index. */
    public void insert( String token ) {
        //
        // YOUR CODE HERE
        //
        if (!term2id.containsKey(token)) {
            int termID = generateTermID();
            // insert into term2id and id2term
            term2id.put(token, termID);
            id2term.put(termID, token);

            // insert into index
            String reg = "^" + token + "$";
            KGramPostingsEntry kpe = new KGramPostingsEntry(termID);
            for (int i=0; i<token.length()+3-K; i++) {
                String kgram = reg.substring(i, i+K);
                if (index.containsKey(kgram)) {
                    if (!index.get(kgram).contains(kpe)) {
                        List<KGramPostingsEntry> lkpe = new ArrayList<KGramPostingsEntry>();
                        lkpe.addAll(index.get(kgram));
                        lkpe.add(kpe);
                        index.put(kgram, lkpe);
                    } 
                } else {
                    index.put(kgram, List.of(kpe));
                }
            }
        }
    }


    /** Get postings for the given k-gram */
    public List<KGramPostingsEntry> getPostings(String kgram) {
        //
        // YOUR CODE HERE
        //
        return index.getOrDefault(kgram, null);
    }

    /** Get id of a term */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /** Get a term by the given id */
    public String getTermByID(Integer id) {
        return id2term.get(id);
    }

    private static HashMap<String,String> decodeArgs( String[] args ) {
        HashMap<String,String> decodedArgs = new HashMap<String,String>();
        int i=0, j=0;
        while ( i < args.length ) {
            if ( "-p".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("patterns_file", args[i++]);
                }
            } else if ( "-f".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("file", args[i++]);
                }
            } else if ( "-k".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("k", args[i++]);
                }
            } else if ( "-kg".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("kgram", args[i++]);
                }
            } else {
                System.err.println( "Unknown option: " + args[i] );
                break;
            }
        }
        return decodedArgs;
    }


    
    public static void main(String[] arguments) throws FileNotFoundException, IOException {
        HashMap<String,String> args = decodeArgs(arguments);

        int k = Integer.parseInt(args.getOrDefault("k", "3"));
        KGramIndex kgIndex = new KGramIndex(k);

        File f = new File(args.get("file"));
        Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
        Tokenizer tok = new Tokenizer( reader, true, false, true, args.get("patterns_file") );
        while ( tok.hasMoreTokens() ) {
            String token = tok.nextToken();
            // k-grams indexes
            kgIndex.insert(token);
        }

        String[] kgrams = args.get("kgram").split(" ");
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {
            if (kgram.length() != k) {
                System.err.println("Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + k + "-gram");
                System.exit(1);
            }

            if (postings == null) {
                postings = kgIndex.getPostings(kgram);
            } else {
                postings = kgIndex.intersect(postings, kgIndex.getPostings(kgram));
            }
        }
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < postings.size(); i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }
    }
}
