package pl.marcinchwedczuk.template.gui.mainwindow;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Platform;
import javafx.util.Callback;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class SerialInterface {
    private final Thread workerThread = new Thread(this::work, "serial-port-thread");
    private final ConcurrentLinkedQueue<Message<?>> messageQueue = new ConcurrentLinkedQueue<>();

    private volatile SerialPortLogger logger = new SerialPortLogger(data -> { });
    public void setLogger(SerialPortLogger logger) {
        this.logger = requireNonNull(logger);
    }

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

    public abstract class Message<R> {
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
                logger.logEvent(String.format("Exception %s: %s", e.getClass().getSimpleName(), e.getMessage()));
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

    public class GetSerialPorts extends Message<List<SerialPort>> {
        @Override
        List<SerialPort> innerExecute() {
            SerialPort[] ports = SerialPort.getCommPorts();

            logger.logEvent("Discovered serial ports:");
            for (SerialPort p : ports) {
                logger.logEvent(String.format("- %s, baud rate: %s, path: %s", p.getManufacturer(), p.getBaudRate(), p.getSystemPortPath()));
            }

            return Arrays.asList(ports);
        }
    }

    public class OpenSerialPort extends Message<SerialPort> {
        private static final int TIMEOUT = 15000;
        private final SerialPort port;

        public OpenSerialPort(SerialPort port) {
            this.port = requireNonNull(port);
        }

        @Override
        SerialPort innerExecute() {
            if (port.openPort()) {
                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, TIMEOUT, TIMEOUT);
                logger.logEvent("Port open.");
                return port;
            }

            throw new RuntimeException("Opening port failed.");
        }
    }

    public class CloseSerialPort extends Message<SerialPort> {
        private final SerialPort port;

        public CloseSerialPort(SerialPort port) {
            this.port = requireNonNull(port);
        }

        @Override
        SerialPort innerExecute() {
            port.closePort();
            return port;
        }
    }

    public class SendCommand extends Message<String> {
        private final SerialPort port;
        private final String message;

        public SendCommand(SerialPort port, String message) {
            this.port = port;
            this.message = message;
        }

        @Override
        String innerExecute() {
            String msg = message;
            if (!message.endsWith("\n")) {
                msg += "\n";
            }

            logger.logSent(message);
            byte[] bytes = msg.getBytes(StandardCharsets.US_ASCII);
            int result = port.writeBytes(bytes, bytes.length);
            if (result == -1) throw new RuntimeException("Write to serial port failed.");

            StringBuilder sb = new StringBuilder();

            byte[] singleChar = new byte[1];
            do {
                if (port.readBytes(singleChar, 1) != 1) {
                    throw new RuntimeException("Reading from serial port failed.");
                }
                sb.append((char) singleChar[0]);
            } while (singleChar[0] != '\n');

            logger.logRecv(message);
            return sb.toString();
        }
    }
}
