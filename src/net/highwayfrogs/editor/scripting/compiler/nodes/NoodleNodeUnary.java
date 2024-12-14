package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.compiler.NoodleUnaryOperator;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * A noodle node with a unary operation.
 */
@Getter
public class NoodleNodeUnary extends NoodleNode {
    private final NoodleUnaryOperator unaryOperator;
    private final NoodleNode node;

    public NoodleNodeUnary(NoodleCodeLocation codeLocation, NoodleUnaryOperator unaryOperator, NoodleNode node) {
        super(NoodleNodeType.UNARY_OPERATOR, codeLocation);
        this.unaryOperator = unaryOperator;
        this.node = node;
    }

    @Override
    public String toString() {
        return super.toString() + " " + getUnaryOperator() + " [" + getNode() + "]";
    }
}
