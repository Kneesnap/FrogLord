package net.highwayfrogs.editor.utils.logging;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
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
    public void publish(LogRecord record) {
        if (!isLoggable(record))
            return;

        synchronized (this.outputStream) {
            this.outputStream.setError(record.getThrown() != null || record.getLevel() == Level.SEVERE || record.getLevel() == Level.WARNING);
            super.publish(record); // Write to System.out/System.err
            this.flush();
        }
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        return super.isLoggable(record) && !LogFormatter.isJavaFXMessage(record);
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

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (this.error) {
                System.err.write(b, off, len);
            } else {
                System.out.write(b, off, len);
            }
        }

        @Override
        public void flush() throws IOException {
            super.flush();

            if (this.error) {
                System.err.flush();
            } else {
                System.out.flush();
            }
        }
    }
}