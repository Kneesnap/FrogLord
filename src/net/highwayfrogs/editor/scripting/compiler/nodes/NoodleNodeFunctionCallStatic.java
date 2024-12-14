package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.List;

/**
 * This node represents a static function call.
 */
@Getter
public class NoodleNodeFunctionCallStatic extends NoodleNode {
    private final NoodleObjectTemplate<?> objectTemplate;
    private final String functionName;
    private final List<NoodleNode> arguments;

    public NoodleNodeFunctionCallStatic(NoodleCodeLocation codeLocation, NoodleObjectTemplate<?> objectType, String functionName, List<NoodleNode> arguments) {
        super(NoodleNodeType.CALL_STATIC, codeLocation);
        this.objectTemplate = objectType;
        this.functionName = functionName;
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return super.toString() + " " + this.objectTemplate.getName() + "." + this.functionName + "[" + this.arguments.size() + " args]";
    }
}