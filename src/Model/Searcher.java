package Model;

import com.medallia.word2vec.Word2VecModel;
import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import javax.lang.model.element.Element;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;


public class Searcher {
    private boolean withSemantic;
    private boolean withEntitiesSearch;
    private boolean withStemmer;
    private boolean withInternet;
    //private Parser parser;
    private Indexer indexer;
    private Ranker ranker;
    private final String USER_AGENT = "Mozilla/5.0";
    double ratio=0.75;

    /**
     * Constructor
     * @param withSemantic
     * @param withEntitiesSearch
     * @param withStemmer
     * @param withInternet
     * @param postFilePath
     * @param indexer
     */
    public Searcher(boolean withSemantic, boolean withEntitiesSearch,boolean withStemmer,boolean withInternet, String postFilePath,Indexer indexer) {
        this.withSemantic = withSemantic;
        this.withEntitiesSearch = withEntitiesSearch;
        this.withStemmer=withStemmer;
        this.withInternet=withInternet;
        //this.parser=new Parser(withStemmer);
        this.ranker= new Ranker(postFilePath,indexer);
        this.indexer=indexer;
    }

    /**
     * Constructor
     * @param postFilePath
     * @param indexer
     */
    public Searcher(String postFilePath,Indexer indexer) {
        this.withSemantic = false;
        this.withEntitiesSearch = false;
        this.withStemmer=false;
        this.withInternet=false;
        //this.parser=new Parser(withStemmer);
        this.ranker= new Ranker(postFilePath,indexer);
        this.indexer=indexer;
    }

    /**
     * this function accepts the query, description and query ID and return the query answer
     * @param query
     * @param queryID
     * @param desc
     * @return
     */
    public LinkedHashMap<String,List<Pair<String,Double>>> searchSingleQuery(String query, String queryID, String desc){
        HashMap<String,Double> queryScore=new HashMap<>();
        if(withSemantic && withInternet){
            try {
                String[] wordsInQuery = StringUtils.split(query, " ");
                for (int i = 0; i < wordsInQuery.length; i++) {
                    String url = "https://api.datamuse.com/words?rel_syn=" + wordsInQuery[i];
                    URL obj = new URL(url);
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                    con.setRequestMethod("GET");
                    con.setRequestProperty("User-Agent", USER_AGENT);

                    // ordering the response
                    StringBuilder response;
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                        String inputLine;
                        response = new StringBuilder();

                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                    }

                    String words=StringUtils.replace(response.toString(),"[","");
                    words=StringUtils.replace(words,"]","");
                    words=StringUtils.replace(words,"\"","");
                    words=StringUtils.replace(words,"word:","");
                    words=StringUtils.replace(words,",score","");
                    words=StringUtils.replace(words,"{","");
                    words=StringUtils.replace(words,"}","");
                    String[] wordsScore= StringUtils.split(words,":,");

                    Document wordDDoc;
                    for(int j=0; j<wordsScore.length && j<6;j=j+2){
                        wordDDoc = indexer.getParser().parseText(wordsScore[j], "defualt");
                        for(String w:wordDDoc.getDocMap().keySet()){
                            queryScore.put(w, 0.2);
                        }

                    }

                }
            }catch (UnknownHostException e){
                withInternet=false;
            }
            catch (IOException e) {
                    e.getMessage();
            }
        }
        else if(withSemantic && !withInternet){
                try {
                    query=StringUtils.replace(query,"-"," ");
                    query=StringUtils.replace(query,","," ");
                    Word2VecModel mod= Word2VecModel.fromTextFile(new File(".\\word2vec.c.output.model.txt"));
                    com.medallia.word2vec.Searcher search= mod.forSearch();

                    String[] wordsInQuery = StringUtils.split(query, " ");
                    String currWord="";
                    String machWord="";
                    int numOfWord=3;
                    List<com.medallia.word2vec.Searcher.Match> matches=null;

                    for (int i = 0; i < wordsInQuery.length; i++) {
                        try {
                            currWord = StringUtils.lowerCase(wordsInQuery[i]);
                            matches = search.getMatches(currWord, numOfWord);


                            for (int j = 1; j < numOfWord; j++) {
                                machWord = StringUtils.substring(matches.get(j).toString(), 0, StringUtils.indexOf(matches.get(j).toString(), " "));
                                Document wordDDoc;
                                //score=Integer.parseInt(returnWord);
                                wordDDoc = indexer.getParser().parseText(machWord, "defualt"); //!! sercher !
                                for (String w : wordDDoc.getDocMap().keySet()) {
                                    queryScore.put(w, 0.1);
                                }
                            }
                        }
                        catch (com.medallia.word2vec.Searcher.UnknownWordException e) { }
                    }
                }catch (IOException e) {
                    e.getMessage();
                }

        }

        Document parsedQueryDoc= indexer.getParser().parseText(query,queryID);
        for (String word:parsedQueryDoc.getDocMap().keySet()) {
            queryScore.put(word,new Double(1));
        }
        double sumQueryRank=parsedQueryDoc.getDocMap().size();

        double currRank;
        if(desc!=null) {
            Document parsedDescDoc = indexer.getParser().parseText(desc, queryID);
            double descRank = sumQueryRank / (ratio * parsedDescDoc.getDocMap().size());
            for (String word : parsedDescDoc.getDocMap().keySet()) {
                if(queryScore.containsKey(word)){
                    currRank=queryScore.get(word);
                    queryScore.replace(word,currRank+descRank);
                }
                else{
                    queryScore.put(word, new Double(descRank));
                }

            }
        }
        double numOfAppearance=0;
        List<String> ans=ranker.rankQuery(queryScore);
        LinkedHashMap<String,List<Pair<String,Double>>> returnQuerySearch= new LinkedHashMap<>();
        String docName;
        String[] entitiesInDoc;
        String[] split;
        String entity;
        double maxEntityAppearance;
        List<Pair<String,Double>> top5;
            for(int i=0; i< ans.size();i++){
                docName= ans.get(i);
                top5= new LinkedList<>();
                if(withEntitiesSearch) {
                    entitiesInDoc= indexer.getAllEntitiesPerDoc(docName);
                    if(entitiesInDoc==null){// no entities in doc
                        returnQuerySearch.put(docName, top5);
                    }
                    else {
                        maxEntityAppearance = 0;
                        for (int j = 0; j < entitiesInDoc.length; j++) {
                            split = StringUtils.split(entitiesInDoc[j], "#");
                            entity = split[0];
                            numOfAppearance = Double.parseDouble(split[1]);
                            if (j == 0) {
                                maxEntityAppearance = numOfAppearance;
                            }
                            top5.add(new Pair<>(entity, (double) (numOfAppearance / maxEntityAppearance)));
                        }
                        returnQuerySearch.put(docName, top5);
                    }
                }
                else{// no entity search
                    returnQuerySearch.put(docName,null);
                }
            }
        return returnQuerySearch;
    }

    /**
     * this function acceptsa path to a query file and return the answer to all queries int the file
     * @param queryFilePath
     * @return
     */
    public LinkedHashMap<String,LinkedHashMap<String, List<Pair<String, Double>>>> searchFileQuery(String queryFilePath) {
        LinkedHashMap<String,LinkedHashMap<String, List<Pair<String, Double>>>> allQueries = new LinkedHashMap<>();
        File file = new File(queryFilePath);
        if(!file.exists()){
            return null;
        }
        try {
            org.jsoup.nodes.Document doc = Jsoup.parse(file, "UTF-8");
            Elements allElemforDoc = doc.getElementsByTag("top");
            Iterator it = allElemforDoc.iterator();
            //String docName="";
            org.jsoup.nodes.Element currQueryElem;
            String currQuery;
            String queryNum;
            String queryText;
            String descText;
            while(it.hasNext()) {
                currQueryElem = (org.jsoup.nodes.Element)it.next();
                currQuery = currQueryElem.toString();

                queryNum =StringUtils.substring(currQuery,StringUtils.indexOf(currQuery, "Number: ") , StringUtils.indexOf(currQuery, "<title>"));
                queryNum =StringUtils.substring(queryNum,StringUtils.indexOf(queryNum, " "),StringUtils.indexOf(queryNum, "\n"));
                queryNum= StringUtils.strip(queryNum," ");

                queryText =StringUtils.substring(currQuery,StringUtils.indexOf(currQuery, "<title>") , StringUtils.indexOf(currQuery, "<desc>"));
                queryText=StringUtils.substring(queryText,StringUtils.indexOf(queryText, " ") , StringUtils.indexOf(queryText, "\r"));
                queryText= StringUtils.strip(queryText," ");

                descText =StringUtils.substring(currQuery,StringUtils.indexOf(currQuery, "<desc>") , StringUtils.indexOf(currQuery, "<narr>"));
                descText=StringUtils.substring(descText,StringUtils.indexOf(descText,":")+2);
                
                allQueries.put(queryNum,searchSingleQuery(queryText,queryNum,descText));
            }
        }
        catch (FileNotFoundException e) {

        } catch (IOException e) {

        }

        return allQueries;
    }

    /**
     * this function writes the query ans the a file by the path accepted
     * @param filePath
     * @param queryID
     * @param queryResultsDocsOnly
     */
    public void writeQueryResultsToFile(String filePath,String queryID,Set<String> queryResultsDocsOnly){
        File results= new File(filePath+"\\results.txt");
        StringBuilder str= new StringBuilder();
        try{
            if(!results.exists()){
                results.createNewFile();
            }
            FileWriter writer= new FileWriter(results,true);
            BufferedWriter bw= new BufferedWriter(writer);
            for (String docName:queryResultsDocsOnly) {
                str.setLength(0);
                str.append(queryID);
                str.append(" ");
                str.append("0");//iter
                str.append(" ");
                str.append(docName);
                str.append(" ");
                str.append("1");//rank
                str.append(" ");
                str.append("1.23");//sim
                str.append(" ");
                str.append("run");
                bw.write(str.toString());
                bw.newLine();
            }
            bw.flush();
            bw.close();
        }catch (IOException e) {
            e.printStackTrace();
        }


    }


}
