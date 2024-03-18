package helpers;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


public class MetaDataManger {

    private static final MetaDataManger INSTANCE = new MetaDataManger();
    private final File META_DATA_FILE = new File(ConfigReader.getInstance().read("MetaDataFileName"));
    private FileReader fileReader;
    private FileWriter fileWriter;


    private MetaDataManger() {
        try {
            fileReader = new FileReader(META_DATA_FILE);
            fileWriter = new FileWriter(META_DATA_FILE);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static MetaDataManger getInstance() {
        return INSTANCE;
    }

    public void writeAll(List<String[]> allLines) throws IOException {
        CSVWriter writer = new CSVWriter(fileWriter);
        writer.writeAll(allLines);
        writer.close();
    }

    public CSVReader getCSVReader() {
        return new CSVReader(fileReader);
    }


    public boolean exists(MetaDataColumns column, String value) throws IOException {
        CSVReader csvReader = getCSVReader();
        String[] nextRecord;
        while ((nextRecord = csvReader.readNext()) != null) {
            if (nextRecord[column.ordinal()].equals(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean existsInTable(MetaDataColumns column, String value, String tableName) throws IOException {
        CSVReader csvReader = getCSVReader();
        String[] nextRecord;
        while ((nextRecord = csvReader.readNext()) != null) {
            if (nextRecord[0].equals(tableName) && nextRecord[column.ordinal()].equals(value)) {
                return true;
            }
        }
        return false;
    }



}
