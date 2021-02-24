/**
 *   Computes the Hubs and Authorities for an every document in a query-specific
 *   link graph, induced by the base set of pages.
 *
 *   @author Dmytro Kalpakchi
 */

package ir;

import java.util.*;
import java.io.*;


public class HITSRanker {

    /**
     *   Max number of iterations for HITS
     */
    int MAX_NUMBER_OF_STEPS = 1000;

    /**
     *   Convergence criterion: hub and authority scores do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.001;

    /**
     *   The inverted index
     */
    Index index;

    /**
     *   Mapping from the titles to internal document ids used in the links file
     */
    HashMap<String,Integer> titleToId = new HashMap<String,Integer>();

    HashMap<Integer,String> IdTotitle = new HashMap<Integer,String>();
    /**
     *   Sparse vector containing hub scores
     */
    HashMap<Integer,Double> hubs = new HashMap<Integer,Double>();

    /**
     *   Sparse vector containing authority scores
     */
    HashMap<Integer,Double> authorities = new HashMap<Integer,Double>();

    HashMap<Integer, HashMap<Integer, Boolean>> inlink = new HashMap<Integer, HashMap<Integer, Boolean>>();
    HashMap<Integer, HashMap<Integer, Boolean>> outlink = new HashMap<Integer, HashMap<Integer, Boolean>>();

    
    /* --------------------------------------------- */

    /**
     * Constructs the HITSRanker object
     * 
     * A set of linked documents can be presented as a graph.
     * Each page is a node in graph with a distinct nodeID associated with it.
     * There is an edge between two nodes if there is a link between two pages.
     * 
     * Each line in the links file has the following format:
     *  nodeID;outNodeID1,outNodeID2,...,outNodeIDK
     * This means that there are edges between nodeID and outNodeIDi, where i is between 1 and K.
     * 
     * Each line in the titles file has the following format:
     *  nodeID;pageTitle
     *  
     * NOTE: nodeIDs are consistent between these two files, but they are NOT the same
     *       as docIDs used by search engine's Indexer
     *
     * @param      linksFilename   File containing the links of the graph
     * @param      titlesFilename  File containing the mapping between nodeIDs and pages titles
     * @param      index           The inverted index
     */
    public HITSRanker( String linksFilename, String titlesFilename, Index index ) {
        this.index = index;
        readDocs( linksFilename, titlesFilename );
    }

    public HITSRanker(Index index) {
        this.index = index;
        readDocs("/Volumes/Yuqi/uni/DD2476/Assignment_2/assignment2/src/main/java/pagerank/linksDavis.txt", "/Volumes/Yuqi/uni/DD2476/Assignment_2/assignment2/src/main/java/pagerank/davisTitles.txt");
        rank();
    }


    /* --------------------------------------------- */

    /**
     * A utility function that gets a file name given its path.
     * For example, given the path "davisWiki/hello.f",
     * the function will return "hello.f".
     *
     * @param      path  The file path
     *
     * @return     The file name.
     */
    private String getFileName( String path ) {
        String result = "";
        StringTokenizer tok = new StringTokenizer( path, "\\/" );
        while ( tok.hasMoreTokens() ) {
            result = tok.nextToken();
        }
        return result;
    }


    /**
     * Reads the files describing the graph of the given set of pages.
     *
     * @param      linksFilename   File containing the links of the graph
     * @param      titlesFilename  File containing the mapping between nodeIDs and pages titles
     */
    void readDocs( String linksFilename, String titlesFilename ) {
        //
        // YOUR CODE HERE
        //
        int fileIndex = 0;
        try {
            // read link file
            System.err.print( "HITS: Reading link file... " );
            BufferedReader in = new BufferedReader( new FileReader( linksFilename ));
            String line;
            while ((line = in.readLine()) != null ) {
                int index = line.indexOf( ";" );
                String title = line.substring( 0, index );
                Integer fromdoc = Integer.parseInt( title );
                if ( outlink.get(fromdoc) == null) {	
                    outlink.put(fromdoc, new HashMap<Integer, Boolean>());
                }
                fileIndex++;
                // Check all outlinks.
                StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
                while ( tok.hasMoreTokens()) {
                    String otherTitle = tok.nextToken();
                    Integer otherDoc = Integer.valueOf( otherTitle );
                    if ( outlink.get(fromdoc).get(otherDoc) == null ) {
                        outlink.get(fromdoc).put(otherDoc, true);
                    }
                    if ( inlink.get(otherDoc) == null ) {
                        HashMap<Integer, Boolean> i = new HashMap<Integer, Boolean>();
                        i.put(fromdoc, true);
                        inlink.put(otherDoc, i);
                    } else {
                        inlink.get(otherDoc).put(fromdoc, true);
                    }
                }
            }
            // read titles file
            fileIndex = 0;
            System.err.print( "HITS: Reading title file... " );
            BufferedReader readTitles = new BufferedReader(new FileReader(titlesFilename));
            String tline;
            while ((tline = readTitles.readLine()) != null) {
                int index = tline.indexOf(";");
                Integer docID = Integer.valueOf(tline.substring(0, index));
                String docTitle = tline.substring(index + 1);
                titleToId.put(docTitle, docID);
                IdTotitle.put(docID, docTitle);
                fileIndex++;               
            }
        } catch (FileNotFoundException e) {
            System.err.println("File " + linksFilename + " not found!");
        } catch (IOException e) {
            System.err.println("Error reading file " + linksFilename);
        }
        System.err.println("Read " + fileIndex + " number of documents---links");
    }

    /**
     * Perform HITS iterations until convergence
     *
     * @param      titles  The titles of the documents in the root set
     */
    private void iterate(String[] titles) {
        //
        // YOUR CODE HERE
        //
        // Initialize ai and hi for hubs and authorities
        for (int i=0; i<titles.length; i++) {
            Integer k = titleToId.get(titles[i]);
            hubs.put(k, 1.0);
            authorities.put(k, 1.0);
        }

        // iterate until convergence
        int iteration = 0;
        boolean hubsConverged = false;
        boolean authoritiesConverged = false;

        while (iteration++ < MAX_NUMBER_OF_STEPS) {

            System.out.println(iteration);
            double differences_hubs = 0.0;
            double differences_authorities = 0.0;


                // calculate hi
                double hubEucLen = 0.0;
                HashMap<Integer,Double> hubs_ = new HashMap<Integer,Double>();
                for (int i=0; i<titles.length; i++) {
                    int hId = titleToId.get(titles[i]);
                    if( outlink.get(hId) != null ) {
                        Iterator<Integer> outlinks = outlink.get(hId).keySet().iterator();
                        double sum_i = 0.0;
                        while (outlinks.hasNext()) {
                            int out = outlinks.next();
                            if (authorities.get(out)!=null) sum_i += authorities.get(out);
                        }
                        hubs_.put(hId, sum_i);
                        hubEucLen += Math.pow(sum_i, 2);
                    } else {
                        hubs_.put(hId, 0.0);
                    }
                    
                }

                // normalization and count differences
                hubEucLen = Math.sqrt(hubEucLen);
                for (int i=0; i<titles.length; i++) {
                    if (titleToId.get(titles[i])!=null) {
                        int hId = titleToId.get(titles[i]);
                        double value = hubs_.get(hId)/hubEucLen;
                        hubs_.put(hId, value);
                        differences_hubs += Math.abs(value-hubs.get(hId));
                        hubs.put(hId, value);
                    }
                }

                if (differences_hubs <= EPSILON) {
                    hubsConverged = true;
                    System.out.println("hub difference:"+ differences_hubs);
                    System.out.println("hubs completed!");
                }



                // calculate ai
                double authEucLen = 0.0;
                HashMap<Integer,Double> authorities_ = new HashMap<Integer,Double>();
                for (int i=0; i<titles.length; i++) {
                    int aId = titleToId.get(titles[i]);
                    if( inlink.get(aId) != null ) {
                        Iterator<Integer> inlinks = inlink.get(aId).keySet().iterator();
                        double sum_i = 0.0;
                        while (inlinks.hasNext()) {
                            int in = inlinks.next();
                            if (hubs.get(in)!=null) sum_i += hubs.get(in);
                        }
                        authorities_.put(aId, sum_i);
                        authEucLen += Math.pow(sum_i, 2);
                    } else {
                        authorities_.put(aId, 0.0);
                    }
                }

                // normalization 
                authEucLen = Math.sqrt(authEucLen);
                for (int i=0; i<titles.length; i++) {
                    if (titleToId.get(titles[i])!=null) {
                        int aId = titleToId.get(titles[i]);
                        double value = authorities_.get(aId)/authEucLen;
                        authorities_.put(aId, value);
                        differences_authorities += Math.abs(authorities.get(aId)-value);
                        authorities.put(aId, value);
                    }
                }               
    
                if ( differences_authorities <= EPSILON) {
                    authoritiesConverged = true;
                    System.out.println("authority difference:"+ differences_authorities);
                    System.out.println("authority completed!");         
                }




            if (hubsConverged && authoritiesConverged ) break;

        }


    }


    /**
     * Rank the documents in the subgraph induced by the documents present
     * in the postings list `post`.
     *
     * @param      post  The list of postings fulfilling a certain information need
     *
     * @return     A list of postings ranked according to the hub and authority scores.
     */
    PostingsList rank(PostingsList post) {
        //
        // YOUR CODE HERE
        //
        HashSet<String> baseSet = new HashSet<String>();
        for (int i=0; i < post.size(); i++) {
            int gen_docId = post.get(i).docID;
            String docTitle = index.docTitles.get(gen_docId);
            if (titleToId.containsKey(docTitle)) {
                baseSet.add(docTitle);
    
                int rankId = titleToId.get(docTitle);
                if (inlink.get(rankId)!=null) {
                    Iterator<Integer> linkto = inlink.get(rankId).keySet().iterator();
                    while (linkto.hasNext()) {
                        int inlk = linkto.next();
                        String docNam = IdTotitle.get(inlk);
                        if (index.docT.containsKey(docNam)) baseSet.add(docNam);
                    }
                }
                if (outlink.get(rankId) != null) {
                    Iterator<Integer> linkfrom = outlink.get(rankId).keySet().iterator();
                    while (linkfrom.hasNext()) {
                        int outl = linkfrom.next();
                        String docNam = IdTotitle.get(outl);
                        if (index.docT.containsKey(docNam)) baseSet.add(docNam);
                    }
                }
                
            }
        }


        String[] Titles = baseSet.toArray(new String[0]);
        // iterate(Titles);
        

        //HashMap<Integer,Double> sortedHubs = sortHashMapByValue(hubs);
        //HashMap<Integer,Double> sortedAuthorities = sortHashMapByValue(authorities);

        PostingsList pl = new PostingsList();
        Iterator<Integer> iteratorHubs = hubs.keySet().iterator();
        while (iteratorHubs.hasNext()) {
            int rankid = iteratorHubs.next();
            if (baseSet.contains(IdTotitle.get(rankid))){
                int gen_id = index.titleDocid.get(IdTotitle.get(rankid));
                double score = 0.5 * hubs.get(rankid) + 0.5 * authorities.get(rankid);
                pl.addToPostingsList(gen_id, score);
            }
        }
        pl.sort();
        return pl;
    }


    /**
     * Sort a hash map by values in the descending order
     *
     * @param      map    A hash map to sorted
     *
     * @return     A hash map sorted by values
     */
    private HashMap<Integer,Double> sortHashMapByValue(HashMap<Integer,Double> map) {
        if (map == null) {
            return null;
        } else {
            List<Map.Entry<Integer,Double> > list = new ArrayList<Map.Entry<Integer,Double> >(map.entrySet());
      
            Collections.sort(list, new Comparator<Map.Entry<Integer,Double>>() {
                public int compare(Map.Entry<Integer,Double> o1, Map.Entry<Integer,Double> o2) { 
                    return (o2.getValue()).compareTo(o1.getValue()); 
                } 
            }); 
              
            HashMap<Integer,Double> res = new LinkedHashMap<Integer,Double>(); 
            for (Map.Entry<Integer,Double> el : list) { 
                res.put(el.getKey(), el.getValue()); 
            }
            return res;
        }
    } 


    /**
     * Write the first `k` entries of a hash map `map` to the file `fname`.
     *
     * @param      map        A hash map
     * @param      fname      The filename
     * @param      k          A number of entries to write
     */
    void writeToFile(HashMap<Integer,Double> map, String fname, int k) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fname));
            
            if (map != null) {
                int i = 0;
                for (Map.Entry<Integer,Double> e : map.entrySet()) {
                    i++;
                    writer.write(e.getKey() + ": " + String.format("%.5g%n", e.getValue()));
                    if (i >= k) break;
                }
            }
            writer.close();
        } catch (IOException e) {}
    }


    /**
     * Rank all the documents in the links file. Produces two files:
     *  hubs_top_30.txt with documents containing top 30 hub scores
     *  authorities_top_30.txt with documents containing top 30 authority scores
     */
    void rank() {
        iterate(titleToId.keySet().toArray(new String[0]));
        HashMap<Integer,Double> sortedHubs = sortHashMapByValue(hubs);
        HashMap<Integer,Double> sortedAuthorities = sortHashMapByValue(authorities);
        writeToFile(sortedHubs, "hubs_top_30.txt", 30);
        writeToFile(sortedAuthorities, "authorities_top_30.txt", 30);
    }


    /* --------------------------------------------- */


    public static void main( String[] args ) {
        if ( args.length != 2 ) {
            System.err.println( "Please give the names of the link and title files" );
        }
        else {
            HITSRanker hr = new HITSRanker( args[0], args[1], null );
            hr.rank();
        }
    }
} 