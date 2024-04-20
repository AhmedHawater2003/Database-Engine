package app;
/**
 * @author A-Y TEAM
 */

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import exceptions.DBAppException;
import handlers.*;
import helpers.ConfigReader;
import helpers.MetaDataColumns;
import helpers.MetaDataManger;
import sql.SQLParser;
import storage.Page;
import storage.Table;
import storage.Tuple;
import storage.bplustree;
import validators.Validator;

import java.io.*;
import java.util.*;
//import java.lang.*;


public class DBApp {

    public DBApp() {
        this.init();
    }

    public static void main(String[] args) {

        try {

            DBApp dbApp = new DBApp();


            ArrayList<StringBuffer> buff = new ArrayList<>();


            String creteTable = "CREATE TABLE Student(id INT PRIMARY KEY,name VARCHAR,gpa double);";
            String createIndex = "CREATE INDEX gpaIndex ON Student(gpa)";
            String insert = "INSERT INTO Student (id,name,gpa) VALUES (10,'Ahd',0.7);";
            String delete = "DELETE FROM Student WHERE id = 10;";
            String update = "UPDATE Student SET gpa=4 WHERE id = 5;";
            String select = "SELECT * FROM Student WHERE name <= 'John Noor' AND gpa < 5;";
            String insert2 = "INSERT INTO Student (id,name,gpa) VALUES (5,'Ahd',0.7),(18,'Ziad Just',2.0);";
            String deleteAll = "DELETE FROM Student;";

            StringBuffer stringBuffer1 = new StringBuffer();
            StringBuffer stringBuffer2 = new StringBuffer();
            StringBuffer stringBuffer3 = new StringBuffer();
            StringBuffer stringBuffer4 = new StringBuffer();
            StringBuffer stringBuffer5 = new StringBuffer();
            StringBuffer stringBuffer6 = new StringBuffer();
            StringBuffer stringBuffer7 = new StringBuffer();
            StringBuffer stringBuffer8 = new StringBuffer();
            stringBuffer1.append(creteTable);
            stringBuffer2.append(createIndex);
            stringBuffer3.append(insert2);
            stringBuffer4.append(insert);
            stringBuffer5.append(delete);
            stringBuffer6.append(update);
            stringBuffer7.append(select);
            stringBuffer8.append(deleteAll);

            buff.add(stringBuffer1);
            buff.add(stringBuffer2);

            var saosan = dbApp.parseSQL(stringBuffer7);
            while(saosan.hasNext()) {
                System.out.println(saosan.next());
            }

        } catch (Exception exp) {
            exp.printStackTrace();
        }
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
    public void createTable(String strTableName, String strClusteringKeyColumn, Hashtable<String, String> htblColNameType) throws DBAppException {

        try {
            Validator.validateTableCreation(strTableName, strClusteringKeyColumn, htblColNameType);

            Table table = new Table(strTableName, strClusteringKeyColumn, htblColNameType);
            table.serialize();
        } catch (Exception e) {
            throw new DBAppException(e.getMessage());
        }
    }

    // following method creates a B+tree index
    public void createIndex(String strTableName, String strColName, String strIndexName) throws DBAppException {
        final int TABLE_NAME = 0;
        final int COLUMN_NAME = 1;
        final int INDEX_NAME = 4;
        final int INDEX_TYPE = 5;


        try {
            Validator.validateIndexCreation(strTableName, strColName, strIndexName);

            FileReader filereader = new FileReader("resources/metadata.csv");
            boolean tableFound = false, columnFound = false, indexValid = false;

            CSVReader csvReader = new CSVReader(filereader);
            String[] nextRecord;
            ArrayList<String[]> lines = new ArrayList<>();

            while ((nextRecord = csvReader.readNext()) != null) {
                lines.add(nextRecord);
                if (nextRecord[TABLE_NAME].equals(strTableName)) {
                    tableFound = true;
                    if (nextRecord[COLUMN_NAME].equals(strColName)) {
                        columnFound = true;
                        if (nextRecord[INDEX_NAME].equals("null")) {
                            indexValid = true;
                            nextRecord[INDEX_NAME] = strIndexName;
                            nextRecord[INDEX_TYPE] = "B+tree";
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

            Table myTable = Table.deserialize(strTableName);

            //create index
            int fanout = ConfigReader.getInstance().readInteger("MaximumRowsCountinPage");
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
        }
    }

    // following method inserts one row only.
    // htblColNameValue must include a value for the primary key
    public void insertIntoTable(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException, IOException, ClassNotFoundException {

        adjustTupleInsertions(htblColNameValue, strTableName);
        Validator.validateInsertion(strTableName, htblColNameValue);

        InsertionHandler insertionHandler = new InsertionHandler();
        insertionHandler.insert(strTableName, htblColNameValue);


    }

    // following method updates one row only
    // htblColNameValue holds the key and new value
    // htblColNameValue will not include clustering key as column name
    // strClusteringKeyValue is the value to look for to find the row to update.
    public void updateTable(String strTableName, String strClusteringKeyValue, Hashtable<String, Object> htblColNameValue) throws DBAppException, IOException, ClassNotFoundException {


        Validator.validateUpdate(strTableName, strClusteringKeyValue, htblColNameValue);
        Table table = Table.deserialize(strTableName);

        if (table.isEmpty()) {
            return;  //throw new DBAppException("Table is empty");
        }
        if (htblColNameValue.containsKey(table.getClusteringKey())) {
            throw new DBAppException("Clustering key must NOT be in the columns list");
        }

        UpdateHandler updateHandler = new UpdateHandler();
        updateHandler.update(table, strClusteringKeyValue, htblColNameValue);


    }

    // following method could be used to delete one or more rows.
    // htblColNameValue holds the key and value. This will be used in search
    // to identify which rows/tuples to delete.
    // htblColNameValue enteries are ANDED together
    public void deleteFromTable(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException {
        try {
            adjustTupleInsertions(htblColNameValue, strTableName);
            Validator.validateDelete(strTableName, htblColNameValue);
            Table table = Table.deserialize(strTableName);
            DeletionHandler deletionHandler = new DeletionHandler(table);
            if (htblColNameValue == null || htblColNameValue.isEmpty()) {
                deletionHandler.deleteAll();
                return;
            }
            if (table.isEmpty()) {
                return;
            }
            deletionHandler.delete(htblColNameValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Iterator selectFromTable(SQLTerm[] arrSQLTerms, String[] strarrOperators) throws DBAppException, IOException, ClassNotFoundException {


        Validator.validateSelect(arrSQLTerms, strarrOperators);

        SelectionHandler selectionHandler = new SelectionHandler(arrSQLTerms[0]._strTableName);
        Stack<Object> operands = new Stack<>();
        Stack<SQLOperator> operators = new Stack<>();

        // Using stacks with postfix notation to evaluate the expression
        operands.push(arrSQLTerms[0]);
        if (strarrOperators.length != 0) {
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

        Object result = operands.pop();
        if (result instanceof SQLTerm) result = selectionHandler.process((SQLTerm) result);
        return ((TreeSet<Tuple>) result).iterator();
    }

    // Convert integers to doubles for consistent type validation
    private void adjustTupleInsertions(Hashtable<String, Object> htbColNameValue, String strTableName) throws IOException {
        List<List<String>> columnNameType = MetaDataManger.getInstance().readTableInfo(strTableName, new MetaDataColumns[]{MetaDataColumns.COLUMN_NAME, MetaDataColumns.COLUMN_TYPE});
        List<String> columnNames = columnNameType.get(0);
        List<String> columnTypes = columnNameType.get(1);
        for (int i = 0; i < columnTypes.size(); i++) {
            if (columnTypes.get(i).equals("java.lang.Double")) {
                if (htbColNameValue.get(columnNames.get(i)) instanceof Integer) {
                    Double tmp = (Integer) htbColNameValue.get(columnNames.get(i)) + 0.0;
                    htbColNameValue.put(columnNames.get(i), tmp);
                }
            }
        }
    }

    public Iterator parseSQL(StringBuffer strbufSQL) throws DBAppException {
        SQLParser parser = new SQLParser(this);
        return parser.parse(strbufSQL);
    }

}