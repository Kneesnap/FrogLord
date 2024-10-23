package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.List;

/**
 * A noodle node with function call data.
 */
@Getter
public class NoodleNodeFunctionCall extends NoodleNode {
    private final String functionName;
    private final List<NoodleNode> args;

    public NoodleNodeFunctionCall(NoodleCodeLocation codeLocation, String functionName, List<NoodleNode> args) {
        super(NoodleNodeType.CALL, codeLocation);
        this.functionName = functionName;
        this.args = args;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(super.toString()).append(" ").append(this.functionName).append("(");
        for (int i = 0; i < getArgs().size(); i++) {
            if (i > 0)
                result.append(", ");
            result.append(getArgs().get(i));
        }

        return result.append(")").toString();
    }
}