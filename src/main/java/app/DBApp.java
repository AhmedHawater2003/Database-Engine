package app;
/**
 * @author Wael Abouelsaadat
 */

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import exceptions.DBAppException;
import handlers.SQLOperator;
import handlers.SelectionHandler;
import helpers.ConfigReader;
import helpers.MetaDataColumns;
import helpers.MetaDataManger;
import helpers.RecordsFetcher;
import storage.Page;
import storage.PageInfo;
import storage.Table;
import storage.Tuple;

import java.io.*;

import storage.bplustree;

import java.util.*;
//import java.lang.*;


public class DBApp {


    public DBApp() {
        this.init();
    }

    // this does whatever initialization you would like
    // or leave it empty if there is no code you want to
    // execute at application startup
    public void init() {
        File file = new File(ConfigReader.getInstance().read("MetaDataFileName"));
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    // following method creates one table only
    // strClusteringKeyColumn is the name of the column that will be the primary
    // key and the clustering column as well. The data type of that column will
    // be passed in htblColNameType
    // htblColNameValue will have the column name as key and the data
    // type as value
    public void createTable(String strTableName,
                            String strClusteringKeyColumn,
                            Hashtable<String, String> htblColNameType) throws DBAppException, IOException {

        if (MetaDataManger.getInstance().exists(MetaDataColumns.TABLE_NAME, strTableName)) {
            throw new DBAppException("Table already exists");
        }

        Table table = new Table(strTableName, strClusteringKeyColumn, htblColNameType);
        table.serialize();
    }


    // following method creates a B+tree index
    public void createIndex(String strTableName,
                            String strColName,
                            String strIndexName) throws DBAppException, IOException, ClassNotFoundException {
        String targetline = "";
        String editedline = "";
        //check that the table exists with the correct naming


            FileReader filereader = new FileReader("resources/metadata.csv");
            boolean tableFound = false, columnFound = false, indexValid = false;


            CSVReader csvReader = new CSVReader(filereader);
            String[] nextRecord;


            while ((nextRecord = csvReader.readNext()) != null) {

                if (nextRecord[0].equals(strTableName)) {
                    tableFound = true;
                    if (nextRecord[1].equals(strColName)) {
                        columnFound = true;
                        if (nextRecord[4].equals("null")) {
                            indexValid = true;
                            //edit the csv and make nextRecord[4] = strIndexName and nextRecord[5] = "B+tree"
                            targetline += nextRecord[0] + "," + nextRecord[1] + "," + nextRecord[2] + "," + nextRecord[3] + "," + nextRecord[4] + "," + nextRecord[5];
                            nextRecord[4] = strIndexName;
                            nextRecord[5] = "B+tree";
                            editedline += nextRecord[0] + "," + nextRecord[1] + "," + nextRecord[2] + "," + nextRecord[3] + "," + nextRecord[4] + "," + nextRecord[5];
                            // write to the actual csv file


                            break;
                        }
                    }
                }
            }
            csvReader.close();
            if (!tableFound) {
                throw new DBAppException("Table Not Found!");
            } else if (!columnFound) {
                throw new DBAppException("Column Not Found!");

            } else if (!indexValid) {
                throw new DBAppException("There is already an Index on this column");
            }




        Table myTable = (Table) Table.deserialize(strTableName);


        //create index
        int fanout = ConfigReader.getInstance().readInteger("MaximumRowsCountinPage");
        bplustree index = new bplustree(fanout);
        try {
            filereader = new FileReader("resources/metadata.csv");
            csvReader = new CSVReader(filereader);
            List<String[]> lines = new ArrayList<>();
            String[] nextLine;
            while ((nextLine = csvReader.readNext()) != null) {
                String temp = nextLine[0] + "," + nextLine[1] + "," + nextLine[2] + "," + nextLine[3] + "," + nextLine[4] + "," + nextLine[5];
                if (temp.equals(targetline)) {
                    nextLine = editedline.split(",");
                }
                lines.add(nextLine);
            }
            csvReader.close();
            CSVWriter writer = new CSVWriter(new FileWriter("resources/metadata.csv"));
            writer.writeAll(lines);
            writer.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!myTable.isEmpty()) {
            for (String pageAddress : myTable.getPagesAddresses()) {
                Page page = null;
                try {

                    FileInputStream fileIn = new FileInputStream(pageAddress);
                    ObjectInputStream in = new ObjectInputStream(fileIn);
                    page = (Page) in.readObject();
                    for (Tuple t : page.getRecords()) {
                        Hashtable<String, Object> c = t.getContent();
//						!TODO: check how will you handle dups
                        index.insert((Comparable) c.get(strColName), pageAddress);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        index.serialize(strIndexName);
    }

    // following method inserts one row only.
    // htblColNameValue must include a value for the primary key
    public void insertIntoTable(String strTableName,
                                Hashtable<String, Object> htblColNameValue) throws DBAppException, IOException, ClassNotFoundException {

        // !TODO validate inserted table content is compatible with the desired table
        // !TODO update the coresponding indicies

        List<List<String>> columnsWithIndex = MetaDataManger.getInstance().getColumnsWithIndex(strTableName);
        ArrayList<bplustree> indices = new ArrayList<>();
        List<String> columnNames = columnsWithIndex.get(0);
        List<String> indexNames = columnsWithIndex.get(1);
        for (String indexName : indexNames) {
            indices.add(bplustree.deserialize(indexName));
        }

        Table table = Table.deserialize(strTableName);
        Tuple tuple = new Tuple(htblColNameValue, table.getClusteringKey());

        if (table.isEmpty()) {
            Page newPage = table.createNewPage(tuple.getClusteringKeyValue());
            newPage.serialize(table.getTargetPageInfo(tuple.getClusteringKeyValue()).getPageAddress());
        }

        PageInfo targetPageInfo = table.getTargetPageInfo(tuple.getClusteringKeyValue());
        Page targetPage = null;

        while (targetPageInfo != null) {
            targetPage = Page.deserialize(targetPageInfo.getPageAddress());
            if (!targetPage.isFull()) break;
            tuple = insertIntoFullPage(table, targetPageInfo, targetPage, tuple ,columnNames,indices);
            targetPageInfo = table.getNextPageInfo(targetPageInfo);
        }

        if (targetPageInfo == null) {
            targetPage = table.createNewPage(tuple.getClusteringKeyValue());
            targetPageInfo = table.getTargetPageInfo(tuple.getClusteringKeyValue());
        } else
            targetPage = Page.deserialize(targetPageInfo.getPageAddress());

        insertIntoIndices(indices,columnNames,tuple,targetPageInfo.getPageAddress());
        targetPage.insert(tuple);
        targetPage.serialize(targetPageInfo.getPageAddress());
        table.serialize();
        for(int i=0;i<indices.size();i++){
            indices.get(i).serialize(indexNames.get(i));
        }

    }



    public Tuple insertIntoFullPage(Table table, PageInfo pageInfo, Page page, Tuple tuple,List<String> columnNames,ArrayList<bplustree> indices)
            throws DBAppException, IOException {
//        we need to update the index from the old index and update to the new index
        if (page.getRecords().lastElement().compareTo(tuple) > 0) {
            //delete from the old index
            insertIntoIndices(indices,columnNames,tuple,pageInfo.getPageAddress());
            table.updatePageInfoMinKey(pageInfo, tuple.getClusteringKeyValue());
            tuple = page.swapRecords(tuple.clone(), page.getRecords().size() - 1);
            deleteFromIndices(indices,columnNames,tuple,pageInfo.getPageAddress());
            page.serialize(pageInfo.getPageAddress());
        }
        return tuple;
    }

    private void insertIntoIndices(ArrayList<bplustree> indices, List<String> columnNames , Tuple record ,String pageAddress){
        for (int i = 0; i < indices.size(); i++) {
            bplustree index = indices.get(i);
            String columnName = columnNames.get(i);
            Hashtable<String, Object> content = record.getContent();
            index.insert((Comparable) content.get(columnName),pageAddress);
        }
    }

    private void deleteFromIndices(ArrayList<bplustree> indices, List<String> columnNames , Tuple record ,String pageAddress){
        for (int i = 0; i < indices.size(); i++) {
            bplustree index = indices.get(i);
            String columnName = columnNames.get(i);
            Hashtable<String, Object> content = record.getContent();
            index.deleteFromPage((Comparable) content.get(columnName),pageAddress);
        }
    }

    // following method updates one row only
    // htblColNameValue holds the key and new value
    // htblColNameValue will not include clustering key as column name
    // strClusteringKeyValue is the value to look for to find the row to update.
    public void updateTable(String strTableName,
                            String strClusteringKeyValue,
                            Hashtable<String, Object> htblColNameValue) throws DBAppException {

        throw new DBAppException("not implemented yet");
    }


    // following method could be used to delete one or more rows.
    // htblColNameValue holds the key and value. This will be used in search
    // to identify which rows/tuples to delete.
    // htblColNameValue enteries are ANDED together
    public void deleteFromTable(String strTableName,
                                Hashtable<String, Object> htblColNameValue) throws DBAppException, IOException, ClassNotFoundException {
        //make sure when deleting a ceratin tuple check if there is any index on another column and if there is go and deserialize this bplus tree and call delete with the
        //page of the tuple being deleted

        //Whenever delete is called and there is an index on a column call it with the page the tuple is in

        //!TODO validate inserted table content is compatible with the desired table
        List<List<String>> columnsWithIndex = MetaDataManger.getInstance().getColumnsWithIndex(strTableName);
        List<String> columnNamesClone = new ArrayList<>();
        List<String> indexNames = columnsWithIndex.get(1);
        List<String> columnNames = columnsWithIndex.get(0);
        for(String columnName : columnNames){
            columnNamesClone.add(columnName);
        }
        for(String colName : columnNames){
            if(htblColNameValue.containsKey(colName)){
                bplustree index = bplustree.deserialize(indexNames.get(columnNames.indexOf(colName)));
                indexNames.remove(columnNames.indexOf(colName));
                columnNamesClone.remove(colName);
                deleteWithIndex(htblColNameValue,index,indexNames,colName,columnNamesClone,strTableName);
                index.serialize(columnsWithIndex.get(1).get(columnsWithIndex.get(0).indexOf(colName)));
                return;
            }
        }
        deleteWithoutIndex(htblColNameValue,strTableName,columnsWithIndex);
    }

    private void deleteWithIndex(Hashtable<String, Object> htblColNameValue, bplustree index,List<String> indexNames, String colName, List<String> columnNames,String tableName) throws IOException, ClassNotFoundException {
        ArrayList<bplustree> indices = new ArrayList<>();
        boolean deserializeFlag = false;
        Comparable minKey = null;
        HashMap<String,Integer> map = index.search((Comparable) htblColNameValue.get(colName));
        for(String pageAddress : map.keySet()){
            Page page = Page.deserialize(pageAddress);
            Vector<Tuple> records = (Vector<Tuple>) page.getRecords().clone();
            for(Tuple tuple : records){
                boolean satisfied = true;
                for(String column : htblColNameValue.keySet()) {
                    if (!tuple.getContent().get(column).equals(htblColNameValue.get(column))) {
                        satisfied = false;
                        break;
                    }
                }
                if(satisfied) {
                    Tuple temp = page.getRecords().firstElement();
                    page.delete(tuple);
                    if (!deserializeFlag) {
                        for (String indexName : indexNames) {
                            indices.add(bplustree.deserialize(indexName));
                        }
                        indices.add(index);
                        columnNames.add(colName);
                        deserializeFlag = true;
                    }
                    deleteFromIndices(indices,columnNames,tuple,pageAddress);
                    minKey = tuple.getClusteringKeyValue();
                    if(tuple.equals(temp) && !page.getRecords().isEmpty()){
                        Comparable newMinKey = page.getRecords().firstElement().getClusteringKeyValue();
                        Comparable oldMinKey = temp.getClusteringKeyValue();
                        Table table = Table.deserialize(tableName);
                        table.updatePageInfoMinKey(pageAddress,oldMinKey,newMinKey);
                        table.serialize();
                    }
                }
            }
            if(!page.isEmpty())
                page.serialize(pageAddress);
            else{
                //To delete page from disk
                System.out.println(pageAddress);
                File file = new File("serialized/pages/"+pageAddress+".class");
                file.delete();
               //Deleting page from the table object
                Table table = Table.deserialize(tableName);
                table.deletePage(pageAddress,minKey);
                table.serialize();
            }
        }
        for(int i=0;i<indices.size()-1;i++){
            indices.get(i).serialize(indexNames.get(i));
        }
    }

    private void deleteWithoutIndex(Hashtable<String, Object> htblColNameValue, String tableName,List<List<String>> columnsWithIndex) throws IOException, ClassNotFoundException {
        Table table = Table.deserialize(tableName);
        ArrayList<bplustree> indices = new ArrayList<>();
        for (String indexName : columnsWithIndex.get(1)) {
            indices.add(bplustree.deserialize(indexName));
        }
        if (htblColNameValue.containsKey(table.getClusteringKey())) {
            PageInfo targetPageInfo = table.getTargetPageInfo((Comparable) htblColNameValue.get(table.getClusteringKey()));
            if (targetPageInfo != null) {
                Page targetPage = Page.deserialize(targetPageInfo.getPageAddress());
                Tuple temp = targetPage.getRecords().firstElement();
                List<Tuple> tempRecords = (Vector<Tuple>) targetPage.getRecords().clone();
                for (Tuple tuple : tempRecords) {
                    boolean satisfied = true;
                    for (String column : htblColNameValue.keySet()) {
                        if (!tuple.getContent().get(column).equals(htblColNameValue.get(column))) {
                            satisfied = false;
                            break;
                        }
                    }
                    if (satisfied) {
                        targetPage.delete(tuple);
                        deleteFromIndices(indices, columnsWithIndex.get(0), tuple, targetPageInfo.getPageAddress());
                        if (tuple.equals(temp) && !targetPage.isEmpty()) {
                            Comparable newMinKey = targetPage.getRecords().firstElement().getClusteringKeyValue();
                            table.updatePageInfoMinKey(targetPageInfo, newMinKey);

                        }
                    }
                    if (!targetPage.isEmpty())
                        targetPage.serialize(targetPageInfo.getPageAddress());
                    else {
                        //To delete page from disk
                        File file = new File(targetPageInfo.getPageAddress());
                        file.delete();
                        //Deleting page from the table object
                        table.deletePage(targetPageInfo.getPageAddress(), (Comparable) htblColNameValue.get(table.getClusteringKey()));
                    }
                }
            } else {
                ArrayList<String> pageAddresses = table.getPagesAddresses();
                for(String pageAddress : pageAddresses){
                    Page page = Page.deserialize(pageAddress);
                    Comparable oldMinKey = null;
                    for(Tuple tuple : page.getRecords()){
                        boolean satisfied = true;
                        for(String column : htblColNameValue.keySet()){
                            if(!tuple.getContent().get(column).equals(htblColNameValue.get(column))){
                                satisfied = false;
                                break;
                            }
                        }
                        if(satisfied){
                            Tuple temp = page.getRecords().firstElement();
                            page.delete(tuple);
                            oldMinKey = tuple.getClusteringKeyValue();
                            deleteFromIndices(indices,columnsWithIndex.get(0),tuple,pageAddress);
                            if(tuple.equals(temp) && !page.isEmpty()){
                                Comparable newMinKey = page.getRecords().firstElement().getClusteringKeyValue();
                                table.updatePageInfoMinKey(pageAddress,oldMinKey,newMinKey);
                            }
                        }
                    }
                    if (!page.isEmpty())
                        page.serialize(pageAddress);
                    else {
                        //To delete page from disk
                        File file = new File("serialized/pages/" + pageAddress + ".class");
                        file.delete();
                        //Deleting page from the table object
                        table.deletePage(pageAddress, oldMinKey);
                    }
                }
            }
            for(int i=0;i<indices.size();i++){
                indices.get(i).serialize(columnsWithIndex.get(1).get(i));
            }
            table.serialize();
        }
    }

    public Iterator selectFromTable(SQLTerm[] arrSQLTerms,
                                    String[] strarrOperators) throws DBAppException, IOException, ClassNotFoundException {



        //TODO validate the input


        SelectionHandler selectionHandler = new SelectionHandler(arrSQLTerms[0]._strTableName);
        Stack<Object> operands = new Stack<>();
        Stack<SQLOperator> operators = new Stack<>();

        operands.push(arrSQLTerms[0]);

        for(int i=1;i< arrSQLTerms.length;i++){
            SQLOperator operator=SQLOperator.valueOf(strarrOperators[i-1]);
            if(!operators.isEmpty() && operators.peek().compareTo(operator)<0){
                SQLOperator topOperator = operators.peek();
                while(!operators.isEmpty() && topOperator.compareTo(operator)<0){
                    ArrayList<Object> tempOperands = new ArrayList<>();
                    tempOperands.add(operands.pop());
                    while(!operators.isEmpty() && operators.peek().compareTo(topOperator)==0){
                        tempOperands.add(operands.pop());
                        operators.pop();
                    }
                    operands.push(selectionHandler.process(tempOperands,topOperator));
                }
                operators.push(operator);
            }
            else operators.push(operator);
            operands.push(arrSQLTerms[i]);
        }


        while(!operators.isEmpty()){
            ArrayList<Object> tempOperands = new ArrayList<>();
            SQLOperator topOperator = operators.peek();
            tempOperands.add(operands.pop());
            while(!operators.isEmpty() && operators.peek().compareTo(topOperator)==0){
                tempOperands.add(operands.pop());
                operators.pop();
            }
            operands.push(selectionHandler.process(tempOperands,topOperator));
        }

        Object result=operands.pop();
        if (result instanceof SQLTerm) result=selectionHandler.process((SQLTerm) result);
        return ((TreeSet<Tuple>)result).iterator();
    }


    public static void main(String[] args) {
//		storage.bplustree b = new storage.bplustree();

        try {
            String strTableName = "Student";
            DBApp dbApp = new DBApp();

//            Hashtable htblColNameType = new Hashtable();
//            htblColNameType.put("id", "java.lang.Integer");
//            htblColNameType.put("name", "java.lang.String");
//            htblColNameType.put("gpa", "java.lang.double");
//            dbApp.createTable(strTableName, "id", htblColNameType);
//            dbApp.createIndex(strTableName, "gpa", "gpaIndex");
//
            Hashtable htblColNameValue = new Hashtable();
//            htblColNameValue.put("id", 1);
//            htblColNameValue.put("name","Ahmed Noor");
//            htblColNameValue.put("gpa", 0.95);
//            dbApp.insertIntoTable(strTableName, htblColNameValue);
//
//            htblColNameValue.clear();
//            htblColNameValue.put("id", 2);
//            htblColNameValue.put("name", "Ahmed Noor");
//            htblColNameValue.put("gpa",0.95);
//            dbApp.insertIntoTable(strTableName, htblColNameValue);
//
//            htblColNameValue.clear();
//            htblColNameValue.put("id", 3);
//            htblColNameValue.put("name","Dalia Noor");
//            htblColNameValue.put("gpa", 1.25);
//            dbApp.insertIntoTable(strTableName, htblColNameValue);
//
//            htblColNameValue.clear();
//            htblColNameValue.put("id", 5);
//            htblColNameValue.put("name","Zaky Noor");
//            htblColNameValue.put("gpa", 0.88);
//            dbApp.insertIntoTable(strTableName, htblColNameValue);
//
//
//            htblColNameValue.clear();
//            htblColNameValue.put("id", 4);
//            htblColNameValue.put("name", "John Noor");
//            htblColNameValue.put("gpa", 1.5);
//            dbApp.insertIntoTable(strTableName, htblColNameValue);
//
//            htblColNameValue.clear();
//            htblColNameValue.put("id", 3);
//
//
//            dbApp.deleteFromTable(strTableName, htblColNameValue);
//
            var dalia = bplustree.deserialize("gpaIndex");
//
//            System.out.println(Page.deserialize("serialized/pages/Student3.class"));
//            System.out.println(Page.deserialize("serialized/pages/Student4.class"));



            app.SQLTerm[] arrSQLTerms;
            arrSQLTerms = new app.SQLTerm[3];
            for (int i = 0; i < 3; i++) {
                arrSQLTerms[i] = new app.SQLTerm();
            }
            arrSQLTerms[0]._strTableName = "Student";
            arrSQLTerms[0]._strColumnName = "gpa";
            arrSQLTerms[0]._strOperator = "=";
            arrSQLTerms[0]._objValue = 0.95;

            arrSQLTerms[1]._strTableName = "Student";
            arrSQLTerms[1]._strColumnName = "name";
            arrSQLTerms[1]._strOperator = "=";
            arrSQLTerms[1]._objValue = "Ahmed Noor";

            arrSQLTerms[2]._strTableName = "Student";
            arrSQLTerms[2]._strColumnName = "id";
            arrSQLTerms[2]._strOperator = "=";
            arrSQLTerms[2]._objValue = 3;



            String[] strarrOperators = new String[2];
            strarrOperators[0] = "AND";
            strarrOperators[1] = "OR";
            // select * from Student where name = "John Noor" or gpa = 1.5;
            Iterator resultSet = dbApp.selectFromTable(arrSQLTerms, strarrOperators);
            while (resultSet.hasNext()) {
                Tuple t = (Tuple) resultSet.next();
                System.out.println(t);
            }
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

}