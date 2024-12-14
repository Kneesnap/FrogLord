package net.highwayfrogs.editor.scripting.compiler.preprocessor.directives;

import net.highwayfrogs.editor.scripting.NoodleUtils;
import net.highwayfrogs.editor.scripting.compiler.NoodleCompileContext;
import net.highwayfrogs.editor.scripting.compiler.NoodleCompiler;
import net.highwayfrogs.editor.scripting.compiler.NoodleSyntaxException;
import net.highwayfrogs.editor.scripting.compiler.nodes.NoodleNodePreprocessorDirective;
import net.highwayfrogs.editor.scripting.compiler.nodes.NoodleNodeString;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessorContext;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.List;

/**
 * Throws an error. Useful for requiring certain compile-time conditions be met.
 */
public class NoodlePreprocessorDirectiveError extends NoodlePreprocessorDirective {
    private String errorMessage = null;

    public NoodlePreprocessorDirectiveError(NoodleCodeLocation location) {
        super(NoodlePreprocessorDirectiveType.ERROR, location);
    }

    @Override
    public void parseTokens(NoodlePreprocessorContext context) {
        this.errorMessage = context.getPreprocessor().evaluateExpression(context).getAsString();
    }

    @Override
    public void parseTokens(NoodleCompileContext context) {
        NoodleCompiler.buildExpression(context, NoodleCompiler.FLAG_NO_OPERATORS);
        this.errorMessage = ((NoodleNodeString) context.getNode()).getStringValue();
    }

    @Override
    public void compile(NoodleCompileContext context, NoodleNodePreprocessorDirective node) {
        throw new NoodleSyntaxException("Code threw error '%s'", node, this.errorMessage);
    }

    @Override
    public List<NoodleToken> processAndWriteTokens(NoodlePreprocessorContext context) {
        throw new NoodleSyntaxException("Code threw error '%s'", this, this.errorMessage);
    }

    @Override
    public void toString(StringBuilder builder) {
        super.toString(builder);
        builder.append(" ").append(NoodleUtils.compiledStringToCodeString(this.errorMessage.toString()));
    }
}
