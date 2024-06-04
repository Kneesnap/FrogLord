package net.highwayfrogs.editor.utils.logging;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * Write logged information (not errors) to the console.
 * Created by Kneesnap on 1/9/2024.
 */
public class ConsoleOutputHandler extends StreamHandler {
    private final ConsoleOutputStream outputStream;

    public ConsoleOutputHandler() {
        setOutputStream(this.outputStream = new ConsoleOutputStream());
    }

    @Override
    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record))
            return;

        boolean isError = record.getThrown() != null;
        if (this.outputStream.isError() != isError)
            this.flush();

        this.outputStream.setError(isError);
        super.publish(record); // Write to System.out
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        return super.isLoggable(record) && !LogFormatter.isJavaFXMessage(record);
    }

    @Override
    public void close() throws SecurityException {
        this.flush();
    }

    @Getter
    @Setter
    private static class ConsoleOutputStream extends OutputStream {
        private boolean error;

        @Override
        public void write(int b) throws IOException {
            if (this.error) {
                System.err.write(b);
            } else {
                System.out.write(b);
            }
        }
    }
}