package Model;

import java.util.HashMap;

public class Document {
    private String docName;
    private int numOfMaxTerm;
    private int numOfUniqueTerms;
    private HashMap<String,Integer> docMap;
    private int numOfEntities;
    private int numOfWordsinDoc;

    /**
     * constructor
     */
    public Document() {
        this.docName="";
        this.numOfMaxTerm=0;
        this.numOfUniqueTerms=0;
        this.docMap= new HashMap<>();
        this.numOfEntities=0;
        this.numOfWordsinDoc=0;
    }
    /**
     * constructor
     */
    public Document(String docName, int numOfMaxTerm, int numOfUniqueTerms, HashMap<String, Integer> docMap, int numOfEntities, int numOfWordsinDoc) {
        this.docName = docName;
        this.numOfMaxTerm = numOfMaxTerm;
        this.numOfUniqueTerms = numOfUniqueTerms;
        this.docMap = docMap;
        this.numOfEntities = numOfEntities;
        this.numOfWordsinDoc = numOfWordsinDoc;
    }

    /**
     * GETTERS AND SETTERS
     * @return
     */
    public String getDocName() {
        return docName;
    }

    public int getNumOfMaxTerm() {
        return numOfMaxTerm;
    }

    public int getNumOfUniqueTerms() {
        return numOfUniqueTerms;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public void setNumOfMaxTerm(int numOfMaxTerm) {
        this.numOfMaxTerm = numOfMaxTerm;
    }

    public void setNumOfUniqueTerms(int numOfUniqueTerms) {
        this.numOfUniqueTerms = numOfUniqueTerms;
    }

    public HashMap<String, Integer> getDocMap() {
        return docMap;
    }

    public int getNumOfEntities() {
        return numOfEntities;
    }

    public int getNumOfWordsinDoc() {
        return numOfWordsinDoc;
    }

    @Override
    public String toString() {
        return docName+","+numOfMaxTerm+","+numOfUniqueTerms+","+numOfEntities+","+numOfWordsinDoc;
    }
}
