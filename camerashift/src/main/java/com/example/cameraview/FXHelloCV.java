package com.example.cameraview;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.opencv.core.Core;

import java.io.IOException;


public class FXHelloCV extends Application {
    private static final int SCENE_WIDTH = 1500;
    private static final int SCENE_HIGH = 750;


    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // load the FXML resource
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXHelloCV.fxml"));

            // store the root element so that the controllers can use it
            Pane root = loader.load();

            // create and style a scene
            Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HIGH);

            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
            // create the stage with the given title and the previously created
            // scene
            primaryStage.setTitle("Camera alignment");
            primaryStage.setScene(scene);

            // show the GUI
            primaryStage.show();

            // set the proper behavior on closing the application
            Controller controller = loader.getController();

            primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
                public void handle(WindowEvent we) {
                    try {
                        controller.setClosed();

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startNewStage() throws IOException {

        FXMLLoader loader2 = new FXMLLoader(FXHelloCV.class.getResource("PreviewTestPage.fxml"));
        AnchorPane root2 = loader2.load();
        Scene scene2 = new Scene(root2);

        scene2.getStylesheets().add(FXHelloCV.class.getResource("testPreview.css").toExternalForm());
        Stage stage = new Stage();
        stage.setTitle("Preview test");

        stage.setScene(scene2);
        stage.show();

        PreviewTestController previewTestController= loader2.getController();
        stage.setOnCloseRequest(we -> {
            try {
                previewTestController.setClosed2();
                stage.close();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }


    public static void main(String[] args) {
        // load the native OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        launch(args);
    }
}


