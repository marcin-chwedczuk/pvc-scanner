package pl.marcinchwedczuk.template.gui.mainwindow;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.*;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import pl.marcinchwedczuk.template.gui.mainwindow.SerialInterface.GetSerialPorts;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class MainWindow implements Initializable {
    public static MainWindow showOn(Stage window) {
        try {
            FXMLLoader loader = new FXMLLoader(MainWindow.class.getResource("MainWindow.fxml"));

            Scene scene = new Scene(loader.load());
            MainWindow controller = (MainWindow) loader.getController();

            window.setTitle("Main Window");
            window.setScene(scene);
            window.setResizable(true);

            window.show();

            return controller;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private BorderPane mainWindow;

    @FXML
    private Button saveButton;

    @FXML
    private ComboBox<SerialPortItem> serialPortComboBox;

    @FXML
    private Button refreshSerialPortsButton;

    @FXML
    private ComboBox<Integer> layersComboBox;

    @FXML
    private ComboBox<Integer> anglesComboBox;

    @FXML
    private Button startButton;

    @FXML
    private Button stopButton;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private SplitPane splitPane;

    @FXML
    private TextArea logTextArea;

    private Group modelGroup;

    private final ScanProcess scanProcess = new ScanProcess();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        saveButton.disableProperty().bind(scanProcess.isScanCompleted.not());

        startButton.disableProperty().bind(scanProcess.isScanning.or(
                serialPortComboBox.getSelectionModel().selectedItemProperty().isNull()));
        stopButton.disableProperty().bind(scanProcess.isScanning.not());
        layersComboBox.disableProperty().bind(scanProcess.isScanning);
        anglesComboBox.disableProperty().bind(scanProcess.isScanning);

        refreshSerialPortsButton.disableProperty().bind(scanProcess.isDiscoveringPorts);
        serialPortComboBox.itemsProperty().bind(scanProcess.discoveredSerialPorts.map(l ->
                FXCollections.observableArrayList(
                    l.stream().map(SerialPortItem::new).toList())));
        scanProcess.serialPort.bind(serialPortComboBox.getSelectionModel().selectedItemProperty().map(SerialPortItem::serialPort));

        logTextArea.textProperty().bind(scanProcess.debugLogs);

        layersComboBox.setItems(FXCollections.observableArrayList(5, 10, 15, 20, 25, 50));
        layersComboBox.getSelectionModel().selectFirst();
        anglesComboBox.setItems(FXCollections.observableArrayList(3, 5, 7, 12, 24, 36, 64, 128, 170));
        anglesComboBox.getSelectionModel().selectFirst();

        scanProcess.layers.bind(layersComboBox.getSelectionModel().selectedItemProperty());
        scanProcess.angles.bind(anglesComboBox.getSelectionModel().selectedItemProperty());

        progressBar.progressProperty().bind(Bindings.divide(scanProcess.scanProgress, 100.0));

        scanProcess.discoverSerialPortsAsync();
    }

    private void initialize3DView(ScannedModel model) {
        AxisMarker axisMarker = new AxisMarker();
        axisMarker.scale(5);
        axisMarker.reverseY();

        modelGroup = new Group();
        modelGroup.getChildren().add(new Box(800, 1, 800));
        modelGroup.getChildren().add(model.model3DNode());
        modelGroup.getChildren().add(axisMarker);
        modelGroup.getTransforms().add(new Scale(1, -1, 1));

        Group sceneGroup = new Group();
        sceneGroup.getChildren().addAll(modelGroup, axisMarker);

        PreviewSubscene d3Scene = new PreviewSubscene(sceneGroup, 800, 640);

        Pane subsceneParent = new Pane();
        d3Scene.setManaged(false);
        subsceneParent.getChildren().add(d3Scene);

        if (splitPane.getItems().size() > 1) {
            splitPane.getItems().removeFirst();
        }
        splitPane.getItems().addFirst(subsceneParent);

        scanProcess.addErrorListener(ex -> {
            UiService.errorDialog("Error: " + ex.getMessage());
        });
    }

    public void refreshSerialPorts(ActionEvent event) {
        scanProcess.discoverSerialPortsAsync();
    }

    public void startScan(ActionEvent event) {
        ScannedModel liveModel = scanProcess.startScanAsync();
        initialize3DView(liveModel);
    }

    public void stopScan(ActionEvent event) {
        scanProcess.cancelScan();
    }

    public void closeApp(ActionEvent event) {
        Platform.exit();
    }

    public void saveModel(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("OBJ files (*.obj)", "*.obj");
        fileChooser.getExtensionFilters().add(extFilter);

        File file = fileChooser.showSaveDialog(mainWindow.getScene().getWindow());

        try {
            if (file != null) {
                scanProcess.getCurrentModel().saveToObjFile(file.toPath());
            }
        } catch (Exception e) {
            UiService.errorDialog("Cannot save model: " + e.getMessage());
        }
    }
}
