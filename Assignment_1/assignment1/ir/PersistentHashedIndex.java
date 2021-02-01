/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.lang.reflect.GenericArrayType;
import java.util.*;
import java.nio.charset.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {

      public int hashCode;
      public long ptr;
      public int length;

      public Entry(int hashCode, long ptr, int length) {
        this.hashCode = hashCode;
        this.ptr = ptr;
        this.length = length;
      }

      public int getHashCode() {
          return this.hashCode;
      }

      public long getPtr() {
        return this.ptr;
      }

      public int getLength() {
        return this.length;
      }

    }


    // ==================================================================

    
    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr ); 
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, long ptr ) {
      try {
        dictionaryFile.seek(ptr);
        dictionaryFile.writeInt(entry.getHashCode());
        dictionaryFile.writeLong( entry.getPtr() );
        dictionaryFile.writeInt( entry.getLength() );
      } catch ( IOException e ) {
        e.printStackTrace();
      }
    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( long ptr) {   
      try {
        if (dictionaryFile.length()>ptr) {
            dictionaryFile.seek( ptr );
            int hashCode = dictionaryFile.readInt();
            long dataPtr = dictionaryFile.readLong();
            int dataLen = dictionaryFile.readInt();
            // if the entry is written
            if (dataLen!=0) return new Entry(hashCode, dataPtr,dataLen);
            // if the entry is empty
            return null;
        }
        else {
          return null;
        }
      } catch ( IOException e ) {
          System.out.println(ptr);
          e.printStackTrace();
          return null;
      }
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for (Map.Entry<Integer,String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(new Integer(data[0]), data[1]);
                docLengths.put(new Integer(data[0]), new Integer(data[2]));
            }
        }
        freader.close();
    }

    public int getHashCode(String token) {
      return Math.abs(token.hashCode());
    }


    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            long dataFilePtr = free;
            long entryFilePtr = 0L;
            // Write the dictionary and the postings list
            Iterator<String> it = index.keySet().iterator();
            while(it.hasNext()){
              String term = it.next().toString();
              int hashCode =  getHashCode(term);
              entryFilePtr =  (hashCode%TABLESIZE)*16;
              Entry entry = readEntry(entryFilePtr);
              while (entry!=null) {
                if ( entry.hashCode != hashCode ) {
                    collisions++;
                    entryFilePtr += 16;  
                    entry = readEntry(entryFilePtr);
                }
                else break;
              } 

              int datalen = writeData(index.get(term).getString(term), dataFilePtr);
              
              writeEntry(new Entry(hashCode, dataFilePtr, datalen), entryFilePtr);
              dataFilePtr = dataFilePtr + datalen;

            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }


    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        int hashCode =  getHashCode(token);
        long entryFilePtr =  (hashCode%TABLESIZE)*16;
        Entry entry = readEntry(entryFilePtr);
        while (entry!=null) {
          if ( entry.hashCode != hashCode ) {
              entryFilePtr += 16;  
              entry = readEntry(entryFilePtr);
          }
          else break;
        }
        if (entry == null) return null;
        else {
          String data = readData( entry.getPtr(), entry.getLength() );
          String pl = data.split("@@##")[1];
          return PostingsList.stringToPL(pl); 
        }

    }


    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
      if (index.containsKey(token)) {
        index.get(token).addToPostingsList(docID, offset);
      }
      else {
        index.put(token, new PostingsList(docID, offset));
      }
    }


    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
    }
}
