package handlers;

import exceptions.DBAppException;
import helpers.MetaDataColumns;
import helpers.MetaDataManger;
import storage.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class UpdateHandler {

    public void update(Table table, String strClusteringKeyValue, Hashtable<String, Object> htblColNameValue) throws IOException, DBAppException, ClassNotFoundException {
        HashMap<String, String> columnWithIndex = getColumnWithIndex(table);

        Comparable castedValue = getCastedClusteringKeyValue(table.getTableName(), strClusteringKeyValue);

        PageInfo pageInfo = table.getTargetPageInfo(castedValue);
        Page targetPage = Page.deserialize(pageInfo.getPageAddress());

        Tuple targetTuple = targetPage.getRecordBS(table.getClusteringKey(), castedValue);
        if (targetTuple == null)
            return;

        Hashtable<String, Object> content = targetTuple.getContent();
        for (String key : htblColNameValue.keySet()) {
            Comparable oldValue = (Comparable) content.get(key);
            content.put(key, htblColNameValue.get(key));

            if (columnWithIndex.containsKey(key)) {
                bplustree index = bplustree.deserialize(columnWithIndex.get(key), table.getTableName());
                index.deleteFromPage(oldValue, pageInfo.getPageAddress());
                index.insert((Comparable) content.get(key), pageInfo.getPageAddress());

                index.serialize(columnWithIndex.get(key), table.getTableName());
            }
        }

        targetPage.serialize(pageInfo.getPageAddress());
    }

    private static HashMap<String, String> getColumnWithIndex(Table table) throws IOException {
        final int COLUMN_NAME = 0, INDEX_NAME = 1;
        HashMap<String, String> columnWithIndex = new HashMap<>();
        List<List<String>> indexList = MetaDataManger.getInstance().getColumnsWithIndex(table.getTableName());
        for (int i = 0; i < indexList.get(0).size(); i++) {
            columnWithIndex.put(indexList.get(COLUMN_NAME).get(i), indexList.get(INDEX_NAME).get(i));
        }
        return columnWithIndex;
    }

    public Comparable getCastedClusteringKeyValue(String tableName, String clusteringKeyValue) throws IOException, DBAppException {
        String clusteringKeyType = MetaDataManger.getInstance().readTableInfo(tableName, new MetaDataColumns[]{MetaDataColumns.COLUMN_TYPE}, strings -> strings[MetaDataColumns.IS_CLUSTERING_KEY.ordinal()].equals("True"), false).get(0).get(0);
        return switch (clusteringKeyType) {
            case "java.lang.Integer" -> Integer.parseInt(clusteringKeyValue);
            case "java.lang.Double" -> Double.parseDouble(clusteringKeyValue);
            case "java.lang.String" -> clusteringKeyValue;
            default -> throw new DBAppException("Invalid data type");
        };
    }
}
