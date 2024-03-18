package storage;

import java.io.Serializable;
import java.util.*;

public class Table implements Serializable {

    private String tableName;

    private ArrayList<String> pages;

    private String clusteringKey;


    public Table(String tableName, String clusteringKey, Hashtable<String, String> colNameType) {
        pages = new ArrayList<>();
        this.tableName = tableName;
        this.clusteringKey = clusteringKey;
        InitTableMetaData(clusteringKey, colNameType);

    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public ArrayList<String> getPages() {
        return pages;
    }

    public void setPages(ArrayList<String> pages) {
        this.pages = pages;
    }

    public String getClusteringKey() {
        return clusteringKey;
    }

    public void setClusteringKey(String clusteringKey) {
        this.clusteringKey = clusteringKey;
    }

    public boolean isEmpty () {
        return pages.isEmpty();
}

    private void InitTableMetaData(String clusteringKey, Hashtable<String, String> colNameType){
        return;
        // !TODO
    }


}
