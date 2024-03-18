package storage;

import com.opencsv.CSVWriter;
import helpers.ConfigReader;

import java.io.*;
import java.util.*;

public class Table implements Serializable {

    private String tableName;

    private ArrayList<String> pages;

    private String clusteringKey;

    public Table(String tableName, String clusteringKey, Hashtable<String, String> colNameType) throws IOException {
        pages = new ArrayList<>();
        this.tableName = tableName;
        this.clusteringKey = clusteringKey;
        initTableMetaData(colNameType);
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

    public boolean isEmpty() {
        return pages.isEmpty();
    }

    private void initTableMetaData(Hashtable<String, String> colNameType) throws IOException {

        var rows = new ArrayList<String[]>();
        rows.add(new String[]{getTableName(), getClusteringKey(), colNameType.get(getClusteringKey()), "True", "null", "null"});
        for (String colName : colNameType.keySet()) {
            if (colName.equals(getClusteringKey())) continue;
            rows.add(new String[]{getTableName(), colName, colNameType.get(colName), "False", "null", "null"});
        }

        File file = new File(ConfigReader.getInstance().read("MetaDataFileName"));
        FileWriter outputFile = new FileWriter(file);
        CSVWriter writer = new CSVWriter(outputFile);
        writer.writeAll(rows);
        writer.close();
    }


    public void serialize() throws IOException {
        FileOutputStream fileOut = new FileOutputStream("serialized/tables" + getTableName() + ".class");
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(this);
        out.close();
        fileOut.close();
    }

}
