package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Represents a noodle code tree node.
 */
@Getter
@AllArgsConstructor
public class NoodleNode {
    @Setter private NoodleNodeType nodeType;
    private NoodleCodeLocation codeLocation;

    @Override
    public String toString() {
        return getNodeType().name();
    }
}
