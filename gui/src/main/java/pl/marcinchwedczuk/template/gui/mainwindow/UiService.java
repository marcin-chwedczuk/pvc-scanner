package pl.marcinchwedczuk.template.gui.mainwindow;

import javafx.scene.control.Alert;

public class UiService {
    public static void infoDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle("Information");
        // ID for tests
        alert.getDialogPane().setId("info-dialog");
        alert.showAndWait();
    }

    public static void errorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle("ERROR");
        // ID for tests
        alert.getDialogPane().setId("error-dialog");
        alert.showAndWait();
    }
}
