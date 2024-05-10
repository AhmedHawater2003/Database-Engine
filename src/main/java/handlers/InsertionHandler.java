package handlers;

import exceptions.DBAppException;
import helpers.MetaDataManger;
import storage.*;
import validators.Validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class InsertionHandler {

    public InsertionHandler() {
    }

    public void insert(String strTableName, Hashtable<String, Object> htblColNameValue ) throws DBAppException, IOException, ClassNotFoundException, DBAppException {
        List<List<String>> columnsWithIndex = MetaDataManger.getInstance().getColumnsWithIndex(strTableName);
        ArrayList<bplustree> indices = new ArrayList<>();
        List<String> columnNames = columnsWithIndex.get(0);
        List<String> indexNames = columnsWithIndex.get(1);
        for (String indexName : indexNames) {
            indices.add(bplustree.deserialize(indexName, strTableName));
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
            Validator.validateClusteringKeyValue(targetPage, tuple, table.getClusteringKey(), targetPageInfo.getPageAddress());
            if (!targetPage.isFull()) break;
            tuple = insertIntoFullPage(table, targetPageInfo, targetPage, tuple, columnNames, indices);
            targetPageInfo = table.getNextPageInfo(targetPageInfo);
        }

        if (targetPageInfo == null) {
            targetPage = table.createNewPage(tuple.getClusteringKeyValue());
            targetPageInfo = table.getTargetPageInfo(tuple.getClusteringKeyValue());
        } else targetPage = Page.deserialize(targetPageInfo.getPageAddress());

        insertIntoIndices(indices, columnNames, tuple, targetPageInfo.getPageAddress());
        targetPage.insert(tuple);
        table.updatePageInfoMinKey(targetPageInfo, tuple.getClusteringKeyValue());
        targetPage.serialize(targetPageInfo.getPageAddress());
        table.serialize();
        for (int i = 0; i < indices.size(); i++) {
            indices.get(i).serialize(indexNames.get(i), strTableName);
        }
    }

    public Tuple insertIntoFullPage(Table table, PageInfo pageInfo, Page page, Tuple tuple, List<String> columnNames, ArrayList<bplustree> indices) throws DBAppException, IOException {
//        we need to update the index from the old index and update to the new index
        if (page.getRecords().lastElement().compareTo(tuple) > 0) {
            //delete from the old index
            insertIntoIndices(indices, columnNames, tuple, pageInfo.getPageAddress());
            table.updatePageInfoMinKey(pageInfo, tuple.getClusteringKeyValue());
            tuple = page.swapRecords(tuple.clone(), page.getRecords().size() - 1);
            deleteFromIndices(indices, columnNames, tuple, pageInfo.getPageAddress());
            page.serialize(pageInfo.getPageAddress());
        }
        return tuple;
    }


    // TODO: move these two below methods to another place
    private void insertIntoIndices(ArrayList<bplustree> indices, List<String> columnNames, Tuple record, String pageAddress) {
        for (int i = 0; i < indices.size(); i++) {
            bplustree index = indices.get(i);
            String columnName = columnNames.get(i);
            Hashtable<String, Object> content = record.getContent();
            index.insert((Comparable) content.get(columnName), pageAddress);
        }
    }

    private void deleteFromIndices(ArrayList<bplustree> indices, List<String> columnNames, Tuple record, String pageAddress) {
        for (int i = 0; i < indices.size(); i++) {
            bplustree index = indices.get(i);
            String columnName = columnNames.get(i);
            Hashtable<String, Object> content = record.getContent();
            index.deleteFromPage((Comparable) content.get(columnName), pageAddress);
        }
    }

}
