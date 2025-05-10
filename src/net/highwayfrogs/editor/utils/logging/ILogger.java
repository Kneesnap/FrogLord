package net.highwayfrogs.editor.utils.logging;

import net.highwayfrogs.editor.utils.StringUtils;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Represents an object used to log FrogLord messages.
 * Created by Kneesnap on 12/10/2024.
 */
public interface ILogger {
    /**
     * Log a record.
     * @param lr the record to log
     */
    void log(LogRecord lr);

    /**
     * Check if a message of the given level would actually be logged by this logger.
     * @param level a message logging level
     * @return true if the given message level is currently being logged.
     */
    boolean isLoggable(Level level);

    /**
     * Gets the name of the logger.
     * @return loggerInfo
     */
    String getName();

    /**
     * Gets information used to identify the logger which a message is written to.
     * @return loggerInfo
     */
    String getLoggerInfo();

    /**
     * Creates a LogRecord for the given message.
     * @param record the record to setup
     * @return logRecord
     */
    default LogRecord setupLogRecord(LogRecord record) {
        record.setLoggerName(getName());
        record.setSourceClassName(getLoggerInfo());
        return record;
    }

    /**
     * Creates a LogRecord for the given message.
     * @param level the log level to apply
     * @param message the message to log
     * @return logRecord
     */
    default LogRecord createLogRecord(Level level, String message) {
        return new LogRecord(level, message);
    }

    /**
     * Creates and sets up a LogRecord for the given message.
     * @param level the log level to apply
     * @param message the message to log
     * @return logRecord
     */
    default LogRecord createAndSetupLogRecord(Level level, String message) {
        return setupLogRecord(createLogRecord(level, message));
    }

    /**
     * Log a message.
     * @param level One of the message level identifiers, e.g., SEVERE
     * @param msg The string message (or a key in the message catalog)
     */
    default void log(Level level, String msg) {
        if (!isLoggable(level))
            return;

        LogRecord lr = createAndSetupLogRecord(level, msg);
        log(lr);
    }

    /**
     * Log a message, specifying source class and method, with no arguments.
     * @param level One of the message level identifiers, e.g., SEVERE
     * @param sourceClass name of class that issued the logging request
     * @param sourceMethod name of method that issued the logging request
     * @param msg The string message (or a key in the message catalog)
     */
    default void logp(Level level, String sourceClass, String sourceMethod, String msg) {
        if (!isLoggable(level))
            return;

        LogRecord lr = createAndSetupLogRecord(level, msg);
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        log(lr);
    }

    /**
     * Log a method entry.
     * @param sourceClass name of class that issued the logging request
     * @param sourceMethod name of method that is being entered
     */
    default void entering(String sourceClass, String sourceMethod) {
        logp(Level.FINER, sourceClass, sourceMethod, "ENTRY");
    }

    /**
     * Log a method return.
     * @param sourceClass name of class that issued the logging request
     * @param sourceMethod name of the method
     */
    default void exiting(String sourceClass, String sourceMethod) {
        logp(Level.FINER, sourceClass, sourceMethod, "RETURN");
    }

    /**
     * Log throwing an exception.
     * <p>
     * This is a convenience method to log that a method is
     * terminating by throwing an exception.  The logging is done
     * using the FINER level.
     * <p>
     * If the logger is currently enabled for the given message
     * level then the given arguments are stored in a LogRecord
     * which is forwarded to all registered output handlers.  The
     * LogRecord's message is set to "THROW".
     * <p>
     * Note that the thrown argument is stored in the LogRecord thrown
     * property, rather than the LogRecord parameters property.  Thus it is
     * processed specially by output Formatters and is not treated
     * as a formatting parameter to the LogRecord message property.
     * <p>
     * @param   sourceClass    name of class that issued the logging request
     * @param   sourceMethod  name of the method.
     * @param   thrown  The Throwable that is being thrown.
     */
    default void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
        if (!isLoggable(Level.FINER))
            return;

        LogRecord lr = createAndSetupLogRecord(Level.FINER, "THROW");
        lr.setSourceClassName(sourceClass);
        lr.setSourceMethodName(sourceMethod);
        lr.setThrown(thrown);
        log(lr);
    }

    /**
     * Log a SEVERE message.
     * @param msg The string message (or a key in the message catalog)
     */
    default void severe(String msg) {
        log(Level.SEVERE, msg);
    }

    /**
     * Log a SEVERE message.
     * @param msgTemplate The string message template (or a key in the message catalog)
     * @param params The arguments to apply to the message template. (Using string formatting rules)
     */
    default void severe(String msgTemplate, Object... params) {
        this.severe(StringUtils.formatStringSafely(msgTemplate, params));
    }

    /**
     * Log a WARNING message.
     * @param msg The string message (or a key in the message catalog)
     */
    default void warning(String msg) {
        log(Level.WARNING, msg);
    }

    /**
     * Log a WARNING message.
     * @param msgTemplate The string message template (or a key in the message catalog)
     * @param params The arguments to apply to the message template. (Using string formatting rules)
     */
    default void warning(String msgTemplate, Object... params) {
        this.warning(StringUtils.formatStringSafely(msgTemplate, params));
    }

    /**
     * Log an INFO message.
     * @param msg The string message (or a key in the message catalog)
     */
    default void info(String msg) {
        log(Level.INFO, msg);
    }

    /**
     * Log an INFO message.
     * @param msgTemplate The string message template (or a key in the message catalog)
     * @param params The arguments to apply to the message template. (Using string formatting rules)
     */
    default void info(String msgTemplate, Object... params) {
        this.info(StringUtils.formatStringSafely(msgTemplate, params));
    }

    /**
     * Log a CONFIG message.
     * @param msg The string message (or a key in the message catalog)
     */
    default void config(String msg) {
        log(Level.CONFIG, msg);
    }

    /**
     * Log a CONFIG message.
     * @param msgTemplate The string message template (or a key in the message catalog)
     * @param params The arguments to apply to the message template. (Using string formatting rules)
     */
    default void config(String msgTemplate, Object... params) {
        this.config(StringUtils.formatStringSafely(msgTemplate, params));
    }

    /**
     * Log a FINE message.
     * @param msg The string message (or a key in the message catalog)
     */
    default void fine(String msg) {
        log(Level.FINE, msg);
    }

    /**
     * Log a FINE message.
     * @param msgTemplate The string message template (or a key in the message catalog)
     * @param params The arguments to apply to the message template. (Using string formatting rules)
     */
    default void fine(String msgTemplate, Object... params) {
        this.fine(StringUtils.formatStringSafely(msgTemplate, params));
    }

    /**
     * Log a FINER message.
     * @param msg The string message (or a key in the message catalog)
     */
    default void finer(String msg) {
        log(Level.FINER, msg);
    }

    /**
     * Log a FINER message.
     * @param msgTemplate The string message template (or a key in the message catalog)
     * @param params The arguments to apply to the message template. (Using string formatting rules)
     */
    default void finer(String msgTemplate, Object... params) {
        this.finer(StringUtils.formatStringSafely(msgTemplate, params));
    }

    /**
     * Log a FINEST message.
     * @param msg The string message (or a key in the message catalog)
     */
    default void finest(String msg) {
        log(Level.FINEST, msg);
    }

    /**
     * Log a FINEST message.
     * @param msgTemplate The string message template (or a key in the message catalog)
     * @param params The arguments to apply to the message template. (Using string formatting rules)
     */
    default void finest(String msgTemplate, Object... params) {
        this.finest(StringUtils.formatStringSafely(msgTemplate, params));
    }
}
