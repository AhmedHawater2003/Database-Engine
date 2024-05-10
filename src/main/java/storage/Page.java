package storage;

import java.io.*;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;
import helpers.ConfigReader;
import exceptions.DBAppException;


public class Page implements Serializable {

    private final int maxNumberOfRecords = ConfigReader.getInstance().readInteger("MaximumRowsCountinPage");
//    private final int maxNumberOfRecords = 1;
    public Vector<Tuple> getRecords() {
        return records;
    }

    private Vector<Tuple> records;

    public Page() {
        records = new Vector<>();
    }

    public void insert(Tuple record) throws DBAppException{
        if(!isFull()) {
            records.add(record);
            Collections.sort(records);
        }
        else throw new DBAppException("Page is full");

        // !TODO sort records in a page
    }

    public Tuple swapRecords(Tuple record, int index) throws DBAppException {
        Tuple temp = records.remove(index);
        insert(record);
        return temp;

    }

    public void delete(Tuple record) {
        records.remove(record);
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    public boolean isFull() {
        return records.size() == maxNumberOfRecords;
    }

    public String toString(){
        StringBuilder out = new StringBuilder();
        for (Tuple record : records) {
            out.append(record).append("\n");
        }
        return out.toString();
    }

    public Tuple getRecordBS(String clustringKeyName,Comparable key) throws DBAppException {
        Hashtable<String, Object> temp = new Hashtable<>();
        temp.put(clustringKeyName, key);
        Tuple dummy = new Tuple(temp,clustringKeyName);
        int idx=Collections.binarySearch(records, dummy);
        if(idx<0) return null; // throw new DBAppException("Record not found");
        return records.get(idx);
    }

    public void serialize(String path) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(path);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(this);
        out.close();
        fileOut.close();
    }
    public static Page deserialize(String path) throws IOException, ClassNotFoundException {
        FileInputStream fileIn = new FileInputStream(path);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        Page page = (Page) in.readObject();
        in.close();
        fileIn.close();
        return page;
    }




}
