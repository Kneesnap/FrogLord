package net.highwayfrogs.editor.scripting.compiler.preprocessor.directives;

import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessorContext;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the '#if' preprocessor directive.
 */
public class NoodlePreprocessorDirectiveIf extends NoodlePreprocessorConditionDirective {
    private final List<NoodleToken> conditionTokens = new ArrayList<>();

    public NoodlePreprocessorDirectiveIf(NoodleCodeLocation location) {
        super(NoodlePreprocessorDirectiveType.IF, location);
    }

    protected NoodlePreprocessorDirectiveIf(NoodlePreprocessorDirectiveType directiveType, NoodleCodeLocation location) {
        super(directiveType, location);
    }

    @Override
    public void parseTokens(NoodlePreprocessorContext context) {
        readTokens(context, this.conditionTokens, false);
        super.parseTokens(context);
    }

    @Override
    protected boolean shouldUseTrueBlock(NoodlePreprocessorContext context) {
        return context.getPreprocessor().evaluateExpressionToBoolean(context, this.conditionTokens);
    }
}
