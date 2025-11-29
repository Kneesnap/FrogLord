package net.highwayfrogs.editor.utils.logging;


import javafx.scene.control.Alert.AlertType;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.StringUtils;
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
    public static final String WARNING_SUMMARY_PLACEHOLDER = "Summary";

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

    /**
     * Returns true iff there is at least one warning or error message tracked.
     */
    public boolean hasErrorsOrWarnings() {
        return getMessageCount(Level.SEVERE) > 0 || getMessageCount(Level.WARNING) > 0;
    }

    /**
     * Shows a popup reporting the successful import of the provided string, or problems.
     * @param importName the name of something which got imported.
     */
    public void showImportPopup(String importName) {
        showPopup("successfully", "Imported '%s' [problem=with ][summary].", importName);
    }

    /**
     * Shows a popup of varying severity based on the severity of logged messages.
     * Template allows the following substitutes:
     *  [error=text displayed only when there is at least one error]
     *  [warning=text displayed only when there is at least one warning, but no errors]
     *  [problem=text displayed only when there is EITHER at least one warning or one error]
     *  [info=text displayed only when there are no errors or warnings]
     *  [summary] will be substituted with the summary text
     *  The substitutions are case-insensitive.
     * @param fallbackSummary the summary text to display if there were no errors and no warnings.
     * @param messageTemplate the message template to display the popup with.
     * @param templateArguments format string arguments to format the template with
     */
    public void showPopup(String fallbackSummary, String messageTemplate, Object... templateArguments) {
        String formattedMessage = StringUtils.formatStringSafely(messageTemplate, templateArguments);
        showPopup(fallbackSummary, formattedMessage);
    }

    /**
     * Shows a popup of varying severity based on the severity of logged messages.
     * Template allows the following substitutes:
     *  [error=text displayed only when there is at least one error]
     *  [warning=text displayed only when there is at least one warning, but no errors]
     *  [problem=text displayed only when there is EITHER at least one warning or one error]
     *  [info=text displayed only when there are no errors or warnings]
     *  [summary] will be substituted with the summary text
     *  The substitutions are case-insensitive.
     * @param fallbackSummary the summary text to display if there were no errors and no warnings.
     * @param messageTemplate the message template to display the popup with.
     */
    public void showPopup(String fallbackSummary, String messageTemplate) {
        if (fallbackSummary == null)
            throw new NullPointerException("fallbackSummary");
        if (messageTemplate == null)
            throw new NullPointerException("messageTemplate");

        int warningCount = getMessageCount(Level.WARNING);
        int errorCount = getMessageCount(Level.SEVERE);
        AlertType alertType;
        String summaryText;
        if (errorCount > 0 || warningCount > 0) {
            StringBuilder builder = new StringBuilder();
            if (errorCount > 0)
                builder.append(errorCount).append(errorCount != 1 ? " errors" : " error");
            if (warningCount > 0) {
                if (errorCount > 0)
                    builder.append(" and ");
                builder.append(warningCount).append(warningCount != 1 ? " warnings" : " warning");
            }

            alertType = errorCount > 0 ? AlertType.ERROR : AlertType.WARNING;
            summaryText = builder.toString();
            // TODO: Consider showing the messages in the popup too.
            // TODO: Probably want to do a threaded delay.
        } else {
            alertType = AlertType.INFORMATION;
            summaryText = fallbackSummary;
        }

        String displayMessage = resolveTemplateText(messageTemplate, alertType, summaryText);
        FXUtils.showPopup(alertType, "Summary:", displayMessage);
    }

    private enum SectionReadState {
        READING_KEY, READING_VALUE
    }

    private static String resolveTemplateText(String input, AlertType level, String summaryText) {
        StringBuilder result = new StringBuilder();

        boolean wroteSummary = false;
        String tempSectionName = null;
        StringBuilder tempBuilder = new StringBuilder();
        SectionReadState sectionState = null;
        for (int i = 0; i < input.length(); i++) {
            char tempChar = input.charAt(i);

            if (sectionState != null) {
                if (tempChar == '[') { // Reset section.
                    result.append('[');
                    if (sectionState == SectionReadState.READING_VALUE)
                        result.append(tempSectionName).append('=');
                    result.append(tempBuilder);
                    tempBuilder.setLength(0);

                    sectionState = null;
                    tempSectionName = null;
                    i--;
                } else if (tempChar == ']') { // Close section.
                    String sectionText = tempBuilder.toString();
                    if (isResultLevelValid(tempSectionName, null)) {
                        if (isResultLevelValid(tempSectionName, level))
                            result.append(sectionText); // Only write the text if it's the correct level.
                    } else if (WARNING_SUMMARY_PLACEHOLDER.equalsIgnoreCase(sectionText)) {
                        wroteSummary = true;
                        result.append(summaryText);
                    } else {
                        result.append('[');
                        if (sectionState == SectionReadState.READING_VALUE)
                            result.append(tempSectionName).append('=');
                        result.append(tempBuilder).append(']');
                    }

                    sectionState = null;
                    tempSectionName = null;
                    tempBuilder.setLength(0);
                } else if (tempChar == '=' && sectionState == SectionReadState.READING_KEY) { // Switch to reading the value.
                    sectionState = SectionReadState.READING_VALUE;
                    tempSectionName = tempBuilder.toString();
                    tempBuilder.setLength(0);
                } else {
                    tempBuilder.append(tempChar);
                }
            } else if (tempChar == '[') {
                tempBuilder.setLength(0);
                sectionState = SectionReadState.READING_KEY;
            } else {
                result.append(tempChar);
            }
        }

        if (sectionState != null) {
            result.append('[');
            if (sectionState == SectionReadState.READING_VALUE)
                result.append(tempSectionName).append('=');
            result.append(tempBuilder);
        }

        if (!wroteSummary)
            throw new IllegalArgumentException("The input string '" + input + "' did not contain the [" + WARNING_SUMMARY_PLACEHOLDER + "] placeholder!");

        return result.toString();
    }

    private static boolean isResultLevelValid(String input, AlertType level) {
        if (StringUtils.isNullOrWhiteSpace(input))
            return false;

        if ("info".equalsIgnoreCase(input)) {
            return level == null || level == AlertType.INFORMATION;
        } else if ("warning".equalsIgnoreCase(input)) {
            return level == null || level == AlertType.WARNING;
        } else if ("error".equalsIgnoreCase(input)) {
            return level == null || level == AlertType.ERROR;
        } else if ("problem".equalsIgnoreCase(input)) {
            return level == null || level == AlertType.ERROR || level == AlertType.WARNING;
        } else {
            return false;
        }
    }
}
