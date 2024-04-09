package net.highwayfrogs.editor.utils.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formats logs according to the specified format.
 * Created by Kneesnap on 1/9/2024.
 */
public class LogFormatter extends Formatter {
    private final String format;
    private final Date date = new Date();

    public static final String FULL_LOG_FORMAT = "[%1$tY-%1$tm-%1$td %1$tT|%4$s|%3$s] %5$s%6$s%n";
    public static final String PARTIAL_LOG_FORMAT = "[%4$s|%3$s] %5$s%6$s";

    public LogFormatter(String format) {
        this.format = format;
    }

    @Override
    public String format(LogRecord record) {
        this.date.setTime(record.getMillis());

        String source;
        if (record.getSourceClassName() != null) {
            source = record.getSourceClassName();
            if (record.getSourceMethodName() != null)
                source += " " + record.getSourceMethodName();
        } else {
            source = record.getLoggerName();
        }

        // Append stack trace, if there is one.
        String message = formatMessage(record);
        String throwable = "";
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }

        // Format error message.
        return String.format(this.format,
                this.date,
                source,
                record.getLoggerName(),
                record.getLevel().getLocalizedName(),
                message,
                throwable);
    }

    /**
     * Test if it's a JavaFX message.
     * @param record The log record to test
     * @return true if it's from JavaFX.
     */
    public static boolean isJavaFXMessage(LogRecord record) {
        if (record == null)
            return false;


        String className = record.getSourceClassName() != null ? record.getSourceClassName() : "";
        String loggerName = record.getLoggerName() != null ? record.getLoggerName() : "";

        // Many of these messages only show up once we use a debugger.
        // And they spam messages we don't care about (usually).
        return (className.startsWith("javafx.") || loggerName.startsWith("javafx."))
                || (className.startsWith("javax.") || loggerName.startsWith("javax."))
                || (className.startsWith("com.sun.") || loggerName.startsWith("com.sun."))
                || (className.startsWith("sun.") || loggerName.startsWith("sun."))
                || (className.startsWith("java.io.serialization") || loggerName.startsWith("java.io.serialization"));
    }
}