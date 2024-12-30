package net.highwayfrogs.editor.utils.logging;


import net.highwayfrogs.editor.utils.logging.InstanceLogger.WrappedLogger;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Tracks messages per-type on the logger.
 * Created by Kneesnap on 12/29/2024.
 */
public class MessageTrackingLogger extends WrappedLogger {
    private final Map<Level, List<LogRecord>> messagesByLevel = new HashMap<>();
    public MessageTrackingLogger(ILogger wrappedLogger) {
        super(wrappedLogger);
    }

    public MessageTrackingLogger(Logger wrappedLogger) {
        super(wrappedLogger);
    }

    public MessageTrackingLogger() {
        super();
    }

    @Override
    public void log(LogRecord lr) {
        List<LogRecord> messages = this.messagesByLevel.computeIfAbsent(lr.getLevel(), key -> new ArrayList<>());
        messages.add(lr);
        super.log(lr);
    }

    /**
     * Gets the messages logged for a particular log level.
     * @param level the log level to get messages from
     * @return messages
     */
    public List<LogRecord> getMessages(Level level) {
        List<LogRecord> messages = this.messagesByLevel.get(level);
        return Collections.unmodifiableList(messages);
    }

    /**
     * Gets the number of messages tracked for the given level.
     * @param level The level to get the message count from
     * @return messageCount
     */
    public int getMessageCount(Level level) {
        List<LogRecord> messages = this.messagesByLevel.get(level);
        return messages != null ? messages.size() : 0;
    }

    /**
     * Get the total number of messages tracked
     * @return messageCount
     */
    public int getTotalMessageCount() {
        int totalCount = 0;
        for (List<LogRecord> messages : this.messagesByLevel.values())
            totalCount += messages.size();

        return totalCount;
    }
}
