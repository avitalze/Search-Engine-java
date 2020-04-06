package Model;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ReadFile {
    //private HashMap<String, Element> allDocs;
    private ArrayList<String> pathsOfFiles;
    private static int corpusFileIndex=0;

    /**
     * CONSTRUCTOR
     */
    public ReadFile() {
        //allDocs = new HashMap();
        pathsOfFiles = new ArrayList();
        corpusFileIndex=0;
    }

    /**
     * clear rpath of files
     */
    public void readFileClear(){
        pathsOfFiles.clear();
    }

    /**
     * gets directory path and gets all the files in the path
     * @param directoryPath
     */
    public void getAllPathsFromDirectory(String directoryPath) {
        File folder = new File(directoryPath);
        File[] allFiles = folder.listFiles();
        File[] var4 = allFiles;
        int numOfFiles = allFiles.length;

        for(int i = 0; i < numOfFiles; ++i) {
            File curr = var4[i];
            if(!curr.getName().equals("05 stop_words.txt")){
                if (curr.isFile()) {
                    this.pathsOfFiles.add(curr.getPath());
                } else {
                    this.getAllPathsFromDirectory(curr.getPath());
                }
            }
        }

    }

    /**
     * load N files from directory
     * @param N
     * @return
     */
    public HashMap<String, Element> loadNFiles(int N) {
        HashMap<String, Element> nDocs= new HashMap<>();
        int count=0;
        while(corpusFileIndex<pathsOfFiles.size() && count<N) {
            String currPath = pathsOfFiles.get(corpusFileIndex);
            try {
                File currFile = new File(currPath);
                Document doc = Jsoup.parse(currFile, "UTF-8");
                Elements allElemforDoc = doc.getElementsByTag("DOC");
                Iterator it = allElemforDoc.iterator();

                while(it.hasNext()) {
                    Element currDoc = (Element)it.next();
                    String docName = currDoc.getElementsByTag("DOCNO").text();
                    nDocs.put(docName, currDoc);
                }
            } catch (Exception e) {
                return null;
            }
            corpusFileIndex++;
            count++;
        }
        return nDocs;
    }

}
