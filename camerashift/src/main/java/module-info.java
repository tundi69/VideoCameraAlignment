module com.example.cameraview {
    requires javafx.controls;
    requires javafx.fxml;
    requires opencv;
    requires java.desktop;
    requires javafx.swing;
    requires org.junit.jupiter.api;


    opens com.example.cameraview to javafx.fxml;
    exports com.example.cameraview;
    exports com.example.cameraview.utils;
    opens com.example.cameraview.utils to javafx.fxml;
}