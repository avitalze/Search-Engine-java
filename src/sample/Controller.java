package sample;

import Model.Indexer;
import Model.Searcher;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class Controller {
    public Button clear_button;
    public Button display_button;
    public Button load_button;
    public CheckBox stemming_checkBox;
    public TextField corpusPath_textField;
    public Label corpus_label;
    public Button browse_corpus_button;
    public Button browse_postFile_button;
    public TextField posting_textFeild;
    public Label posting_label;
    public Label Header_label;
    public Button buildInvertedIndex_button;
    /******/
    public Button browse_query_button;
    public TextField single_query_text_field;
    public TextField file_query_text_field;
    public Button run_single_query_button;
    public Button run_file_query_button;
    public TextField result_textFeild;
    public Button browse_result_button;
    public CheckBox entities_checkbox;
    public CheckBox semantic_search_checkbox;
    //public Checkbox internet_checkBox;

    public Stage mainStage;
    public Scene mainScene;


    public Indexer indexer;
    public CheckBox internet_checkBox;

    public void initialize( Stage mainStage, Scene mainScene) {
        this.mainScene = mainScene;
        this.mainStage = mainStage;
    }

    /**
     * opens the browser to select the corpus path
     */
    public void browseCorpus(){
        DirectoryChooser fc = new DirectoryChooser();
        fc.setTitle("Corpus Path");
        File filePath = new File("./");//????????????????
        if (!filePath.exists())
            filePath.mkdir();
        fc.setInitialDirectory(filePath);

        File selectedDirectory = fc.showDialog(mainStage);

        //Make sure a file was selected, if not return default

        if(selectedDirectory != null){
            //corpusPath=selectedDirectory.getAbsolutePath();
            corpusPath_textField.setText(selectedDirectory.getAbsolutePath());
        }
    }
    /**
     * opens the browser to select the corpus path
     */
    public void browsePosting(){
        DirectoryChooser fc = new DirectoryChooser();
        fc.setTitle("Posting Path");
        File filePath = new File("./");
        if (!filePath.exists())
            filePath.mkdir();
        fc.setInitialDirectory(filePath);

        File selectedDirectory = fc.showDialog(mainStage);

        //Make sure a file was selected, if not return default

        if(selectedDirectory != null){
            //postingPath=selectedDirectory.getAbsolutePath();
            posting_textFeild.setText(selectedDirectory.getAbsolutePath());
        }
    }

    public void browseQueryFile(){
        FileChooser fc= new FileChooser();
        fc.setTitle("Query File");
        File filePath = new File("./");//????????????????
        if (!filePath.exists())
            filePath.mkdir();
        fc.setInitialDirectory(filePath);

        File selectedFile = fc.showOpenDialog(mainStage);

        //Make sure a file was selected, if not return default
        if(selectedFile != null){
            //queryPath= selectedFile.getAbsolutePath();
            file_query_text_field.setText(selectedFile.getAbsolutePath());
        }
    }

    public void browseResultPath(){
        DirectoryChooser fc = new DirectoryChooser();
        fc.setTitle("Choose Answer Path");

        File filePath = new File("./");//????????????????
        if (!filePath.exists())
            filePath.mkdir();
        fc.setInitialDirectory(filePath);

        File selectedDirectory = fc.showDialog(mainStage);

        //Make sure a file was selected, if not return default
        if(selectedDirectory != null){
            result_textFeild.setText(selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * build the whole inverted index and dictionary
     */
    public void buildInvertedIndex(){
        if(corpusPath_textField.getText().length()==0 || posting_textFeild.getText().length()==0){
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Important Message!!!!");
            alert.setHeaderText("You didn't choose corpus and posting paths. Please choose and try again :)");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                alert.close();
            } else {
                alert.close();
            }
        }
        else{
            if(stemming_checkBox.isSelected()){// with stemming
                indexer=new Indexer(corpusPath_textField.getText(),posting_textFeild.getText(),true);
            }
            else{//without stemming
                indexer=new Indexer(corpusPath_textField.getText(),posting_textFeild.getText(),false);
            }
            long runtime=indexer.runDictionaryBuilder();
            if(runtime==0){
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Important Message!!!!");
                alert.setHeaderText("The building of the inverted index has failed");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK){
                    alert.close();
                } else {
                    alert.close();
                }
            }
            else{
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                if(stemming_checkBox.isSelected()){
                    alert.setTitle("Dictionary Details with Stemming!");
                }
                else{
                    alert.setTitle("Dictionary Details without Stemming!");
                }
                alert.setHeaderText("Num of Indexed Documents: "+indexer.getNumOfDocs()+"\n"+
                        "Num of unique terms in corpus: "+ indexer.getDictionary().size()+"\n"+
                        "Total runtime: "+runtime);
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK){
                    alert.close();
                } else {
                    alert.close();
                }
            }
        }
    }

    /**
     * delete all posting files in path
     */
    public void deletePostingFiles(){
        System.gc();
        File stemFolder= new File(posting_textFeild.getText()+"\\withStemmer");
        if(stemFolder.exists()){
            deleteAll(stemFolder);
            stemFolder.delete();
        }
        System.gc();
        File noStemFolder= new File(posting_textFeild.getText()+"\\noStemmer");
        if(noStemFolder.exists()){
            deleteAll(noStemFolder);
            noStemFolder.delete();
        }

        if(indexer!=null){
            indexer.indexerClearAll();
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Important Message!!!!");
        alert.setHeaderText("All Posting files in the path were deleted");
        alert.setContentText("Thank You");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK){
            alert.close();
        } else {
            alert.close();
        }
    }

    /**
     * delete all file in folder
     * @param folder
     */
    private void deleteAll(File folder){
        System.gc();
        File[] allFiles = folder.listFiles();
        int numOfFiles = allFiles.length;
        File curr;
        for(int i = 0; i < numOfFiles; ++i) {
            curr=allFiles[i];
            if (curr.isFile() && curr.getName().endsWith(".txt")) {
                curr.delete();
            } else {
                deleteAll(curr);
                curr.delete();
            }
        }
    }

    /**
     * represent the dictionary to user
     * @param actionEvent
     */
    public void showDic(ActionEvent actionEvent) {
        if(indexer==null || indexer.getDictionary().size()==0){
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Important Message!!!!");
            alert.setHeaderText("There is no dictionary to present, please upload a distionary from file or build inverted index");
            alert.setContentText("Thank You");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                alert.close();
            } else {
                alert.close();
            }
        }else{
            TableView <HashMap.Entry<String, String>> table = new TableView();
            Stage stage = new Stage();
            Scene scene = new Scene(new Group());
            stage.setTitle("Table View Sample");
            stage.setWidth(300);
            stage.setHeight(500);

            table.getItems().addAll(indexer.getDictionary().entrySet());

            TableColumn<HashMap.Entry<String, String>, String> column1 = new TableColumn<>("Term");
            column1.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getKey()));
            Comparator<String> tComparator = new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }
            };
            column1.setComparator(tComparator);

            TableColumn<HashMap.Entry<String, String>, String> column2 = new TableColumn<>("Num in Corpus");
            column2.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getValue()));

            table.getColumns().addAll(column1,column2);
            table.getSortOrder().add(column1);
            final VBox vbox = new VBox();
            vbox.setSpacing(5);
            vbox.setPadding(new Insets(10, 0, 0, 10));
            vbox.getChildren().addAll(table);

            ((Group) scene.getRoot()).getChildren().addAll(vbox);

            stage.setScene(scene);
            stage.show();

        }


    }

    /**
     * upload dictionary to memory
     * @param event
     */
    public void uploadDictionary(ActionEvent event){
        String folderPath="";
        if(stemming_checkBox.isSelected()){
            folderPath=posting_textFeild.getText()+"\\withStemmer";
        }
        else{
            folderPath=posting_textFeild.getText()+"\\noStemmer";
        }
        File folder= new File(folderPath);
        if(!folder.exists()){
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Important Message!!!!");
            alert.setHeaderText("There is no dictionary in the posting file path, please enter a new path and try again.");
            alert.setContentText("Thank You");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                alert.close();
            } else {
                alert.close();
            }
        }
        else{
            indexer=new Indexer(corpusPath_textField.getText(),posting_textFeild.getText(),stemming_checkBox.isSelected());
            if(indexer.uploadDictionary(folder)){
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Dictionary Uploaded");
                alert.setHeaderText("The dictionary was loaded to memory\n"+
                        "Dictionary Size: "+ indexer.getDictionary().size());
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK){
                    alert.close();
                } else {
                    alert.close();
                }
            }
            else{
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Important Message!!!!");
                alert.setHeaderText("There is no dictionary in the posting file path, please enter a new path and try again.");
                alert.setContentText("Thank You");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK){
                    alert.close();
                } else {
                    alert.close();
                }
                //????????????
            }

        }
    }

    public void runSingleQuery(){
        if(single_query_text_field.getText().trim().isEmpty()){
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Important Message!!!!");
            alert.setHeaderText("You didn't insert a query. Please insert text and try again :)");
            alert.setContentText("Thank You");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                alert.close();
            } else {
                alert.close();
            }
        }
        if(indexer==null){
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Important Message!!!!");
            alert.setHeaderText("You didn't uplaod a dictionary .Please try again :)");
            alert.setContentText("Thank You");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                alert.close();
            } else {
                alert.close();
            }
        }
        else{

            Searcher searcher;
            if(stemming_checkBox.isSelected()){
                searcher=new Searcher(semantic_search_checkbox.isSelected(),entities_checkbox.isSelected(),stemming_checkBox.isSelected(),internet_checkBox.isSelected(),posting_textFeild.getText()+"\\withStemmer",indexer);
            } else{
                searcher=new Searcher(semantic_search_checkbox.isSelected(),entities_checkbox.isSelected(),stemming_checkBox.isSelected(),internet_checkBox.isSelected(),posting_textFeild.getText()+"\\noStemmer",indexer);
            }
            String queryID= ""+(int)(Math.random()*9000);
            LinkedHashMap<String,List<Pair<String,Double>>> queryAns= searcher.searchSingleQuery(single_query_text_field.getText().trim(),queryID,null);

            StringBuilder resultToDisplay= new StringBuilder();
            resultToDisplay.append("Results for Query: \n");
            resultToDisplay.append("Number of Docs returned:");
            resultToDisplay.append(queryAns.size());
            resultToDisplay.append("\n");
            int count=1;
            if(!entities_checkbox.isSelected()){// no entity search
                for (String docName: queryAns.keySet()) {
                    resultToDisplay.append("\t");
                    resultToDisplay.append(count);
                    resultToDisplay.append(". Doc Name: ");
                    resultToDisplay.append(docName);
                    resultToDisplay.append("\n");
                    count++;
                }
            }
            else{
                for (HashMap.Entry<String,List<Pair<String,Double>>> entry: queryAns.entrySet()){
                    resultToDisplay.append("\t");
                    resultToDisplay.append(count);
                    resultToDisplay.append(". Doc Name: ");
                    resultToDisplay.append(entry.getKey());
                    resultToDisplay.append("\n");
                    resultToDisplay.append("\t\t");
                    resultToDisplay.append(" Dominant entities in document:\n");
                    count++;
                    for (Pair<String,Double> entityAndRank:entry.getValue()) {
                        resultToDisplay.append("\t\t");
                        resultToDisplay.append("\u2022");
                        resultToDisplay.append(" ");
                        resultToDisplay.append(entityAndRank.getKey());
                        resultToDisplay.append(", rank: ");
                        resultToDisplay.append(entityAndRank.getValue());
                        resultToDisplay.append("\n");
                    }
                }
            }


            Label queryAnsLabel= new Label();
            queryAnsLabel.setText(resultToDisplay.toString());
            ScrollPane  secondaryLayout = new ScrollPane();
            secondaryLayout.setContent(queryAnsLabel);

            Scene secondScene = new Scene(secondaryLayout, 300, 500);

            // New window (Stage)
            Stage newWindow = new Stage();
            newWindow.setTitle("Query Answer");
            newWindow.setScene(secondScene);

            newWindow.show();

            ///WRITE TO RESULTS FILE
            if(!result_textFeild.getText().trim().isEmpty()){
                searcher.writeQueryResultsToFile(result_textFeild.getText().trim(),queryID,queryAns.keySet());
            }
        }
    }

    public void runFileQuery(){
        if(file_query_text_field.getText().trim().isEmpty()){
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Important Message!!!!");
            //alert.getDialogPane().getStylesheets().add("Popup.css");
            alert.setHeaderText("You didn't insert a query file. Please insert file path and try again :)");
            alert.setContentText("Thank You");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                alert.close();
            } else {
                alert.close();
            }
        }
        else if(indexer==null){
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Important Message!!!!");
            alert.setHeaderText("You didn't uplaod a dictionary .Please try again :)");
            alert.setContentText("Thank You");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK){
                alert.close();
            } else {
                alert.close();
            }
        }
        else{
            Searcher searcher;
            if(stemming_checkBox.isSelected()){
                searcher=new Searcher(semantic_search_checkbox.isSelected(),entities_checkbox.isSelected(),stemming_checkBox.isSelected(),internet_checkBox.isSelected(),posting_textFeild.getText()+"\\withStemmer",indexer);
            } else{
                searcher=new Searcher(semantic_search_checkbox.isSelected(),entities_checkbox.isSelected(),stemming_checkBox.isSelected(),internet_checkBox.isSelected(),posting_textFeild.getText()+"\\noStemmer",indexer);
            }
            LinkedHashMap<String,LinkedHashMap<String, List<Pair<String, Double>>>> queryAns= searcher.searchFileQuery(file_query_text_field.getText());
            if(queryAns==null){//FILE NOT EXIST
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Important Message!!!!");
                //alert.getDialogPane().getStylesheets().add("Popup.css");
                alert.setHeaderText("The file you chose doesn't exist, please choose a new file and try again");
                alert.setContentText("Thank You");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK){
                    alert.close();
                } else {
                    alert.close();
                }
            }
            else{
                if(queryAns.size()==0){//NO QUERIES IN FILE
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Important Message!!!!");
                    //alert.getDialogPane().getStylesheets().add("Popup.css");
                    alert.setHeaderText("No queries found in the file you chose, please choose a new file and try again");
                    alert.setContentText("Thank You");
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.get() == ButtonType.OK){
                        alert.close();
                    } else {
                        alert.close();
                    }
                }
                else{
                    StringBuilder resultToDisplay= new StringBuilder();
                    for (HashMap.Entry<String,LinkedHashMap<String, List<Pair<String, Double>>>> singleQueryAns: queryAns.entrySet()) {
                        resultToDisplay.append("Results for query number: ");
                        resultToDisplay.append(singleQueryAns.getKey());
                        resultToDisplay.append("\n");
                        resultToDisplay.append("Number of Docs returned:");
                        resultToDisplay.append(singleQueryAns.getValue().size());
                        resultToDisplay.append("\n");
                        int count=1;
                        if(!entities_checkbox.isSelected()){// no entity search
                            for (String docName: singleQueryAns.getValue().keySet()) {
                                resultToDisplay.append("\t");
                                resultToDisplay.append(count);
                                resultToDisplay.append(". Doc Name: ");
                                resultToDisplay.append(docName);
                                resultToDisplay.append("\n");
                                count++;
                            }
                            resultToDisplay.append("\n");
                        }
                        else{
                            for (HashMap.Entry<String,List<Pair<String,Double>>> entry: singleQueryAns.getValue().entrySet()){
                                resultToDisplay.append("\t");
                                resultToDisplay.append(count);
                                resultToDisplay.append(". Doc Name: ");
                                resultToDisplay.append(entry.getKey());
                                resultToDisplay.append("\n");
                                resultToDisplay.append("\t\t");
                                resultToDisplay.append("Dominant entities in document:\n");
                                count++;
                                for (Pair<String,Double> entityAndRank:entry.getValue()) {
                                    resultToDisplay.append("\t\t");
                                    resultToDisplay.append("\u2022");
                                    resultToDisplay.append(" ");
                                    resultToDisplay.append(entityAndRank.getKey());
                                    resultToDisplay.append(", rank: ");
                                    resultToDisplay.append(entityAndRank.getValue());
                                    resultToDisplay.append("\n");
                                }

                            }
                            resultToDisplay.append("\n");
                        }
                    }

                    Label queryAnsLabel= new Label();
                    queryAnsLabel.setText(resultToDisplay.toString());
                    ScrollPane secondaryLayout = new ScrollPane();
                    secondaryLayout.setContent(queryAnsLabel);

                    Scene secondScene = new Scene(secondaryLayout, 300, 500);

                    // New window (Stage)
                    Stage newWindow = new Stage();
                    newWindow.setTitle("Query Answers");
                    newWindow.setScene(secondScene);

                    newWindow.show();

                    ///WRITE TO RESULTS FILE
                    if(!result_textFeild.getText().trim().isEmpty()){
                        for (HashMap.Entry<String,LinkedHashMap<String, List<Pair<String, Double>>>> singleQueryAns: queryAns.entrySet()) {
                            searcher.writeQueryResultsToFile(result_textFeild.getText().trim(),singleQueryAns.getKey(),singleQueryAns.getValue().keySet());
                        }

                    }
                }

            }

        }
    }


}
