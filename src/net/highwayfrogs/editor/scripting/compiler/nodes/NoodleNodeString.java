package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleUtils;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * A noodle node which keeps track of a string.
 */
@Getter
public class NoodleNodeString extends NoodleNode {
    private final String stringValue;

    public NoodleNodeString(NoodleNodeType nodeType, NoodleCodeLocation codeLocation, String str) {
        super(nodeType, codeLocation);
        this.stringValue = str;
    }

    @Override
    public String toString() {
        return super.toString() + " \"" + NoodleUtils.compiledStringToCodeString(getStringValue()) + "\"";
    }
}
