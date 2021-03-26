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
import java.util.HashSet;
import java.util.*;
import java.lang.Math;
import java.util.stream.*;

/**
 * Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    private HashMap<String, Integer> docIDs = new HashMap<String, Integer>();

    /** Constructor */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    public void buildNameToID() {
        for (Map.Entry<Integer, String> entry : index.docNames.entrySet()) {
            docIDs.put(getFileName(entry.getValue()), entry.getKey());
        }
    }

    private String getFileName(String path) {
        int index = path.lastIndexOf("\\");
        String result = path.substring(index + 1, path.length());
        return result;
    }

    public ArrayList<String> sortTerms(ArrayList<String> terms, HashMap<String, PostingsList> termsPostingsList) {

        for (int i = 0; i < terms.size() - 1; i++) {
            for (int j = 0; j < terms.size() - 1 - i; j++) {
                if (termsPostingsList.get(terms.get(j)).size() > termsPostingsList.get(terms.get(j + 1)).size()) {
                    String temp = terms.get(j + 1);
                    terms.set(j + 1, terms.get(j));
                    terms.set(j, temp);
                }
            }
        }
        return terms;
    }

    public PostingsList intersect(PostingsList p1, PostingsList p2) {
        PostingsList answer = new PostingsList();
        if (p1!=null && p2!=null) {
            int size1 = p1.size();
            int size2 = p2.size();
            int i = 0;
            int j = 0;
            int docID1 = 0;
            int docID2 = 0;
    
            if (size1 == 0 && size2 == 0) {
                return null;
            } else if (size1 == 0 && size2 > 0) {
                return p2;
            } else if (size1 > 0 && size2 == 0) {
                return p1;
            } else {
                p1.sortByDocId();
                p2.sortByDocId();
                while (i < size1 && j < size2) {
                    docID1 = p1.get(i).docID;
                    docID2 = p2.get(j).docID;
                    if (docID1 == docID2) {
                        answer.addToPostingsList(docID1);
                        ++i;
                        ++j;
                    } else if (docID1 < docID2) {
                        ++i;
                    } else
                        ++j;
                }
                return answer;
            }
        } else return null;

    }

    public PostingsList phrase(PostingsList p1, PostingsList p2) {
        PostingsList answer = new PostingsList();
        int size1 = p1.size();
        int size2 = p2.size();
        int i = 0;
        int j = 0;
        int docID1 = 0;
        int docID2 = 0;

        p1.sortByDocId();
        p2.sortByDocId();
        while (i < size1 && j < size2) {
            
            if (p1.get(i) != null && p2.get(j) != null) {
                docID1 = p1.get(i).docID;
                docID2 = p2.get(j).docID;
                if (docID1 == docID2) {
                    int m = 0;
                    int n = 0;

                    ArrayList<Integer> pp1 = p1.getOffsetsByDocID(docID1).offsets;
                    ArrayList<Integer> pp2 = p2.getOffsetsByDocID(docID2).offsets;
                    
                    while (m < pp1.size() && n < pp2.size()) {
                        if (pp2.get(n) - pp1.get(m) == 1) {
                            answer.addToPostingsList(docID1, pp2.get(n));
                            m++;
                            n++;
                        } else if (pp2.get(n) > pp1.get(m))
                            m++;
                        else
                            n++;
                    }
                    ++i;
                    ++j;
                } else if (docID1 < docID2) {
                    ++i;
                } else
                    ++j;
            } else {
                break;
            }
        
        }
        return answer;
    }

    public PostingsList phraseQuery(ArrayList<String> terms, HashMap<String, PostingsList> termsPostingsList) {
        // PostingsList result = termsPostingsList.get(terms.get(0));
        // terms.remove(0);
        // while (!terms.isEmpty() && result != null) {
        //     result = phrase(result, termsPostingsList.get(terms.get(0)));
        //     terms.remove(0);
        // }

        PostingsList result = new PostingsList();
        // kgram terms
        List<List<String>> kterms = terms.stream()
                .map(term -> term.contains("*") ? kgIndex.getKGramPostings(term) : List.of(term))
                .collect(Collectors.toList());

        // kgram postinglists
        List<PostingsList> pls = new ArrayList<PostingsList>();
        for (List<String> kterm : kterms) {
            PostingsList pl = new PostingsList();
            for (String term : kterm) {
                //System.err.println(index.getPostings(term));
                pl.mergePLByOffsets(index.getPostings(term));
            }
            pls.add(pl);
        }

        result = pls.get(0);
        int i = 1;
        while (i<pls.size() && result != null) {
            result = phrase(result, pls.get(i));
            i++;
        }
        // for (PostingsList pl : pls) {
        //     result = phrase(result, pl);
        // }
        return result;
    }

    public PostingsList intersectQuery(ArrayList<String> terms, HashMap<String, PostingsList> termsPostingsList) {
        //terms = sortTerms(terms, termsPostingsList);
        PostingsList result = new PostingsList();
        // kgram terms
        List<List<String>> kterms = terms.stream()
                .map(term -> term.contains("*") ? kgIndex.getKGramPostings(term) : List.of(term))
                .collect(Collectors.toList());

        // kgram postinglists
        List<PostingsList> pls = new ArrayList<PostingsList>();
        for (List<String> kterm : kterms) {
            PostingsList pl = new PostingsList();
            for (String term : kterm) {
                //System.err.println(index.getPostings(term));
                if(index.getPostings(term)!=null)
                    pl.mergePL(index.getPostings(term));
            }
            pls.add(pl);
        }

        for (PostingsList pl : pls) {
            result = intersect(result, pl);
        }
        return result;
    }

    public PostingsList rankedQuery(ArrayList<String> terms, HashMap<String, PostingsList> termsPostingsList) {
        PostingsList pl = new PostingsList();
        int N = index.docNames.size();

        List<List<String>> kterms = terms.stream()
        .map(term -> term.contains("*") ? kgIndex.getKGramPostings(term) : List.of(term))
        .collect(Collectors.toList());

        for (List<String> kterm : kterms) {
            //PostingsList pl = new PostingsList();
            for (String term : kterm) {
                //System.err.println(index.getPostings(term));
                PostingsList pl_t = index.getPostings(term);
                double qt_w = 1.0;
                if(pl_t!=null) {
                    int df_t = pl_t.size();
                    double idf = Math.log(N / df_t);
                    for (int j = 0; j < df_t; j++) {
                        int docId = pl_t.get(j).docID;
                        int tf_d = pl_t.get(j).getTf();
                        double tf_idf = qt_w * idf * tf_d / index.docLengths.get(docId);
                        pl.addToPostingsList(docId, tf_idf);
                    }
                }
                    //pl.mergePL(index.getPostings(term));
            }
            //pls.add(pl);
        }

        // for (int i = 0; i < terms.size(); i++) {
        //     String term = terms.get(i);
        //     double qt_w = 1;
        //     PostingsList pl_t = termsPostingsList.get(term);
        //     // document frequency of term
        //     int df_t = pl_t.size();
        //     double idf = Math.log(N / df_t);
        //     for (int j = 0; j < df_t; j++) {
        //         int docId = pl_t.get(j).docID;
        //         int tf_d = pl_t.get(j).getTf();
        //         double tf_idf = qt_w * idf * tf_d / index.docLengths.get(docId);
        //         pl.addToPostingsList(docId, tf_idf);
        //     }
        // }
        pl.sort();
        return pl;
    }

    public PostingsList rankedEucQuery(ArrayList<String> terms, HashMap<String, PostingsList> termsPostingsList) {
        PostingsList pl = new PostingsList();
        int N = index.docNames.size();
        for (int i = 0; i < terms.size(); i++) {
            String term = terms.get(i);
            double qt_w = 1;
            PostingsList pl_t = termsPostingsList.get(term);
            // document frequency of term
            int df_t = pl_t.size();
            double idf = Math.log(N / df_t);
            System.out.println("idf:" + idf);
            for (int j = 0; j < df_t; j++) {
                int docId = pl_t.get(j).docID;
                int tf_d = pl_t.get(j).getTf();
                double tf_idf = qt_w * idf * tf_d / index.docEucLens.get(docId);
                // System.out.println("euclidean length:" + index.docEucLens.get(docId));
                pl.addToPostingsList(docId, tf_idf);
            }
        }
        pl.sort();
        return pl;
    }

    public PostingsList hitsRankSearch(ArrayList<String> terms, HashMap<String, PostingsList> termsPostingsList) {
        PostingsList pl = new PostingsList();
        HITSRanker hitsRanker = new HITSRanker(this.index);
        HashSet<String> baseSet = new HashSet<String>();
        for (int i = 0; i < terms.size(); i++) {
            PostingsList pl_i = termsPostingsList.get(terms.get(i));
            for (int j = 0; j < pl_i.size(); j++) {
                pl.addToPostingsList(pl_i.get(j).docID);
            }
        }

        return hitsRanker.rank(pl);
    }

    public PostingsList pageRank() {
        PageRank pr = new PageRank(index);
        return pr.computePageRank();
    }

    public PostingsList combineEuc(ArrayList<String> terms, HashMap<String, PostingsList> termsPostingsList) {
        PostingsList pl1 = pageRank();
        PostingsList pl2 = rankedEucQuery(terms, termsPostingsList);
        PostingsList pl = new PostingsList();
        for (int i = 0; i < pl2.size(); i++) {
            double score = 0.5 * pl2.get(i).score + 0.5 * pl1.get(i).score;
            pl.addToPostingsList(pl2.get(i).docID, score);
        }
        pl.sort();
        return pl;
    }

    public PostingsList combineMan(ArrayList<String> terms, HashMap<String, PostingsList> termsPostingsList) {
        PostingsList pl1 = pageRank();
        PostingsList pl2 = rankedEucQuery(terms, termsPostingsList);
        PostingsList pl = new PostingsList();
        for (int i = 0; i < pl2.size(); i++) {
            double score = 0.5 * pl2.get(i).score + 0.5 * pl1.get(i).score;
            pl.addToPostingsList(pl2.get(i).docID, score);
        }
        pl.sort();
        return pl;
    }

    /**
     * Searches the index for postings matching the query.
     * 
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType, NormalizationType normType) {
        //
        // REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        ArrayList<String> terms = new ArrayList<String>();
        HashMap<String, PostingsList> termsPostingsList = new HashMap<String, PostingsList>();
        for (int i = 0; i < query.size(); i++) {
            terms.add(query.queryterm.get(i).term);
            termsPostingsList.put(query.queryterm.get(i).term, index.getPostings(query.queryterm.get(i).term));
        }
        switch (queryType) {
        case INTERSECTION_QUERY:
            return intersectQuery(terms, termsPostingsList);
        case PHRASE_QUERY:
            return phraseQuery(terms, termsPostingsList);
        case RANKED_QUERY:
            switch (rankingType) {
            case TF_IDF:
                switch (normType) {
                case EUCLIDEAN:
                    return rankedEucQuery(terms, termsPostingsList);
                case NUMBER_OF_WORDS:
                    return rankedQuery(terms, termsPostingsList);
                }
            case PAGERANK:
                return pageRank();
            case COMBINATION:
                switch (normType) {
                case EUCLIDEAN:
                    return combineEuc(terms, termsPostingsList);
                case NUMBER_OF_WORDS:
                    return combineMan(terms, termsPostingsList);
                }
            case HITS:
                return hitsRankSearch(terms, termsPostingsList);
            }
        }
        return null;
    }

}