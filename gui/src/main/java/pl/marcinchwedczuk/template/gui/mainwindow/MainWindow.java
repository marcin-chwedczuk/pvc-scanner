package pl.marcinchwedczuk.template.gui.mainwindow;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import pl.marcinchwedczuk.template.gui.mainwindow.SerialInterface.GetSerialPorts;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    private Button refreshSerialPortsButton;

    @FXML
    private SplitPane splitPane;

    @FXML
    private TextArea logTextArea;

    @FXML
    private ComboBox<SerialPortItem> serialPortComboBox;

    ScannedModel scannedModel;
    SerialInterface serialInterface;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        serialInterface = new SerialInterface();
        serialInterface.setLogger(new SerialPortLogger(logLine -> {
            logTextArea.appendText(logLine);
            logTextArea.appendText(System.lineSeparator());
        }));
        serialInterface.start();

        scannedModel = new ScannedModel(12, 12, 50f);

        /*
        Timer t = new Timer("layersTimer", true);
        t.scheduleAtFixedRate(new TimerTask() {
            private int points = 0;

            @Override
            public void run() {
                if (points >= scannedModel.angles() * scannedModel.layers()) {
                    this.cancel();
                    return;
                }
                points++;

                Platform.runLater(() -> {
                    Random r = new Random();
                    scannedModel.addScanPoint(r.nextFloat()*70 + 400);
                });
            }
        }, 1000, 200);
         */

        AxisMarker axisMarker = new AxisMarker();
        axisMarker.scale(5);
        axisMarker.reverseY();

        Group model = new Group();
        model.getChildren().add(new Box(800, 1, 800));
        model.getChildren().add(scannedModel.scannedModel());
        model.getChildren().add(axisMarker);
        model.getTransforms().add(new Scale(1, -1, 1));


        Group sceneGroup = new Group();
        sceneGroup.getChildren().addAll(model, axisMarker);

        Pane subsceneParent = new Pane();
        PreviewSubscene d3Scene = new PreviewSubscene(sceneGroup, 800, 640);
        d3Scene.setManaged(false);
        subsceneParent.getChildren().add(d3Scene);
        splitPane.getItems().addFirst(subsceneParent);


        serialInterface.sent(serialInterface.new GetSerialPorts()
                .setOnSuccess(ports -> {
                    List<SerialPortItem> uiPorts = ports.stream().map(SerialPortItem::new).toList();
                    serialPortComboBox.setItems(FXCollections.observableArrayList(uiPorts));
                })
                .setOnFailure(this::handleSerialException));
    }

    @FXML
    private void clicked() {
        try {
            scannedModel.saveToObjFile(Paths.get("dummy.obj"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSerialException(Exception e) {
        logTextArea.appendText("ERROR " + e.getClass() + ": " + e.getMessage());
    }

    public void refreshSerialPorts(ActionEvent event) {
        refreshSerialPortsButton.setDisable(true);

        serialInterface.sent(serialInterface.new GetSerialPorts()
                .setOnSuccess(ports -> {
                    refreshSerialPortsButton.setDisable(false);
                    List<SerialPortItem> uiPorts = ports.stream().map(SerialPortItem::new).toList();
                    serialPortComboBox.setItems(FXCollections.observableArrayList(uiPorts));
                })
                .setOnFailure(e -> {
                    refreshSerialPortsButton.setDisable(false);
                    handleSerialException(e);
                }));
    }

    public void startScan(ActionEvent event) {

    }

    public void stopScan(ActionEvent event) {

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
                scannedModel.saveToObjFile(file.toPath());
            }
        } catch (Exception e) {
            UiService.errorDialog("Cannot save model: " + e.getMessage());
        }
    }
}
