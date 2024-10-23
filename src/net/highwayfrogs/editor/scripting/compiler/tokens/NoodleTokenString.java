package net.highwayfrogs.editor.scripting.compiler.tokens;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleUtils;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * A noodle instruction with a string.
 */
@Getter
public class NoodleTokenString extends NoodleToken {
    private final String stringData;

    public NoodleTokenString(NoodleTokenType tokenType, NoodleCodeLocation codeLocation, String dataString) {
        super(tokenType, codeLocation);
        this.stringData = dataString;
    }

    @Override
    public String toString() {
        return super.toString() + " \"" + NoodleUtils.compiledStringToCodeString(this.stringData) + "\"";
    }
}
