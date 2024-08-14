package pl.marcinchwedczuk.template.gui.mainwindow;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import pl.marcinchwedczuk.template.gui.mainwindow.SerialInterface.CloseSerialPort;
import pl.marcinchwedczuk.template.gui.mainwindow.SerialInterface.GetSerialPorts;
import pl.marcinchwedczuk.template.gui.mainwindow.SerialInterface.OpenSerialPort;
import pl.marcinchwedczuk.template.gui.mainwindow.SerialInterface.SendCommand;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final Pattern SCAN_RESPONSE = Pattern.compile("OK distance = (?<dist>(\\d){1,6}).");
    private int currentLayer = 0;
    private int currentAngle = 0;

    private void scanWorkflow(ScanWorkflow state) {
        scanWorkflow(state, null);
    }

    private void scanWorkflow(ScanWorkflow state, Exception optError) {
        if (cancelFlag.get()) {
            if (state == ScanWorkflow.START) {
                state = ScanWorkflow.CANCELED;
            } if (state == ScanWorkflow.COMPLETED || state == ScanWorkflow.ERROR || state == ScanWorkflow.CANCELED) {
                // just continue
            }
            else {
                state = ScanWorkflow.CLOSE_PORT;
            }
        }

        switch (state) {
            case START -> {
                scanProgress.set(0);
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
                                goTo(ScanWorkflow.CLOSE_PORT, new RuntimeException("Unrecognized device."));
                            }
                        })
                        .setOnFailure(err -> goTo(ScanWorkflow.CLOSE_PORT, err)));
            }
            case DEVICE_VALIDATED -> {
                serialInterface.sent(new SendCommand(serialPort.get(), "RESET")
                        .setOnSuccess(response -> {
                            if (response.equals("OK")) {
                                goTo(ScanWorkflow.DEVICE_RESET);
                            } else {
                                goTo(ScanWorkflow.CLOSE_PORT, new RuntimeException("Cannot reset device: " + response));
                            }
                        })
                        .setOnFailure(err -> goTo(ScanWorkflow.CLOSE_PORT, err)));
            }
            case DEVICE_RESET -> {
                this.currentLayer = 0;
                this.currentAngle = 0;
                goTo(ScanWorkflow.SCAN_SET_ANGLE);
            }
            case SCAN_SET_ANGLE -> {
                // We actually scann between [safetyMargin, 180 - safetyMargin]
                int safetyMarginDegrees = 5;
                int angleDegrees = (int)Math.round(safetyMarginDegrees + (180.0 - 2*safetyMarginDegrees) * (currentAngle + 1) / scannedModel.angles());

                serialInterface.sent(new SendCommand(serialPort.get(), "ANGLE " + angleDegrees)
                        .setOnSuccess(response -> {
                            if (response.startsWith("OK")) {
                                goTo(ScanWorkflow.SCAN_SET_LAYER);
                            } else {
                                goTo(ScanWorkflow.CLOSE_PORT, new RuntimeException("Cannot set angle: " + response));
                            }
                        })
                        .setOnFailure(err -> goTo(ScanWorkflow.CLOSE_PORT, err)));
            }
            case SCAN_SET_LAYER -> {
                serialInterface.sent(new SendCommand(serialPort.get(), "LAYER " + currentLayer)
                        .setOnSuccess(response -> {
                            if (response.startsWith("OK")) {
                                goTo(ScanWorkflow.SCAN);
                            } else {
                                goTo(ScanWorkflow.CLOSE_PORT, new RuntimeException("Cannot set layer: " + response));
                            }
                        })
                        .setOnFailure(err -> goTo(ScanWorkflow.CLOSE_PORT, err)));
            }
            case SCAN -> {
                serialInterface.sent(new SendCommand(serialPort.get(), "SCAN")
                        .setOnSuccess(response -> {
                            Matcher m = SCAN_RESPONSE.matcher(response);
                            if (m.matches()) {
                                int distanceMM = Integer.parseInt(m.group("dist"));
                                scannedModel.addScanPoint(distanceMM);

                                currentAngle++;
                                if (currentAngle >= scannedModel.angles()) {
                                    currentAngle = 0;
                                    currentLayer++;
                                }

                                if (currentLayer >= scannedModel.layers()) {
                                    scanProgress.set(100);
                                    goTo(ScanWorkflow.CLOSE_PORT);
                                } else {
                                    scanProgress.set((int)(100.0 * (currentAngle + 1) * (currentLayer + 1) / (scannedModel.angles() * scannedModel.layers())));
                                    goTo(ScanWorkflow.SCAN_SET_ANGLE);
                                }
                            } else {
                                goTo(ScanWorkflow.CLOSE_PORT, new RuntimeException("Scan failed: '" + response + "'."));
                            }
                        })
                        .setOnFailure(err -> goTo(ScanWorkflow.CLOSE_PORT, err)));

            }
            case CLOSE_PORT -> {
                serialInterface.sent(new CloseSerialPort(serialPort.get())
                        .setOnSuccess(port -> {
                            if (cancelFlag.get()) {
                                goTo(ScanWorkflow.CANCELED);
                            } if (optError != null) {
                                goTo(ScanWorkflow.ERROR, optError);
                            }
                            else {
                                goTo(ScanWorkflow.COMPLETED);
                            }
                        })
                        .setOnFailure(err -> goTo(ScanWorkflow.ERROR, err)));
            }
            case COMPLETED -> {
                isScanCompleted.set(true);
                isScanning.set(false);
                scanProgress.set(100);
            }
            case ERROR -> {
                if (optError != null) {
                    errorListener.onError(optError);
                }
                goTo(ScanWorkflow.CANCELED);
            }
            case CANCELED -> {
                cancelFlag.set(false);

                isScanCompleted.set(false);
                isScanning.set(false);
                scanProgress.set(0);
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
        SCAN_SET_ANGLE,
        SCAN_SET_LAYER,
        SCAN,
        CLOSE_PORT,
        COMPLETED,
        ERROR,
        CANCELED
    }
}
