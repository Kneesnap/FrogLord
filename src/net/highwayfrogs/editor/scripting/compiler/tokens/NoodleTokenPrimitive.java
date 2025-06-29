package net.highwayfrogs.editor.scripting.compiler.tokens;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Represents a noodle instruction with a number.
 */
@Getter
public class NoodleTokenPrimitive extends NoodleToken {
    private final NoodlePrimitive primitive;

    public NoodleTokenPrimitive(NoodleTokenType tokenType, NoodleCodeLocation codeLocation, NoodlePrimitive primitive) {
        super(tokenType, codeLocation);
        this.primitive = primitive;
    }

    @Override
    public String toString() {
        return super.toString() + "=" + getPrimitive();
    }
}
