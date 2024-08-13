package pl.marcinchwedczuk.template.gui.mainwindow;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import pl.marcinchwedczuk.template.gui.mainwindow.SerialInterface.GetSerialPorts;
import pl.marcinchwedczuk.template.gui.mainwindow.SerialInterface.OpenSerialPort;
import pl.marcinchwedczuk.template.gui.mainwindow.SerialInterface.SendCommand;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

public class ScanProcess {
    private final AtomicBoolean cancelFlag = new AtomicBoolean(false);
    private final SerialInterface serialInterface = new SerialInterface();
    private ErrorListener errorListener = e -> { };

    private ScannedModel scannedModel = null;

    public final BooleanProperty isScanning = new SimpleBooleanProperty(this, "isScanning", false);
    public final BooleanProperty isScanCompleted = new SimpleBooleanProperty(this, "isScanCompleted", false);
    public final IntegerProperty scanProgress = new SimpleIntegerProperty(this, "scanProgress", 0);

    public final ObjectProperty<SerialPort> serialPort = new SimpleObjectProperty<>(this, "serialPort", null);
    public final SimpleObjectProperty<ObservableList<SerialPort>> discoveredSerialPorts = new SimpleObjectProperty<>(this, "discoveredSerialPorts", FXCollections.observableArrayList());
    public final BooleanProperty isDiscoveringPorts = new SimpleBooleanProperty(this, "isDiscoveringPorts", false);

    public final IntegerProperty angles = new SimpleIntegerProperty(this, "angles", 10);
    public final IntegerProperty layers = new SimpleIntegerProperty(this, "layers", 10);

    public final StringProperty debugLogs = new SimpleStringProperty(this, "debugLogs", "");

    public ScanProcess() {
        serialInterface.setLogger(new SerialPortLogger(logLine -> {
            var sb = new StringBuilder(debugLogs.get());
            sb.append(logLine).append(System.lineSeparator());
            debugLogs.set(sb.toString());
        }));

        serialInterface.startWorkerThread();
    }

    public ScannedModel getCurrentModel() {
        return scannedModel;
    }

    public void addErrorListener(ErrorListener l) {
        this.errorListener = requireNonNull(l);
    }

    public void discoverSerialPortsAsync() {
        isDiscoveringPorts.set(true);

        serialInterface.sent(new GetSerialPorts()
                .setOnSuccess(ports -> discoveredSerialPorts.set(FXCollections.observableArrayList(ports)))
                .setOnFailure(e -> errorListener.onError(e))
                .setAtExit(() -> isDiscoveringPorts.set(false)));
    }

    public ScannedModel startScanAsync() {
        if (isScanning.get()) {
            // already scanning
            return scannedModel;
        }

        cancelFlag.set(false);
        debugLogs.set("");
        isScanCompleted.set(false);
        isScanning.set(true);

        this.scannedModel = new ScannedModel(angles.get(), layers.get(), 10.0f);
        goTo(ScanWorkflow.START);
        return scannedModel;
    }

    private int currentLayer = 0;
    private int currentAngle = 0;

    private void scanWorkflow(ScanWorkflow state) {
        scanWorkflow(state, null);
    }

    private void scanWorkflow(ScanWorkflow state, Exception optError) {
        if (cancelFlag.get()) {
            goTo(ScanWorkflow.CANCELED);
            return;
        }

        switch (state) {
            case START -> {
                serialInterface.sent(new OpenSerialPort(serialPort.get())
                        .setOnSuccess(port -> goTo(ScanWorkflow.PORT_OPENED))
                        .setOnFailure(err -> goTo(ScanWorkflow.ERROR)));
            }
            case PORT_OPENED -> {
                serialInterface.sent(new SendCommand(serialPort.get(), "DESCRIBE")
                        .setOnSuccess(response -> {
                            if (response.equals("OK PCV Scanner 1.0")) {
                                goTo(ScanWorkflow.DEVICE_VALIDATED);
                            } else {
                                goTo(ScanWorkflow.ERROR, new RuntimeException("Unrecognized device."));
                            }
                        })
                        .setOnFailure(err -> goTo(ScanWorkflow.ERROR)));
            }
            case DEVICE_VALIDATED -> {
                serialInterface.sent(new SendCommand(serialPort.get(), "RESET")
                        .setOnSuccess(response -> {
                            if (response.equals("OK")) {
                                goTo(ScanWorkflow.DEVICE_RESET);
                            } else {
                                goTo(ScanWorkflow.ERROR, new RuntimeException("Cannot reset device."));
                            }
                        })
                        .setOnFailure(err -> goTo(ScanWorkflow.ERROR)));
            }
            case DEVICE_RESET -> {
                goTo(ScanWorkflow.COMPLETED);
            }
            case SCAN -> {
            }
            case CLOSE_PORT -> {
            }
            case COMPLETED -> {
            }
            case ERROR -> {
                if (optError != null) {
                    errorListener.onError(optError);
                }
            }
            case CANCELED -> {

            }
        }
    }

    private void goTo(ScanWorkflow state) {
        goTo(state, null);
    }

    private void goTo(ScanWorkflow state, Exception optError) {
        Platform.runLater(() -> scanWorkflow(state, optError));
    }

    public void cancelScan() {
        if (!isScanning.get()) return; // already not scanning
        cancelFlag.set(true);
    }

    public interface ErrorListener {
        void onError(Exception e);
    }

    private enum ScanWorkflow {
        START,
        PORT_OPENED,
        DEVICE_VALIDATED,
        DEVICE_RESET,
        SCAN,
        CLOSE_PORT,
        COMPLETED,
        ERROR,
        CANCELED
    }
}
