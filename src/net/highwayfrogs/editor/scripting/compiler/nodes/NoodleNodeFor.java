package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Holds data for a for statement.
 */
@Getter
public class NoodleNodeFor extends NoodleNode {
    private final NoodleNode initial;
    private final NoodleNode condition;
    private final NoodleNode post;
    private final NoodleNode body;

    public NoodleNodeFor(NoodleCodeLocation codeLocation, NoodleNode initial, NoodleNode condition, NoodleNode post, NoodleNode body) {
        super(NoodleNodeType.FOR, codeLocation);
        this.initial = initial;
        this.condition = condition;
        this.post = post;
        this.body = body;
    }

    @Override
    public String toString() {
        return super.toString() + " [" + getInitial() + "] [" + getCondition() + "] [" + getPost() + "] [" + getBody() + "]";
    }
}
