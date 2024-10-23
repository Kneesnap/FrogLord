package net.highwayfrogs.editor.scripting.compiler.nodes;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.directives.NoodlePreprocessorDirective;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Represents a preprocessor directive.
 * A preprocessor directive is an instruction to the preprocessor.
 */
@Getter
public class NoodleNodePreprocessorDirective extends NoodleNode {
    private final NoodlePreprocessorDirective directive;

    public NoodleNodePreprocessorDirective(NoodleCodeLocation codeLocation, NoodlePreprocessorDirective directive) {
        super(NoodleNodeType.PREPROCESSOR_DIRECTIVE, codeLocation);
        this.directive = directive;
    }

    @Override
    public String toString() {
        return super.toString() + " [" + this.directive.toString() + "]";
    }
}
