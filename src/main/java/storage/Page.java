package storage;

import java.io.Serializable;
import java.util.Collections;
import java.util.Vector;
import helpers.ConfigReader;
import exceptions.DBAppException;


public class Page implements Serializable {

    private final int maxNumberOfRecords = ConfigReader.getInstance().readInteger("MaximumRowsCountinPage");
    Vector<Tuple> records;

    public Page() {
        records = new Vector<>();
    }

    public void insert(Tuple record) throws DBAppException{
        if(!isFull()) {
            records.add(record);
        }
        else throw new DBAppException("Page is full");

        // !TODO sort records in a page
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

}
