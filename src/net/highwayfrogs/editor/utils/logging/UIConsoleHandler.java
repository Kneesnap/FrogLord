package net.highwayfrogs.editor.utils.logging;

import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GUIMain;

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
        logMessage(msg);
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        return super.isLoggable(record) && !LogFormatter.isJavaFXMessage(record);
    }

    /**
     * Print a message to the console window.
     * @param message The message to print.
     */
    public static void logMessage(String message) {
        for (GameInstance gameInstance : GUIMain.getActiveGameInstances())
            gameInstance.addConsoleLogEntry(message);
    }
}