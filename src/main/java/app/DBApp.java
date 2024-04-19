package app;
/**
 * @author Wael Abouelsaadat
 */

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import exceptions.DBAppException;
import handlers.DeletionHandler;
import handlers.SQLOperator;
import handlers.SelectionHandler;
import helpers.ConfigReader;
import helpers.MetaDataColumns;
import helpers.MetaDataManger;
import storage.Page;
import storage.PageInfo;
import storage.Table;
import storage.Tuple;

import java.io.*;

import storage.bplustree;
import validators.Validator;

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
                            Hashtable<String, String> htblColNameType) throws DBAppException {

        try {
            Validator.validateTableCreation(strTableName, strClusteringKeyColumn, htblColNameType);

            Table table = new Table(strTableName, strClusteringKeyColumn, htblColNameType);
            table.serialize();
        }
        catch (Exception e) {
            throw new DBAppException(e.getMessage());
        }
    }


    // following method creates a B+tree index
    public void createIndex(String strTableName, String strColName, String strIndexName) throws DBAppException {
        try {
            Validator.validateIndexCreation(strTableName, strColName, strIndexName);

            FileReader filereader = new FileReader("resources/metadata.csv");
            boolean tableFound = false, columnFound = false, indexValid = false;

            CSVReader csvReader = new CSVReader(filereader);
            String[] nextRecord;
            ArrayList<String[]> lines = new ArrayList<>();

            while ((nextRecord = csvReader.readNext()) != null) {
                lines.add(nextRecord);
                if (nextRecord[0].equals(strTableName)) {
                    tableFound = true;
                    if (nextRecord[1].equals(strColName)) {
                        columnFound = true;
                        if (nextRecord[4].equals("null")) {
                            indexValid = true;
                            nextRecord[4] = strIndexName;
                            nextRecord[5] = "B+tree";
                            lines.remove(lines.size() - 1);
                            lines.add(nextRecord);
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
            } else {
                CSVWriter writer = new CSVWriter(new FileWriter("resources/metadata.csv"));
                writer.writeAll(lines);
                writer.close();
            }

            Table myTable = (Table) Table.deserialize(strTableName);

            //create index
            int fanout = ConfigReader.getInstance().readInteger("MaximumRowsCountinPage");
//            String colType = MetaDataManger.getInstance().getColumnType(strTableName, strColName);
            bplustree index = new bplustree(fanout + 1);
            if (!myTable.isEmpty()) {
                for (String pageAddress : myTable.getPagesAddresses()) {
                    Page page = null;
                    try {
                        FileInputStream fileIn = new FileInputStream(pageAddress);
                        ObjectInputStream in = new ObjectInputStream(fileIn);
                        page = (Page) in.readObject();
                        for (Tuple t : page.getRecords()) {
                            Hashtable<String, Object> c = t.getContent();
                            index.insert((Comparable) c.get(strColName), pageAddress);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            index.serialize(strIndexName, strTableName);
        } catch (Exception e) {
             throw new DBAppException(e.getMessage());
//            e.printStackTrace();
        }
    }

    private void adjustTupleInsertions(Hashtable<String,Object> htbColNameValue , String strTableName) throws IOException {
        List<List<String>> columnNameType = MetaDataManger.getInstance().readTableInfo(strTableName, new MetaDataColumns[]{MetaDataColumns.COLUMN_NAME, MetaDataColumns.COLUMN_TYPE});
        List<String> columnNames = columnNameType.get(0);
        List<String> columnTypes = columnNameType.get(1);
        for (int i = 0; i < columnTypes.size(); i++) {
            if(columnTypes.get(i).equals("java.lang.Double")){
                if(htbColNameValue.get(columnNames.get(i)) instanceof Integer){
                    Double tmp = (Integer) htbColNameValue.get(columnNames.get(i)) + 0.0;
                    htbColNameValue.put(columnNames.get(i),tmp);
                }
            }
        }
    }

    // following method inserts one row only.
    // htblColNameValue must include a value for the primary key
    public void insertIntoTable(String strTableName,
                                Hashtable<String, Object> htblColNameValue) throws DBAppException, IOException, ClassNotFoundException {

        // !TODO validate inserted table content is compatible with the desired table

        adjustTupleInsertions(htblColNameValue,strTableName);
        Validator.validateInsertion(strTableName, htblColNameValue);

        List<List<String>> columnsWithIndex = MetaDataManger.getInstance().getColumnsWithIndex(strTableName);
        ArrayList<bplustree> indices = new ArrayList<>();
        List<String> columnNames = columnsWithIndex.get(0);
        List<String> indexNames = columnsWithIndex.get(1);
        for (String indexName : indexNames) {
            indices.add(bplustree.deserialize(indexName, strTableName));
        }

        Table table = Table.deserialize(strTableName);
        Tuple tuple = new Tuple(htblColNameValue, table.getClusteringKey());
        //validation of clustering key value


        if (table.isEmpty()) {
            Page newPage = table.createNewPage(tuple.getClusteringKeyValue());
            newPage.serialize(table.getTargetPageInfo(tuple.getClusteringKeyValue()).getPageAddress());
        }

        PageInfo targetPageInfo = table.getTargetPageInfo(tuple.getClusteringKeyValue());
        Page targetPage = null;

        while (targetPageInfo != null) {
            targetPage = Page.deserialize(targetPageInfo.getPageAddress());
            Validator.validateClusteringKeyValue(targetPage,tuple,table.getClusteringKey(),targetPageInfo.getPageAddress());
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
        table.updatePageInfoMinKey(targetPageInfo, tuple.getClusteringKeyValue());
        targetPage.serialize(targetPageInfo.getPageAddress());
        table.serialize();
        for(int i=0;i<indices.size();i++){
            indices.get(i).serialize(indexNames.get(i),strTableName);
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
    public void updateTable(String strTableName, String strClusteringKeyValue, Hashtable<String, Object> htblColNameValue) throws DBAppException, IOException, ClassNotFoundException {


        Validator.validateUpdate(strTableName, strClusteringKeyValue, htblColNameValue);
        Table table = Table.deserialize(strTableName);
        if(table.isEmpty()){
            return;  //throw new DBAppException("Table is empty");
        }
        if(htblColNameValue.containsKey(table.getClusteringKey())) {
            throw new DBAppException("Clustering key must NOT be in the columns list");
        }

        HashMap<String, String> columnWithIndex = new HashMap<>();
        List<List<String>> indexList = MetaDataManger.getInstance().getColumnsWithIndex(strTableName);
        for (int i = 0; i < indexList.get(0).size(); i++) {
            columnWithIndex.put(indexList.get(0).get(i), indexList.get(1).get(i));
        }

        Comparable castedValue = getCastedClusteringKeyValue(strTableName, strClusteringKeyValue);

        PageInfo pageInfo = table.getTargetPageInfo(castedValue);
        Page targetPage = Page.deserialize(pageInfo.getPageAddress());

        Tuple targetTuple = targetPage.getRecordBS(table.getClusteringKey(), castedValue);
        if(targetTuple==null) return; //throw new DBAppException("There is no such tuple with the entered clustering key");
        Hashtable<String, Object> content = targetTuple.getContent();

        for (String key : htblColNameValue.keySet()) {

            Comparable oldValue = (Comparable) content.get(key);
            content.put(key, htblColNameValue.get(key));

            if (columnWithIndex.containsKey(key)) {
                bplustree index = bplustree.deserialize(columnWithIndex.get(key), strTableName);
                index.deleteFromPage(oldValue, pageInfo.getPageAddress());
                index.insert((Comparable) content.get(key), pageInfo.getPageAddress());

                index.serialize(columnWithIndex.get(key), strTableName);
            }
        }

        targetPage.serialize(pageInfo.getPageAddress());

    }

    public Comparable getCastedClusteringKeyValue(String tableName, String clusteringKeyValue) throws IOException, DBAppException {
        String clusteringKeyType = MetaDataManger.getInstance().readTableInfo(tableName, new MetaDataColumns[]{MetaDataColumns.COLUMN_TYPE}, strings -> strings[MetaDataColumns.IS_CLUSTERING_KEY.ordinal()].equals("True"), false).get(0).get(0);

        switch (clusteringKeyType) {
            case "java.lang.Integer":
                return Integer.parseInt(clusteringKeyValue);
            case "java.lang.Double":
                return Double.parseDouble(clusteringKeyValue);
            case "java.lang.String":
                return clusteringKeyValue;
            default:
                throw new DBAppException("Invalid data type");
        }
    }

    // following method could be used to delete one or more rows.
    // htblColNameValue holds the key and value. This will be used in search
    // to identify which rows/tuples to delete.
    // htblColNameValue enteries are ANDED together
    public void deleteFromTable(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException {
        try {
            adjustTupleInsertions(htblColNameValue,strTableName);
            Validator.validateDelete(strTableName, htblColNameValue);
            Table table = Table.deserialize(strTableName);
            DeletionHandler deletionHandler = new DeletionHandler(table);
            if(htblColNameValue==null || htblColNameValue.isEmpty()){
                deletionHandler.deleteAll();
                return;
            }
            if(table.isEmpty()){
                return;
            }
            deletionHandler.delete(htblColNameValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Iterator selectFromTable(SQLTerm[] arrSQLTerms,
                                    String[] strarrOperators) throws DBAppException, IOException, ClassNotFoundException {


        Validator.validateSelect(arrSQLTerms, strarrOperators);

        SelectionHandler selectionHandler = new SelectionHandler(arrSQLTerms[0]._strTableName);
        Stack<Object> operands = new Stack<>();
        Stack<SQLOperator> operators = new Stack<>();

        operands.push(arrSQLTerms[0]);
        if(strarrOperators.length!=0) {
            for (int i = 1; i < arrSQLTerms.length; i++) {
                SQLOperator operator = SQLOperator.valueOf(strarrOperators[i - 1]);
                if (!operators.isEmpty() && operators.peek().compareTo(operator) < 0) {
                    while (!operators.isEmpty() && operators.peek().compareTo(operator) < 0) {
                        SQLOperator topOperator = operators.peek();
                        ArrayList<Object> tempOperands = new ArrayList<>();
                        tempOperands.add(operands.pop());
                        while (!operators.isEmpty() && operators.peek().compareTo(topOperator) == 0) {
                            tempOperands.add(operands.pop());
                            operators.pop();
                        }
                        operands.push(selectionHandler.process(tempOperands, topOperator));
                    }
                    operators.push(operator);
                } else operators.push(operator);
                operands.push(arrSQLTerms[i]);
            }


            while (!operators.isEmpty()) {
                ArrayList<Object> tempOperands = new ArrayList<>();
                SQLOperator topOperator = operators.peek();
                tempOperands.add(operands.pop());
                while (!operators.isEmpty() && operators.peek().compareTo(topOperator) == 0) {
                    tempOperands.add(operands.pop());
                    operators.pop();
                }
                operands.push(selectionHandler.process(tempOperands, topOperator));
            }
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
//
//            Hashtable htblColNameType = new Hashtable( );
//            htblColNameType.put("id", "java.lang.Integer");
//            htblColNameType.put("name", "java.lang.String");
//            htblColNameType.put("gpa", "java.lang.Double");
//            dbApp.createTable( strTableName, "id", htblColNameType );
//            dbApp.createIndex( strTableName, "gpa", "gpaIndex" );
//            dbApp.createIndex( strTableName, "name", "nameIndex" );
//            bplustree index = bplustree.deserialize("gpaIndex", "Student");
//            System.out.println(index);
//            htblColNameType.put("gpa", "java.lang.Double");
//            dbApp.createTable( strTableName, "id", htblColNameType );
//            dbApp.createIndex( strTableName, "gpa", "gpaIndex" );
//
//            System.out.println(Page.deserialize("serialized/pages/Student0.class"));
//            System.out.println(Page.deserialize("serialized/pages/Student1.class"));
//            System.out.println(Page.deserialize("serialized/pages/Student2.class"));
            Hashtable htblColNameValue = new Hashtable();
//            htblColNameValue.put("id", 3 );
//            htblColNameValue.put("name", "khaled ahmed" );
//            htblColNameValue.put("gpa" , 0.76);
//            dbApp.insertIntoTable( strTableName , htblColNameValue );
//            htblColNameValue.clear();
//            htblColNameValue.put("id", 5 );
//            htblColNameValue.put("name", "Ahmed Mohamed" );
//            htblColNameValue.put("gpa",  0.7 );
//            dbApp.insertIntoTable( strTableName , htblColNameValue );
//
//            htblColNameValue.clear();
//            htblColNameValue.put("id", 1 );
//            htblColNameValue.put("name", "Dalia Noor" );
//            htblColNameValue.put("gpa",  1.95 );
//            dbApp.insertIntoTable( strTableName , htblColNameValue );
//            htblColNameValue.clear();
//            htblColNameValue.put("id", 6);
//            htblColNameValue.put("name", "Dalia Mohamed" );
//            htblColNameValue.put("gpa",  1 );
//            dbApp.insertIntoTable( strTableName , htblColNameValue );
//
//            htblColNameValue.clear();
//            htblColNameValue.put("id", 2 );
//            htblColNameValue.put("name", "Ali Noor" );
//            htblColNameValue.put("gpa",  0.95 );
//            dbApp.insertIntoTable( strTableName , htblColNameValue );
//            htblColNameValue.clear();
//            htblColNameValue.put("id", 4 );
//            htblColNameValue.put("name", "Ali Mohamed" );
//            htblColNameValue.put("gpa",  3 );
//            dbApp.insertIntoTable( strTableName , htblColNameValue );
////
//            htblColNameValue.clear();
//            htblColNameValue.put("id", 7 );
//            htblColNameValue.put("name", "Omar Mohamed" );
//            htblColNameValue.put("gpa",  3.4 );
//            dbApp.insertIntoTable( strTableName , htblColNameValue );
//            htblColNameValue.clear();
//            htblColNameValue.put("id", 8 );
//            htblColNameValue.put("name", "Ahmed Mohamed" );
//            htblColNameValue.put("gpa",  0.7 );
//            dbApp.insertIntoTable( strTableName , htblColNameValue );

//            Table table = Table.deserialize(strTableName);
//
//            System.out.println(table.getPagesAddresses());
//            System.out.println(returnColumnTypes("Student","gpa"));
//
//            Page page = Page.deserialize("serialized/pages/Student11.class");
//            System.out.println(page);
//            htblColNameValue.clear();
            htblColNameValue.put("gpa", 1);
            dbApp.deleteFromTable("strTableName", htblColNameValue);


//             table = Table.deserialize(strTableName);
//
//             System.out.println(bplustree.deserialize("gpaIndex", "Student"));
//            System.out.println(table.getPagesAddresses());
//            System.out.println(table.isEmpty());

//            bplustree index = bplustree.deserialize("gpaIndex", "Student");
//            System.out.println(index);
//            bplustree index2 = bplustree.deserialize("nameIndex", "Student");
//            System.out.println(index2);
//

//            htblColNameValue.clear();
//            htblColNameValue.put("id", 5 );
//            htblColNameValue.put("name", "Dalia Mohamed");
//            htblColNameValue.put("gpa",  3.5 );
//            dbApp.insertIntoTable( strTableName , htblColNameValue );


//
//            htblColNameValue.clear( );
//            htblColNameValue.put("id", 5);
//            htblColNameValue.put("name", "Ali Mohamed");
//            htblColNameValue.put("gpa",  1.0);
//            dbApp.insertIntoTable( strTableName , htblColNameValue );

//            var page = Page.deserialize("serialized/pages/Student0.class");
//            System.out.println(page);
//            var page = Page.deserialize("serialized/pages/Student1.class");
//            System.out.println(page);
//            page = Page.deserialize("serialized/pages/Student2.class");
//            System.out.println(page);
//            page = Page.deserialize("serialized/pages/Student3.class");
//            System.out.println(page);
//            page = Page.deserialize("serialized/pages/Student4.class");
//            System.out.println(page);
//            System.out.println("---------------------------------------------------");
//
//            var index = bplustree.deserialize("gpaIndex", "Student");
//            System.out.println(page);
//            System.out.println(index);

//            dbApp.createIndex("Student", "name", "nameIndex");
//
//            htblColNameValue.clear( );
//            htblColNameValue.put("id", 23498);
//            htblColNameValue.put("name", "John Noor");
//            htblColNameValue.put("gpa",  1.5);
//            dbApp.insertIntoTable( strTableName , htblColNameValue );
//
//            htblColNameValue.clear( );
//            htblColNameValue.put("id", 78452 );
//            htblColNameValue.put("name", "Zaky Noor");
//            htblColNameValue.put("gpa",  0.88);
//            dbApp.insertIntoTable( strTableName , htblColNameValue );


//            SQLTerm[] arrSQLTerms;
//            arrSQLTerms = new SQLTerm[6];
//            for (int i = 0; i < 6; i++) {
//                arrSQLTerms[i] = new app.SQLTerm();
//            }
//            arrSQLTerms[0]._strTableName = "Student";
//            arrSQLTerms[0]._strColumnName = "name";
//            arrSQLTerms[0]._strOperator = "<=";
//            arrSQLTerms[0]._objValue = "John Noor";
//
////            arrSQLTerms[1]._strTableName = "Student";
////            arrSQLTerms[1]._strColumnName = "gpa";
////            arrSQLTerms[1]._strOperator = "=";
////            arrSQLTerms[1]._objValue = 1.5;
////
////            arrSQLTerms[2]._strTableName = "Student";
////            arrSQLTerms[2]._strColumnName = "gpa";
////            arrSQLTerms[2]._strOperator = ">";
////            arrSQLTerms[2]._objValue = 4.0;
////
////            arrSQLTerms[3]._strTableName = "Student";
////            arrSQLTerms[3]._strColumnName = "gpa";
////            arrSQLTerms[3]._strOperator = "!=";
////            arrSQLTerms[3]._objValue = 3.0;
////
////            arrSQLTerms[4]._strTableName = "Student";
////            arrSQLTerms[4]._strColumnName = "gpa";
////            arrSQLTerms[4]._strOperator = "<";
////            arrSQLTerms[4]._objValue = 4.0;
////
////            arrSQLTerms[5]._strTableName = "Student";
////            arrSQLTerms[5]._strColumnName = "id";
////            arrSQLTerms[5]._strOperator = ">";
////            arrSQLTerms[5]._objValue = 3;
//
//            String[] strarrOperators = new String[0];
////            strarrOperators[0] = "OR";
////            strarrOperators[1] = "AND";
////            strarrOperators[2] = "AND";
////            strarrOperators[3] = "AND";
////            strarrOperators[4] = "XOR";
//
//            // select * from Student where name = "John Noor" or gpa = 1.5;
//            Iterator resultSet = dbApp.selectFromTable(arrSQLTerms, strarrOperators);
//            while (resultSet.hasNext()) {
//                Tuple t = (Tuple) resultSet.next();
//                System.out.println(t);
//            }
//
//            System.out.println("---------------------------------------------------");
//
//
//            htblColNameValue.clear();
//            htblColNameValue.put("name", "Ashraf Mansour11");
////            htblColNameValue.put("gpa", 8.0);
//            dbApp.updateTable("Student", "5674567", htblColNameValue);
//
//
//            resultSet = dbApp.selectFromTable(arrSQLTerms, strarrOperators);
//            while (resultSet.hasNext()) {
//                Tuple t = (Tuple) resultSet.next();
//                System.out.println(t);
//            }
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

}