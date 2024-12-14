package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.compiler.NoodleOperator;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Contains information for the "set" instruction, so "x = y".
 */
@Getter
public class NoodleNodeSet extends NoodleNode {
    private final NoodleOperator operator;
    private final NoodleNode destination;
    private final NoodleNode value;

    public NoodleNodeSet(NoodleCodeLocation codeLocation, NoodleOperator operator, NoodleNode destination, NoodleNode value) {
        super(NoodleNodeType.SET, codeLocation);
        this.operator = operator;
        this.destination = destination;
        this.value = value;
    }

    @Override
    public String toString() {
        return super.toString() + " [" + getDestination() + "] " + getOperator().getSymbol() + " [" + getValue() + "]";
    }
}
