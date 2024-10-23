package net.highwayfrogs.editor.scripting.compiler.preprocessor.directives;

import net.highwayfrogs.editor.scripting.compiler.NoodleCompileContext;
import net.highwayfrogs.editor.scripting.compiler.NoodleSyntaxException;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessorContext;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Implementation of the '#else' preprocessor directive.
 */
public class NoodlePreprocessorDirectiveElse extends NoodlePreprocessorConditionDirective {
    protected NoodlePreprocessorDirectiveElse(NoodleCodeLocation location) {
        super(NoodlePreprocessorDirectiveType.ELSE, location);
    }

    @Override
    public void parseTokens(NoodleCompileContext context) {
        super.parseTokens(context);
        if (this.nextDirective != null)
            throw new NoodleSyntaxException("#else directive MUST be followed by #endif directive, but was instead followed by '%s'.", context.getCurrentToken(), this.nextDirective);
    }

    @Override
    protected boolean shouldUseTrueBlock(NoodlePreprocessorContext context) {
        return true;
    }
}
