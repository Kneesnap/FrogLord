package net.highwayfrogs.editor.scripting.compiler.preprocessor.directives;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.scripting.compiler.NoodleCompileContext;
import net.highwayfrogs.editor.scripting.compiler.nodes.NoodleNodePreprocessorDirective;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessorContext;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenType;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeSource;

import java.util.List;

/**
 * Represents a noodle preprocessor directive.
 */
@Getter
@AllArgsConstructor
public abstract class NoodlePreprocessorDirective {
    @NonNull private NoodlePreprocessorDirectiveType directiveType;
    private NoodleCodeLocation codeLocation;

    /**
     * Parses tokens into data used by this directive.
     * @param context The context to parse from.
     */
    public void parseTokens(NoodleCompileContext context) {
        // By default, do nothing.
    }

    /**
     * Parses tokens into data used by this directive.
     * @param context The context to parse from.
     */
    public abstract void parseTokens(NoodlePreprocessorContext context);

    /**
     * Writes the tokens which result from this compiler directive, if any.
     * @param context The context to write to.
     */
    public abstract List<NoodleToken> processAndWriteTokens(NoodlePreprocessorContext context);

    /**
     * Whether this directive should be kept for when the AST is built.
     * Defaults to false.
     */
    public boolean shouldStayForAST() {
        return false;
    }

    /**
     * Reads tokens until the end of the line.
     * @param context The context to read the tokens from.
     * @param output The output to write the tokens to.
     * @param allowMultipleLines If this is true, reading will continue to the next line if there is a '\' at the end of the first line.
     */
    protected void readTokens(NoodlePreprocessorContext context, List<NoodleToken> output, boolean allowMultipleLines) {
        NoodleToken lastToken = context.getCurrentToken();
        NoodleCodeSource mainCodeSource = lastToken.getCodeLocation().getSource();
        int lastAllowedLine = lastToken.getLineNumber();

        while (context.hasMoreTokens()) {
            NoodleToken peekToken = context.getCurrentToken();
            NoodleCodeSource peekTokenSource = peekToken.getCodeLocation().getSource();
            int tokenLine = peekToken.getLineNumber();

            // If tokens have been included from somewhere else (eg macro, include, etc), we want to know that,
            //  so we don't mistake that for being a new line outside of the current macro.
            // We also need to be sure we handle macros defined in the same file.
            boolean isMainSource = (mainCodeSource == peekTokenSource) && (tokenLine >= lastAllowedLine);

            // Exit if we have reached the next line without having extended to support that new line.
            if (isMainSource && tokenLine > lastAllowedLine)
                break; // Reached a new line.

            if (isMainSource && allowMultipleLines && peekToken.getTokenType() == NoodleTokenType.BACKSLASH) {
                lastAllowedLine = tokenLine + 1;
            } else {
                output.add(peekToken);
                lastAllowedLine = tokenLine; // If there's a backslash, then a non-backslash, we don't want to allow the next line.
            }

            context.incrementToken();
        }
    }

    /**
     * Compiles the preprocessor directive into noodle instructions.
     * Usually, directives will not add any instructions. This is because they are usually compile-time only.
     * However, this does work in the case where such a thing is necessary.
     * @param context The compiler context.
     * @param node The node which represents this directive.
     */
    public void compile(NoodleCompileContext context, NoodleNodePreprocessorDirective node) {
        // By default, do nothing.
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        this.toString(builder);
        return builder.toString();
    }

    /**
     * Gets a stringified version of this directive.
     * @param builder The builder to write to.
     */
    public void toString(StringBuilder builder) {
        builder.append("#").append(this.directiveType.getMnemonic());
    }
}
