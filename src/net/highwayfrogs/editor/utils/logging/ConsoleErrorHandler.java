package net.highwayfrogs.editor.utils.logging;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * Write logged errors to the console.
 * Created by Kneesnap on 1/9/2024.
 */
public class ConsoleErrorHandler extends StreamHandler {
    public ConsoleErrorHandler() {
        setOutputStream(System.err);
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record))
            return;

        super.publish(record); // Write to System.out
        this.flush();
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        return super.isLoggable(record) && record.getThrown() != null;
    }

    @Override
    public void close() throws SecurityException {
        this.flush();
    }
}

