
/**
 * @author Wael Abouelsaadat
 */

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import exceptions.DBAppException;
import helpers.ConfigReader;
import helpers.MetaDataColumns;
import helpers.MetaDataManger;
import storage.Table;

import java.io.*;
import storage.Page;
import storage.Table;
import storage.Tuple;
import storage.bplustree;

import java.io.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
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
	public void createIndex(String   strTableName,
							String   strColName,
							String   strIndexName) throws DBAppException{
		String targetline = "";
		String editedline = "";
		//check that the table exists with the correct naming
		try {

			FileReader filereader = new FileReader("metadata.csv");
			boolean tableFound = false , columnFound = false , indexValid = false;


			CSVReader csvReader = new CSVReader(filereader);
			String[] nextRecord;


			while ((nextRecord = csvReader.readNext()) != null) {

				if(nextRecord[0].equals(strTableName)){
					tableFound = true;
					if(nextRecord[1].equals(strColName)){
						columnFound=true;
						if(nextRecord[4].equals("null")){
							indexValid = true;
							//edit the csv and make nextRecord[4] = strIndexName and nextRecord[5] = "B+tree"
							targetline+=nextRecord[0]+","+nextRecord[1]+","+nextRecord[2]+","+nextRecord[3]+","+nextRecord[4]+","+nextRecord[5];
							nextRecord[4] = strIndexName;
							nextRecord[5] = "B+tree";
							editedline+=nextRecord[0]+","+nextRecord[1]+","+nextRecord[2]+","+nextRecord[3]+","+nextRecord[4]+","+nextRecord[5];
							// write to the actual csv file



							break;
						}
					}
				}
			}
			if(!tableFound){
				throw new DBAppException("Table Not Found!");
			}
			else if(!columnFound){
				throw new DBAppException("Column Not Found!");

			}
			else if(!indexValid){
				throw new DBAppException("There is already an Index on this column");
			}


		}
		catch (Exception e) {
			e.printStackTrace();
		}


		Table myTable = null;


		//load the table
		try {

			FileInputStream fileIn = new FileInputStream("serialized/tables/" + strTableName +".class");
			ObjectInputStream in = new ObjectInputStream(fileIn);


			myTable = (Table) in.readObject();

			in.close();
			fileIn.close();



		} catch (Exception i) {
			i.printStackTrace();
		}
		//create index
		int fanout = ConfigReader.getInstance().readInteger("MaximumRowsCountinPage");
		bplustree index = new bplustree(fanout);
		try{
			FileReader filereader = new FileReader("metadata.csv");

			CSVReader csvReader = new CSVReader(filereader);
			CSVWriter writer = new CSVWriter(new FileWriter("metadata.csv"));
			String[] nextLine;

			while ((nextLine = csvReader.readNext()) != null) {
				String temp = nextLine[0]+","+nextLine[1]+","+nextLine[2]+","+nextLine[3]+","+nextLine[4]+","+nextLine[5];
				if(temp.equals(targetline)){
					nextLine = editedline.split(",");
				}
				writer.writeNext(nextLine);
			}
			writer.close();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
        if(!myTable.isEmpty()) {
			for (String pageName : myTable.getPages()) {
				Page page = null;
				try {

					FileInputStream fileIn = new FileInputStream("serialized/pages/" + pageName + ".class");
					ObjectInputStream in = new ObjectInputStream(fileIn);
					page = (Page) in.readObject();
					for (Tuple t : page.getRecords()) {
						Hashtable<String,Object> c = t.getContent();
//						!TODO: check how will you handle dups
						index.insert((Comparable) c.get(strColName), pageName);
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
                                Hashtable<String, Object> htblColNameValue) throws DBAppException {

        throw new DBAppException("not implemented yet");
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
                                Hashtable<String, Object> htblColNameValue) throws DBAppException {

        throw new DBAppException("not implemented yet");
    }


    public Iterator selectFromTable(SQLTerm[] arrSQLTerms,
                                    String[] strarrOperators) throws DBAppException {

        return null;
    }


    public static void main(String[] args) {
//		storage.bplustree b = new storage.bplustree();

//        try {
//            String strTableName = "Student";
//            DBApp dbApp = new DBApp();
//
//            Hashtable htblColNameType = new Hashtable();
//            htblColNameType.put("id", "java.lang.Integer");
//            htblColNameType.put("name", "java.lang.String");
//            htblColNameType.put("gpa", "java.lang.double");
//            dbApp.createTable(strTableName, "id", htblColNameType);
//            dbApp.createIndex(strTableName, "gpa", "gpaIndex");
//
//            Hashtable htblColNameValue = new Hashtable();
//            htblColNameValue.put("id", new Integer(2343432));
//            htblColNameValue.put("name", new String("Ahmed Noor"));
//            htblColNameValue.put("gpa", new Double(0.95));
//            dbApp.insertIntoTable(strTableName, htblColNameValue);
//
//            htblColNameValue.clear();
//            htblColNameValue.put("id", new Integer(453455));
//            htblColNameValue.put("name", new String("Ahmed Noor"));
//            htblColNameValue.put("gpa", new Double(0.95));
//            dbApp.insertIntoTable(strTableName, htblColNameValue);
//
//            htblColNameValue.clear();
//            htblColNameValue.put("id", new Integer(5674567));
//            htblColNameValue.put("name", new String("Dalia Noor"));
//            htblColNameValue.put("gpa", new Double(1.25));
//            dbApp.insertIntoTable(strTableName, htblColNameValue);
//
//            htblColNameValue.clear();
//            htblColNameValue.put("id", new Integer(23498));
//            htblColNameValue.put("name", new String("John Noor"));
//            htblColNameValue.put("gpa", new Double(1.5));
//            dbApp.insertIntoTable(strTableName, htblColNameValue);
//
//            htblColNameValue.clear();
//            htblColNameValue.put("id", new Integer(78452));
//            htblColNameValue.put("name", new String("Zaky Noor"));
//            htblColNameValue.put("gpa", new Double(0.88));
//            dbApp.insertIntoTable(strTableName, htblColNameValue);
//
//
//            SQLTerm[] arrSQLTerms;
//            arrSQLTerms = new SQLTerm[2];
//            arrSQLTerms[0]._strTableName = "Student";
//            arrSQLTerms[0]._strColumnName = "name";
//            arrSQLTerms[0]._strOperator = "=";
//            arrSQLTerms[0]._objValue = "John Noor";
//
//            arrSQLTerms[1]._strTableName = "Student";
//            arrSQLTerms[1]._strColumnName = "gpa";
//            arrSQLTerms[1]._strOperator = "=";
//            arrSQLTerms[1]._objValue = new Double(1.5);
//
//            String[] strarrOperators = new String[1];
//            strarrOperators[0] = "OR";
//            // select * from Student where name = "John Noor" or gpa = 1.5;
//            Iterator resultSet = dbApp.selectFromTable(arrSQLTerms, strarrOperators);
//        } catch (Exception exp) {
//            exp.printStackTrace();
//        }

        var dbApp = new DBApp();
        Hashtable htblColNameType = new Hashtable();
        htblColNameType.put("id", "java.lang.Integer");
        htblColNameType.put("name", "java.lang.String");
        htblColNameType.put("gpa", "java.lang.double");
        try {
            dbApp.createTable("Student", "id", htblColNameType);
        } catch (DBAppException | IOException e) {
            e.printStackTrace();

        }
    }

}