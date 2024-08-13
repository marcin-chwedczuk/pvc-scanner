package pl.marcinchwedczuk.template.gui.mainwindow;

import javafx.application.Platform;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class SerialPortLogger {
    private final Consumer<String> loggerAction;

    public SerialPortLogger(Consumer<String> loggerAction) {
        this.loggerAction = requireNonNull(loggerAction);
    }

    public void logSent(String data) {
        Platform.runLater(() -> {
            loggerAction.accept("-> " + data);
        });
    }

    public void logRecv(String data) {
        Platform.runLater(() -> {
            loggerAction.accept("<- " + data);
        });
    }

    public void logEvent(String event) {
        Platform.runLater(() -> {
            loggerAction.accept(event);
        });
    }
}
