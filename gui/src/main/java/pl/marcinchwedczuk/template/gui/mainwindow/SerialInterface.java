package pl.marcinchwedczuk.template.gui.mainwindow;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.util.Callback;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class SerialInterface {
    private final Thread workerThread = new Thread(this::work, "serial-port-thread");
    private final ConcurrentLinkedQueue<Message<?>> messageQueue = new ConcurrentLinkedQueue<>();

    public void start() {
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private void work() {
        try {
            while(true) {
                Message<?> msg = messageQueue.poll();
                if (msg == null) {
                    Thread.sleep(20);
                    continue;
                }

                msg.execute();
            }
        } catch (InterruptedException e) {
            // exit loop
        }
    }

    public void sent(Message<?> msg) {
        requireNonNull(msg);
        messageQueue.offer(msg);
    }

    public abstract static class Message<R> {
        private Consumer<R> onSuccess = p -> { };
        private Consumer<Exception> onFailure = p -> { };

        abstract R innerExecute();

        void execute() {
            R result;
            try {
                result = innerExecute();
                Platform.runLater(() -> {
                    onSuccess.accept(result);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    onFailure.accept(e);
                });
            }
        }

        public Message<R> setOnSuccess(Consumer<R> onSuccess) {
            this.onSuccess = requireNonNull(onSuccess);
            return this;
        }

        public Message<R> setOnFailure(Consumer<Exception> onFailure) {
            this.onFailure = requireNonNull(onFailure);
            return this;
        }
    }

    public static class GetSerialPorts extends Message<List<SerialPort>> {
        @Override
        List<SerialPort> innerExecute() {
            SerialPort[] ports = SerialPort.getCommPorts();
            return Arrays.asList(ports);
        }
    }
}
