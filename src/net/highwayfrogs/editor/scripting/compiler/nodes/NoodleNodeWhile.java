package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * A node with while loop data.
 */
@Getter
public class NoodleNodeWhile extends NoodleNode {
    private final NoodleNode condition;
    private final NoodleNode loop;

    public NoodleNodeWhile(NoodleNodeType nodeType, NoodleCodeLocation codeLocation, NoodleNode condition, NoodleNode loop) {
        super(nodeType, codeLocation);
        this.condition = condition;
        this.loop = loop;
    }

    @Override
    public String toString() {
        return super.toString() + " (" + getCondition() + ") [" + getLoop() + "]";
    }
}
