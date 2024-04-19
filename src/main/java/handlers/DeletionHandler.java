package handlers;

import exceptions.DBAppException;
import helpers.ConfigReader;
import helpers.MetaDataManger;
import storage.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DeletionHandler {
    private Table table;
    private List<String> columnNames,indexNames;


    public DeletionHandler(Table table) throws IOException {
        this.table = table;
        List<List<String>> columnsWithIndex = MetaDataManger.getInstance().getColumnsWithIndex(table.getTableName());
        this.columnNames = columnsWithIndex.get(0);
        this.indexNames = columnsWithIndex.get(1);
    }
    public void deleteAll() throws IOException {
        ArrayList<String> pageAddresses = table.getPagesAddresses();
        for(String pageAddress : pageAddresses){
            File file = new File(pageAddress);
            file.delete();
        }
        TreeSet<PageInfo> pagesInfo = new TreeSet<>();
        table.setPagesInfo(pagesInfo);
        List<String> indices = MetaDataManger.getInstance().getColumnsWithIndex(table.getTableName()).get(1);
        for(String indexName : indices){
            File file = new File("serialized/indices/"+table.getTableName()+"_"+indexName+".class");
            file.delete();
            bplustree index = new bplustree(ConfigReader.getInstance().readInteger("MaximumRowsCountinPage")+1);
            index.serialize(indexName,table.getTableName());
        }
        table.serialize();
    }

    public void delete(Hashtable<String, Object> htblColNameValue) throws IOException, ClassNotFoundException, DBAppException {
        List<String> columnNamesClone = new ArrayList<>(columnNames);
        for (String colName : columnNames) {
            if (htblColNameValue.containsKey(colName)) {
                bplustree index = bplustree.deserialize(indexNames.get(columnNames.indexOf(colName)), table.getTableName());
                String indexName = indexNames.remove(columnNames.indexOf(colName));
                columnNamesClone.remove(colName);
                deleteWithIndex(htblColNameValue, index, indexNames, colName, columnNamesClone, table);
                index.serialize(indexName, table.getTableName());
                return;
            }
        }
        deleteWithoutIndex(htblColNameValue, table);
    }

    private void deleteWithIndex(Hashtable<String, Object> htblColNameValue, bplustree index,List<String> indexNames, String colName, List<String> columnNames,Table table) throws IOException, ClassNotFoundException {
        ArrayList<bplustree> indices = new ArrayList<>();
        boolean deserializeFlag = false;
        Comparable minKey = null;
        HashMap<String,Integer> map = index.search((Comparable) htblColNameValue.get(colName));
        if(map!=null){
            HashMap<String, Integer> tempMap = (HashMap<String, Integer>) map.clone();
            for (String pageAddress : tempMap.keySet()) {
                Page page = Page.deserialize(pageAddress);
                Vector<Tuple> records = (Vector<Tuple>) page.getRecords().clone();
                for (Tuple tuple : records) {
                    boolean satisfied = true;
                    for (String column : htblColNameValue.keySet()) {
                        if (!tuple.getContent().get(column).equals(htblColNameValue.get(column))) {
                            satisfied = false;
                            break;
                        }
                    }
                    if (satisfied) {
                        Tuple temp = page.getRecords().firstElement();
                        page.delete(tuple);
                        if (!deserializeFlag) {
                            for (String indexName : indexNames) {
                                indices.add(bplustree.deserialize(indexName, table.getTableName()));
                            }
                            indices.add(index);
                            columnNames.add(colName);
                            deserializeFlag = true;
                        }
                        deleteFromIndices(indices, columnNames, tuple, pageAddress);
                        minKey = tuple.getClusteringKeyValue();
                        if (tuple.equals(temp) && !page.getRecords().isEmpty()) {
                            Comparable newMinKey = page.getRecords().firstElement().getClusteringKeyValue();
                            Comparable oldMinKey = temp.getClusteringKeyValue();
                            table.updatePageInfoMinKey(pageAddress, oldMinKey, newMinKey);
                            table.serialize();
                        }
                    }
                }
                if (!page.isEmpty())
                    page.serialize(pageAddress);
                else {
                    //To delete page from disk
                    File file = new File(pageAddress);
                    file.delete();
                    //Deleting page from the table object
                    table.deletePage(pageAddress, minKey);
                    table.serialize();
                }
            }
            for (int i = 0; i < indices.size() - 1; i++) {
                indices.get(i).serialize(indexNames.get(i), table.getTableName());
            }
        }
    }

    private void deleteWithoutIndex(Hashtable<String, Object> htblColNameValue, Table table) throws IOException, ClassNotFoundException, DBAppException {
        ArrayList<bplustree> indices = new ArrayList<>();
        for (String indexName : indexNames) {
            indices.add(bplustree.deserialize(indexName, table.getTableName()));
        }
        if (htblColNameValue.containsKey(table.getClusteringKey())) {
            PageInfo targetPageInfo = table.getTargetPageInfo((Comparable) htblColNameValue.get(table.getClusteringKey()));
            if (targetPageInfo != null) {
                boolean satisfied = true;
                Page targetPage = Page.deserialize(targetPageInfo.getPageAddress());
                Tuple temp = targetPage.getRecords().firstElement();
                Tuple tuple = targetPage.getRecordBS(table.getClusteringKey(), (Comparable) htblColNameValue.get(table.getClusteringKey()));
                if(tuple == null) return;
                for (String column : htblColNameValue.keySet()) {
                    if (!tuple.getContent().get(column).equals(htblColNameValue.get(column))) {
                        satisfied = false;
                        break;
                    }
                }
                if (satisfied) {
                    targetPage.delete(tuple);
                    deleteFromIndices(indices, columnNames, tuple, targetPageInfo.getPageAddress());
                    if (tuple.equals(temp) && !targetPage.isEmpty()) {
                        Comparable newMinKey = targetPage.getRecords().firstElement().getClusteringKeyValue();
                        table.updatePageInfoMinKey(targetPageInfo.getPageAddress(),targetPageInfo.getMinKey(), newMinKey);

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
        }
        else {
            ArrayList<String> pageAddresses = table.getPagesAddresses();
            for (String pageAddress : pageAddresses) {
                Page page = Page.deserialize(pageAddress);
                Vector<Tuple> records = (Vector<Tuple>) page.getRecords().clone();
                Comparable oldMinKey = null;
                for (Tuple tuple : records) {
                    boolean satisfied = true;
                    for (String column : htblColNameValue.keySet()) {
                        if (!tuple.getContent().get(column).equals(htblColNameValue.get(column))) {
                            satisfied = false;
                            break;
                        }
                    }
                    if (satisfied) {
                        Tuple temp = page.getRecords().firstElement();
                        page.delete(tuple);
                        oldMinKey = tuple.getClusteringKeyValue();
                        deleteFromIndices(indices, columnNames, tuple, pageAddress);
                        if (tuple.equals(temp) && !page.isEmpty()) {
                            Comparable newMinKey = page.getRecords().firstElement().getClusteringKeyValue();
                            table.updatePageInfoMinKey(pageAddress, oldMinKey, newMinKey);
                        }
                    }
                }
                if (!page.isEmpty())
                    page.serialize(pageAddress);
                else {
                    //To delete page from disk
                    File file = new File(pageAddress);
                    file.delete();
                    //Deleting page from the table object
                    table.deletePage(pageAddress, oldMinKey);
                }
            }
        }
        for (int i = 0; i < indices.size(); i++) {
            indices.get(i).serialize(indexNames.get(i), table.getTableName());
        }
        table.serialize();
    }

    private void deleteFromIndices(ArrayList<bplustree> indices, List<String> columnNames , Tuple record ,String pageAddress){
        for (int i = 0; i < indices.size(); i++) {
            bplustree index = indices.get(i);
            String columnName = columnNames.get(i);
            Hashtable<String, Object> content = record.getContent();
            index.deleteFromPage((Comparable) content.get(columnName),pageAddress);
        }
    }

}
