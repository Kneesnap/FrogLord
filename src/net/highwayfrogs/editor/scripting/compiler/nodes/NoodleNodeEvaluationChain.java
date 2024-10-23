package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Represents one step of an evaluation chain. An example evaluation chain is "player.location.block.setType("AIR")"
 * An example of what this node might hold is the node for "player" and the node for "location.block.setType("AIR"))" (which, would recursively use these nodes until we reach the last one.)
 */
@Getter
public class NoodleNodeEvaluationChain extends NoodleNode {
    private final NoodleNode currentNode; // The node which represents the "source", which the other node evaluates on.
    private final NoodleNode remainingChainNode; // The node which transforms the source into something else.

    public NoodleNodeEvaluationChain(NoodleCodeLocation codeLocation, NoodleNode sourceNode, NoodleNode remainingChainNode) {
        super(NoodleNodeType.EVALUATION_CHAIN, codeLocation);
        this.currentNode = sourceNode;
        this.remainingChainNode = remainingChainNode;
    }

    @Override
    public String toString() {
        return super.toString() + " [" + this.currentNode + "." + this.remainingChainNode + "]";
    }
}
