package helpers;

import app.SQLTerm;
import exceptions.DBAppException;
import storage.*;

import java.io.IOException;
import java.util.*;


public class RecordsFetcher {

    public static TreeSet<Tuple> fetchWithIndex(SQLTerm sqlTerm, String indexName) throws IOException, ClassNotFoundException, DBAppException {
        bplustree index = bplustree.deserialize(indexName,sqlTerm._strTableName);
        String operator = sqlTerm._strOperator;
        TreeSet <Tuple> result = new TreeSet<>();

        switch (operator){
            case "=":
                result = fetchEqual(sqlTerm,index); break;
            case ">":
                result = fetchGreaterThan(sqlTerm,index,false); break;
            case "<":
                result = fetchLessThan(sqlTerm,index,false); break;
            case ">=":
                result = fetchGreaterThan(sqlTerm,index,true); break;
            case "<=":
                result = fetchLessThan(sqlTerm,index,true); break;
            case "!=":
                result = fetchWithoutIndex(sqlTerm);
        }
        index.serialize(indexName,sqlTerm._strTableName);
        return result;
    }

    //helper for "=" operator using index
    private static TreeSet<Tuple> fetchEqual(SQLTerm sqlTerm,bplustree index) throws IOException, ClassNotFoundException {
        HashMap<String,Integer> map = index.search((Comparable) sqlTerm._objValue);
        TreeSet<Tuple> result = new TreeSet<>();

        if(map == null) return result; // handle if key is not found in the index

        for(String pageAddress : map.keySet()){
            Page page = Page.deserialize(pageAddress);
            Vector <Tuple> records = page.getRecords();
            for(Tuple record : records){
                if(record.getContent().get(sqlTerm._strColumnName).equals(sqlTerm._objValue)) {
                    result.add(record);
                }
            }
            page.serialize(pageAddress);
        }
        return result;
    }

    //helper for "=" operator without index
    private static TreeSet<Tuple> fetchEqual(SQLTerm sqlTerm,Table table) throws IOException, ClassNotFoundException, DBAppException {
        TreeSet<Tuple> result = new TreeSet<>();
        if(sqlTerm._strColumnName.equals(table.getClusteringKey())){
            PageInfo pageInfo = table.getTargetPageInfo((Comparable) sqlTerm._objValue);
            if(pageInfo != null) {
                Page page = Page.deserialize(pageInfo.getPageAddress());
                Tuple record = page.getRecordBS(table.getClusteringKey(), (Comparable) sqlTerm._objValue);
                if(record != null) result.add(record);
                page.serialize(pageInfo.getPageAddress());
            }
            return result;
        }
        for(String pageAddress : table.getPagesAddresses()){
            Page page = Page.deserialize(pageAddress);
            Vector <Tuple> records = page.getRecords();
            for(Tuple record : records){
                if(record.getContent().get(sqlTerm._strColumnName).equals(sqlTerm._objValue)) {
                    result.add(record);
                }
            }
            page.serialize(pageAddress);
        }
        return result;
    }

    //helper for ">"/">=" operator using index
    private static TreeSet<Tuple> fetchGreaterThan(SQLTerm sqlTerm,bplustree index,boolean equals) throws IOException, ClassNotFoundException {
        ArrayList<HashMap<String,Integer>> pages = index.searchGreaterThan((Comparable) sqlTerm._objValue,equals);
        TreeSet<Tuple> result = new TreeSet<>();
        int eq = equals ? -1 : 0;
            for(HashMap<String,Integer> map : pages){
                for(String pageAddress : map.keySet()){
                    Page page = Page.deserialize(pageAddress);
                    Vector <Tuple> records = page.getRecords();
                    for(Tuple record : records){
                        if(((Comparable)record.getContent().get(sqlTerm._strColumnName)).compareTo((Comparable) sqlTerm._objValue) > eq) {
                            result.add(record);
                        }
                    }
                    page.serialize(pageAddress);
                }
        }
        return result;
    }

    //helper for ">"/">=" operator without index
    private static TreeSet<Tuple> fetchGreaterThan(SQLTerm sqlTerm,Table table,boolean equals) throws IOException, ClassNotFoundException {
        TreeSet<Tuple> result = new TreeSet<>();
        int eq = equals ? -1 : 0;
        int startIndex = 0;
        ArrayList<String> pages = table.getPagesAddresses();
        if(sqlTerm._strColumnName.equals(table.getClusteringKey())) {
            PageInfo pageInfo = table.getTargetPageInfo((Comparable) sqlTerm._objValue);
            startIndex = pages.indexOf(pageInfo.getPageAddress());
        }
        for(int i = startIndex; i < pages.size(); i++){
            String pageAddress = pages.get(i);
            Page page = Page.deserialize(pageAddress);
            Vector <Tuple> records = page.getRecords();
            for(Tuple record : records){
                if(((Comparable)record.getContent().get(sqlTerm._strColumnName)).compareTo((Comparable) sqlTerm._objValue) > eq) {
                    result.add(record);
                }
            }
            page.serialize(pageAddress);
        }
        return result;
    }

    //helper for "<"/"<=" operator using index
    private static TreeSet<Tuple> fetchLessThan(SQLTerm sqlTerm,bplustree index,boolean equals) throws IOException, ClassNotFoundException {
        ArrayList<HashMap<String,Integer>> pages = index.searchLessThan((Comparable) sqlTerm._objValue,equals);
        TreeSet<Tuple> result = new TreeSet<>();
        int eq = equals ? 1 : 0;
        for(HashMap<String,Integer> map : pages){
            for(String pageAddress : map.keySet()){
                Page page = Page.deserialize(pageAddress);
                Vector <Tuple> records = page.getRecords();
                for(Tuple record : records){
                    if(((Comparable)record.getContent().get(sqlTerm._strColumnName)).compareTo((Comparable) sqlTerm._objValue) < eq) {
                        result.add(record);
                    }
                }
                page.serialize(pageAddress);
            }
        }
        return result;
    }

    //helper for "<"/"<=" operator without index
    private static TreeSet<Tuple> fetchLessThan(SQLTerm sqlTerm,Table table,boolean equals) throws IOException, ClassNotFoundException {
        TreeSet<Tuple> result = new TreeSet<>();
        int eq = equals ? 1 : 0;
        boolean done = false;
        for(String pageAddress : table.getPagesAddresses()){
            Page page = Page.deserialize(pageAddress);
            Vector <Tuple> records = page.getRecords();
            for(Tuple record : records){
                if(((Comparable)record.getContent().get(sqlTerm._strColumnName)).compareTo((Comparable) sqlTerm._objValue) < eq) {
                    result.add(record);
                }
                else if(sqlTerm._strColumnName.equals(table.getClusteringKey())&&((Comparable)record.getContent().get(sqlTerm._strColumnName)).compareTo((Comparable) sqlTerm._objValue) >= eq){
                    done = true;
                    break;
                }
            }
            page.serialize(pageAddress);
            if(done) break;
        }
        return result;
    }

    //helper for "!=" operator
    private static TreeSet<Tuple> fetchNotEqual(SQLTerm sqlTerm,Table table) throws IOException, ClassNotFoundException {
        TreeSet<Tuple> result = new TreeSet<>();
        for(String pageAddress : table.getPagesAddresses()){
            Page page = Page.deserialize(pageAddress);
            Vector <Tuple> records = page.getRecords();
            for(Tuple record : records){
                if(!record.getContent().get(sqlTerm._strColumnName).equals(sqlTerm._objValue)) {
                    result.add(record);
                }
            }
            page.serialize(pageAddress);
        }
        return result;
    }

    public static TreeSet<Tuple> fetchWithoutIndex(SQLTerm sqlTerm) throws IOException, ClassNotFoundException, DBAppException {
        TreeSet<Tuple> result = new TreeSet<>();
        //TODO : validate the table name & sqlTerm parameters
        Table table = (Table) Table.deserialize(sqlTerm._strTableName);
        if(table.isEmpty())return result;
        String operator = sqlTerm._strOperator;

        switch (operator){
            case "=":
                result = fetchEqual(sqlTerm,table); break;
            case ">":
                result = fetchGreaterThan(sqlTerm,table,false); break;
            case "<":
                result = fetchLessThan(sqlTerm,table,false); break;
            case ">=":
                result = fetchGreaterThan(sqlTerm,table,true); break;
            case "<=":
                result = fetchLessThan(sqlTerm,table,true); break;
            case "!=":
                result = fetchNotEqual(sqlTerm,table);break;
        }

        table.serialize();
        return result;

    }
}