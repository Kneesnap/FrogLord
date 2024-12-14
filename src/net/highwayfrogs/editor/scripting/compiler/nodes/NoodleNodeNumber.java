package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * A noodle node that keeps track of a number.
 */
@Getter
public class NoodleNodeNumber extends NoodleNode {
    private final double numberValue;

    public NoodleNodeNumber(NoodleCodeLocation codeLocation, double numValue) {
        super(NoodleNodeType.NUMBER, codeLocation);
        this.numberValue = numValue;
    }

    @Override
    public String toString() {
        return super.toString() + " " + this.numberValue;
    }
}
