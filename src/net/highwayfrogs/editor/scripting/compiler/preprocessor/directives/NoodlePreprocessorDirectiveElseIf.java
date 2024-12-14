package net.highwayfrogs.editor.scripting.compiler.preprocessor.directives;

import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Implementation of the '#elseif' preprocessor directive.
 */
public class NoodlePreprocessorDirectiveElseIf extends NoodlePreprocessorDirectiveIf {
    public NoodlePreprocessorDirectiveElseIf(NoodleCodeLocation location) {
        super(NoodlePreprocessorDirectiveType.ELSEIF, location);
    }
}
