package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.List;

/**
 * A noodle node with switch data.
 */
@Getter
public class NoodleNodeSwitch extends NoodleNode {
    private final NoodleNode expression;
    private final List<NoodleNode> caseStatements;
    private final List<NoodleNode> caseBlocks;
    private final NoodleNode defaultCase;

    public NoodleNodeSwitch(NoodleCodeLocation codeLocation, NoodleNode expression, List<NoodleNode> caseStatements, List<NoodleNode> caseBlocks, NoodleNode defaultCase, boolean isSelect) {
        super(isSelect ? NoodleNodeType.SELECT : NoodleNodeType.SWITCH, codeLocation);
        this.expression = expression;
        this.caseStatements = caseStatements;
        this.caseBlocks = caseBlocks;
        this.defaultCase = defaultCase;
    }

    @Override
    public String toString() {
        return super.toString() + " (" + getExpression() + ")";
    }
}
