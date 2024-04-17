package validators;

import app.SQLTerm;
import exceptions.DBAppException;
import helpers.MetaDataColumns;
import helpers.MetaDataManger;
import storage.Page;
import storage.PageInfo;
import storage.Table;
import storage.Tuple;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;


public class Validator {


    public static void validateTableCreation(String strTableName, String strClusteringKeyColumn, Hashtable<String, String> htblColNameType) throws DBAppException, IOException {

        if (strTableName == null) {
            throw new DBAppException("Table name can't be null");
        }

        if (strClusteringKeyColumn == null) {
            throw new DBAppException("Clustering key column name can't be null");
        }

        if (htblColNameType == null) {
            throw new DBAppException("Can't create table with no columns");
        }

        if (tableNameExist(strTableName)) {
            throw new DBAppException("Table already exists");
        }

        if (!htblColNameType.containsKey(strClusteringKeyColumn)) {
            throw new DBAppException("Clustering key column must be in the columns list");
        }

        for (String colName : htblColNameType.keySet()) {
            if (colName == null) {
                throw new DBAppException("Column name can't be null");
            }
            if (htblColNameType.get(colName) == null) {
                throw new DBAppException("Column type can't be null");
            }
            if (!isValidType(htblColNameType.get(colName))) {
                throw new DBAppException("Invalid column type");
            }
        }


    }

    public static void validateIndexCreation(String strTableName, String strColName, String strIndexName) throws DBAppException, IOException {
        if (strTableName == null) {
            throw new DBAppException("Table name can't be null");
        }
        if (strColName == null) {
            throw new DBAppException("Column name can't be null");
        }
        if (strIndexName == null) {
            throw new DBAppException("Index name can't be null");
        }
    }

    public static void validateInsertion(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException, IOException, ClassNotFoundException {

        if (strTableName == null) {
            throw new DBAppException("Table name can't be null");
        }
        if (htblColNameValue == null) {
            throw new DBAppException("Can't insert with no columns");
        }
        if (!tableNameExist(strTableName)) {
            throw new DBAppException("Table doesn't exist");
        }
        checkColNameValue(strTableName, htblColNameValue, true);

    }

    //    public static void validateClusteringKeyValue(Table table, Tuple tuple) throws IOException, ClassNotFoundException, DBAppException {
//        if(!table.isEmpty()) {
//            PageInfo targetPageInfo = table.getTargetPageInfo(tuple.getClusteringKeyValue());
//
//            Page page = Page.deserialize(targetPageInfo.getPageAddress());
//            if (page.getRecordBS(table.getClusteringKey(), tuple.getClusteringKeyValue()) != null) {
//                page.serialize(targetPageInfo.getPageAddress());
//                throw new DBAppException("Duplicate entry '" + tuple.getClusteringKeyValue() + "' for key " + table.getClusteringKey());
//            }
//
//
//            page.serialize(targetPageInfo.getPageAddress());
//            if(table.getNextPageInfo(targetPageInfo)!=null){
//                targetPageInfo = table.getNextPageInfo(targetPageInfo);
//                page.deserialize(targetPageInfo.getPageAddress());
//                if (page.getRecordBS(table.getClusteringKey(), tuple.getClusteringKeyValue()) != null) {
//                    page.serialize(targetPageInfo.getPageAddress());
//                    throw new DBAppException("Duplicate entry '" + tuple.getClusteringKeyValue() + "' for key " + table.getClusteringKey());
//                }
//                page.serialize(targetPageInfo.getPageAddress());
//            }
//
//        }
//    }
//
    public static void validateClusteringKeyValue(Page page, Tuple tuple, String clusteringName, String pageAddress) throws DBAppException, IOException {
        if (page.getRecordBS(clusteringName, tuple.getClusteringKeyValue()) != null) {
            page.serialize(pageAddress);
            throw new DBAppException("Duplicate entry '" + tuple.getClusteringKeyValue() + "' for key " + clusteringName);
        }
    }

    public static void validateUpdate(String strTableName, String strClusteringKeyValue, Hashtable<String, Object> htblColNameValue) throws DBAppException, IOException {

        if (strTableName == null) {
            throw new DBAppException("Table name can't be null");
        }
        if (strClusteringKeyValue == null) {
            throw new DBAppException("Clustering key value can't be null");
        }
        if (htblColNameValue == null) {
            throw new DBAppException("Can't update with no columns");
        }
        if (!tableNameExist(strTableName)) {
            throw new DBAppException("Table doesn't exist");
        }
        checkColNameValue(strTableName, htblColNameValue, false);


    }

    public static void validateDelete(String strTableName, Hashtable<String, Object> htblColNameValue) throws DBAppException, IOException, ClassNotFoundException {
        if (strTableName == null) {
            throw new DBAppException("Table name can't be null");
        }
        if (htblColNameValue == null) {
            throw new DBAppException("Can't delete with no columns");
        }
        if (!tableNameExist(strTableName)) {
            throw new DBAppException("Table doesn't exist");
        }
        checkColNameValue(strTableName, htblColNameValue, false);


    }

    public static void validateSelect(SQLTerm[] arrSQLTerms, String[] strarrOperators) throws DBAppException, IOException {
        if (arrSQLTerms == null || strarrOperators == null) {
            throw new DBAppException("Can't select with no terms or operators");
        }
        if ((arrSQLTerms.length != strarrOperators.length + 1)) {
            throw new DBAppException("Invalid number of terms or operators");
        }

        String tableName = arrSQLTerms[0]._strTableName;

        List<List<String>> tmp = MetaDataManger.getInstance().readTableInfo(tableName,new MetaDataColumns[]{MetaDataColumns.COLUMN_NAME,MetaDataColumns.COLUMN_TYPE});
        HashMap<String, String> colNameType = new HashMap<>();
        for(int i = 0; i<tmp.get(0).size(); i++){
            colNameType.put(tmp.get(0).get(i), tmp.get(1).get(i));
        }

        for (int i = 0 ; i<arrSQLTerms.length; i++)
            validateSQLTerm(arrSQLTerms[i]);
        for (String operator : strarrOperators) {
            if (!isValidOperator(operator)) {
                throw new DBAppException("Invalid operator");
            }
        }
        //TODO validate the input(no null, no empty strings, operators=terms-1)
    }

    public static void validateSQLTerm(SQLTerm term) throws DBAppException, IOException {
        if (term._strTableName == null) {
            throw new DBAppException("Table name can't be null");
        }
        if (term._strColumnName == null) {
            throw new DBAppException("Column name can't be null");
        }
        if (term._strOperator == null) {
            throw new DBAppException("Operator can't be null");
        }
        if (term._objValue == null) {
            throw new DBAppException("Value can't be null");
        }
        if (!isValidSQLTermOperator(term._strOperator)) {
            throw new DBAppException("Invalid operator");
        }
        if (!tableNameExist(term._strTableName)) {
            throw new DBAppException("Table doesn't exist");
        }

        String colType = MetaDataManger.getInstance().getColumnType(term._strTableName, term._strColumnName);
        if (colType.equals("java.lang.Double")) {
            if (term._objValue instanceof Integer) {
                Double tmp = (Integer) term._objValue + 0.0;
                term._objValue = tmp;
            }
        }
        if (!MetaDataManger.getInstance().existsInTable(MetaDataColumns.COLUMN_NAME, term._strColumnName, term._strTableName)) {
            throw new DBAppException("Column: '" + term._strColumnName + "' doesn't exist in table '" + term._strTableName + "'");
        } else if (term._objValue == null) {
            throw new DBAppException("Column: '" + term._strColumnName + "' value can't be null");
        } else if (!term._objValue.getClass().getName().equals(MetaDataManger.getInstance().getColumnType(term._strTableName, term._strColumnName))) {
            throw new DBAppException("Column: '" + term._strColumnName + "' value type doesn't match the column type in table '" + term._strTableName + "'");
        }
        //TODO: check if column exists in table

    }


    private static boolean tableNameExist(String strTableName) throws IOException {
        return MetaDataManger.getInstance().exists(MetaDataColumns.TABLE_NAME, strTableName);
    }

    private static boolean indexNameExist(String strTableName, String strIndexName) throws IOException {
        return MetaDataManger.getInstance().exists(MetaDataColumns.INDEX_NAME, strIndexName);
    }

    private static boolean isValidType(String type) {
        if (type.equals("java.lang.Integer") || type.equals("java.lang.Double") || type.equals("java.lang.String"))
            return true;
        return false;
    }

    private static boolean isValidSQLTermOperator(String operator) {
        if (operator.equals("=") || operator.equals("!=") || operator.equals(">") || operator.equals("<") || operator.equals(">=") || operator.equals("<=")
                || operator.equals("=<") || operator.equals("=<"))
            return true; //TODO: I added the last two because I'm not sure if the user will enter the operator correctly
        return false;
    }

    private static boolean isValidOperator(String operator) {
        if (operator.equals("AND") || operator.equals("OR") || operator.equals("XOR")) return true;
        return false;
    }

    private static void checkColNameValue(String tableName, Hashtable<String, Object> htblColNameValue, Boolean checkForNullsInInsert) throws IOException, DBAppException {
        List<List<String>> tmp = MetaDataManger.getInstance().readTableInfo(tableName,new MetaDataColumns[]{MetaDataColumns.COLUMN_NAME,MetaDataColumns.COLUMN_TYPE});
        HashMap<String, String> colNameType = new HashMap<>();
        for(int i = 0; i<tmp.get(0).size(); i++){
            colNameType.put(tmp.get(0).get(i), tmp.get(1).get(i));
        }

        for (String colName : htblColNameValue.keySet()) {
            if (colNameType.containsKey(colName)) {
                throw new DBAppException("Column: '" + colName + "' doesn't exist in table '" + tableName + "'");
            } else if (htblColNameValue.get(colName) == null) {
                throw new DBAppException("Column: '" + colName + "' value can't be null");
            } else if (!htblColNameValue.get(colName).getClass().getName().equals(colNameType.get(colName))) {
                throw new DBAppException("Column: '" + colName + "' value type doesn't match the column type in table '" + tableName + "'");
            }
        }
        if (checkForNullsInInsert) {
            if (htblColNameValue.size() != MetaDataManger.getInstance().getColumnsCount(tableName)) {
                throw new DBAppException("Can't insert NULL values for any column");
            }
        }
    }
}
//    private static boolean isColumnExist(String tableName, String columnName) throws IOException {
//        return MetaDataManger.getInstance().existsInTable(MetaDataColumns.COLUMN_NAME, columnName, tableName);
//    }
//    private static boolean isColumnCompatibleWithTable(String tableName, String columnName, Object value) throws IOException{
//
//
//
//    }



