package net.highwayfrogs.editor.scripting.compiler.preprocessor.directives;

import net.highwayfrogs.editor.scripting.compiler.NoodleCompileContext;
import net.highwayfrogs.editor.scripting.compiler.NoodleSyntaxException;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessorContext;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenString;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenType;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Implementation of the '#ifdef' preprocessor directive.
 */
public class NoodlePreprocessorDirectiveIfDef extends NoodlePreprocessorConditionDirective {
    private String symbolName;

    protected NoodlePreprocessorDirectiveIfDef(NoodleCodeLocation location) {
        super(NoodlePreprocessorDirectiveType.IFDEF, location);
    }

    @Override
    public void parseTokens(NoodleCompileContext context) {
        NoodleToken startToken = context.getCurrentToken();
        if (startToken.getTokenType() != NoodleTokenType.IDENTIFIER)
            throw new NoodleSyntaxException("Required IDENTIFIER to follow #ifdef, but received '%s' instead.", startToken, startToken);

        this.symbolName = ((NoodleTokenString) startToken).getStringData();

        // Build normal.
        super.parseTokens(context);
    }

    @Override
    protected boolean shouldUseTrueBlock(NoodlePreprocessorContext context) {
        return context.getCompileContext().getMacros().hasByName(this.symbolName);
    }
}
