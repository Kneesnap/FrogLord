package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * A noodle node that keeps track of a number.
 */
@Getter
public class NoodleNodePrimitive extends NoodleNode {
    private final NoodlePrimitive numberValue;

    public NoodleNodePrimitive(NoodleCodeLocation codeLocation, NoodlePrimitive numValue) {
        super(NoodleNodeType.PRIMITIVE, codeLocation);
        this.numberValue = numValue;
    }

    @Override
    public String toString() {
        return super.toString() + " " + this.numberValue;
    }
}
