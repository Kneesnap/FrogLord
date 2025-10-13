package net.highwayfrogs.editor.utils.commandparser;

import net.highwayfrogs.editor.utils.StringUtils;

/**
 * Represents a syntax error from improperly formatted syntax in a command list.
 * Created by Kneesnap on 10/11/2025.
 */
public class CommandListException extends Throwable {
    public CommandListException(String template, Object... arguments) {
        super(StringUtils.formatStringSafely(template, arguments));
    }

    public CommandListException(Throwable cause, String template, Object... arguments) {
        super(StringUtils.formatStringSafely(template, arguments), cause);
    }

    public CommandListException(CommandLocation location, String template, Object... arguments) {
        super(getMessage(StringUtils.formatStringSafely(template, arguments), location));
    }

    public CommandListException(CommandLocation location, Throwable cause, String template, Object... arguments) {
        super(getMessage(StringUtils.formatStringSafely(template, arguments), location), cause);
    }

    protected static String getMessage(String message, CommandLocation location) {
        return message + (location != null ? " (" + location.getPositionText(false) + ")" : "");
    }

    /**
     * Represents an error which occurred regarding the syntax of a command.
     */
    public static class CommandListSyntaxError extends CommandListException {
        public CommandListSyntaxError(String template, Object... arguments) {
            super(template, arguments);
        }

        public CommandListSyntaxError(CommandLocation location, String template, Object... arguments) {
            super(location, template, arguments);
        }

        public CommandListSyntaxError(CommandLocation location, Throwable cause, String template, Object... arguments) {
            super(location, cause, template, arguments);
        }
    }

    /**
     * Represents an error which occurred during the execution of a command.
     */
    public static class CommandListExecutionError extends CommandListException {
        public CommandListExecutionError(String template, Object... arguments) {
            super(template, arguments);
        }

        public CommandListExecutionError(Throwable cause, String template, Object... arguments) {
            super(cause, template, arguments);
        }

        public CommandListExecutionError(CommandLocation location, String template, Object... arguments) {
            super(location, template, arguments);
        }

        public CommandListExecutionError(CommandLocation location, Throwable cause, String template, Object... arguments) {
            super(location, cause, template, arguments);
        }
    }
}