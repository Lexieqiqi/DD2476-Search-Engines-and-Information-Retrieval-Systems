package ir;

import java.lang.*;
import java.io.*;
import java.util.*;
import java.nio.charset.*;

public class PersistentScalableHashedIndex extends PersistentHashedIndex {

    public int filedId = 1;
    public ArrayList<String> indexedFile = new ArrayList<String>();
    public static final int BOUNDARY = 75000;
    public static final long TABLESIZE = 3500000L;
    public long dicPtr = 0L;
    public long dataPtr = 0L;
    public ArrayList<MergeThread> threads = new ArrayList<MergeThread>();

    class MergeThread extends Thread {
        private MergeThread prevMerger;
        private int num = 0;

        public MergeThread(MergeThread prevMerger, int num) {
            this.prevMerger = prevMerger;
            this.num = num;
        }

        @override
        public void run() {
            try {
                if (prevMerger != null) {
                    prevMerger.join();
                }
                mergingFile();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void insert(String token, int docID, int offset) {

        if (index.size() == BOUNDARY) {
            cleanup();
            if (indexedFile.size() >= 2) {
                if (threads.size() > 0) {
                    MergeThread thread = new MergeThread(threads.get(threads.size() - 1), threads.size());
                    threads.add(thread);
                    thread.start();
                } else {
                    MergeThread thread = new MergeThread(null, threads.size());
                    threads.add(thread);
                    thread.start();
                }
            }
        }
        if (index.containsKey(token)) {
            index.get(token).addToPostingsList(docID, offset);
        } else {
            index.put(token, new PostingsList(docID, offset));
        }

    }

    public void mergingFile() {
        if (indexedFile.size() >= 2) {

            try {

                PersistentHashedIndex pIndex1 = new PersistentHashedIndex();
                PersistentHashedIndex pIndex2 = new PersistentHashedIndex();
                PersistentHashedIndex mIndex = new PersistentHashedIndex();

                pIndex1.dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME + indexedFile.get(0), "rw");
                pIndex1.dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME + indexedFile.get(0), "rw");

                pIndex2.dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME + indexedFile.get(1),"rw");
                pIndex2.dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME + indexedFile.get(1), "rw");

                mIndex.dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME + indexedFile.get(0) + indexedFile.get(1), "rw");
                mIndex.dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME + indexedFile.get(0) + indexedFile.get(1), "rw");

                String fileName1 = INDEXDIR + "/" + DATA_FNAME + indexedFile.get(0);
                String fileName2 = INDEXDIR + "/" + DATA_FNAME + indexedFile.get(1);
                String mergedFileName = INDEXDIR + "/" + DATA_FNAME + indexedFile.get(0) + indexedFile.get(1);

                indexedFile.add(indexedFile.get(0) + indexedFile.get(1));
                indexedFile.remove(0);
                indexedFile.remove(0);

                int dataLen = 0;
                long dataPtr = 0L;
                long dicPtr = 0L;

                // scan the dictionaryFile
                System.out.println("pIndex1 " + pIndex1.dictionaryFile.length());
                System.out.println("pIndex2 " + pIndex2.dictionaryFile.length());
                System.out.println("mIndex " + mIndex.dictionaryFile.length());
                long size = pIndex1.dictionaryFile.length();
                for (long i = 0L; i < size; i = i + 20) {
                    dicPtr = i;
                    System.out.print("\rread: " + i + "/" + pIndex2.dictionaryFile.length());
                    Entry entry1 = pIndex1.readEntry(i);
                    //Entry entry2 = pIndex2.readEntry(i);
                    if (entry1 != null) {
                        System.out.println("fileName:" + fileName1 + "  mergedFileName:" + mergedFileName);
                        System.out.println("fileName:" + fileName2 + "  mergedFileName:" + mergedFileName);
                        System.out.println("Entry1:" + "length:" + entry1.getLength() + "   ptr:" + entry1.getPtr());
                        //System.out.println("Entry2:" + "length:" + entry2.getLength() + "   ptr:" + entry2.getPtr());
                        String dataString = pIndex1.readData(entry1.getPtr(), entry1.getLength());
                        //String dataString2 = pIndex2.readData(entry2.getPtr(), entry2.getLength());
                        System.out.println("readDataString1:"+dataString);
                        //System.out.println("readDataString2:"+dataString2);
                        String term1 = dataString.split("好")[0];
                        
                        System.out.println("token:"+term1);
                        PostingsList pl1 = pIndex1.getPostings(term1);
                        PostingsList pl2 = pIndex2.getPostings(term1);
                        if (pl2 == null) {
                            dataLen = mIndex.writeData(dataString, dataPtr);
                        } else if (pl2 != null) {
                            // String term2 = pIndex2.readData(entry2.getPtr(),
                            // entry2.getLength()).split("好")[0];
                            dataLen = mIndex.writeData(PostingsList.mergePL(pl1, pl2, term1), dataPtr);
                            // delete the entry2
                            pIndex2.writeEntry(null, getEntryPtr(term1));
                        }
                        while (mIndex.readEntry(dicPtr)!=null) {
                            dicPtr += 20;
                        }
                        mIndex.writeEntry(new Entry(entry1.getHashCode(), dataPtr, dataLen, entry1.collisions), dicPtr);
                        dataPtr += dataLen;

                    // } 
                    // else if (entry1 != null && entry2 == null) {
                    //     System.out.println("Entry1:" + "length:" + entry1.getLength() + "   ptr:" + entry1.getPtr());
                    //     String dataString = pIndex1.readData(entry1.getPtr(), entry1.getLength());
                    //     System.out.println("readDataString:"+dataString);
                    //     String term1 = dataString.split("好")[0];
                    //     PostingsList pl1 = pIndex1.getPostings(term1);
                    //     if (pl1 != null) {
                    //         dataLen = mIndex.writeData(dataString, dataPtr);
                    //         while (mIndex.readEntry(dicPtr)!=null) {
                    //             dicPtr += 20;
                    //         }
                    //         mIndex.writeEntry(new Entry(entry1.getHashCode(), dataPtr, dataLen, entry1.collisions), dicPtr);
                    //         dataPtr += dataLen;
                    //     }
                    } else {
                        //
                    }
                }

                System.out.println(pIndex2.dictionaryFile.length());
                for (long i = 0L; i < pIndex2.dictionaryFile.length(); i = i + 20) {
                    System.out.print("\rread2: " + i + "/" + pIndex2.dictionaryFile.length());

                    Entry entry = pIndex2.readEntry(i);
                    if (pIndex2.readEntry(i) != null) {
                        int collisions = 0;
                        dicPtr = i;
                        while (mIndex.readEntry(dicPtr) != null) {
                            if (dicPtr >= mIndex.dictionaryFile.length())
                                break;
                            collisions++;
                            dicPtr = dicPtr + 20;
                        }
                        dataLen = mIndex.writeData(pIndex2.readData(entry.getPtr(), entry.getLength()), dataPtr);
                        mIndex.writeEntry(new Entry(entry.hashCode, dicPtr, dataLen, collisions), dicPtr);
                        dataPtr += dataLen;
                    } else {
                        //
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // thread.interrupt();
        }
    }

    @Override
    public void cleanup() {
        System.err.println(index.keySet().size() + " unique words");
        System.err.print("Writing index to disk...");
        try {
            dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME + "_" + filedId, "rw");
            dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME + "_" + filedId, "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }
        writeIndex();
        System.err.println("done!");
        free = 0L;
        indexedFile.add("_" + filedId);
        filedId = filedId + 1;
        if (index.size() < BOUNDARY) {
            MergeThread thread = new MergeThread(threads.get(threads.size() - 1), threads.size());
            threads.add(thread);
            thread.start();
            String merged_id = "";
            for (int i = 1; i <= filedId; i++) {
                merged_id += "_" + i;
            }
            // try {
            // dictionaryFile = new RandomAccessFile(INDEXDIR + "/" + DICTIONARY_FNAME +
            // merged_id, "rw");
            // dataFile = new RandomAccessFile(INDEXDIR + "/" + DATA_FNAME + merged_id,
            // "rw");
            // } catch (IOException e) {
            // e.printStackTrace();
            // }
        }
        index = new HashMap<String, PostingsList>();
    }

}
