package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Represents a return statement.
 */
@Getter
public class NoodleNodeReturn extends NoodleNode {
    private final NoodleNode returnValue;

    public NoodleNodeReturn(NoodleCodeLocation codeLocation, NoodleNode node) {
        super(NoodleNodeType.RETURN, codeLocation);
        this.returnValue = node;
    }

    @Override
    public String toString() {
        return super.toString() + " [" + this.returnValue + "]";
    }
}
