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

    private void InitTableMetaData(String clusteringKey, Hashtable<String, String> colNameType){
        return;
        // !TODO
    }


}
