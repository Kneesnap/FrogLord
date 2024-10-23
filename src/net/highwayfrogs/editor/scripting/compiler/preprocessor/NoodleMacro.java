package net.highwayfrogs.editor.scripting.compiler.preprocessor;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.compiler.NoodleCallHolder.INoodleCallable;
import net.highwayfrogs.editor.scripting.compiler.NoodleSyntaxException;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a compile-time macro in Noodle.
 * Behavior of noodle macros are near identical to C/C++.
 */
@Getter
public class NoodleMacro implements INoodleCallable {
    private final String name;
    private final List<String> argumentNames = new ArrayList<>();
    private final Set<String> argumentNameSet = new HashSet<>();
    private final List<NoodleToken> tokens = new ArrayList<>();

    public NoodleMacro(String name) {
        this.name = name;
    }

    /**
     * Adds an argument name.
     * @param token The token which the argument name came from.
     * @param name The name to add.
     */
    public void addArgumentName(NoodleToken token, String name) {
        if (!this.argumentNameSet.add(name))
            throw new NoodleSyntaxException("The macro parameter named '%s' is defined more than once.", token, name);
        this.argumentNames.add(name);
    }

    /**
     * Clears the arguments to this macro.
     */
    public void clearArgumentNames() {
        this.argumentNames.clear();
        this.argumentNameSet.clear();
    }

    /**
     * Evaluates the macro recursively.
     * @param context The context to read the macro under.
     * @param macroNameToken The token which prompted macro evaluation.
     * @param replacements The replacements to replace the original tokens with.
     * @return tokens from the evaluated macros.
     */
    public List<NoodleToken> evaluateMacro(NoodlePreprocessorContext context, NoodleToken macroNameToken, List<List<NoodleToken>> replacements) {
        int replacementCount = replacements != null ? replacements.size() : 0;
        if (getArgumentCount() != replacementCount)
            throw new NoodleSyntaxException("The macro '%s' takes %d arguments, but %d were given.", macroNameToken, getSignature(), getArgumentCount(), replacementCount);
        if (!context.getCompileContext().getMacrosCurrentlyEvaluating().add(this))
            throw new NoodleSyntaxException("The macro '%s(%d args)' references itself, making a circular evaluation.", macroNameToken, this.name, getArgumentCount());

        NoodlePreprocessorContext newContext = new NoodlePreprocessorContext(context.getCompileContext(), this.tokens);
        List<NoodleToken> results = new ArrayList<>();
        while (newContext.hasMoreTokens()) {
            NoodleToken tempToken = newContext.getCurrentTokenIncrement();

            // Clone token and use new position.
            if (tempToken.getTokenType() == NoodleTokenType.BACKSLASH) {
                continue; // Skip, don't add to the results.
            } else if (tempToken.getTokenType() != NoodleTokenType.IDENTIFIER) {
                results.add(tempToken);
                continue;
            }

            // Evaluates the identifier.
            newContext.decrementToken(); // Go back to the identifier.
            List<NoodleToken> macroTokens = context.getPreprocessor().evaluateIdentifier(newContext, true);
            if (macroTokens != null) {
                results.addAll(macroTokens);
            } else {
                results.add(tempToken);
                newContext.incrementToken();
            }
        }

        // Allow evaluating this macro again.
        context.getCompileContext().getMacrosCurrentlyEvaluating().remove(this);
        return results;
    }
}
