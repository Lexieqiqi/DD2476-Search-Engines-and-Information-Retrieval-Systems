/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.util.*;

public class SpellChecker {
    /** The regular inverted index to be used by the spell checker */
    Index index;

    /** K-gram index to be used by the spell checker */
    KGramIndex kgIndex;

    /**
     * The auxiliary class for containing the value of your ranking function for a
     * token
     */
    class KGramStat implements Comparable {
        double score;
        String token;

        KGramStat(String token, double score) {
            this.token = token;
            this.score = score;
        }

        public String getToken() {
            return token;
        }

        public int compareTo(Object other) {
            if (this.score == ((KGramStat) other).score)
                return 0;
            return this.score < ((KGramStat) other).score ? -1 : 1;
        }

        public String toString() {
            return token + ";" + score;
        }
    }

    /**
     * The threshold for Jaccard coefficient; a candidate spelling correction should
     * pass the threshold in order to be accepted
     */
    private static final double JACCARD_THRESHOLD = 0.4;

    /**
     * The threshold for edit distance for a candidate spelling correction to be
     * accepted.
     */
    private static final int MAX_EDIT_DISTANCE = 2;

    public SpellChecker(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     * Computes the Jaccard coefficient for two sets A and B, where the size of set
     * A is <code>szA</code>, the size of set B is <code>szB</code> and the
     * intersection of the two sets contains <code>intersection</code> elements.
     */
    private double jaccard(int szA, int szB, int intersection) {
        //
        // YOUR CODE HERE
        //
        double score = (double) intersection / (double) (szA + szB - intersection);
        return (double) intersection / (double) (szA + szB - intersection);
    }

    /**
     * Computing Levenshtein edit distance using dynamic programming. Allowed
     * operations are: => insert (cost 1) => delete (cost 1) => substitute (cost 2)
     */
    private int editDistance(String s1, String s2) {
        //
        // YOUR CODE HERE
        //
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] editMatrix = new int[len1+1][len2+1];
        for (int i=0; i<len1; i++) editMatrix[i][0]=i;
        for (int i=0; i<len2; i++) editMatrix[0][i]=i;
        for (int i = 1; i < len1+1; i++) {
            for (int j = 1; j < len2+1; j++) {
                int cost = s1.charAt(i-1) == s2.charAt(j-1) ? 0 : 1;
                int delete = editMatrix[i-1][j]+1;
                int insert = editMatrix[i][j-1]+1;
                int substitution = editMatrix[i-1][j-1] + cost;
                editMatrix[i][j] = Math.min(delete,Math.min(insert,substitution));
            }
        }
        return editMatrix[len1][len2];
    }

    /**
     * Checks spelling of all terms in <code>query</code> and returns up to
     * <code>limit</code> ranked suggestions for spelling correction.
     */
    public String[] check(Query query, int limit) {
        //
        // YOUR CODE HERE
        //
        int checkNum = 0;
        ArrayList<KGramStat> kgss = new ArrayList<KGramStat>();
        ArrayList<ArrayList<KGramStat>> qkgrams = new ArrayList<ArrayList<KGramStat>>();
        for (int i = 0; i < query.size(); i++) {
            // isolated word
            String token = query.queryterm.get(i).term;
            kgss = new ArrayList<KGramStat>();
            if (index.getPostings(token) == null) {
                checkNum++;
                // calculate the Jacqard coefficient
                // get kgrams for this token
                List<String> kg1 = kgIndex.getKGrams(token);

                long startTime0 = System.currentTimeMillis();
                HashMap<String,Integer> kgOccur = new HashMap<String,Integer>();
                for (String kg : kg1) {
                    List<KGramPostingsEntry> kpe = kgIndex.index.get(kg);
                    for (int k=0;k<kpe.size();k++) {
                        String possibleToken = kgIndex.getTermByID(kpe.get(k).tokenID);
                        if (kgOccur.containsKey(possibleToken)){
                            int occur = kgOccur.get(possibleToken) + 1;
                            kgOccur.put(possibleToken,occur);
                        } else {
                            kgOccur.put(possibleToken,1);
                        }
                    }
                }
                List<String> results = new ArrayList<String>();
                Iterator re = kgOccur.keySet().iterator();
                while (re.hasNext()) {
                    String next = re.next().toString();
                    results.add(next);
                }
                // HashMap<String, Integer> results = new LinkedHashMap<>();
                // kgOccur.entrySet()
                // .stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                // .forEachOrdered(x -> results.put(x.getKey(), x.getValue()));;
                //List<String> results = kgIndex.getKGramPostings(token);
                long elapsedTime0 = System.currentTimeMillis() - startTime0;
                System.out.println("It takes "+elapsedTime0 / 1000.0 + "s to get all the possible words");
                
                long startTime = System.currentTimeMillis();
                for (String result : results) {
                    // kgIndex.term2id.get(result);
                    // for (String kg : kg1) {
                    //     kgIndex.index.get(kg)
                    // }
                    List<String> kg2 = kgIndex.getKGrams(result);
                    int intersectNum = 0;
                    for (String kg : kg1) {
                        if (kg2.contains(kg))
                            intersectNum++;
                    }
                    
                    double score = jaccard(kg1.size(), kg2.size(), intersectNum);
                    
                    if (score >= JACCARD_THRESHOLD) {
                        int maxC = editDistance(result, token);
                        if (maxC<=MAX_EDIT_DISTANCE){
                            KGramStat kgs = new KGramStat(result, maxC);
                            kgss.add(kgs);
                        }
                    }
                }
                long elapsedTime = System.currentTimeMillis() - startTime;
                System.out.println("for check possible words, it takes "+elapsedTime / 1000.0 + "s to check");
                kgss.sort(new Comparator<KGramStat>() {
                    @Override
                    public int compare(KGramStat kgs1, KGramStat kgs2) {
                        return kgs1.compareTo(kgs2);
                    }
                });
            } 
            qkgrams.add(kgss);
        }

        List<KGramStat> qtokens = new ArrayList<KGramStat>();
        if(checkNum==0){
            return null;
        } else if (checkNum==1) {
            qtokens = qkgrams.get(0);
        } 
        if(checkNum>1) {
            qtokens = mergeCorrections(qkgrams, limit);
        }
        int len = limit < qtokens.size() ? limit : qtokens.size();

        String[] results = new String[len];
        for (int i = 0; i < len; i++) {
            results[i] = qtokens.get(i).token;
        }
        return results;
    }

    /**
     * Merging ranked candidate spelling corrections for all query terms available
     * in <code>qCorrections</code> into one final merging of query phrases. Returns
     * up to <code>limit</code> corrected phrases.
     */
    private List<KGramStat> mergeCorrections(ArrayList<ArrayList<KGramStat>> qCorrections, int limit) {
        //
        // YOUR CODE HERE
        //
        ArrayList<KGramStat> kgss = qCorrections.get(0);
        for (int i = 0; i < qCorrections.size() - 1; i++) {
            kgss = new ArrayList<KGramStat>();
            List<KGramStat> ekgram1 = qCorrections.get(i);
            List<KGramStat> ekgram2 = qCorrections.get(i + 1);
            int len1 = ekgram1.size()<limit ? ekgram1.size() : limit;
            int len2 = ekgram2.size()<limit ? ekgram2.size() : limit;
            for (int m = 0; m<len1; m++) {
                for (int n=0; n<len2; n++) {
                    String token = ekgram1.get(m).token + " " + ekgram2.get(n).token;
                    double score = (ekgram1.get(m).score + ekgram2.get(n).score) / 2;
                    kgss.add(new KGramStat(token, score));
                }
            }
        }
        kgss.sort(new Comparator<KGramStat>() {
            @Override
            public int compare(KGramStat kgs1, KGramStat kgs2) {
                return kgs1.compareTo(kgs2);
            }
        });
        List<KGramStat> result = new ArrayList<KGramStat>();
        int len = result.size()<limit ? kgss.size() : limit;
        for (int i = 0; i < len; i++) {
            result.add(kgss.get(i));
        }
        return result;
    }

}
