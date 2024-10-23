package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.compiler.NoodleOperator;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * A noodle node which has data for a binary operation.
 */
@Getter
public class NoodleNodeBinaryOp extends NoodleNode {
    private final NoodleOperator operator;
    private final NoodleNode first;
    private final NoodleNode second;

    public NoodleNodeBinaryOp(NoodleCodeLocation codeLocation, NoodleOperator operator, NoodleNode first, NoodleNode second) {
        super(NoodleNodeType.BINARY_OPERATOR, codeLocation);
        this.operator = operator;
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return super.toString() + " [" + getFirst() + "] " + getOperator().getSymbol() + " [" + getSecond() + "]";
    }
}
