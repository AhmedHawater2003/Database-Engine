import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        var mySambosa = new Sambosa();

        try {
            // Create an output stream for the file where the object will be stored
            FileOutputStream fileOut = new FileOutputStream("src/main/pages/mySambosa.class");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);

            // Write the mySambosa object to the output stream
            out.writeObject(mySambosa);

            // Close the output stream
            out.close();
            fileOut.close();

            System.out.println("Serialized data is saved in pages/mySambosa.class");
        } catch (Exception i) {
            i.printStackTrace();
        }

        try {
            // Create an input stream for the file where the object is stored
            FileInputStream fileIn = new FileInputStream("src/main/pages/mySambosa.class");
            ObjectInputStream in = new ObjectInputStream(fileIn);

            // Read the mySambosa object from the input stream
            mySambosa = (Sambosa) in.readObject();

            // Close the input stream
            in.close();
            fileIn.close();


            System.out.println("Deserialized Sambosa...");
        } catch (Exception i) {
            i.printStackTrace();
        }

        try {
            // Get the Class object associated with java.lang.Integer
            Class<?> integerClass = Class.forName("java.lang.Integer");

            // Get the constructor that takes a String as an argument
            Constructor<?> constructor = integerClass.getConstructor(String.class);

            // Create a new instance of Integer using the constructor
            Object integerObject = constructor.newInstance("100");

            System.out.println( integerObject.getClass());

            System.out.println("Created Integer object: " + integerObject);
        } catch (Exception e) {
            e.printStackTrace();
        }



    }
}

class Sambosa implements Serializable {
    int x = 0;
    transient Hashw[] arr = {new Hashw()};
}

class Hashw{
    String hashw = "meat";
}