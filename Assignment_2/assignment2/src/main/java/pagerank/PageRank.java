package pagerank;
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

       
    /* --------------------------------------------- */


    public PageRank( String filename ) {
	int noOfDocs = readDocs( filename );
	iterate( noOfDocs, 1000 );
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
    void iterate( int numberOfDocs, int maxIterations ) {
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
                outlinks.put(i, null);
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

        DocRank[] output = new DocRank[numberOfDocs];
        for(int i=0;i<numberOfDocs;i++){
            output[i] = new DocRank(i,x_[i]);
        }

        Arrays.sort(output);

        for(int i=0;i<30;i++){ 
            System.out.println(docName[output[i].docID]+": "+output[i].rank); 
        }
        
    }



    /* --------------------------------------------- */


    public static void main( String[] args ) {
	if ( args.length != 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
	else {
	    new PageRank( args[0] );
	}
    }
}
