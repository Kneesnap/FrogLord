package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.List;

/**
 * A noodle node which defines a function.
 */
@Getter
public class NoodleNodeFunctionDefinition extends NoodleNode {
    private final String functionName;
    private final List<NoodleNodeString> argumentNames;
    private final NoodleNodeBlock functionBody;

    public NoodleNodeFunctionDefinition(NoodleCodeLocation codeLocation, String functionName, List<NoodleNodeString> argumentNames, NoodleNodeBlock functionBody) {
        super(NoodleNodeType.DEFINE_FUNCTION, codeLocation);
        this.functionName = functionName;
        this.argumentNames = argumentNames;
        this.functionBody = functionBody;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(super.toString()).append(" ").append(this.functionName).append("(");
        for (int i = 0; i < getArgumentNames().size(); i++) {
            if (i > 0)
                result.append(", ");
            result.append(getArgumentNames().get(i));
        }

        return result.append(")").toString();
    }
}