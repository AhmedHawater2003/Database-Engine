package storage;

import com.opencsv.CSVWriter;
import helpers.ConfigReader;
import helpers.MetaDataManger;

import java.io.*;
import java.util.*;


public class Table implements Serializable {

    private int latestPageIndex = 0;
    private String tableName;


    private TreeSet<PageInfo> pagesInfo;
    private String clusteringKey;



    private void initTableMetaData(Hashtable<String, String> colNameType) throws IOException {

        var rows = new ArrayList<String[]>();
        rows.add(new String[]{getTableName(), getClusteringKey(), colNameType.get(getClusteringKey()), "True", "null", "null"});
        for (String colName : colNameType.keySet()) {
            if (colName.equals(getClusteringKey())) continue;
            rows.add(new String[]{getTableName(), colName, colNameType.get(colName), "False", "null", "null"});
        }

        MetaDataManger.getInstance().writeAll(rows);
    }


    public Table(String tableName, String clusteringKey, Hashtable<String, String> colNameType) throws IOException {
        this.pagesInfo = new TreeSet<>();
        this.tableName = tableName;
        this.clusteringKey = clusteringKey;
        initTableMetaData(colNameType);
    }

    public String getTableName() {
        return tableName;
    }


    public String getClusteringKey() {
        return clusteringKey;
    }


    public PageInfo getTargetPageInfo(Comparable key) {
        PageInfo targetPageInfo = pagesInfo.first();
        if (key.compareTo(targetPageInfo.getMinKey()) > 0) {
            targetPageInfo = pagesInfo.floor(new PageInfo(key));
        }
        return targetPageInfo;
    }

    public PageInfo getNextPageInfo(PageInfo pageInfo) {
        return pagesInfo.higher(pageInfo);
    }

    public Page createNewPage(Comparable minKey) {
        PageInfo pageInfo = new PageInfo("serialized/pages/" + tableName + latestPageIndex++ + ".class", minKey);
        pagesInfo.add(pageInfo);
        return new Page();
    }

    public void updatePageInfoMinKey(PageInfo oldPageInfo, Comparable newMinKey) {
        pagesInfo.remove(oldPageInfo);
        newMinKey = (newMinKey.compareTo(oldPageInfo.getMinKey()) < 0) ? newMinKey : oldPageInfo.getMinKey();
        pagesInfo.add(new PageInfo(oldPageInfo.getPageAddress(), newMinKey));
    }
    public void updatePageInfoMinKey(String pageAddress , Comparable oldMinKey, Comparable newMinKey) {
        pagesInfo.remove(new PageInfo(pageAddress, oldMinKey));
        pagesInfo.add(new PageInfo(pageAddress, newMinKey));
    }

    public boolean isEmpty() {
        return pagesInfo.isEmpty();
    }

    public ArrayList<String> getPagesAddresses(){
        var result = new ArrayList<String>();
        for(var pageInfo : pagesInfo){
            result.add(pageInfo.getPageAddress());
        }
        return result;
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
    public void deletePage(String pageAddress, Comparable minKey) {
        pagesInfo.remove(new PageInfo(pageAddress, minKey));
    }

}
