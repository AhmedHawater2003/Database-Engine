package helpers;

import app.SQLTerm;
import storage.*;

import java.io.IOException;
import java.util.*;


public class RecordsFetcher {

    public static HashSet<Tuple> fetchWithIndex(SQLTerm sqlTerm, String indexName) throws IOException, ClassNotFoundException {
        bplustree index = bplustree.deserialize(indexName);
        String operator = sqlTerm._strOperator;
        HashSet <Tuple> result = new HashSet<>();
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
        index.serialize(indexName);
        return result;
    }

    //helper for "=" operator using index
    private static HashSet<Tuple> fetchEqual(SQLTerm sqlTerm,bplustree index) throws IOException, ClassNotFoundException {
        HashMap<String,Integer> map = index.search((Comparable) sqlTerm._objValue);
        HashSet<Tuple> result = new HashSet<>();
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
    private static HashSet<Tuple> fetchEqual(SQLTerm sqlTerm,Table table) throws IOException, ClassNotFoundException {
        HashSet<Tuple> result = new HashSet<>();
        if(sqlTerm._strColumnName.equals(table.getClusteringKey())){
            PageInfo pageInfo = table.getTargetPageInfo((Comparable) sqlTerm._objValue);
            Page page = Page.deserialize(pageInfo.getPageAddress());
            Vector <Tuple> records = page.getRecords();
            for(Tuple record : records){
                if(record.getContent().get(sqlTerm._strColumnName).equals(sqlTerm._objValue)) {
                    result.add(record);
                    break; //there can't be duplicates in the clustering key
                }
            }
            page.serialize(pageInfo.getPageAddress());
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
    private static HashSet<Tuple> fetchGreaterThan(SQLTerm sqlTerm,bplustree index,boolean equals) throws IOException, ClassNotFoundException {
        ArrayList<HashMap<String,Integer>> pages = index.searchGreaterThan((Comparable) sqlTerm._objValue,equals);
        HashSet<Tuple> result = new HashSet<>();
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
    private static HashSet<Tuple> fetchGreaterThan(SQLTerm sqlTerm,Table table,boolean equals) throws IOException, ClassNotFoundException {
        HashSet<Tuple> result = new HashSet<>();
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
    private static HashSet<Tuple> fetchLessThan(SQLTerm sqlTerm,bplustree index,boolean equals) throws IOException, ClassNotFoundException {
        ArrayList<HashMap<String,Integer>> pages = index.searchLessThan((Comparable) sqlTerm._objValue,equals);
        HashSet<Tuple> result = new HashSet<>();
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
    private static HashSet<Tuple> fetchLessThan(SQLTerm sqlTerm,Table table,boolean equals) throws IOException, ClassNotFoundException {
        HashSet<Tuple> result = new HashSet<>();
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
    private static HashSet<Tuple> fetchNotEqual(SQLTerm sqlTerm,Table table) throws IOException, ClassNotFoundException {
        HashSet<Tuple> result = new HashSet<>();
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

    public static HashSet<Tuple> fetchWithoutIndex(SQLTerm sqlTerm) throws IOException, ClassNotFoundException {
        HashSet<Tuple> result = new HashSet<>();
        Table table = (Table) Table.deserialize(sqlTerm._strTableName);
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
                result = fetchNotEqual(sqlTerm,table);
        }

        table.serialize();
        return result;

    }
}