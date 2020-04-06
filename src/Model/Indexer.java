package Model;

import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Indexer {

    private ReadFile readFile;
    public Parser parser;
    private boolean withStemmer;
    public static LinkedHashMap<String,String> dictionary= new LinkedHashMap<>();
    private static int readSize = 10;//!!!!!!!!!!!!
    private int indexOfTempFiles;
    private String folderName;
    private String corpusSavePath;
    private String postingSavePath;
    private int numOfDocs=0;
    private int sumWordsInCorpus=0;
    // for tests
    long startTime;
    long endTime;
    private HashMap<String,String[]> allEntitiesPerDoc;

    /**
     * Constructor
     */
    public Indexer(){
        readFile = new ReadFile();
        parser = new Parser(false);
        withStemmer = false;
        indexOfTempFiles=0;
        corpusSavePath= "";
        postingSavePath="";
        folderName="";
        numOfDocs=0;
        sumWordsInCorpus=0;
        allEntitiesPerDoc= new HashMap<>();
    }
    /**
     * Constructor
     */
    public Indexer(String corpusPath,String postingPath) {
        readFile = new ReadFile();
        parser = new Parser(false);


        withStemmer = false;
        indexOfTempFiles=0;
        //dictionary= new LinkedHashMap<>();
        corpusSavePath= corpusPath;
        postingSavePath=postingPath;
        folderName=postingPath+"\\noStemmer";
        if(! parser.readStopWordsFile(corpusPath+"\\05 stop_words.txt")){
            parser.readStopWordsFile(folderName+"\\05 stop_words.txt");
        }
        numOfDocs=0;
        sumWordsInCorpus=0;
        allEntitiesPerDoc=new HashMap<>();
    }
    /**
     * Constructor
     */
    public Indexer(String corpusPath,String postingPath,boolean userWantsStemmer) {
        readFile = new ReadFile();
        parser = new Parser(userWantsStemmer);
        withStemmer = userWantsStemmer;
        indexOfTempFiles=0;
        //dictionary= new LinkedHashMap<>();
        corpusSavePath= corpusPath;
        postingSavePath=postingPath;
        if(userWantsStemmer){
            folderName=postingPath+"\\withStemmer";
        }
        else {
            folderName=postingPath+"\\noStemmer";
        }
        if(! parser.readStopWordsFile(corpusPath+"\\05 stop_words.txt")){
            parser.readStopWordsFile(folderName+"\\05 stop_words.txt");
        }
        numOfDocs=0;
        sumWordsInCorpus=0;
        allEntitiesPerDoc= new HashMap<>();
    }

    /**
     * This program build the dictionary and return the time it took to build in seconds
     */
    public long runDictionaryBuilder(){
        try {
            startTime = System.nanoTime();
            if(buildInvertedIndex()){
                mergeTempPostingFiles();
                writeEntityFile();
                copyStopWordsFile();
                endTime   = System.nanoTime();
            }
            else{
                return 0;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return TimeUnit.NANOSECONDS.toSeconds(endTime - startTime);
    }

    private void copyStopWordsFile() {
        try{
            File source= new File(corpusSavePath+"\\05 stop_words.txt");
            File dest=new File(folderName+"\\05 stop_words.txt");
            Files.copy(source.toPath(),dest.toPath());
        } catch (FileAlreadyExistsException e){
            System.out.println("File already Exist. no copy needed");
        } catch(IOException e) {

        }


    }


    /**
     * clear dictionary
     */
    public void clearDictionary(){
        this.dictionary.clear();
    }

    /**
     * clear all dictionary
     */
    public void indexerClearAll(){
        parser.parserClear();
        readFile.readFileClear();
        dictionary.clear();
    }

    /**
     *
     * @return the dictionary
     */
    public LinkedHashMap<String,String> getDictionary() {
        return dictionary;
    }



    /**
     * this program prases the whole corpus and builds temporary posting files
     * @throws IOException
     */
    public boolean buildInvertedIndex() throws IOException {
        numOfDocs=0;
        sumWordsInCorpus=0;
        allEntitiesPerDoc.clear();

        readFile.getAllPathsFromDirectory(corpusSavePath);

        File mainFolder= new File(folderName);
        mainFolder.mkdir();

        HashMap<String, Element> docsFromNFiles = readFile.loadNFiles(readSize);
        if( docsFromNFiles== null){
            return false;
        }
        File infoAllDocs=new File (folderName+"\\infoAllDocs.txt");
        if(!infoAllDocs.exists()){
            infoAllDocs.createNewFile();
        }
        else {
            System.out.println("File already exists");
        }
        File folder=new File (folderName+"\\tempFiles");
        folder.mkdir();
        BufferedWriter docWriter=new BufferedWriter(new FileWriter(infoAllDocs));
        while (docsFromNFiles.size() != 0) {
            Iterator iterator = docsFromNFiles.entrySet().iterator();
            HashMap<String, Integer> parseForCurrDoc;
            LinkedHashMap<String, Term> termPerNFiles = new LinkedHashMap<>();
            HashSet<Document> docsDetails = new HashSet<>();
            Document currDoc;
            while (iterator.hasNext()) {// for each doc in docsFromNFiles
                //parse doc and insert to termPerNDoc map
                HashMap.Entry doc = (HashMap.Entry) iterator.next();
                Element docElem = (Element) doc.getValue();
                String docName = (String) doc.getKey();
                currDoc=parser.parseText(docElem.getElementsByTag("TEXT").text(), docName);
                parseForCurrDoc = currDoc.getDocMap();
                String currKey;
                Term currTerm;
                Integer currInt;
                for (HashMap.Entry<String, Integer> word : parseForCurrDoc.entrySet()) {
                    currKey = word.getKey();
                    currInt = word.getValue();
                    boolean addedToTermMap = false;
                    if (Character.isUpperCase(currKey.charAt(0))) {
                        if (termPerNFiles.containsKey(currKey.toLowerCase())) {
                            currTerm = termPerNFiles.get(currKey.toLowerCase());
                            currTerm.AddDocToTermList(docName, currInt);
                            addedToTermMap = true;
                        }
                        else if(termPerNFiles.containsKey(currKey)){
                            currTerm = termPerNFiles.get(currKey);
                            currTerm.AddDocToTermList(docName, currInt);
                            addedToTermMap = true;
                        }
                    }
                    else if (Character.isLowerCase(currKey.charAt(0))) {
                        if (termPerNFiles.containsKey(currKey.toUpperCase())) {
                            Term newTerm = termPerNFiles.get(currKey.toUpperCase());
                            termPerNFiles.remove(currKey.toUpperCase());
                            newTerm.AddDocToTermList(docName, currInt);
                            newTerm.setWord(currKey.toLowerCase());
                            termPerNFiles.put(currKey, newTerm);
                            addedToTermMap = true;
                        }
                        else if(termPerNFiles.containsKey(currKey)){
                            currTerm = termPerNFiles.get(currKey);
                            currTerm.AddDocToTermList(docName, currInt);
                            addedToTermMap = true;
                        }
                    }
                    if(!addedToTermMap){
                        currTerm = new Term(currKey, docName, currInt);
                        termPerNFiles.put(currKey, currTerm);
                    }
                }
                // write to DOC hashMap
                docsDetails.add(currDoc);


            }// while on docs In N files.

            //sort the term Hash Map
            LinkedHashMap<String, Term> sortedTermPerNFiles = new LinkedHashMap<>();
            termPerNFiles.entrySet()
                    .stream()
                    .sorted(new Comparator<Map.Entry<String, Term>>() {
                        @Override
                        public int compare(Map.Entry<String, Term> o1, Map.Entry<String, Term> o2) {
                            return o1.getKey().compareToIgnoreCase(o2.getKey());
                        }
                    })
                    .forEachOrdered(x -> sortedTermPerNFiles.put(x.getKey(), x.getValue()));


            //write to temporary file all terms of N files
            try {
                File termsTempFile=new File (folderName+"\\tempFiles\\tempSortedTermPerNFiles"+indexOfTempFiles+".txt");
                termsTempFile.createNewFile();
                indexOfTempFiles++;
                BufferedWriter wordWriter=new BufferedWriter(new FileWriter(termsTempFile));
                for (HashMap.Entry<String, Term> wordEntry: sortedTermPerNFiles.entrySet()) {
                    Term wordTerm=wordEntry.getValue();
                    wordWriter.write(wordTerm.toString());
                    wordWriter.newLine();
                }
                wordWriter.flush();
                wordWriter.close();

                // write information of all docs of N files
                for (Document doc:docsDetails) {
                    docWriter.write(doc.toString());
                    docWriter.newLine();
                    sumWordsInCorpus+=doc.getNumOfWordsinDoc();
                }
                docWriter.flush();
                numOfDocs+=docsDetails.size();

            } catch (FileNotFoundException ex) {
                System.out.println("Error with specified file");
                ex.printStackTrace();
            } catch (IOException ex) {
                System.out.println("Error with I/O processes");
                ex.printStackTrace();
            }



            //CLEAR DATA STRUCTURES
            sortedTermPerNFiles.clear();
            termPerNFiles.clear();
            docsDetails.clear();

            //write data on dictionary
            File dataFile=new File (folderName+"\\infoDictionary.txt");
            BufferedWriter dataWriter=new BufferedWriter(new FileWriter(dataFile));
            dataWriter.write(numOfDocs+"");
            dataWriter.newLine();
            dataWriter.write(sumWordsInCorpus+"");
            dataWriter.newLine();
            dataWriter.flush();
            dataWriter.close();
            //load the next N files(if exists
            docsFromNFiles = readFile.loadNFiles(readSize);
        }// while there are still files to read



        try{
            docWriter.flush();
            docWriter.close();
        }
        catch(Exception e){
        }


        endTime   = System.nanoTime();
        //System.out.println("corpus was parsed in "+(TimeUnit.NANOSECONDS.toMinutes(endTime - startTime))+" minutes?");
        //System.out.println("Num of Docs in corpus: "+ numOfDocs);
        //

        //change the number of entities in doc file
        updateEntityNumberInDocFile();
        //writeEntityFile();

        return  true;
    }

    /**
     * this program updates the number of entities in each document
     */
    public void updateEntityNumberInDocFile(){
        try{
            //System.out.println("NOW WE ARE UPDATING THE ENTITY FILE");
            String line = null;
            File docInfo = new File(folderName+"\\infoAllDocs.txt");
            File newDocInfo= new File(folderName+"\\infoAllDocsUpdated.txt");
            newDocInfo.createNewFile();
            BufferedReader reader = new BufferedReader(new FileReader(docInfo));
            BufferedWriter out = new BufferedWriter( new FileWriter(newDocInfo));
            String[] splitedLine;
            String oldEntityNum;
            Integer newEntityNum;
            String oldNumOfUniqueTerms;
            Integer newNumOfUniqueTerms;
            Integer toSubtract;
            //System.out.println("Started the while!!!!!!!");
            //System.out.println(Parser.worldSuspectedEntites.size());

            while ((line = reader.readLine()) != null) {
                splitedLine=StringUtils.split(line,",");
                if(Parser.worldSuspectedEntitesDocs.containsKey(splitedLine[0])){
                    //System.out.println("old line number"+i+" : "+line);//test
                    toSubtract= Parser.worldSuspectedEntitesDocs.get(splitedLine[0]);
                    oldEntityNum=splitedLine[3];
                    newEntityNum= Integer.parseInt(oldEntityNum)-toSubtract;
                    oldNumOfUniqueTerms=splitedLine[2];
                    newNumOfUniqueTerms= Integer.parseInt(oldNumOfUniqueTerms)-toSubtract;
                    line=splitedLine[0]+","+splitedLine[1]+","+newNumOfUniqueTerms+","+newEntityNum+","+splitedLine[4];
                    //System.out.println("new line number"+i+" : "+line);//test

                }
                out.write(line);
                out.newLine();
            }
            out.flush();
            out.close();
            reader.close();
            //delete the old file
            docInfo.delete();
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            Parser.worldSuspectedEntitesDocs.clear();

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        endTime   = System.nanoTime();
        //System.out.println("entities updated in doc file. "+(TimeUnit.NANOSECONDS.toMinutes(endTime - startTime))+" minutes?");
    }



    /**
     * this program merges all the temporary posting files to one
     */
    public void mergeTempPostingFiles(){

        try {
            System.gc();
            File folder = new File(folderName+"\\tempFiles");
            LinkedList<File> allFiles= new LinkedList<File>(Arrays.asList(folder.listFiles()));
            File aFile;
            File bFile;
            File mergedFile=null;

            FileReader fileReaderA;
            FileReader fileReaderB;
            BufferedReader bufferedReaderA;
            BufferedReader bufferedReaderB;
            String lineA = "";
            String lineB = "";
            String[] dividedLineA=null;
            String[] dividedLineB=null;
            BufferedWriter mergeWriter;


            while(allFiles.size()>1){//2 or more files to merge
                aFile=allFiles.pollFirst();
                bFile=allFiles.pollFirst();
                mergedFile = new File(folderName+"\\mergeFile"+indexOfTempFiles+".txt");
                mergedFile.createNewFile();
                mergeWriter = new BufferedWriter(new FileWriter(mergedFile));
                fileReaderA= new FileReader(aFile);
                fileReaderB= new FileReader(bFile);
                bufferedReaderA = new BufferedReader(fileReaderA);
                bufferedReaderB = new BufferedReader(fileReaderB);
                lineA = bufferedReaderA.readLine();
                lineB = bufferedReaderB.readLine();
                while (lineA != null || lineB != null) {
                    if(lineA == null){
                        mergeWriter.append(lineB);
                        mergeWriter.newLine();
                        mergeWriter.flush();
                        lineB = bufferedReaderB.readLine();
                    }
                    else if(lineB == null){
                        mergeWriter.append(lineA);
                        mergeWriter.newLine();
                        mergeWriter.flush();
                        lineA = bufferedReaderA.readLine();
                    }
                    else{ // lines a and b not null
                        dividedLineA = StringUtils.split(lineA,"#");
                        dividedLineB = StringUtils.split(lineB,"#");
                        if(Parser.worldSuspectedEntites.containsKey(dividedLineA[0])){
                            lineA = bufferedReaderA.readLine();
                            //newLine=false;
                        }
                        else if(Parser.worldSuspectedEntites.containsKey(dividedLineB[0])){
                            lineB = bufferedReaderB.readLine();
                            //newLine=false;
                        }
                        else if (dividedLineA[0].compareToIgnoreCase(dividedLineB[0]) < 0) { // a before b
                            mergeWriter.write(lineA);
                            mergeWriter.newLine();
                            lineA = bufferedReaderA.readLine();
                        }
                        else if (dividedLineA[0].compareToIgnoreCase(dividedLineB[0]) > 0) {// b before a
                            mergeWriter.write(lineB);
                            mergeWriter.newLine();
                            lineB = bufferedReaderB.readLine();
                        }
                        else { //  a == b
                            int aNumOfDocs;
                            int bNumOfDocs;
                            int aNumOfCorpus;
                            int bNumOfCorpus;
                            String aDocList;
                            String bDocList;
                            Integer sumOfDocs;
                            Integer sumOfCorpus;
                            if (dividedLineA[0].compareTo(dividedLineB[0]) == 0) { // AV = AV or av==av
                                aNumOfDocs = Integer.parseInt(dividedLineA[1]);
                                bNumOfDocs = Integer.parseInt(dividedLineB[1]);
                                aNumOfCorpus=Integer.parseInt(dividedLineA[2]);
                                bNumOfCorpus=Integer.parseInt(dividedLineB[2]);
                                aDocList = dividedLineA[3];
                                bDocList = dividedLineB[3];
                                sumOfDocs = aNumOfDocs + bNumOfDocs;
                                sumOfCorpus=aNumOfCorpus+bNumOfCorpus;
                                mergeWriter.write(dividedLineA[0]);
                                mergeWriter.write("#");
                                mergeWriter.write(sumOfDocs.toString());
                                mergeWriter.write("#");
                                mergeWriter.write(sumOfCorpus.toString());
                                mergeWriter.write("#");
                                mergeWriter.write(aDocList);
                                mergeWriter.write(bDocList);
                                mergeWriter.newLine();
                                lineA = bufferedReaderA.readLine();
                                lineB = bufferedReaderB.readLine();
                            } else if (Character.isLowerCase(dividedLineA[0].charAt(0)) && Character.isUpperCase(dividedLineB[0].charAt(0))) { // avi == AVI
                                aNumOfDocs = Integer.parseInt(dividedLineA[1]);
                                bNumOfDocs = Integer.parseInt(dividedLineB[1]);
                                aNumOfCorpus=Integer.parseInt(dividedLineA[2]);
                                bNumOfCorpus=Integer.parseInt(dividedLineB[2]);
                                aDocList = dividedLineA[3];
                                bDocList = dividedLineB[3];
                                sumOfDocs = aNumOfDocs + bNumOfDocs;
                                sumOfCorpus=aNumOfCorpus+bNumOfCorpus;
                                mergeWriter.write(dividedLineA[0]);
                                mergeWriter.write("#");
                                mergeWriter.write(sumOfDocs.toString());
                                mergeWriter.write("#");
                                mergeWriter.write(sumOfCorpus.toString());
                                mergeWriter.write("#");
                                mergeWriter.write(aDocList);
                                mergeWriter.write(bDocList);
                                mergeWriter.newLine();
                                lineA = bufferedReaderA.readLine();
                                lineB = bufferedReaderB.readLine();
                            } else { // AVI == avi
                                aNumOfDocs = Integer.parseInt(dividedLineA[1]);
                                bNumOfDocs = Integer.parseInt(dividedLineB[1]);
                                aNumOfCorpus=Integer.parseInt(dividedLineA[2]);
                                bNumOfCorpus=Integer.parseInt(dividedLineB[2]);
                                aDocList = dividedLineA[3];
                                bDocList = dividedLineB[3];
                                sumOfDocs = aNumOfDocs + bNumOfDocs;
                                sumOfCorpus=aNumOfCorpus+bNumOfCorpus;
                                mergeWriter.write(dividedLineB[0]);
                                mergeWriter.write("#");
                                mergeWriter.write(sumOfDocs.toString());
                                mergeWriter.write("#");
                                mergeWriter.write(sumOfCorpus.toString());
                                mergeWriter.write("#");
                                mergeWriter.write(aDocList);
                                mergeWriter.write(bDocList);
                                mergeWriter.newLine();
                                lineA = bufferedReaderA.readLine();
                                lineB = bufferedReaderB.readLine();
                            }

                        }// else  a == b

                    }// lines a and b not null

                }// while

                mergeWriter.flush();
                mergeWriter.close();
                allFiles.add(mergedFile);
                bufferedReaderA.close();
                bufferedReaderA=null;
                bufferedReaderB.close();
                bufferedReaderB=null;
                fileReaderA.close();
                fileReaderA=null;
                fileReaderB.close();
                fileReaderB=null;
                aFile.delete();
                bFile.delete();
                indexOfTempFiles++;
            }

            if(allFiles.size()!=0){
                sortBigPostingToABCFiles(allFiles.pollFirst());
            }

            endTime   = System.nanoTime();
            //System.out.println("all indexer runntime is "+(TimeUnit.NANOSECONDS.toMinutes(endTime - startTime))+" minutes?");

        } // try

        catch(IOException e){
            e.printStackTrace();
        }

    }

    /**
     * this program accepts the merged posting file and splits it to posting files by the first letter
     * @param mergedFile
     */
    public void sortBigPostingToABCFiles (File mergedFile){
        try {
            File folder=new File (folderName+"\\postingsFiles");
            folder.mkdir();
            File currFile;
            char currChar=(char)32;
            //char currCharUpper='A';
            FileInputStream fstream = new FileInputStream(mergedFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            BufferedWriter wordWriter;
            //Read File Line By Line
            strLine = br.readLine();
            String[] splited;
            while (strLine != null){
                char firstCharTerm=strLine.charAt(0);
                if(Character.toLowerCase(firstCharTerm) == currChar){
                    currFile=new File(folderName+"\\postingsFiles\\"+Character.toLowerCase(currChar)+".txt");
                    currFile.createNewFile();
                    wordWriter=new BufferedWriter(new FileWriter(currFile));
                    while(strLine!=null && (Character.toLowerCase(firstCharTerm) == currChar) ){
                        splited=StringUtils.split(strLine,"#");
                        String term= splited[0];
                        String numInCorpus=splited[2];
                        dictionary.put(term,numInCorpus);
                        wordWriter.write(strLine);
                        wordWriter.newLine();
                        wordWriter.flush();
                        strLine = br.readLine();
                        if(strLine != null){
                            firstCharTerm=strLine.charAt(0);
                        }
                    }

                }
                    currChar++;

                //currCharUpper++;
            }
            //Close the input stream
            fstream.close();

        }// try

        catch (IOException e) {
            e.printStackTrace(); }


    }

    /**
     * this program writes the top 5 entites of each doc to a file
     */
    private void writeEntityFile(){
        try {
            File entityFile = new File(folderName + "\\EntityPerDoc.txt");
            if(!entityFile.exists()){
                entityFile.createNewFile();
            }
            List<String> currEntitiyList;
            String currEntAndNum;
            int indexOfHashTag;
            BufferedWriter entityWriter = new BufferedWriter(new FileWriter(entityFile));
            String[] top5;
            int sizeToWrite;
            for (HashMap.Entry<String, List<String>> entry : Parser.allEntitiesPerDoc.entrySet()) {
                currEntitiyList=entry.getValue();
                currEntitiyList.removeIf(ent -> dictionary.containsKey(StringUtils.substring(ent, 0, StringUtils.indexOf(ent, "#"))) == false);// if not in dictionary delete
                Collections.sort(currEntitiyList, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        Integer i2=Integer.parseInt(StringUtils.substring(o2,StringUtils.indexOf(o2, "#")+1));
                        Integer i1=Integer.parseInt(StringUtils.substring(o1,StringUtils.indexOf(o1, "#")+1));
                        return i2.compareTo(i1);
                    }
                });
                sizeToWrite=5;
                if(currEntitiyList.size()<5){
                    sizeToWrite=currEntitiyList.size();
                }
                //Write to file
                entityWriter.write(entry.getKey());
                entityWriter.write(".");
                for(int i=0;i<sizeToWrite;i++){
                    currEntAndNum=currEntitiyList.get(i);
                    indexOfHashTag=StringUtils.indexOf(currEntAndNum, "#");
                    entityWriter.write(StringUtils.substring(currEntAndNum, 0, indexOfHashTag));
                    entityWriter.write("#");
                    entityWriter.write(StringUtils.substring(currEntAndNum,indexOfHashTag+1));
                    if(i!=sizeToWrite-1) {
                        entityWriter.write(",");
                    }
                }
                entityWriter.newLine();
                top5=currEntitiyList.toArray(new String[0]);
                allEntitiesPerDoc.put(entry.getKey(),top5);
            }
            entityWriter.flush();
            entityWriter.close();
            Parser.worldSuspectedEntitesDocs.clear();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * returns to sum of words in corpus
     * @return
     */
    public int getSumWordsInCorpus() {
        return sumWordsInCorpus;
    }
    /**
     *
     * @return num of documents
     */
    public int getNumOfDocs() {
        return numOfDocs;
    }


    /**
     * returns thr parser
     * @return
     */
    public Parser getParser() {
        return parser;
    }


    /**
     * This program uploads the dictionary to memory
     * @param folder- merged posting file
     * @return
     */
    public boolean uploadDictionary(File folder){
        clearDictionary();
            if (readMergedFile(folder)){
                if(readDataFile()){
                    if(readEntityFile()){
                        return true;
                    }
                }
            }
        return false;
    }

    /**
     * this function reads the merged file and update the dictionary
     * @param folder
     * @return
     */
    private boolean readMergedFile(File folder){
        try {
            File[] allFiles = folder.listFiles();
            File mergedDoc = null;
            for (int i = 0; i < allFiles.length; i++) {
                if (allFiles[i].getName().startsWith("merge")) {
                    mergedDoc = allFiles[i];
                }
            }
            if(mergedDoc==null){
                return false;
            }
            BufferedReader bufferedReader = new BufferedReader(new FileReader(mergedDoc));
            String line = bufferedReader.readLine();
            String[] splited;
            String term;
            String numInCorpus;
            while (line != null) {
                splited = StringUtils.split(line, "#");
                term = splited[0];
                numInCorpus = splited[2];
                dictionary.put(term, numInCorpus);
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
        }catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * this function read the data file and updates the number of word in coprus and number od docs
     * @return
     */
    public boolean readDataFile(){
        try{
            File dataFile = new File(folderName + "\\infoDictionary.txt");
            if(!dataFile.exists()){
                return false;
            }
            BufferedReader dataReader = new BufferedReader(new FileReader(dataFile));
            String dataLine = dataReader.readLine();
            Integer numOfDocs = Integer.parseInt(dataLine);
            dataLine = dataReader.readLine();
            Integer sumOfWords = Integer.parseInt(dataLine);
            this.numOfDocs=numOfDocs;
            this.sumWordsInCorpus= sumOfWords;
            dataReader.close();
        }catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    /**
     * this function reads the entity file and updates the entity hash map
     * @return
     */
    public boolean readEntityFile(){
        try{
            String line;
            File entityFile= new File(folderName+"\\EntityPerDoc.txt");
            if(!entityFile.exists()){
                return false;
            }
            BufferedReader entityReader = new BufferedReader(new FileReader(entityFile));
            line=entityReader.readLine();
            String[] split;
            String[] top5Entities;
            while (line!=null){
                split=StringUtils.split(line,".");
                if(split.length==2){
                    top5Entities=StringUtils.split(split[1],",");
                    allEntitiesPerDoc.put(split[0],top5Entities);
                }

                line=entityReader.readLine();
            }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * returns the top 5 entity for file name
     * @param docName
     * @return
     */
    public String[] getAllEntitiesPerDoc(String docName) {
        return allEntitiesPerDoc.getOrDefault(docName,null);

    }
}