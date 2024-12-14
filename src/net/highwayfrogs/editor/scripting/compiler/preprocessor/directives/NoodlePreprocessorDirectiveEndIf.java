package net.highwayfrogs.editor.scripting.compiler.preprocessor.directives;

import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessorContext;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.List;

/**
 * Implementation of the '#endif' preprocessor directive.
 */
public class NoodlePreprocessorDirectiveEndIf extends NoodlePreprocessorDirective {
    public NoodlePreprocessorDirectiveEndIf(NoodleCodeLocation location) {
        super(NoodlePreprocessorDirectiveType.ENDIF, location);
    }

    @Override
    public void parseTokens(NoodlePreprocessorContext context) {
        // Don't need to read anything.
    }

    @Override
    public List<NoodleToken> processAndWriteTokens(NoodlePreprocessorContext context) {
        // Don't write this, do nothing.
        return null;
    }
}