package net.highwayfrogs.editor.games.sony.frogger.data.scripts;

/**
 * An error which occurs while parsing a script.
 * Created by Kneesnap on 8/1/2019.
 */
public class FroggerScriptParseException extends RuntimeException {
    public FroggerScriptParseException(String message) {
        super(message);
    }

    public FroggerScriptParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
