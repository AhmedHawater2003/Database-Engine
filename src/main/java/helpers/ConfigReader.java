package helpers;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigReader {
    private static final ConfigReader INSTANCE = new ConfigReader();
    private final Properties prop = new Properties();

    private ConfigReader() {
        try {
            prop.load(new FileInputStream("resources/DBApp.config"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ConfigReader getInstance() {
        return INSTANCE;
    }

    public int readInteger(String propertyName) {
        return Integer.parseInt(prop.getProperty(propertyName));
    }

    public String read(String propertyName) {
        return prop.getProperty(propertyName);
    }

}
