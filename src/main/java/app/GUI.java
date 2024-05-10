package app;

import exceptions.DBAppException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import storage.Tuple;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import static javafx.application.Application.launch;

public class GUI extends Application {
    public static DBApp dbApp;
    public static TextArea textArea;

    public static String whiteStyle = "-fx-prompt-text-fill: grey;-fx-control-inner-background: black; " +
            "-fx-text-fill: white; -fx-font-family: 'monospace';-fx-border-color: black; -fx-font-size: 12pt; " +
            "-fx-highlight-fill: #000000; -fx-highlight-text-fill: #ffffff; ";
    public static String redStyle = whiteStyle + "-fx-text-fill: red;";
    public static String greenStyle = whiteStyle + "-fx-text-fill: green;";



    static ArrayList<String> history = new ArrayList<>();
    static int historyIndex = 0;
    public void start(Stage primaryStage) {
        dbApp = new DBApp();


        textArea = new TextArea();
        textArea.setStyle(whiteStyle);
        String[] pixelatedA = {
                "  **  ",
                " *  * ",
                "*    *",
                "******",
                "*    *",
                "*    *"
        };

        String[] pixelatedY = {
                "*   *",
                "*   *",
                " * * ",
                "  *  ",
                "  *  ",
                "  *  "
        };

        for (int i = 0; i < pixelatedA.length; i++) {
            textArea.appendText(pixelatedA[i] + "   " + pixelatedY[i]+"\n");
        }
        textArea.appendText("All rights reserved to AY © 2024 \n");
        textArea.appendText("-----------------------------------------\n");
        textArea.appendText("Welcome to AYDBMS\n");
        textArea.appendText("Press Escape to start\n");
        textArea.setPromptText("Enter your SQL command here..");
        textArea.setEditable(true);
        textArea.setWrapText(true);


        StackPane root = new StackPane();
        root.getChildren().add(textArea);

        // Create a Scene with the StackPane as the root
        Scene scene = new Scene(root, 600, 400);
        scene.getRoot().setStyle("-fx-background-color: black; -fx-prompt-text-fill: white; -fx-text-fill: white;");


        textArea.setOnKeyPressed(e -> {
            try {
                if (e.getCode().toString().equals("ENTER")) {

                    Iterator resultSet = null;
                    String command = textArea.getText();
                    history.add(command.trim());
                    StringBuffer output = new StringBuffer();
                    output.append(command);
                    textArea.clear();
                    textArea.appendText("Executing command:...");


                    resultSet = dbApp.parseSQL(output);

                    if (!textArea.getText().contains("Error")) {
                        if (resultSet != null) {
                            while (resultSet.hasNext()) {
                                textArea.appendText("\n" + resultSet.next().toString());
                            }
                        } else {
                            textArea.setStyle(greenStyle);
                            textArea.appendText("\n" + "Command executed successfully");
                        }
                    }
                    textArea.appendText("\n" + "Press ESC to enter another command");

                }
                if (e.getCode().toString().equals("ESCAPE")) {
                    textArea.setStyle(whiteStyle);
                    textArea.clear();
                    textArea.setPromptText("Enter your SQL command here..");
                }

                if (e.getCode().toString().equals("UP")) {
                    textArea.clear();
                    if(history.size()>0){
                        historyIndex = (history.size() + 1) % (history.size());
                        textArea.appendText(history.get(historyIndex));
                    }
                }
                if (e.getCode().toString().equals("DOWN") && history.size() > 0) {
                    textArea.clear();
                    if(history.size()>0){
                        historyIndex = (((historyIndex - 1) % (history.size())) + history.size()) % history.size();
                        textArea.appendText(history.get(historyIndex));
                    }
                }
            } catch (DBAppException ex) {
                String err = "Error" + ex.getMessage();
                System.out.println(ex.getMessage());
                textArea.appendText("\n" +err);
            }
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("Terminal GUI");
        primaryStage.show();
    }

        public static void main(String[] args) {
            launch(args);
        }
}
