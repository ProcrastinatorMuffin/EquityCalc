package com.equitycalc.ui.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/matrixTest.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Range Matrix Test");
        stage.setWidth(500);
        stage.setHeight(500);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}