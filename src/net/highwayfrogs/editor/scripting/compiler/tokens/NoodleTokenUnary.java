package net.highwayfrogs.editor.scripting.compiler.tokens;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.compiler.NoodleUnaryOperator;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * NoodleToken with a unary operator.
 */
@Getter
public class NoodleTokenUnary extends NoodleToken {
    private final NoodleUnaryOperator unaryOperator;

    public NoodleTokenUnary(NoodleTokenType tokenType, NoodleCodeLocation codeLocation, NoodleUnaryOperator unaryOperator) {
        super(tokenType, codeLocation);
        this.unaryOperator = unaryOperator;
    }

    @Override
    public String toString() {
        return super.toString() + " " + this.unaryOperator.name();
    }
}
