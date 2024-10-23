package net.highwayfrogs.editor.scripting.compiler.preprocessor.directives;

import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessorContext;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;

import java.util.List;

/**
 * Manually preprocesses results.
 */
public interface INoodleManualPreprocessor {

    /**
     * Runs the preprocessor on the given tokens with special handling.
     * @param context The context to run the preprocessor under.
     * @param tokens The tokens to handle preprocessing of.
     */
    public void runPreprocessor(NoodlePreprocessorContext context, List<NoodleToken> tokens);
}
