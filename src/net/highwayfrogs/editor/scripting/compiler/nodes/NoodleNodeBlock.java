package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.List;

/**
 * A node which is a block of nodes.
 */
@Getter
public class NoodleNodeBlock extends NoodleNode {
    private final List<NoodleNode> nodes;

    public NoodleNodeBlock(NoodleCodeLocation codeLocation, List<NoodleNode> nodes) {
        super(NoodleNodeType.BLOCK, codeLocation);
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return super.toString() + " " + getNodes().size() + " nodes";
    }
}
