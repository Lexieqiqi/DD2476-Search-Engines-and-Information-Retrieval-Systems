package ir;
import java.util.*;

import javax.security.auth.kerberos.KeyTab;

import java.io.*;

public class PageRank {

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    HashMap<String,Integer> docNumber = new HashMap<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];


    HashMap<String,Integer> titleToId = new HashMap<String,Integer>();

    HashMap<Integer,String> IdTotitle = new HashMap<Integer,String>();


    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a HashMap, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a HashMap whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    HashMap<Integer,HashMap<Integer,Boolean>> link = new HashMap<Integer,HashMap<Integer,Boolean>>();

    HashMap<Integer,HashMap<Integer,Boolean>> outlinks = new HashMap<Integer,HashMap<Integer,Boolean>>();

    ArrayList<Integer> nullLinks = new ArrayList<Integer>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

    Index index;
       
    /* --------------------------------------------- */


    public PageRank( String filename, String method ) {
    int noOfDocs = readDocs( filename );
    computePageRank(noOfDocs, method);
    //computeDiffPR(noOfDocs);
    }

    public PageRank(Index index ) {
        int noOfDocs = readDocs( "/Volumes/Yuqi/uni/DD2476/Assignment_2/assignment2/src/main/java/pagerank/linksDavis.txt" );
        this.index = index;
        readTitles("/Volumes/Yuqi/uni/DD2476/Assignment_2/assignment2/src/main/java/pagerank/davisTitles.txt");
    }


    class DocRank implements Comparable<DocRank>{                                
        public int docID;
        public double rank;

        public DocRank(int docIDIn, double rankIn){
            docID = docIDIn;
            rank = rankIn;
        }

        public int compareTo(DocRank other){
            if(this.rank > other.rank){return -1;}
            if(this.rank < other.rank){return 1;}
            return 0;
        }
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and fills the data structures. 
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
	int fileIndex = 0;
	try {
	    System.err.print( "Reading file... " );
	    BufferedReader in = new BufferedReader( new FileReader( filename ));
	    String line;
	    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
		int index = line.indexOf( ";" );
		String title = line.substring( 0, index );
		Integer fromdoc = docNumber.get( title );
		//  Have we seen this document before?
		if ( fromdoc == null ) {	
		    // This is a previously unseen doc, so add it to the table.
		    fromdoc = fileIndex++;
		    docNumber.put( title, fromdoc );
		    docName[fromdoc] = title;
		}
		// Check all outlinks.
		StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
		while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
		    String otherTitle = tok.nextToken();
		    Integer otherDoc = docNumber.get( otherTitle );
		    if ( otherDoc == null ) {
			// This is a previousy unseen doc, so add it to the table.
			otherDoc = fileIndex++;
			docNumber.put( otherTitle, otherDoc );
			docName[otherDoc] = otherTitle;
		    }
		    // Set the probability to 0 for now, to indicate that there is
		    // a link from fromdoc to otherDoc.
		    if ( link.get(fromdoc) == null ) {
			link.put(fromdoc, new HashMap<Integer,Boolean>());
		    }
		    if ( link.get(fromdoc).get(otherDoc) == null ) {
			link.get(fromdoc).put( otherDoc, true );
			out[fromdoc]++;
		    }
		}
	    }
	    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
		System.err.print( "stopped reading since documents table is full. " );
	    }
	    else {
		System.err.print( "done. " );
	    }
	}
	catch ( FileNotFoundException e ) {
	    System.err.println( "File " + filename + " not found!" );
	}
	catch ( IOException e ) {
	    System.err.println( "Error reading file " + filename );
	}
	System.err.println( "Read " + fileIndex + " number of documents" );
	return fileIndex;
    }


    /* --------------------------------------------- */


    double[] monteCarlo(int numberOfDocs, int numWalks, int method) {
        double[] x = new double[numberOfDocs];
        Random random = new Random();
        boolean isDangling = false;

        if (method == 1) {
            // random start
            for (int i=0; i < numWalks; i++) {
                int initPage = random.nextInt(numberOfDocs);
                LinkedList<Integer> list = randomWalk(numberOfDocs, initPage, isDangling);
                x[list.removeLast().intValue()]++;
            }
            // Simmulate N runs of random walk
            for (int i=0; i<numberOfDocs; i++) {
                x[i] = x[i] / (double)numWalks;
            }
        } else if (method==2) {
            // cyclic start
            int initMtimes = numberOfDocs;
            for (int i=0; i<initMtimes; i++) {
                for (int j=0; j<100; j++) {
                    LinkedList<Integer> list = randomWalk(numberOfDocs, i, isDangling);
                    x[list.removeLast().intValue()]++;
                }
            }
            // Simulate N=mn runs of the random walk
            double N = (double) (numWalks*initMtimes);
            for (int i=0; i<numberOfDocs; i++) {
                x[i] = x[i] / N;
            }
        } else if (method == 4) {
            // complete path 
            isDangling = true;
            int initMtimes = numberOfDocs;
            int total_visits = 0;
            // #visits to node j during walk
            for (int i=0; i<initMtimes; i++) {
                for (int j=0; j<100; j++) {
                    LinkedList<Integer> list = randomWalk(numberOfDocs, i, isDangling);
                    total_visits += list.size();
                    while (list.size() != 0) {
                        x[list.removeLast().intValue()]++;
                    }
                }
            }
            for (int i=0; i<numberOfDocs; i++) {
                x[i] = x[i] / (double)total_visits;
            }
        } else if (method == 5) {
            isDangling = true;
            int total_visits = 0;
            for (int i=0; i<numWalks; i++) {
                int initPage = random.nextInt(numberOfDocs);
                LinkedList<Integer> list = randomWalk(numberOfDocs, initPage, isDangling);
                total_visits += list.size();
                while (list.size() != 0) {
                    x[list.removeLast().intValue()]++;
                }
            }
            for (int i=0; i<numberOfDocs; i++) {
                x[i] = x[i] / (double)total_visits;
            }
        }

        return x;
    }

    LinkedList<Integer> randomWalk(int numberOfDocs, int initPage, boolean isDangling) {
        LinkedList<Integer> list = new LinkedList<Integer>();
        Random random = new Random();
        int page = initPage;
        list.add(page);

        while (true) {
            // if bored, start a new walk
            if(random.nextDouble() < BORED) {
                break;
            }
            else {
                HashMap<Integer, Boolean> walk = link.get(page);
                if (walk == null) {
                    if (isDangling) break;
                } else {
                    ArrayList<Integer> walks = new ArrayList<Integer>(walk.keySet());
                    page = walks.get(random.nextInt(walks.size())).intValue();
                }
                list.add(page);
            }
        }
        return list;
    }

    double diff(double[] x1, double[] x2) {
        double difference = 0.0;
        for (int i=0; i<x1.length; i++) {
            difference += Math.abs(x1[i]-x2[i]);
        }
        return difference;
    }

    /*
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
    double[] iterate( int numberOfDocs, int maxIterations ) {
        // convert link to outlinks
        for (int i=0; i< numberOfDocs; i++) {
            if (link.get(i)!=null) {
                Iterator<Integer> it = link.get(i).keySet().iterator();
                while (it.hasNext()) {
                    int key = it.next();
                    if (outlinks.get(key)!=null) {
                        outlinks.get(key).put(i, true);
                    } else {
                        HashMap<Integer, Boolean> kpointi = new HashMap<Integer,Boolean>();
                        kpointi.put(i, true);
                        outlinks.put(key, kpointi);
                    }
                }
            } else {
                nullLinks.add(i);
            }
        }

        System.out.println("finish outlinks");

        double[] x = new double[numberOfDocs];
        double[] x_ = new double[numberOfDocs];


        // initialization 
        x[0] = 1.0;
        int iteration = 0;
        // power iteration 
        // xG
        do {
            if (iteration!=0) {
                x = x_.clone();
            }
            double nullValue = 0.0;
            for (int j = 0; j < nullLinks.size(); j++) {
                nullValue += (1-BORED) * (double)x[nullLinks.get(j)] / (double)numberOfDocs;
            }
            //System.out.println(nullValue);
            // xG = (1-c)xJ + xcP
            for (int i = 0; i<numberOfDocs; i++) {
                x_[i] = BORED / (double)numberOfDocs;
                if (outlinks.get(i)!=null) {
                    Iterator<Integer> it = outlinks.get(i).keySet().iterator();
                    while (it.hasNext()) {
                        int outlink = it.next();
                        x_[i] += x[outlink] * (1-BORED) / link.get(outlink).size();
                    }
                }
                x_[i] += nullValue;
            }
            double sum = 0.0;
            for (int i = 0; i<x_.length; i++) {
                sum += x_[i];
            }
            for (int i = 0; i<x_.length; i++) {
                x_[i] = x_[i] / sum;
            }
            iteration++;
            System.out.println(diff(x, x_));
        } while (iteration<maxIterations && diff(x, x_) > EPSILON);

        return x_;
        
    }


    void computeDiffPR(int numberOfDocs) {
        double[] prs = computePageRank(numberOfDocs, "0");
        System.out.println(0);
        double[] aPrs1 = computePageRank(numberOfDocs, "1");
        System.out.println(1);
        double[] aPrs2 = computePageRank(numberOfDocs, "2");
        System.out.println(2);
        double[] aPrs4 = computePageRank(numberOfDocs, "4");
        System.out.println(3);
        double[] aPrs5 = computePageRank(numberOfDocs, "5");
        System.out.println(4);
        double[] differences = new double[4];
        for (int i=0; i<30; i++) {
            differences[0] += Math.pow(aPrs1[i]-prs[i],2);
            differences[1] += Math.pow(aPrs2[i]-prs[i],2);
            differences[2] += Math.pow(aPrs4[i]-prs[i],2);
            differences[3] += Math.pow(aPrs5[i]-prs[i],2);
        }
        for (int i=0; i<4; i++) {
            System.out.println(i + " : " + differences[i]);
        }
    }


    void readTitles( String titlesFilename ) {
        int fileIndex = 0;
        try {
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
        }catch (FileNotFoundException e) {
            System.err.println("File " + titlesFilename + " not found!");
        } catch (IOException e) {
            System.err.println("Error reading file " + titlesFilename);
        }
    }


    double[] computePageRank(int numberOfDocs, String input) {
        int method = Integer.parseInt(input);
        double[] pr = new double[numberOfDocs];
        // power iteration method
        if (method == 0 || method == 6) {
            pr = iterate( numberOfDocs, 1000 );
        } else {
            pr = monteCarlo(numberOfDocs, 100000, method);
        }
        if (method== 6) {
            File file = new File("" + "./pagerank.score");
    		if (!file.exists()) {
    			try {
    				file.createNewFile();
    				FileWriter fw = new FileWriter(file, true);
    				for (int i = 0; i < pr.length; i++) {
    					fw.append(docName[i] + ":" + pr[i]);
    					fw.append(System.lineSeparator());
    				}
    				fw.close();
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
            }
            return null;
        } else {
            DocRank[] output = new DocRank[numberOfDocs];
            for(int i=0;i<numberOfDocs;i++){
                output[i] = new DocRank(i,pr[i]);
            }
            Arrays.sort(output);
            double[] ranks = new double[30];
            for(int i=0;i<30;i++){ 
                ranks[i] = output[i].rank;
                System.out.println(docName[output[i].docID]+": "+output[i].rank); 
            }
            return ranks;
        }
    }

    PostingsList computePageRank() {
        int numberOfDocs = titleToId.size();
        double[] pr = new double[numberOfDocs];
        PostingsList pl = new PostingsList();

        pr = iterate( numberOfDocs, 1000 );

        DocRank[] output = new DocRank[numberOfDocs];
        for(int i=0;i<numberOfDocs;i++){
            output[i] = new DocRank(i,pr[i]);
        }
        Arrays.sort(output);
        for (int i=0; i<output.length; i++) {
            if (index.docT.containsKey( IdTotitle.get(output[i].docID))) {
                int gen_docId = index.titleDocid.get(IdTotitle.get(output[i].docID));
                pl.addToPostingsList(gen_docId,output[i].rank);
            }
        }
        return pl;
    }




    /* --------------------------------------------- */


    public static void main( String[] args ) {
	if ( args.length < 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
	else if (args.length < 2) {
        System.err.println( "Please give the rank method" );
    } else {
	    new PageRank( args[0], args[1] );
    }
    }   
}
