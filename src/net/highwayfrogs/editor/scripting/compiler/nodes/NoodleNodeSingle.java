package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * A noodle node which has a singular node held.
 */
@Getter
public class NoodleNodeSingle extends NoodleNode {
    private final NoodleNode node;

    public NoodleNodeSingle(NoodleNodeType nodeType, NoodleCodeLocation codeLocation, NoodleNode node) {
        super(nodeType, codeLocation);
        this.node = node;
    }

    @Override
    public String toString() {
        return super.toString() + " [" + getNode() + "]";
    }
}
