package helpers;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MetaDataManger {

    private static final MetaDataManger INSTANCE = new MetaDataManger();
    private final File META_DATA_FILE = new File(ConfigReader.getInstance().read("MetaDataFileName"));

    private MetaDataManger() {

    }

    public static MetaDataManger getInstance() {
        return INSTANCE;
    }

    public void writeAll(List<String[]> allLines) throws IOException {
        FileWriter fileWriter = new FileWriter(META_DATA_FILE);
        CSVWriter writer = new CSVWriter(fileWriter);
        writer.writeAll(allLines);
        writer.close();
    }

    // WARNING! The reader should be closed after use
    public CSVReader getCSVReader() throws IOException {
        FileReader fileReader = new FileReader(META_DATA_FILE);
        return new CSVReader(fileReader);
    }


    public List<List<String>> readTableInfo(String tableName, MetaDataColumns[] targetColumns) throws IOException {
        List<List<String>> result = new ArrayList<>();
        CSVReader csvReader = getCSVReader();

        List<String[]> TableInfo = csvReader.readAll().stream()
                .filter(strings -> strings[MetaDataColumns.TABLE_NAME.ordinal()].equals(tableName)).toList();

        csvReader.close();

        for (String[] columnInfo : TableInfo) {
            List<String> row = new ArrayList<>();
            for (MetaDataColumns column : targetColumns) {
                row.add(columnInfo[column.ordinal()]);
            }
            result.add(row);
        }
        return result;
    }




    public boolean exists(MetaDataColumns column, String value) throws IOException {
        boolean exists = false;
        CSVReader csvReader = getCSVReader();
        String[] nextRecord;
        while ((nextRecord = csvReader.readNext()) != null) {
            if (nextRecord[column.ordinal()].equals(value)) {
                exists = true;
                break;
            }
        }
        csvReader.close();
        return exists;
    }

    public boolean existsInTable(MetaDataColumns column, String value, String tableName) throws IOException {
        boolean exists = false;
        CSVReader csvReader = getCSVReader();
        String[] nextRecord;
        while ((nextRecord = csvReader.readNext()) != null) {
            if (nextRecord[MetaDataColumns.TABLE_NAME.ordinal()].equals(tableName) && nextRecord[column.ordinal()].equals(value)) {
                exists = true;
                break;
            }
        }
        csvReader.close();
        return exists;
    }


}
