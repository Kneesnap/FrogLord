package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Used for ++value or value++.
 */
@Getter
public class NoodleNodePrePostOperator extends NoodleNode {
    private final NoodleNode node;
    private final double value;

    public NoodleNodePrePostOperator(NoodleNodeType nodeType, NoodleCodeLocation codeLocation, NoodleNode node, double value) {
        super(nodeType, codeLocation);
        this.node = node;
        this.value = value;
    }

    @Override
    public String toString() {
        return super.toString() + " " + getValue();
    }
}
