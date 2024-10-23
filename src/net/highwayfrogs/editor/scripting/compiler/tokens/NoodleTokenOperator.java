package net.highwayfrogs.editor.scripting.compiler.tokens;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.compiler.NoodleOperator;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Represents an instruction with an operator at the end.
 */
@Getter
public class NoodleTokenOperator extends NoodleToken {
    private final NoodleOperator operator;

    public NoodleTokenOperator(NoodleTokenType tokenType, NoodleCodeLocation codeLocation, NoodleOperator operator) {
        super(tokenType, codeLocation);
        this.operator = operator;
    }

    @Override
    public String toString() {
        String operatorName = this.operator != null ? this.operator.getSymbol() : "NULL";
        if (operatorName == null)
            operatorName = this.operator.name();

        return super.toString() + " " + operatorName;
    }
}
