package net.highwayfrogs.editor.scripting.runtime;

import net.highwayfrogs.editor.utils.StringUtils;

/**
 * An exception that occurred while a script was running.
 */
public class NoodleRuntimeException extends RuntimeException {
    public NoodleRuntimeException(String error) {
        super(error);
    }

    public NoodleRuntimeException(Throwable ex, String error) {
        super(error, ex);
    }

    public NoodleRuntimeException(String error, Object... args) {
        super(StringUtils.formatStringSafely(error, args));
    }

    public NoodleRuntimeException(Throwable ex, String error, Object... args) {
        super(StringUtils.formatStringSafely(error, args), ex);
    }
}
