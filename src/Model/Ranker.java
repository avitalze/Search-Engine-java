package Model;

import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;

public class Ranker {
    boolean withSemantic;
    private String postingPath;
    private Indexer indexer;
    double B= 0.1;
    double K=0.1 ;

    /**
     * Constructor
     * @param postingPath
     * @param indexer
     */
    public Ranker(String postingPath,Indexer indexer) {
        this.withSemantic = false;
        this.postingPath = postingPath;
        this.indexer= indexer;
    }

    /**
     * Constructor
     * @param withSemantic
     * @param numOfDocs
     * @param averageDocLength
     * @param postingPath
     * @param indexer
     */
    public Ranker(boolean withSemantic, int numOfDocs, double averageDocLength, String postingPath, Indexer indexer) {
        this.withSemantic = withSemantic;
        this.postingPath = postingPath;
    }


    /**
     * This function accepts a Hash map of term with there wieght and returns the 50 most relevent documents
     * @param query
     * @return
     */
    public List<String> rankQuery(HashMap<String, Double> query) {
        LinkedHashMap<String, Double> finalScorePerDoc = new LinkedHashMap<>();
        try {
            int numOfDocs = indexer.getNumOfDocs();
            double averageDocLength = indexer.getSumWordsInCorpus()/ numOfDocs;
            Double currScore;
            String currWord;
            File postFile;
            BufferedReader postFileReader;
            String[] splitedLine;
            String linePostFile = null;
            String[] allDocs = null;
            String numDoc = "";
            String numOfAppearances = "";
            File DocsInfo = new File(postingPath + "\\infoAllDocsUpdated.txt");
            BufferedReader readDocInfo;
            String lineOfDoc = null;
            String[] splitedLineInfoDoc;
            String nameCurrDoc;
            double tf;
            double idf;
            double rank;
            double currRank;
            int freq = 0;
            int docLength = 0;
            int numDocsContainWord = 0;
            int counter=0;
            String lowerWord;
            int numMaxTerm;
            for (HashMap.Entry<String, Double> wordEntry : query.entrySet()) {
                currWord = wordEntry.getKey();
                currScore = wordEntry.getValue();
                lowerWord= StringUtils.lowerCase(currWord);
                if (Indexer.dictionary.containsKey(StringUtils.upperCase(currWord)) || Indexer.dictionary.containsKey(StringUtils.lowerCase(currWord))) { // if dic contain word
                    postFile = new File(postingPath + "\\postingsFiles\\" + currWord.charAt(0) + ".txt");//to lower case?!?!?!??
                    postFileReader = new BufferedReader(new FileReader(postFile));

                    while ((linePostFile = postFileReader.readLine()) != null) {
                        splitedLine = StringUtils.split(linePostFile, "#");
                        if (splitedLine[0].equals(currWord) || splitedLine[0].equals(lowerWord)) {
                            numDocsContainWord = Integer.parseInt(splitedLine[1]);
                            allDocs = StringUtils.split(splitedLine[3], ".");
                            break;
                        }
                    }

                    if(allDocs!=null){
                        HashMap<String, Integer> docsAndAppearence = new HashMap<>();
                        for (int i = 0; i < allDocs.length; i++) {
                            numDoc = StringUtils.substring(allDocs[i], 0, StringUtils.indexOf(allDocs[i], ","));
                            numOfAppearances = StringUtils.substring(allDocs[i], StringUtils.indexOf(allDocs[i], ",") + 1);
                            docsAndAppearence.put(numDoc, Integer.parseInt(numOfAppearances));
                        }

                        readDocInfo = new BufferedReader(new FileReader(DocsInfo));

                        counter=0;
                        while ((lineOfDoc = readDocInfo.readLine()) != null && counter<allDocs.length) {
                            splitedLineInfoDoc = StringUtils.split(lineOfDoc, ",");
                            nameCurrDoc = splitedLineInfoDoc[0];
                            if (docsAndAppearence.containsKey(nameCurrDoc)) {
                                // culc score

                                docLength = Integer.parseInt(splitedLineInfoDoc[4]);
                                //numMaxTerm= Integer.parseInt(splitedLineInfoDoc[1]);
                                freq = docsAndAppearence.get(numDoc) ;/// numMaxTerm;//docsAndAppearence.size();
                                tf = freq / (freq + K * (1 - B + B * (docLength / averageDocLength)));
                                idf = Math.log((numOfDocs - numDocsContainWord + 0.5) / (numDocsContainWord + 0.5));//add +1!!!!!!!!
                                rank = tf * idf * currScore;
                                currRank = 0;
                                if (finalScorePerDoc.containsKey(nameCurrDoc)) {
                                    currRank = finalScorePerDoc.get(nameCurrDoc);
                                    finalScorePerDoc.replace(nameCurrDoc, currRank + rank);
                                } else {
                                    finalScorePerDoc.put(nameCurrDoc, rank);
                                }
                                counter++;
                            }// if
                        }// while
                    }
                }
            }//for
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //sorting
        List<Map.Entry<String, Double>> entries = new ArrayList<Map.Entry<String, Double>>(finalScorePerDoc.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> a, Map.Entry<String, Double> b){
                return b.getValue().compareTo(a.getValue());
            }
        });
        int listSize=entries.size();
        if(listSize>50){
            listSize=50;
        }
        List<String> queryAns=new LinkedList<>();
        for(int j=0;j<listSize;j++){
            queryAns.add(entries.get(j).getKey());
        }
        return queryAns;
    }

}
