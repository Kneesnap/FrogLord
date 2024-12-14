package net.highwayfrogs.editor.scripting.compiler.nodes;

import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * A node which represents null.
 */
public class NoodleNodeNull extends NoodleNode {
    public NoodleNodeNull(NoodleCodeLocation codeLocation) {
        super(NoodleNodeType.NULL, codeLocation);
    }
}
