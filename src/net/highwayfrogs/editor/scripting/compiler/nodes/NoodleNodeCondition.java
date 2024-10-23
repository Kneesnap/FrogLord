package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * A noodle node with conditional data.
 */
@Getter
public class NoodleNodeCondition extends NoodleNode {
    private final NoodleNode condition;
    private final NoodleNode trueLogic;
    private final NoodleNode falseLogic;

    public NoodleNodeCondition(NoodleNodeType nodeType, NoodleCodeLocation codeLocation, NoodleNode condition, NoodleNode trueLogic, NoodleNode falseLogic) {
        super(nodeType, codeLocation);
        this.condition = condition;
        this.trueLogic = trueLogic;
        this.falseLogic = falseLogic;
    }

    @Override
    public String toString() {
        return super.toString() + " [" + getCondition() + "]";
    }
}
