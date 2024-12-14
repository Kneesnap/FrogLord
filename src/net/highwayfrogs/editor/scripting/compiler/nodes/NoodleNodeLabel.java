package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * A node with label data.
 */
@Getter
public class NoodleNodeLabel extends NoodleNode {
    private final String name;
    private final NoodleNode node;

    public NoodleNodeLabel(NoodleCodeLocation codeLocation, String name, NoodleNode node) {
        super(NoodleNodeType.LABEL, codeLocation);
        this.name = name;
        this.node = node;
    }

    @Override
    public String toString() {
        return super.toString() + " " + getName();
    }
}
