package validators;

import app.SQLTerm;
import exceptions.DBAppException;
import helpers.MetaDataColumns;
import helpers.MetaDataManger;

import java.io.IOException;
import java.util.Hashtable;


public class Validator {



    public static void validateTableCreation(String strTableName,String strClusteringKeyColumn,Hashtable<String, String> htblColNameType) throws DBAppException, IOException {

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

        for(String colName : htblColNameType.keySet()){
            if (colName == null) {
                throw new DBAppException("Column name can't be null");
            }
            if (htblColNameType.get(colName) == null) {
                throw new DBAppException("Column type can't be null");
            }
            if(!isValidType(htblColNameType.get(colName))){
                throw new DBAppException("Invalid column type");
            }
        }


    }

    public static void validateIndexCreation(String strTableName,String strColName,String strIndexName) throws DBAppException, IOException {

            if(strTableName == null){
                throw new DBAppException("Table name can't be null");
            }
            if(strColName == null){
                throw new DBAppException("Column name can't be null");
            }
            if(strIndexName == null){
                throw new DBAppException("Index name can't be null");
            }
            if(!tableNameExist(strTableName)){
                throw new DBAppException("Table doesn't exist");
            }
            if(indexNameExist(strTableName,strColName)){
                throw new DBAppException("Column doesn't exist in table");
            }
    }

    public static void validateInsertion(String strTableName,Hashtable<String, Object> htblColNameValue) throws DBAppException, IOException {

            if(strTableName == null){
                throw new DBAppException("Table name can't be null");
            }
            if(htblColNameValue == null){
                throw new DBAppException("Can't insert with no columns");
            }
            if(!tableNameExist(strTableName)){
                throw new DBAppException("Table doesn't exist");
            }
            //TODO validate inserted table content is compatible with the desired table
    }

    public static void validateUpdate(String strTableName, String strClusteringKeyValue, Hashtable<String, Object> htblColNameValue) throws DBAppException, IOException {

        if(strTableName == null){
            throw new DBAppException("Table name can't be null");
        }
        if(strClusteringKeyValue == null){
            throw new DBAppException("Clustering key value can't be null");
        }
        if(htblColNameValue == null){
            throw new DBAppException("Can't update with no columns");
        }
        if(!tableNameExist(strTableName)){
            throw new DBAppException("Table doesn't exist");
        }

        //TODO validate inserted table content is compatible with the desired table
        //TODO validate clustering key exists
    }

    public static void validateDelete(){}

    public static void validateSelect(SQLTerm[] arrSQLTerms,String[] strarrOperators) throws DBAppException, IOException {
        if(arrSQLTerms == null || strarrOperators == null){
            throw new DBAppException("Can't select with no terms or operators");
        }
        if(arrSQLTerms.length != strarrOperators.length + 1){
            throw new DBAppException("Invalid number of terms or operators");
        }
        for(SQLTerm term : arrSQLTerms)validateSQLTerm(term);
        for(String operator : strarrOperators){
            if(!isValidOperator(operator)){
                throw new DBAppException("Invalid operator");
            }
        }
        //TODO validate the input(no null, no empty strings, operators=terms-1)
    }

    public static void validateSQLTerm(SQLTerm term) throws DBAppException, IOException {
        if(term._strTableName == null){
            throw new DBAppException("Table name can't be null");
        }
        if(term._strColumnName == null){
            throw new DBAppException("Column name can't be null");
        }
        if(term._strOperator == null){
            throw new DBAppException("Operator can't be null");
        }
        if(term._objValue == null){
            throw new DBAppException("Value can't be null");
        }
        if(!isValidSQLTermOperator(term._strOperator)){
            throw new DBAppException("Invalid operator");
        }
        if(!tableNameExist(term._strTableName)){
            throw new DBAppException("Table doesn't exist");
        }
        //TODO: check if column exists in table
        //TODO: check if column type same as value type
    }


    private static boolean tableNameExist(String strTableName) throws IOException {
        return MetaDataManger.getInstance().exists(MetaDataColumns.TABLE_NAME, strTableName);
    }
    private static boolean indexNameExist(String strTableName,String strIndexName) throws IOException {
//        return MetaDataManger.getInstance().exists(MetaDataColumns.INDEX_NAME, strIndexName);
        return false;
    }
    private static boolean isValidType(String type){
        if(type.equals("java.lang.Integer") || type.equals("java.lang.Double") || type.equals("java.lang.String")) return true;
        return false;
    }
    private static boolean isValidSQLTermOperator(String operator){
        if(operator.equals("=") || operator.equals("!=") || operator.equals(">") || operator.equals("<") || operator.equals(">=") || operator.equals("<=")
                ||operator.equals("=<")||operator.equals("=<")) return true; //TODO: I added the last two because I'm not sure if the user will enter the operator correctly
        return false;
    }
    private static boolean isValidOperator(String operator){
        if(operator.equals("AND") || operator.equals("OR") || operator.equals("XOR")) return true;
        return false;
    }
//    private static boolean isColumnExist(String tableName, String columnName) throws IOException {
//        return MetaDataManger.getInstance().existsInTable(MetaDataColumns.COLUMN_NAME, columnName, tableName);
//    }
//    private static boolean isColumnCompatibleWithTable(String tableName, String columnName, Object value) throws IOException{
//
//
//
//    }


}
