package storage;

import com.opencsv.CSVWriter;
import helpers.ConfigReader;
import helpers.MetaDataManger;

import java.io.*;
import java.util.*;

public class Table implements Serializable {

    private String tableName;

    private ArrayList<String> pages;

    private String clusteringKey;

    public Table(String tableName, String clusteringKey, Hashtable<String, String> colNameType) throws IOException {
        this.pages = new ArrayList<>();
        this.tableName = tableName;
        this.clusteringKey = clusteringKey;
        initTableMetaData(colNameType);
    }

    public String getTableName() {
        return tableName;
    }

    public ArrayList<String> getPages() {
        return pages;
    }

    public String getClusteringKey() {
        return clusteringKey;
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

        MetaDataManger.getInstance().writeAll(rows);
    }


    public void serialize() throws IOException {
        FileOutputStream fileOut = new FileOutputStream("serialized/tables/" + getTableName() + ".class");
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(this);
        out.close();
        fileOut.close();
    }

    public static Table deserialize(String tableName) throws IOException, ClassNotFoundException {
        FileInputStream fileIn = new FileInputStream("serialized/tables/" + tableName + ".class");
        ObjectInputStream in = new ObjectInputStream(fileIn);
        Table table = (Table) in.readObject();
        in.close();
        fileIn.close();
        return table;
    }
}
