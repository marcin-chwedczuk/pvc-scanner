package pl.marcinchwedczuk.template.gui;

import javafx.application.Application;
import javafx.stage.Stage;
import pl.marcinchwedczuk.template.gui.mainwindow.MainWindow;

/**
 * JavaFX App
 */
public class App extends Application {
    @Override
    public void start(Stage stage) {
        MainWindow.showOn(stage);
    }

    public static void main(String[] args) {
        System.setProperty("fazecast.jSerialComm.appid", "pvc_scanner");

        launch();
    }
}