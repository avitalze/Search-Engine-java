package Model;

import java.io.Serializable;
import java.util.HashMap;

public class Term implements Serializable {
    String word;
    private int numOfAppearanceInDocs;
    private HashMap<String,Integer> termAppearanceDocs;
    private int numOfAppearanceInCorpus;

    /**
     * constructor
     * @param word
     */
    public Term(String word) {
        this.word = word;
        this.numOfAppearanceInDocs=0;
        this.termAppearanceDocs= new HashMap<>();
        this.numOfAppearanceInCorpus =0;
    }

    /**
     * constructor
     * @param word
     * @param docName
     * @param numOfTimesInDoc
     */
    public Term(String word,String docName, Integer numOfTimesInDoc){
        this.word=word;
        this.numOfAppearanceInDocs=1;
        this.termAppearanceDocs= new HashMap<>();
        termAppearanceDocs.put(docName,numOfTimesInDoc);
        this.numOfAppearanceInCorpus =numOfTimesInDoc;
    }

    /**
     * returns the word that the term represents
     * @return
     */
    public String getWord() {
        return word;
    }

    /**
     * get number of docs the word appears
     * @return
     */
    public int getNumOfAppearanceInDocs() {
        return numOfAppearanceInDocs;
    }

    /**
     * return the list of doc the word appears
     * @return
     */
    public HashMap<String, Integer> getTermAppearanceDocs() {
        return termAppearanceDocs;
    }

    /**
     * add doc to list of docs
     * @param docName
     * @param numOfTimesInDoc
     */
    public void AddDocToTermList(String docName, Integer numOfTimesInDoc){
        if(termAppearanceDocs.containsKey(docName)){
            Integer count= termAppearanceDocs.get(docName);
            termAppearanceDocs.replace(docName,count+numOfTimesInDoc);
            numOfAppearanceInCorpus +=numOfTimesInDoc;
        }
        else{
            termAppearanceDocs.put(docName,numOfTimesInDoc);
            numOfAppearanceInDocs++;
            numOfAppearanceInCorpus +=numOfTimesInDoc;
        }
    }

    /**
     * change the word
     * @param word
     */
    public void setWord(String word) {
        this.word = word;
    }

    /**
     * override to string
     * @return
     */
    @Override
    public String toString() {
        String allDocsAppearance="";
        for (HashMap.Entry<String,Integer> entry:termAppearanceDocs.entrySet()) {
            allDocsAppearance+=entry.getKey()+","+entry.getValue()+".";
        }
        return word+"#" + numOfAppearanceInDocs +"#" +numOfAppearanceInCorpus+"#"+ allDocsAppearance ;
    }
}
