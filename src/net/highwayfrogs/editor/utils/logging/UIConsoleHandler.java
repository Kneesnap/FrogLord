package net.highwayfrogs.editor.utils.logging;

import net.highwayfrogs.editor.gui.MainController;

import java.util.logging.ErrorManager;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * Feeds logging output into the text console.
 * It feeds it to System.out as well as the GUI console.
 * Created by Kneesnap on 1/8/2024.
 */
public class UIConsoleHandler extends StreamHandler {
    public UIConsoleHandler() {
        setOutputStream(System.out);
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record))
            return;

        String msg;
        try {
            msg = getFormatter().format(record);
        } catch (Exception ex) {
            // We don't want to throw an exception here, but we
            // report the exception to any registered ErrorManager.
            reportError(null, ex, ErrorManager.FORMAT_FAILURE);
            return;
        }

        // Add to UI.
        MainController.addMessage(msg);
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        return super.isLoggable(record) && !LogFormatter.isJavaFXMessage(record);
    }
}