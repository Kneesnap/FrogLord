package net.highwayfrogs.editor.scripting.compiler.tokens;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Represents a single noodle instruction.
 */
@Getter
@AllArgsConstructor
public class NoodleToken {
    private NoodleTokenType tokenType;
    private NoodleCodeLocation codeLocation;

    /**
     * Gets the line number which this token came from.
     */
    public int getLineNumber() {
        return this.codeLocation != null ? this.codeLocation.getLineNumber() : -1;
    }

    @Override
    public String toString() {
        return this.tokenType.name();
    }
}
