package net.highwayfrogs.editor.file.config.script;

/**
 * An error which occurs while parsing a script.
 * Created by Kneesnap on 8/1/2019.
 */
public class ScriptParseException extends RuntimeException {
    public ScriptParseException(String message) {
        super(message);
    }

    public ScriptParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
