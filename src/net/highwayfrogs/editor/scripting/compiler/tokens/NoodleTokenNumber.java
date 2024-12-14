package net.highwayfrogs.editor.scripting.compiler.tokens;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Represents a noodle instruction with a number.
 */
@Getter
public class NoodleTokenNumber extends NoodleToken {
    private final double number;

    public NoodleTokenNumber(NoodleTokenType tokenType, NoodleCodeLocation codeLocation, double number) {
        super(tokenType, codeLocation);
        this.number = number;
    }

    @Override
    public String toString() {
        return super.toString() + "=" + getNumber();
    }
}
