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
        return readTableInfo(tableName, targetColumns, strings -> true, false);
    }

    public List<String[]> readAll() {
        List<String[]> result = new ArrayList<>();
        try {
            CSVReader csvReader = getCSVReader();
            result = csvReader.readAll();
            csvReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<List<String>> readTableInfo(String tableName, MetaDataColumns[] targetColumns,
                                            MetaDataFilterFunction function, boolean distinct) throws IOException {
        List<List<String>> result = new ArrayList<>();
        CSVReader csvReader = getCSVReader();

        List<String[]> TableInfo = csvReader.readAll().stream()
                .filter(strings -> strings[MetaDataColumns.TABLE_NAME.ordinal()].equals(tableName) &&
                        function.filter(strings)).toList();

        csvReader.close();

        for (MetaDataColumns column : targetColumns) {
            List<String> temp = new ArrayList<>();
            for (String[] columnInfo : TableInfo) {
                temp.add(columnInfo[column.ordinal()]);
            }
            result.add(distinct ? temp.stream().distinct().toList() : temp);
        }

        return result;
    }

    public List<List<String>> getColumnsWithIndex(String tableName) throws IOException {
        List<List<String>> tableInfo = readTableInfo(tableName, new MetaDataColumns[]{MetaDataColumns.COLUMN_NAME, MetaDataColumns.INDEX_NAME});
        List<List<String>> columnsWithIndex = new ArrayList<>();
        List<String> indexNames = tableInfo.get(1);
        List<String> validColumns = new ArrayList<>();
        List<String> validIndexes = new ArrayList<>();
        for (String indexName : indexNames) {
            if (!indexName.equals("null")) {
                validColumns.add(tableInfo.get(0).get(indexNames.indexOf(indexName)));
                validIndexes.add(indexName);
            }
        }
        columnsWithIndex.add(validColumns);
        columnsWithIndex.add(validIndexes);
        return columnsWithIndex;
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

public String getColumnType(String tableName, String columnName) throws IOException {
        CSVReader csvReader = getCSVReader();
        String[] nextRecord;
        String columnType = null;
        while ((nextRecord = csvReader.readNext()) != null) {
            if (nextRecord[MetaDataColumns.TABLE_NAME.ordinal()].equals(tableName) && nextRecord[MetaDataColumns.COLUMN_NAME.ordinal()].equals(columnName)) {
                columnType = nextRecord[MetaDataColumns.COLUMN_TYPE.ordinal()];
                break;
            }
        }
        csvReader.close();
        return columnType;
    }

    public int getColumnsCount(String tableName) throws IOException {
        CSVReader csvReader = getCSVReader();
        String[] nextRecord;
        int columnsCount = 0;
        while ((nextRecord = csvReader.readNext()) != null) {
            if (nextRecord[MetaDataColumns.TABLE_NAME.ordinal()].equals(tableName)) {
                columnsCount++;
            }
        }
        csvReader.close();
        return columnsCount;
    }
}
