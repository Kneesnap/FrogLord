package net.highwayfrogs.editor.scripting.compiler.preprocessor.directives;

import net.highwayfrogs.editor.scripting.compiler.NoodleCompiler;
import net.highwayfrogs.editor.scripting.compiler.NoodleSyntaxException;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodleMacro;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessorContext;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenString;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenType;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.List;

/**
 * Implementation of the '#define' preprocessor directive.
 */
public class NoodlePreprocessorDirectiveDefine extends NoodlePreprocessorDirective {
    private NoodleMacro macro;

    public NoodlePreprocessorDirectiveDefine(NoodleCodeLocation location) {
        super(NoodlePreprocessorDirectiveType.DEFINE, location);
    }

    @Override
    public void parseTokens(NoodlePreprocessorContext context) {
        NoodleToken startToken = context.getCurrentTokenIncrement();
        if (startToken.getTokenType() != NoodleTokenType.IDENTIFIER)
            throw new NoodleSyntaxException("#define expected an identifier, but got '%s' instead.", startToken, startToken);

        String macroName = ((NoodleTokenString) startToken).getStringData();
        NoodleMacro macro = new NoodleMacro(macroName);

        // Read arguments, if any.
        readArgumentNames(macro, context);

        // Read tokens.
        readTokens(context, macro.getTokens(), true);

        // Verify we can register this macro.
        NoodleCompiler.verifyNameIsAvailable(context.getCompileContext(), startToken, "macro", macro.getName(), macro.getArgumentCount());

        // Register macro.
        if (!context.getCompileContext().getMacros().registerCallable(macro))
            throw new NoodleSyntaxException("Macro '%s(%d args)' is defined more than once.", startToken, macro.getName(), macro.getArgumentCount()); // This should not throw here, since it should throw before with verifyNameIsAvailable.

        this.macro = macro;
    }

    @Override
    public List<NoodleToken> processAndWriteTokens(NoodlePreprocessorContext context) {
        // Macro definitions don't write anything, only macro usages.
        return null;
    }

    private void readArgumentNames(NoodleMacro macro, NoodlePreprocessorContext context) {
        NoodleToken startToken = context.getCurrentToken();
        if (startToken.getTokenType() != NoodleTokenType.PAR_OPEN)
            return; // Nothing to read.

        // Attempt to read argument names.
        int startTokenIndex = context.getCurrentTokenIndex();
        int macroDefinitionLine = startToken.getLineNumber();
        boolean hasValidArguments = false;
        context.incrementToken(); // Skip parenthesis open.
        while (context.hasMoreTokens()) {
            NoodleToken argumentToken = context.getCurrentTokenIncrement();

            if (argumentToken.getLineNumber() != macroDefinitionLine) {
                break; // Line changed.
            } else if (argumentToken.getTokenType() == NoodleTokenType.COMMA) {
                continue;
            } else if (argumentToken.getTokenType() == NoodleTokenType.PAR_CLOSE) {
                hasValidArguments = true;
                break;
            } else if (argumentToken.getTokenType() == NoodleTokenType.IDENTIFIER) {
                macro.addArgumentName(argumentToken, ((NoodleTokenString) argumentToken).getStringData());
            } else {
                // Not arguments.
                break;
            }
        }

        // If the tokens didn't actually form arguments, they're probably just part of the macro.
        if (!hasValidArguments) {
            // Reset time.
            context.setCurrentTokenIndex(startTokenIndex);
            macro.clearArgumentNames();
        }
    }

    @Override
    public void toString(StringBuilder builder) {
        super.toString(builder);

        builder.append(' ');
        this.macro.writeSignature(builder);

        // Write tokens.
        for (int i = 0; i < this.macro.getTokens().size(); i++)
            builder.append(' ').append(this.macro.getTokens().get(i));
    }
}
