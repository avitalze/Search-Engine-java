package Model;

import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.tartarus.snowball.ext.englishStemmer;

import java.io.*;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;


public class Parser {
    private HashSet<String> stopWords;
    public static HashMap<String,String> worldSuspectedEntites=new HashMap<>();
    public static HashMap<String,Integer> worldSuspectedEntitesDocs=new HashMap<>();
    public static HashMap<String,List<String>> allEntitiesPerDoc= new HashMap<>();// with suspected entities.
    private static HashSet<String> worldEntites=new HashSet<>();
    private NumberFormat nf;
    private final static HashSet<String> months= new HashSet<>(Arrays.asList("JAN","JANUARY","FEB","FEBRUARY","MAR","MARCH","APR","APRIL","MAY","JUN","JUNE","JUL","JULY","AUG","AUGUST","SEP","SEPTEMBER","OCT","OCTOBER","NOV","NOVEMBER","DEC","DECEMBER"));
    private final static HashSet<String> thousands= new HashSet<>(Arrays.asList("THOUSAND","K"));
    private final static HashSet<String> million= new HashSet<>(Arrays.asList("MILLION", "M"));
    private final static HashSet<String> billion= new HashSet<>(Arrays.asList("BILLION","BN","B"));
    private final static HashSet<String> trillion= new HashSet<>(Arrays.asList("TRILLION","TN","T"));
    private final static HashSet<String> percent= new HashSet<>(Arrays.asList("PERCENT","PERCENTAGE","%"));
    private final static HashSet<String> dollars= new HashSet<>(Arrays.asList("DOLLAR","DOLLARS","$"));
    private final static HashSet<String> kilometers= new HashSet<>(Arrays.asList("KM","KILOMETERS","KILOMETER"));
    private final static HashSet<String> centimeters= new HashSet<>(Arrays.asList("CM","CENTIMETERS","CENTIMETERE"));
    private englishStemmer stem;
    private boolean withStemmer;


    /**
     * constructor
     * @param withStemmer
     */
    public Parser(boolean withStemmer) {
        this.stopWords= new HashSet<>();
        nf = NumberFormat.getInstance(); // get instance
        nf.setMaximumFractionDigits(3); // set decimal places
        nf.setRoundingMode(RoundingMode.FLOOR);
        stem = new englishStemmer();
        this.withStemmer=withStemmer;
        worldSuspectedEntites.clear();
        worldSuspectedEntitesDocs.clear();
        worldEntites.clear();
        allEntitiesPerDoc.clear();
    }

    /**
     * This method obtains a path to the stop words file, reads it, and inserts all words into the class's stopwords data structure.
     * @param stopWordPath
     */
    public boolean readStopWordsFile(String stopWordPath){

        File file = new File(stopWordPath);// the stop word file need to be in the corpus!!!!
        if(!file.exists()){
            return false;
        }
        else{
            try(BufferedReader br= new BufferedReader(new FileReader(file))){
                for(String line ; (line=br.readLine())!=null ;){
                    stopWords.add(line.trim());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;

    }

    /**
     * This method resets the class data structures that store document information in the repository. We use this class when a reset button is pressed in the interface.
     */
    public void parserClear(){
        worldSuspectedEntites.clear();
        worldSuspectedEntitesDocs.clear();
        worldEntites.clear();
        allEntitiesPerDoc.clear();
    }


    /**
     * This method takes the document name and text as a string, and returns a Document object that holds the details about this document,
     * including all the terms that we have interpreted in this document according to the rules defined at work and the rules we added.
     * @param str
     * @param docName
     * @return
     */
    public Document parseText(String str, String docName) {
        boolean endWithDot=false;
        int entityCounter=0;
        List<String> termsPerCurr=new LinkedList<>();
        HashSet<String> localSuspectedEntites= new HashSet<>();
        String curr="";
        Integer termCount=0;
        String next1="";
        HashMap<String,Integer> docMap=new HashMap<>();
        HashSet<String> allEntities= new HashSet<>();
        // dell all tabs: ? ! " "

        str=StringUtils.replace(str, "[", " ");
        str=StringUtils.replace(str, "]", " ");
        str=StringUtils.replace(str, "(", " ");
        str=StringUtils.replace(str, ")", " ");
        str=StringUtils.replace(str, "{", " ");
        str=StringUtils.replace(str, "}", " ");
        str=StringUtils.replace(str, "?", " ");
        str=StringUtils.replace(str, "!", " ");
        str=StringUtils.replace(str, "\\", " "); // to check if work !!
        str=StringUtils.replace(str, "\"", " ");
        str=StringUtils.replace(str, "|", " ");
        str=StringUtils.replace(str, ":", " ");
        str=StringUtils.replace(str, ";", " ");
        str=StringUtils.replace(str, "*", " ");
        str=StringUtils.replace(str, "^", " ");
        str=StringUtils.replace(str, "#", " ");
        str=StringUtils.replace(str, "@", " ");
        str=StringUtils.replace(str, "=", " ");
        str=StringUtils.replace(str, "+", " ");
        str=StringUtils.replace(str, "~", " ");
        str=StringUtils.replace(str, "_", " ");
        str=StringUtils.replace(str, "<", " ");
        str=StringUtils.replace(str, ">", " ");
        str=StringUtils.replace(str, "?", " ");
        str=StringUtils.replace(str, "--", " ");
        str=StringUtils.replace(str, "?", " ");
        str=StringUtils.replace(str, "°", " ");
        str=StringUtils.replace(str, "'", "");
        str=StringUtils.replace(str, "`", "");
        str=StringUtils.replace(str, "�", "");

        // trim the first string if it is [TEXT]
        String[] splitedStr= StringUtils.split(str);
        ArrayList<String> text= new ArrayList<>();
        text.addAll(Arrays.asList(splitedStr));

        for (int textIndex = 0; textIndex < text.size(); textIndex++) {
            endWithDot=false;
            termsPerCurr.clear();
            curr = text.get(textIndex);
            if (StringUtils.endsWith(curr,".") ||StringUtils.endsWith(curr,","))  {// if the word ends with . or ,
                curr = StringUtils.stripEnd(curr, "., ");
                endWithDot=true;
            }
            curr=StringUtils.strip(curr,"., ");
            if (!stopWords.contains(StringUtils.lowerCase(curr))) {//!!!!
                if (curr.matches("^.*\\d+.*$")) {// the string contains a digit(maybe also characters)

                    if (curr.matches("^[0-9\\,\\.\\/]+$") && (curr.length()-StringUtils.replace(curr,".","").length())<=1 && (curr.length()-StringUtils.replace(curr,"/", "").length())<=1) {// the string doesnt contain char, only digits ,no signs)
                        if (textIndex != text.size() - 1) {
                            next1 = text.get(textIndex + 1);
                            if (StringUtils.endsWith(next1,".") || StringUtils.endsWith(next1,",")) {
                                next1 = StringUtils.substring(next1,0, next1.length() - 1);
                            }
                            if (thousands.contains(StringUtils.upperCase(next1))) {
                                if (checkIfRepresentDollar(textIndex,text)) {
                                    termsPerCurr=digitWithKDollar(curr);
                                } else {
                                    textIndex++;
                                    textIndex++;
                                    termsPerCurr=digitWithThousand(curr);
                                }
                            } else if (million.contains(StringUtils.upperCase(next1))) {
                                if (checkIfRepresentDollar(textIndex,text)) {
                                    termsPerCurr=digitWithMDollar(curr);
                                } else {
                                    textIndex++;
                                    termsPerCurr=digitWithMillion(curr);
                                }
                            } else if (billion.contains(StringUtils.upperCase(next1))) {
                                if (checkIfRepresentDollar(textIndex,text)) {
                                    termsPerCurr=digitWithBDollar(curr);
                                } else {
                                    textIndex++;
                                    termsPerCurr=digitWithBillion(curr);
                                }
                            } else if (dollars.contains(StringUtils.upperCase(next1))) { //!!!!!!!!!!!!!
                                textIndex++;
                                termsPerCurr=digitWithDollar(curr);
                            } else if (percent.contains(StringUtils.upperCase(next1))) {
                                textIndex++;
                                termsPerCurr=digitWithPercent(curr);
                            } else if (next1.matches("^[0-9]+[\\/][0-9]+$")  && curr.matches("^[0-9\\,]+$")&& !StringUtils.contains(next1,".")) {// number with fraction
                                if (checkIfRepresentDollar(textIndex, text)) {
                                    textIndex = textIndex + 2;
                                    termsPerCurr = fractionWithDollar(curr, next1);/// it is different fromm the other dollars because we need to include the curr and next

                                } else {
                                    textIndex++;
                                    termsPerCurr = numberAndFraction(curr, next1);
                                }
                            } else if (months.contains(StringUtils.upperCase(next1)) && !curr.matches("^[0-9]+[\\/][0-9]+$")) {
                                if (curr.length() <= 2 && !StringUtils.contains(curr,",") && !StringUtils.contains(curr,".")) {
                                    Integer a = Integer.parseInt(curr);
                                    if (curr.length() <= 2 && a < 32 && a > 0 ) {
                                        textIndex++;
                                        termsPerCurr=dateDDMM(curr, next1);
                                    } else {
                                        termsPerCurr=checkNumberValue(curr);
                                    }
                                } else {
                                    termsPerCurr=checkNumberValue(curr);
                                }
                            }
                            else if (kilometers.contains(StringUtils.upperCase(next1))){
                                textIndex++;
                                termsPerCurr= digitWithKilometers(curr);
                            }
                            else if(centimeters.contains(StringUtils.upperCase(next1))){
                                textIndex++;
                                termsPerCurr= digitWithCentimeters(curr);
                            }
                            // the word trillion will not apear much in the text
                            else if (trillion.contains(StringUtils.upperCase(next1))) {
                                if (checkIfRepresentDollar(textIndex,text)) {
                                    termsPerCurr=digitWithTDollar(curr);
                                } else {
                                    textIndex++;
                                    termsPerCurr=digitWithTrillion(curr);
                                }
                            } else { // number - no units- only digits
                                if(!(StringUtils.contains(curr,".")&& StringUtils.contains(curr,"/"))){
                                    termsPerCurr=checkNumberValue(curr);
                                }
                                else{
                                    termsPerCurr.add(curr);
                                }
                            }
                        } else {// if the last word in the text is a number
                            if(!(StringUtils.contains(curr,".")&& StringUtils.contains(curr,"/"))){
                                termsPerCurr=checkNumberValue(curr);
                            }
                            else{
                                termsPerCurr.add(curr);
                            }
                        }

                    }
                    //NUMBERS WITH SIGN
                    else if (curr.matches("^[0-9.,]+(k|K)$") && (curr.length()-StringUtils.replace(curr,".", "").length())<=1) {
                        if (textIndex != text.size() - 1) {
                            next1 = text.get(textIndex + 1);
                            if (StringUtils.endsWith(next1,".") || StringUtils.endsWith(next1,",")) {
                                next1 = StringUtils.substring(next1,0, next1.length() - 1);
                            }
                            if (dollars.contains(StringUtils.upperCase(next1))) {
                                textIndex++;
                                termsPerCurr=digitWithKNoSpaceDollars(curr);
                            } else {
                                termsPerCurr=digitWithKNoSpace(curr);
                            }
                        } else {
                            termsPerCurr=digitWithKNoSpace(curr);
                        }
                    } else if (curr.matches("^[0-9.,]+(m|M)$")&& (curr.length()-StringUtils.replace(curr,".", "").length())<=1) {
                        if (textIndex != text.size() - 1) {
                            next1 = text.get(textIndex + 1);
                            if (StringUtils.endsWith(next1,".") || StringUtils.endsWith(next1,",")) {
                                next1 = StringUtils.substring(next1,0, next1.length() - 1);
                            }
                            if (dollars.contains(StringUtils.upperCase(next1))) {
                                textIndex++;
                                termsPerCurr=digitWithMNoSpaceDollars(curr);
                            } else {
                                termsPerCurr=digitWithMNoSpace(curr);
                            }
                        } else {
                            termsPerCurr=digitWithMNoSpace(curr);
                        }
                    } else if (curr.matches("^[0-9.,]+(bn|BN|Bn)$") && (curr.length()-StringUtils.replace(curr,".", "").length())<=1) {
                        if (textIndex != text.size() - 1) {
                            next1 = text.get(textIndex + 1);
                            if (StringUtils.endsWith(next1,".") || StringUtils.endsWith(next1,",")) {
                                next1 = StringUtils.substring(next1,0, next1.length() - 1);
                            }
                            if (dollars.contains(StringUtils.upperCase(next1))) {
                                textIndex++;
                                termsPerCurr=digitWithBNoSpaceDollars(curr);
                            } else {
                                termsPerCurr=digitWithBNoSpace(curr);
                            }
                        } else {
                            termsPerCurr=digitWithBNoSpace(curr);
                        }
                    } else if (curr.matches("^[0-9.,]+(tr|TR)$") && (curr.length()-StringUtils.replace(curr,".", "").length())<=1) {
                        if (textIndex != text.size() - 1) {
                            next1 = text.get(textIndex + 1);
                            if (StringUtils.endsWith(next1,".") || StringUtils.endsWith(next1,",")) {
                                next1 = StringUtils.substring(next1,0, next1.length() - 1);
                            }
                            if (dollars.contains(StringUtils.upperCase(next1))) {
                                textIndex++;
                                termsPerCurr=digitWithTRNoSpaceDollars(curr);
                            } else {
                                termsPerCurr=digitWithTRNoSpace(curr);
                            }
                        } else {
                            termsPerCurr=digitWithTRNoSpace(curr);
                        }
                    }
                    else if (StringUtils.contains(curr,"-")) {
                        int indexofchar = curr.indexOf("-");

                        if(curr.matches("^\\-{1}[0-9\\.\\,]+$")){
                            termsPerCurr=checkNumberValue(StringUtils.substring(curr,1));
                            String minusNumber="-"+((LinkedList<String>) termsPerCurr).poll();
                            termsPerCurr.add(minusNumber);
                        }
                        else{
                            String left = StringUtils.substring(curr,0, indexofchar);
                            String right = StringUtils.substring(curr,indexofchar + 1, curr.length());
                            if (left.matches("^[0-9.,]+$") &&(left.length()-StringUtils.replace(left,".", "").length())<=1 && right.matches("^[0-9.,]+$") && (right.length()-StringUtils.replace(right,".", "").length())<=1) {
                                termsPerCurr=rangeOfNumbers(curr, left, right);
                            } else if (left.matches("^[a-zA-Z]+$") && right.matches("^[0-9.,]+$") && (right.length()-StringUtils.replace(right,".", "").length())<=1) {
                                termsPerCurr=rangeWordAndNumber(curr, left, right);
                            } else if (left.matches("^[0-9.,]+$") && right.matches("^[a-zA-Z]+$") && (left.length()-StringUtils.replace(left,".", "").length())<=1) {
                                if (thousands.contains(StringUtils.upperCase(right))) {
                                    curr = left;
                                    termsPerCurr=digitWithThousand(curr);
                                } else if (million.contains(StringUtils.upperCase(right))) {
                                    curr = left;
                                    termsPerCurr=digitWithMillion(curr); //????????????????
                                } else if (billion.contains(StringUtils.upperCase(right))) {
                                    curr = left;
                                    termsPerCurr=digitWithBillion(curr);
                                } else if (percent.contains(StringUtils.upperCase(right))) {
                                    curr = left;
                                    termsPerCurr=digitWithPercent(curr);
                                } else if (dollars.contains(StringUtils.upperCase(right))) {
                                    curr = left;
                                    termsPerCurr=digitWithDollar(curr);
                                }
                                else if(kilometers.contains(StringUtils.upperCase(right))){
                                    curr=left;
                                    termsPerCurr=digitWithKilometers(curr);
                                }
                                else if(centimeters.contains(StringUtils.upperCase(right))){
                                    curr=left;
                                    termsPerCurr=digitWithCentimeters(curr);
                                }
                                else {
                                    termsPerCurr=rangeNumberAndWord(curr, left, right);
                                }
                            } else {
                                if (StringUtils.contains(left,"$") && StringUtils.contains(right,"$")) {
                                    termsPerCurr=rangeWithDollarSign(curr, left, right);
                                } else if (StringUtils.contains(left,"%") && StringUtils.contains(right,"%")) {
                                    termsPerCurr=rangeWithPercentSign(curr, left, right);
                                } else {
                                    termsPerCurr=rangeSavedAsOneTerm(curr, left, right);
                                }
                            }
                        }
                    }

                    else if ((curr.length()-StringUtils.replace(curr,".", "").length())<=1 &&(curr.matches("^(\\%)[0-9.,]+$") || curr.matches("^[0-9.,]+(\\%)$"))) {
                        termsPerCurr=digitWithPercentNoSpace(curr);
                    }
                    else if ((curr.length()-StringUtils.replace(curr,".", "").length())<=1 &&(curr.matches("^(\\$)[0-9.,]+$") || curr.matches("^[0-9.,]+(\\$)$")) ) {
                        if (textIndex != text.size() - 1) {
                            next1 = text.get(textIndex + 1);
                            if (StringUtils.endsWith(next1,".") || StringUtils.endsWith(next1,",")) {
                                next1 = StringUtils.substring(next1,0, next1.length() - 1);
                            }
                            if (million.contains(StringUtils.upperCase(next1))) {
                                textIndex++;
                                termsPerCurr=digitWith$Million(curr);
                            } else if (billion.contains(StringUtils.upperCase(next1))) {
                                textIndex++;
                                termsPerCurr=digitWith$Billion(curr);
                            }
                            else if(StringUtils.contains(curr,"m")||StringUtils.contains(curr,"M")){
                                termsPerCurr=digitWith$Million(StringUtils.substring(curr,0,curr.length()-1));
                            }
                            else if(StringUtils.contains(curr,"B")|| StringUtils.contains(curr,"b")){
                                termsPerCurr=digitWith$Billion(StringUtils.substring(curr,1,curr.length()-1));
                            }
                            else {
                                termsPerCurr=digitWith$NoSpace(curr);
                            }
                        } else {
                            termsPerCurr=digitWith$NoSpace(curr);
                        }
                    }  else {/// else for checking
                        termsPerCurr.add(curr);
                    }
                }
                // no digits! with all kinds of signs (?!#,.'") but not numbers
                else if(curr.matches("^[a-zA-Z]+$")){// words only
                    if(months.contains(StringUtils.upperCase(curr))){
                        if (textIndex != text.size() - 1) {
                            next1 = text.get(textIndex + 1);
                            if (StringUtils.endsWith(next1,".") || StringUtils.endsWith(next1,",")) {
                                next1 = StringUtils.substring(next1,0, next1.length() - 1);
                            }
                            if(next1.matches("^[0-9]+$")){
                                Integer a = Integer.parseInt(next1);
                                if (next1.length() <= 2 && a < 32 && a > 0 ) {
                                    termsPerCurr=dateMMDD(curr,next1);
                                    textIndex++;
                                }
                                else if(next1.length()==4){
                                    termsPerCurr=dateMMYYYY(curr,next1);
                                    textIndex++;
                                }
                            }
                            else{
                                termsPerCurr.add(curr);
                            }
                        }
                    }
                    else if(Character.isUpperCase(curr.charAt(0)) ){//maybe entity
                        StringBuilder entity= new StringBuilder(curr);
                        //StringBuilder entity = new StringBuilder(curr);
                        int i=textIndex+1;
                        String next="";
                        boolean keepSearching=true;
                        while (keepSearching && !endWithDot && i<text.size() && text.get(i).matches("^[a-zA-Z.,\\-]+$")&& Character.isUpperCase(text.get(i).charAt(0)) ){
                            next=text.get(i);
                            if(months.contains(StringUtils.upperCase(next)) && i+1<text.size() && text.get(i+1).matches("^[0-9,.]+$")){
                                break;
                            }
                            if(StringUtils.endsWith(next,".") || StringUtils.endsWith(next,",")){
                                next=StringUtils.substring(next,0,next.length()-1);
                                keepSearching=false;
                            }
                            entity.append(" ");
                            entity.append(next);
                            //entity.append(" "+next);
                            i++;
                        }
                        textIndex=i-1;
                        int currCount=0;
                        if(curr.equals(entity.toString())){// reqular word with capital letter
                            termsPerCurr.add(curr);
                        }
                        else{// entity!
                            if(!foundInWorld(StringUtils.upperCase(entity.toString()))){
                                if(!localSuspectedEntites.contains(StringUtils.upperCase(entity.toString()))){
                                    localSuspectedEntites.add(StringUtils.upperCase(entity.toString()));
                                }
                            }
                            // in both ways we need to add to term list
                            if(entity.toString().toLowerCase().equals("the ingaas")){
                                System.out.println("    ");
                            }
                            termsPerCurr= entityParse(entity.toString());
                            entityCounter++;
                            allEntities.add(StringUtils.strip(entity.toString(),".,- "));
                        }

                    }

                    else{//just a  word
                        termsPerCurr.add(curr);
                    }
                }
                else if(curr.matches("^[a-zA-Z\\-]+$") && !curr.matches("^[\\-]+$")){//word-word-wrod....

                    String[] splitedCurr= StringUtils.split(curr,"-");
                    if(checkIfRepresentRangeEntity(splitedCurr)){
                        if(!foundInWorld(StringUtils.upperCase(curr))){
                            if(!localSuspectedEntites.contains(StringUtils.upperCase(curr))){
                                localSuspectedEntites.add(StringUtils.upperCase(curr));

                            }
                        }

                        // in both ways we need to add to term list
                        termsPerCurr= entityParse(curr);
                        entityCounter++;
                        allEntities.add(StringUtils.strip(curr,".,- "));
                    }
                    else{
                        String term;
                        for (int i=0;i<splitedCurr.length;i++) {
                            term = splitedCurr[i];
                            if (term.length()>0 &&!stopWords.contains(StringUtils.lowerCase(term))) {
                                termsPerCurr.add(term);
                            }
                        }
                        termsPerCurr.add((curr));
                    }


                }
            }else if(curr.equals("between") && checkIfRepresentBetween(textIndex,text)){
                next1=text.get(textIndex + 1);
                String next3= text.get(textIndex + 3);
                if(StringUtils.endsWith(next3,".") || StringUtils.endsWith(next3,",")){
                    next3=StringUtils.substring(next3,0,next3.length()-1);
                }
                if(StringUtils.contains(next1,"$") && StringUtils.contains(next3,"$")){
                    termsPerCurr=betweenDollars(curr,next1,next3);
                }
                else if(StringUtils.contains(next1,"%") && StringUtils.contains(next3,"%")){
                    termsPerCurr=rangeWithPercentSign(next1+"-"+next3,next1,next3);
                }
                else{
                    termsPerCurr= betweenNumberAndNumber(curr,text.get(textIndex + 1),text.get(textIndex + 3));
                }
                textIndex=textIndex+3;
            }
            else if(months.contains(StringUtils.upperCase(curr))) {//exp: MAY 18
                if (textIndex != text.size() - 1) {
                    next1 = text.get(textIndex + 1);
                    if (StringUtils.endsWith(next1,".") || StringUtils.endsWith(next1,",")) {
                        next1 = StringUtils.substring(next1,0, next1.length() - 1);
                    }
                    if (next1.matches("^[0-9]+$")) {
                        Integer a = Integer.parseInt(next1);
                        if (next1.length() <= 2 && a < 32 && a > 0) {
                            termsPerCurr = dateMMDD(curr, next1);
                            textIndex++;
                        } else if (next1.length() == 4) {
                            termsPerCurr = dateMMYYYY(curr, next1);
                            textIndex++;
                        }
                    } else {
                        termsPerCurr.add(curr);
                    }
                }
            }
            //check if stop word is a part of entity
            else if(Character.isUpperCase(curr.charAt(0)) && stopWords.contains(StringUtils.lowerCase(curr))){
                //CHECK IF WORD IS STOP WORD!! IF IT STARTES WITH CAPITAL LETTER CHECK THE NEXT- it is etitentity!!! if not, next. exp: THE WHO. but only add the whole term and not each stop word sepertly
                StringBuilder entity= new StringBuilder(curr);
                int i=textIndex+1;
                String next="";
                boolean keepSearching=true;
                while (keepSearching && !endWithDot && i<text.size() && text.get(i).matches("^[a-zA-Z.,\\-]+$")&& Character.isUpperCase(text.get(i).charAt(0)) ){
                    next=text.get(i);
                    if(next.equals("Ingaas") || next.equals("Ingaas.")||next.equals("Ingaas,")){
                        System.out.println("***********************");
                    }
                    if(months.contains(StringUtils.upperCase(next)) && i+1<text.size() && text.get(i+1).matches("^[0-9,.]+$")){
                        break;
                    }
                    if(StringUtils.endsWith(next,".") || StringUtils.endsWith(next,",")){
                        next=StringUtils.substring(next,0,next.length()-1);
                        keepSearching=false;
                    }
                    entity.append(" ");
                    entity.append(next);
                    i++;
                }
                textIndex=i-1;
                int currCount=0;
                if(!curr.equals(entity.toString())) {//stop word is part of entity
                    if (!foundInWorld(StringUtils.upperCase(entity.toString()))) {
                        if (!localSuspectedEntites.contains(StringUtils.upperCase(entity.toString()))) {
                            localSuspectedEntites.add(StringUtils.upperCase(entity.toString()));

                        }
                    }
                    // in both ways we need to add to term list
                    termsPerCurr=entityParse(entity.toString());
                    entityCounter++;
                    allEntities.add(StringUtils.strip(entity.toString(),".,- "));
                }
            }//end of if- stop word

            //ADD TERMS TO DOCMAP

            int value=0;
            String term;
            boolean isUpper=false;
            for (int i=0; i<termsPerCurr.size(); i++) {
                isUpper=false;
                term=termsPerCurr.get(i);
                if(term.matches("^\\-{1}[0-9\\.\\,]+$")){
                    term=StringUtils.strip(term,"., ");
                }
                else{
                    term=StringUtils.strip(term,".,- ");
                }
                isUpper=Character.isUpperCase(term.charAt(0));
                if(withStemmer && !allEntities.contains(term)){
                    stem.setCurrent(term.toLowerCase());
                    if(stem.stem()){
                        term=stem.getCurrent();
                    }
                    if(isUpper){
                        term=term.toUpperCase();
                    }
                }
                if(stopWords.contains(term.toLowerCase())){
                    term="";
                }
                if(term.length() >0) {
                    boolean inDocMap = false;
                    if (Character.isUpperCase(term.charAt(0))) {
                        if (docMap.containsKey(StringUtils.lowerCase(term))) {
                            value = docMap.get(StringUtils.lowerCase(term));
                            docMap.replace(StringUtils.lowerCase(term), value + 1);
                            inDocMap = true;
                        }
                        else if(docMap.containsKey(StringUtils.upperCase(term))){
                            termCount = docMap.get(StringUtils.upperCase(term));
                            docMap.replace(StringUtils.upperCase(term), termCount + 1);
                            inDocMap = true;
                        }
                    }
                    else if (Character.isLowerCase(term.charAt(0))) {
                        String search = StringUtils.upperCase(term);
                        if (docMap.containsKey(search)) {
                            value = docMap.get(search);
                            docMap.remove(search);
                            docMap.put(StringUtils.lowerCase(term), value + 1);
                            inDocMap = true;
                        }
                        else if(docMap.containsKey(StringUtils.lowerCase(term))){
                            termCount = docMap.get(StringUtils.lowerCase(term));
                            docMap.replace(StringUtils.lowerCase(term), termCount + 1);
                            inDocMap = true;
                        }
                    }
                    if(!inDocMap){
                        if (Character.isUpperCase(term.charAt(0))) {
                            docMap.put(StringUtils.upperCase(term),1);
                        }
                        else{
                            docMap.put(StringUtils.lowerCase(term),1);
                        }
                    }
                }
            }
        }//END OF BIG FOR- ON ALL THE TEXT

        //update the world suspected ith the local
        for (String entity: localSuspectedEntites) {
            worldSuspectedEntites.put(entity,docName);
        }
        worldSuspectedEntitesDocs.put(docName,localSuspectedEntites.size());


        StringBuilder entityANDnum= new StringBuilder();
        Integer numInDoc;
        List<String> entityList= new ArrayList<>();
        for (String entity: allEntities) {
            entityANDnum.setLength(0);
            entity=StringUtils.upperCase(entity);
            entity=StringUtils.strip(entity,".,- ");
            numInDoc=docMap.get(entity);
            if(numInDoc==null){
                entity=StringUtils.lowerCase(entity);
                numInDoc=docMap.get(entity);
            }
            if(numInDoc==null){
                System.out.println("***********************"+entity);
                System.out.println();
            }
            entityANDnum.append(entity);//upper case
            entityANDnum.append("#");
            entityANDnum.append(numInDoc.toString());
            entityList.add(entityANDnum.toString());
        }

        allEntitiesPerDoc.put(docName,entityList);

        /*ADD ENTITIES
        StringBuilder entityANDnum= new StringBuilder();
        Integer numInDoc;
        String entity;
        PriorityQueue<Pair<String,Integer>> entitiyDominant= new PriorityQueue<>(new Comparator<Pair<String, Integer>>() {
            @Override
            public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        List<String> top5; top5= new LinkedList<>();
        for(int i=0; i<allEntities.size();i++) {
            entityANDnum.setLength(0);
            entity = StringUtils.upperCase(allEntities.get(i));
            entity = StringUtils.strip(entity, ".,- ");
            numInDoc = docMap.get(entity);// num of appearncs

            if (numInDoc == null) {
                entity = StringUtils.lowerCase(entity);
                numInDoc = docMap.get(entity);
            }
            if (numInDoc == null) {
                System.out.printf("***********************" + entity);
            }
            entitiyDominant.add(new Pair<>(entity, numInDoc));
        }
        int numOfEntity=allEntities.size();
        if( numOfEntity>5) {
            numOfEntity=5;
        }
        for (int k = 0; k < numOfEntity; k++) {
            Pair<String,Integer> currEntity=entitiyDominant.poll();
            top5.add(currEntity.getKey()+"#"+currEntity.getValue());
        }
        allEntitiesPerDoc.put(docName,top5);
        //allEntitiesPerDoc.put(docName,allEntities);
        */
        //BUILD NEW DOCUMENT TO RETURN
        int numOfMaxTerm = 0;
        if(docMap.size()!=0){
            numOfMaxTerm = Collections.max(docMap.values());
        }
        Document docAns= new Document(docName,numOfMaxTerm,docMap.size(),docMap,entityCounter,text.size());


        return docAns;
    }

    /**
     * Gets an entity returns true if it appears in the entity's list of entities.
     * @param entity
     * @return
     */
    private boolean foundInWorld(String entity){
        if(worldEntites.contains(StringUtils.upperCase(entity))){
            return true;
        }
        else if(worldSuspectedEntites.containsKey(StringUtils.upperCase(entity))){
            String docname= worldSuspectedEntites.get(StringUtils.upperCase(entity));
            worldSuspectedEntites.remove(StringUtils.upperCase(entity));
            Integer docNum= worldSuspectedEntitesDocs.get(docname);
            if(docNum-1==0){
                worldSuspectedEntitesDocs.remove(docname);
            }
            else{
                worldSuspectedEntitesDocs.replace(docname,docNum-1);
            }
            worldEntites.add(StringUtils.upperCase(entity));
            return true;
        }
        return false;
    }

    /**
     * This method takes an array of string and returns whether it represents an entity, meaning each word begins with a capital letter.
     * @param str
     * @return
     */
    private boolean checkIfRepresentRangeEntity(String[] str){
        String word;
        if(str.length<=1){
            return false;
        }
        for (int i=0;i<str.length;i++) {
            word=str[i];
            if(word.length()==0){
                return false;
            }
            if(! Character.isUpperCase(word.charAt(0))){
                return false;
            }
        }
        return true;
    }

    /**
     *This method accepts an entity and returns all options that it will store in the dictionary as a single term and each word (if it is not stop word) will also be saved separately.
     * @param entity
     * @return
     */
    private LinkedList<String> entityParse(String entity){
        LinkedList<String>ans = new LinkedList<>();
        ans.add(entity);
        String[] splitedStr= entity.split("[\\-\\s]");//??
        String term;
        for (int i=0; i<splitedStr.length ; i++) {
            term = splitedStr[i];
            if(term.length()>0 &&!stopWords.contains(StringUtils.lowerCase(term))){
                ans.add(term);
            }
        }
        return ans;
    }

    // check if the next strings include dollar

    /**
     * This method receives a word and returns whether it represents a price (in dollars).
     * @param textIndex
     * @param text
     * @return
     */
    private boolean checkIfRepresentDollar(int textIndex,ArrayList<String> text) {
        String next2="";
        String next3="";
        if (textIndex != text.size() - 2) {
            next2 = text.get(textIndex + 2);
            if(next2.equals("U.S.")){
                if (textIndex != text.size() - 3) {
                    next3 = text.get(textIndex + 3);
                    if(StringUtils.endsWith(next3,".") || StringUtils.endsWith(next3,",")){
                        next3=StringUtils.substring(next3,0, next3.length()-1);
                    }
                    if (dollars.contains(StringUtils.upperCase(next3))) {
                        textIndex = textIndex + 3;
                        return true;
                    }
                }
            }
            else{
                if(StringUtils.endsWith(next2,".") || StringUtils.endsWith(next2,",")){
                    next2=StringUtils.substring(next2,0, next2.length()-1);
                }
                if (dollars.contains(StringUtils.upperCase(next2))) {
                    textIndex = textIndex + 2;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This method receives a word and returns whether it represents a range (with the word between).
     * @param textIndex
     * @param text
     * @return
     */
    private boolean checkIfRepresentBetween(int textIndex,ArrayList<String> text){
        String next1="";
        String next2="";
        String next3="";
        if (textIndex != text.size() - 1) {
            next1 = text.get(textIndex + 1);
            if (next1.matches("^[0-9.,$%mbkMBK]+$") && !StringUtils.endsWith(next1,".") && !StringUtils.endsWith(next1,",")) {
                if (textIndex != text.size() - 2) {
                    next2 = text.get(textIndex + 2);
                    if (next2.equals("and")) {
                        if (textIndex != text.size() - 3) {
                            next3 = text.get(textIndex + 3);
                            if (next3.matches("^[0-9.,$%mbkMBK]+$")) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * This method takes a word in miles format and returns how we keep it in the dictionary.
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithKilometers(String curr){
        LinkedList<String>ans = new LinkedList<>();
        if(curr.matches("^[0-9]+[\\/][0-9]+$")){
            curr=fractionToDecimal(curr);
        }
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        curr=parseTheNumber(curr);
        ans.add(curr+"KM");
        return ans;

    }

    /**
     * This method takes a word in centimeters of distance and returns how we keep it in the dictionary.
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithCentimeters(String curr){
        LinkedList<String>ans = new LinkedList<>();
        if(curr.matches("^[0-9]+[\\/][0-9]+$")){
            curr=fractionToDecimal(curr);
        }
        curr=StringUtils.replace(curr, ",", "");
        curr=parseTheNumber(curr);
        ans.add(curr+"CM");
        return ans;
    }

    /**
     * This method takes an integer word and fragment word and returns how we keep it in the dictionary.
     * @param curr
     * @param next1
     * @return
     */
    private LinkedList<String> numberAndFraction(String curr, String next1) { // eg: 76 3/2
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        next1=StringUtils.replace(next1, ",", "");// ? ! " "
        String[] rat = StringUtils.split(next1,"/");
        Double fractionVal= Double.parseDouble(rat[0]) / Double.parseDouble(rat[1]);
        Double wholeValue=Double.parseDouble(curr)+ fractionVal;
        String toAns=parseTheNumber(wholeValue+"");
        ans.add(toAns);
        return ans;
    }

    /**
     * This method takes a word in the number of thousands and returns how we keep it in the dictionary.
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithThousand(String curr) { // 10 thousand/k or 10.22 K
        LinkedList<String> ans=new LinkedList<>();
        String termVal="";
        if(curr.matches("^[0-9]+[\\/][0-9]+$")){
            curr=fractionToDecimal(curr);
        }
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        curr=parseTheNumber(curr);
        ans.add(curr+"K");
        return ans;
    }

    /**
     * This method takes a word in a number format in millions and returns how we keep it in the dictionary.
     * @param curr
     * @return
     */
    private  LinkedList<String> digitWithMillion(String curr) {  // 10 million/m or 10.22m
        LinkedList<String> ans=new LinkedList<>();
        String termVal="";
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        if(curr.matches("^[0-9]+[\\/][0-9]+$")){
            curr=fractionToDecimal(curr);
        }

        curr=parseTheNumber(curr);
        ans.add(curr+"M");
        return ans;

    }

    /**
     * This method takes a word in the number of billions and returns how we keep it in the dictionary.
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithBillion(String curr) { //10 billion/bn or 10.22B
        LinkedList<String> ans=new LinkedList<>();
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        if(curr.matches("^[0-9]+[\\/][0-9]+$")){
            curr=fractionToDecimal(curr);
        }
        String termVal="";

        curr=parseTheNumber(curr);
        ans.add(curr+"B");
        return ans;
    }

    /**
     *This method takes a word in the number of trillions and returns how we keep it in the dictionary.
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithTrillion(String curr) { //10 trillion
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        if(curr.matches("^[0-9]+[\\/][0-9]+$")){
            curr=fractionToDecimal(curr);
        }

        LinkedList<String>ans = new LinkedList<>();
        curr=parseTheNumber(curr);
        ans.add(curr+"T");
        return ans;
    }

    /**
     * This method receives a word in percentage format and returns how we save it in the dictionary.
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithPercent(String curr) { // 10.23 % or 111,000 percent or 12,000.3355 percentage

        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        if(curr.matches("^[0-9]+[\\/][0-9]+$")){
            curr=fractionToDecimal(curr);
        }
        curr= parseTheNumber(curr);
        ans.add(curr+"%");
        return ans;
    }

    /**
     * This method takes a word in percentage format, without a space between the number and the percentage, and returns how we keep it in the dictionary.
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithPercentNoSpace(String curr) { // 10.23%
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        if(StringUtils.endsWith(curr,"%")){
            curr= StringUtils.substring(curr,0,curr.length()-1);
        }
        else{
            curr=StringUtils.substring(curr,1);
        }
        ans.addAll(digitWithPercent(curr));//delete the %
        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithDollar(String curr) {  // 10,000 dolLar or 12,00.23 DOLLAR or 2
        // to check size of num and sed to the right func

        LinkedList<String> ans=new LinkedList<>();
        String termVal="";

        if(curr.matches("^[0-9]+[\\/][0-9]+$")){
            curr=fractionToDecimal(curr);
        }
        if(StringUtils.contains(curr,",")){
            if(StringUtils.contains(curr,".")){
                int dotIndex=curr.indexOf(".");
                if(dotIndex<=7){
                    if(curr.length()>dotIndex+4){
                        String shortCurr=StringUtils.substring(curr,0,dotIndex+4);
                        shortCurr=trimZeroAtTheEndOfNumber(shortCurr);
                        ans.add(shortCurr+ " Dollars");
                    }
                    else{
                        curr=trimZeroAtTheEndOfNumber(curr);
                        ans.add(curr+ " Dollars");
                    }
                }
                else{ // larger them million\
                    curr=StringUtils.replace(curr, ",", "");// ? ! " "
                    curr=StringUtils.substring(curr,0,dotIndex-3);
                    curr=StringUtils.substring(curr,0,curr.length()-6)+"."+StringUtils.substring(curr,curr.length()-6,curr.length()-3);
                    curr=trimZeroAtTheEndOfNumber(curr);
                    ans.add(curr+ " M Dollars");

                }
            }
            else{ // with , no .
                if(curr.length()<=7){
                    ans.add(curr+ " Dollars");
                }
                else{
                    curr=StringUtils.replace(curr, ",", "");// ? ! " "
                    curr=StringUtils.substring(curr,0,curr.length()-3);
                    curr=StringUtils.substring(curr,0,curr.length()-3)+"."+StringUtils.substring(curr,curr.length()-3,curr.length());
                    curr=trimZeroAtTheEndOfNumber(curr);
                    ans.add(curr+ " M Dollars");

                }
            }
        }
        else{ // no ,
            if(StringUtils.contains(curr,".")){
                int dotIndex=curr.indexOf(".");
                if(dotIndex<=6){
                    if(curr.length()>dotIndex+4){
                        String shortCurr=StringUtils.substring(curr,0,dotIndex+4);
                        shortCurr=trimZeroAtTheEndOfNumber(shortCurr);
                        ans.add(shortCurr+ " Dollars");
                    }
                    else{
                        curr=trimZeroAtTheEndOfNumber(curr);
                        ans.add(curr+ " Dollars");
                    }
                }
                else{ // big than million , no .
                    curr=StringUtils.substring(curr,0,dotIndex-3);
                    curr=StringUtils.substring(curr,0,curr.length()-3)+"."+StringUtils.substring(curr,curr.length()-3,curr.length());
                    curr=trimZeroAtTheEndOfNumber(curr);
                    ans.add(curr+ " M Dollars");
                }
            }
            else{ // no . no ,
                if(curr.length()<=6){ // less then million
                    ans.add(curr+ " Dollars");
                }
                else{ // bigger tyhen million
                    curr=StringUtils.substring(curr,0,curr.length()-3);
                    curr=StringUtils.substring(curr,0,curr.length()-3)+"."+StringUtils.substring(curr,curr.length()-3,curr.length());
                    curr=trimZeroAtTheEndOfNumber(curr);
                    ans.add(curr+ " M Dollars");
                }

            }

        }

        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithKDollar(String curr) { // 100 k DOLLARS
        LinkedList<String> ans=new LinkedList<>();
        String termVal="";
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        if(curr.matches("^[0-9]+[\\/][0-9]+$")){
            curr=fractionToDecimal(curr);
        }

        if(StringUtils.contains(curr,".")){
            Double value= Double.parseDouble(curr);
            value =value*1000;
            int dotIndex=curr.indexOf(".");
            // if((curr.substring(dotIndex,curr.length())).length()>3){
            String valOfInt= String.format ("%.9f", value);
            valOfInt=parseTheNumber(valOfInt);
            ans.add(valOfInt+ " Dollars");
        }
        else{ // no dot
            int intValue= Integer.parseInt(curr);
            intValue = intValue*1000;
            ans.add(intValue+ " Dollars");
        }
        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithMDollar(String curr) { // 20.6 M DOLLARS
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        if(curr.matches("^[0-9]+[\\/][0-9]+$")){
            curr=fractionToDecimal(curr);
        }

        if(StringUtils.contains(curr,".")){
            String value=parseTheNumber(curr);
            ans.add(value+ " M Dollars");
        }
        else{
            ans.add(curr+ " M Dollars");
        }

        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithBDollar(String curr) { //100,000 B DOLLARS
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        if(curr.matches("^[0-9]+[\\/][0-9]+$")){
            curr=fractionToDecimal(curr);
        }

        if(StringUtils.contains(curr,".")){
            Double value= Double.parseDouble(curr);
            value =value*1000;
            int dotIndex=curr.indexOf(".");
            // if((curr.substring(dotIndex,curr.length())).length()>3){
            String valOfInt= String.format ("%.9f", value);
            valOfInt=parseTheNumber(valOfInt);
            ans.add(valOfInt+ " M Dollars");
        }
        else{ // no dot
            int intValue= Integer.parseInt(curr);
            intValue = intValue*1000;
            ans.add(intValue+ " M Dollars");
        }
        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithTDollar (String curr) { //100 T DOLLARS
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        if(curr.matches("^[0-9]+[\\/][0-9]+$")){
            curr=fractionToDecimal(curr);
        }

        if(StringUtils.contains(curr,".")){
            Double value= Double.parseDouble(curr);
            value =value*1000000;
            int dotIndex=curr.indexOf(".");
            // if((curr.substring(dotIndex,curr.length())).length()>3){
            String valOfInt= String.format ("%.9f", value);
            valOfInt=parseTheNumber(valOfInt);
            ans.add(valOfInt+ " M Dollars");
        }
        else{ // no dot
            int intValue= Integer.parseInt(curr);
            intValue = intValue*1000000;
            ans.add(intValue+ " M Dollars");
        }
        return ans;
    }

    /**
     *
     * @param curr
     * @param next1
     * @return
     */
    private LinkedList<String> fractionWithDollar(String curr, String next1) { // 22 1/2 DolLar
        //need to conclude the curr and next!!
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        next1=StringUtils.replace(next1, ",", "");// ? ! " "
        String[] rat = StringUtils.split(next1,"/");
        Double fractionVal= Double.parseDouble(rat[0]) / Double.parseDouble(rat[1]);
        Double wholeValue=Double.parseDouble(curr)+ fractionVal;
        String toAns=parseTheNumber(wholeValue+"");
        ans.addAll(digitWithDollar(toAns));
        return ans;
    }


    /**
     *
     * @param curr
     * @param next1
     * @return
     */
    private LinkedList<String> dateDDMM(String curr, String next1) {  // 14 MAY
        // use next

        LinkedList<String>ans = new LinkedList<>();
        ans.add(curr+"-"+getTheNumberOfMonth(StringUtils.upperCase(next1)));
        return ans;
    }

    /**
     *
     * @param curr
     * @param next1
     * @return
     */
    private LinkedList<String> dateMMDD(String curr, String next1) { // MAY 15
        LinkedList<String>ans = new LinkedList<>();
        ans.add(getTheNumberOfMonth(StringUtils.upperCase(curr))+"-"+next1);
        return ans;
    }

    /**
     *
     * @param curr
     * @param next1
     * @return
     */
    private LinkedList<String> dateMMYYYY(String curr, String next1) { // MAY 1994
        LinkedList<String>ans = new LinkedList<>();
        ans.add(next1+"-"+getTheNumberOfMonth(StringUtils.upperCase(curr)));
        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> checkNumberValue(String curr) { // 10.65 OR 10,000,000 OR 10,100.3514
        // check if has dot
        // if has ,
        // check the size of the val or count the digits
        //send to relevant funk
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        if(curr.matches("^[0-9]+[\\/][0-9]+$")){
            curr=fractionToDecimal(curr);
        }

        int dotOrEnd = 0;
        if(StringUtils.contains(curr,".")) {
            dotOrEnd = curr.indexOf(".");
        }
        else {
            dotOrEnd = curr.length();
        }
        if (dotOrEnd <= 3) {//number less than 1000
            if (curr.length() > dotOrEnd + 4) {
                curr = StringUtils.substring(curr,0, dotOrEnd + 4);
            }
            curr = trimZeroAtTheEndOfNumber(curr);
            ans.add(curr);
        }
        else if (dotOrEnd <= 6) {//thousand to million
            curr = StringUtils.substring(curr,0, dotOrEnd - 3) + "." + StringUtils.substring(curr,dotOrEnd - 3, dotOrEnd);
            curr = trimZeroAtTheEndOfNumber(curr);
            ans.add(curr + "K");
        }
        else if (dotOrEnd <= 9) {//million to billion
            curr = StringUtils.substring(curr,0, dotOrEnd - 6) + "." + StringUtils.substring(curr,dotOrEnd - 6, dotOrEnd - 3);
            curr = trimZeroAtTheEndOfNumber(curr);
            ans.add(curr + "M");
        } else {//over billion
            curr = StringUtils.substring(curr,0, dotOrEnd -9) + "." + StringUtils.substring(curr,dotOrEnd - 9, dotOrEnd - 6);
            curr = trimZeroAtTheEndOfNumber(curr);
            ans.add(curr + "B");
        }

        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithKNoSpace(String curr) { // 10.3k
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.substring(curr,0,curr.length()-1);//trim the K
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        String toAns= parseTheNumber(curr);
        ans.add(toAns+"K");
        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithKNoSpaceDollars(String curr) { // 10.3k 15,000K dollar
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.substring(curr,0,curr.length()-1);
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        ans.addAll(digitWithKDollar(curr));
        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithMNoSpaceDollars(String curr) { // 10.3m 15,000M dollar
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.substring(curr,0,curr.length()-1);
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        ans.addAll(digitWithMDollar(curr));
        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithBNoSpaceDollars(String curr) { // 10.3bn 15,000B dollar
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.substring(curr,0,curr.length()-2);
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        ans.addAll(digitWithBDollar(curr));
        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithTRNoSpaceDollars(String curr) { // 10.3tr dollar 15,000TR dollar
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.substring(curr,0,curr.length()-2);
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        ans.addAll(digitWithTDollar(curr));
        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWith$Million(String curr){//$100 million or 100$ million
        LinkedList<String>ans = new LinkedList<>();
        if(curr.startsWith("$")){
            curr=StringUtils.substring(curr,1);
            ans.addAll(digitWithMDollar(curr));
        }
        else{
            curr=StringUtils.substring(curr,0,curr.length()-1);
            ans.addAll(digitWithMDollar(curr));
        }
        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWith$Billion(String curr){//$100 billion or 334$ billion
        LinkedList<String>ans = new LinkedList<>();
        if(curr.startsWith("$")){
            curr=StringUtils.substring(curr,1);
            ans.addAll(digitWithBDollar(curr));
        }
        else{
            curr=StringUtils.substring(curr,0,curr.length()-1);
            ans.addAll(digitWithBDollar(curr));
        }
        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String>  digitWith$NoSpace(String curr){// $450,000 or $1,000,000,000 or 1000,000.0454$
        LinkedList<String>ans = new LinkedList<>();
        if(curr.startsWith("$")){
            curr=StringUtils.substring(curr,1);
            ans.addAll(digitWithDollar(curr));
        }
        else{
            curr=StringUtils.substring(curr,0,curr.length()-1);
            ans.addAll(digitWithDollar(curr));
        }
        return ans;
    }


    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithMNoSpace(String curr) { // 10.3m 15,000M
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.substring(curr,0,curr.length()-1);//trim the M
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        String toAns= parseTheNumber(curr);
        ans.add(toAns+"M");
        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithBNoSpace(String curr) { // 10.3bn 15,000BN
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.substring(curr,0,curr.length()-2);//trim the BN
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        String toAns= parseTheNumber(curr);
        ans.add(toAns+"B");
        return ans;
    }

    /**
     *
     * @param curr
     * @return
     */
    private LinkedList<String> digitWithTRNoSpace(String curr) { // 10.3tr 15,000TR
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.substring(curr,0,curr.length()-2);//trim the TR
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        String toAns= parseTheNumber(curr);
        ans.add(toAns+"T");
        return ans;
    }

    /**
     *
     * @param curr
     * @param left
     * @param right
     * @return
     */
    private LinkedList<String> rangeOfNumbers(String curr,String left, String right){ // 6-5 or 6.535-1,000
        //save both sepretly ans also together (all the term- curr)
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        ans.add(curr);
        ans.addAll(checkNumberValue(left));
        ans.addAll(checkNumberValue(right));
        return ans;
    }

    /**
     *
     * @param curr
     * @param left
     * @param right
     * @return
     */
    private LinkedList<String> betweenNumberAndNumber(String curr,String left,String right){
        LinkedList<String>ans = new LinkedList<>();
        ans.add(curr);
        ans.addAll(checkNumberValue(left));
        ans.addAll(checkNumberValue(right));
        ans.add(left+"-"+right);
        return ans;
    }

    /**
     *
     * @param curr
     * @param next1
     * @param next3
     * @return
     */
    private LinkedList<String> betweenDollars(String curr,String next1,String next3){
        LinkedList<String>ans = new LinkedList<>();
        if((StringUtils.contains(next1,"M")||StringUtils.contains(next1,"m")) && (StringUtils.contains(next3,"M")||StringUtils.contains(next3,"m"))){
            ans.addAll(digitWithMNoSpaceDollars(StringUtils.substring(next1,1)));
            ans.addAll(digitWithMNoSpaceDollars(StringUtils.substring(next3,1)));
        }
        else if((StringUtils.contains(next1,"K")||StringUtils.contains(next1,"k")) && (StringUtils.contains(next3,"K")||StringUtils.contains(next3,"k"))){
            ans.addAll(digitWithKNoSpaceDollars(StringUtils.substring(next1,1)));
            ans.addAll(digitWithKNoSpaceDollars(StringUtils.substring(next3,1)));
        }
        else if((StringUtils.contains(next1,"B")||StringUtils.contains(next1,"b")) && (StringUtils.contains(next3,"B")||StringUtils.contains(next3,"b"))){
            ans.addAll(digitWithBNoSpaceDollars(StringUtils.substring(next1,1)));
            ans.addAll(digitWithBNoSpaceDollars(StringUtils.substring(next3,1)));
        }
        else{
            ans.addAll(digitWith$NoSpace(StringUtils.substring(next1,1)));
            ans.addAll(digitWith$NoSpace(StringUtils.substring(next3,1)));
        }
        ans.add(next1+"-"+next3);
        return ans;
    }

    /**
     *
     * @param curr
     * @param left
     * @param right
     * @return
     */
    private LinkedList<String> rangeWordAndNumber(String curr,String left, String right){// word-10  or chocolate-10
        LinkedList<String>ans = new LinkedList<>();
        right=StringUtils.replace(right, ",", "");// ? ! " "
        right=parseTheNumber(right);
        ans.add(left);//!!!!!!!!!!!!!!!!!!!
        ans.add(right);
        ans.add(left+"-"+right);
        return ans;
    }

    /**
     *
     * @param curr
     * @param left
     * @param right
     * @return
     */
    private LinkedList<String> rangeNumberAndWord(String curr,String left, String right){
        LinkedList<String>ans = new LinkedList<>();
        ans.add(right);
        String leftVal="";
        left=StringUtils.replace(left, ",", "");// ? ! " "
        if(StringUtils.contains(left,".")){
            Double value= Double.parseDouble(left);
            leftVal= nf.format(value);
        }
        else{
            leftVal=left;
        }
        ans.add(leftVal);
        ans.add(leftVal+"-"+right);//curr
        return ans;
    }

    /**
     *
     * @param curr
     * @param left
     * @param right
     * @return
     */
    private LinkedList<String> rangeSavedAsOneTerm(String curr,String left, String right){
        LinkedList<String>ans = new LinkedList<>();
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        ans.add(curr);
        return ans;
    }

    /**
     *
     * @param curr
     * @param left
     * @param right
     * @return
     */
    private LinkedList<String> rangeWithDollarSign(String curr,String left, String right){// 10$-10.56$
        //save both sepretly and also together (all the term- curr)
        LinkedList<String> ans= new LinkedList<>();
        ans.addAll(digitWith$NoSpace(left));
        ans.addAll(digitWith$NoSpace(right));
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        ans.add(curr);

        return ans;
    }

    /**
     *
     * @param curr
     * @param left
     * @param right
     * @return
     */
    private LinkedList<String> rangeWithPercentSign(String curr,String left, String right){// 10%-10.56%
        //save both sepretly and also together (all the term- curr)
        LinkedList<String> ans= new LinkedList<>();
        ans.addAll(digitWithPercentNoSpace(left));
        ans.addAll(digitWithPercentNoSpace(right));
        curr=StringUtils.replace(curr, ",", "");// ? ! " "
        ans.add(curr);
        return ans;
    }

    /**
     *
     * @param str
     * @return
     */
    private String parseTheNumber(String str){
        str=StringUtils.replace(str, ",", "");// ? ! " "
        if(StringUtils.contains(str,".")){
            int dotIndex=str.indexOf(".");
            if(str.length()>dotIndex+4){
                str=StringUtils.substring(str,0,dotIndex+4);// take 3 digits after the dot
            }
            str=trimZeroAtTheEndOfNumber(str);
        }
        return str;
    }

    /**
     *
     * @param month
     * @return
     */
    private String getTheNumberOfMonth(String month){
        switch (month) {
            case "JAN":
            case "JANUARY": {
                return "01";
            }
            case "FEB":
            case "FEBRUARY":{
                return "02";
            }
            case "MAR":
            case "MARCH":{
                return"03";
            }
            case "APR":
            case"APRIL":{
                return "04";
            }
            case "MAY":{
                return"05";
            }
            case "JUN":
            case "JUNE":{
                return "06";
            }
            case "JUL":
            case "JULY":{
                return "07";
            }
            case "AUG":
            case "AUGUST":{
                return "08";
            }
            case "SEP":
            case "SEPTEMBER":{
                return "09";
            }
            case "OCT":
            case "OCTOBER":{
                return "10";
            }
            case "NOV":
            case "NOVEMBER":{
                return "11";
            }
            case "DEC":
            case"DECEMBER":{
                return "12";
            }
        }
        return null;
    }

    /**
     *
     * @param number
     * @return
     */
    private String trimZeroAtTheEndOfNumber(String number){
        if(StringUtils.contains(number,".")){
            while (StringUtils.endsWith(number,"0")) {
                number = StringUtils.substring(number,0, number.length() - 1);
            }
            if (StringUtils.endsWith(number,".")) {
                number = StringUtils.substring(number,0, number.length() - 1);
            }
        }

        return number;
    }

    /**
     *
     * @param fraction
     * @return
     */
    private String fractionToDecimal(String fraction){
        String[] rat = StringUtils.split(fraction,"/");
        if(rat[1].equals("0")){
            return "0";
        }
        else{
            Double fractionVal= Double.parseDouble(rat[0]) / Double.parseDouble(rat[1]);
            return fractionVal.toString();
        }

    }

}