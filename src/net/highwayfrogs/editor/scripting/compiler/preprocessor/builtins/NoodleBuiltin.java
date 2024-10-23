package net.highwayfrogs.editor.scripting.compiler.preprocessor.builtins;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.compiler.NoodleCallHolder.INoodleCallable;
import net.highwayfrogs.editor.scripting.compiler.NoodleSyntaxException;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessorContext;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a noodle preprocessor builtin value.
 */
@Getter
public abstract class NoodleBuiltin implements INoodleCallable {
    private final String name;
    private final List<String> argumentNames;

    public NoodleBuiltin(String name, String... argumentNames) {
        this.name = name;
        this.argumentNames = new ArrayList<>(Arrays.asList(argumentNames));
    }

    /**
     * Executes this builtin macro.
     * @param context The context to execute under.
     * @param parameters The parameters to execute with.
     * @return result
     */
    public abstract List<NoodleToken> execute(NoodlePreprocessorContext context, List<List<NoodleToken>> parameters);

    /**
     * Gets a single noodle token.
     * @param parameters The parameters to get the token from.
     * @param index The index into the parameters to get.
     * @return singleNoodleToken
     */
    protected NoodleToken getSingleNoodleToken(List<List<NoodleToken>> parameters, int index) {
        if (index >= parameters.size())
            throw new NoodleSyntaxException("Expected argument at index %d for preprocessor command %s.", (NoodleToken) null, index, this.name);
        List<NoodleToken> tokens = parameters.get(index);
        if (index >= tokens.size())
            throw new NoodleSyntaxException("Expected only one token at index %d for preprocessor command %s, but got %d.", (NoodleToken) null, index, this.name, tokens.size());
        return tokens.get(0);
    }

    /**
     * Gets a single noodle token of a given type.
     * @param parameters The parameters to get the token from.
     * @param index The index into the parameters to get.
     * @return singleNoodleToken
     */
    protected NoodleToken getSingleNoodleToken(List<List<NoodleToken>> parameters, int index, NoodleTokenType tokenType) {
        if (index >= parameters.size())
            throw new NoodleSyntaxException("Expected argument at index %d for preprocessor command %s.", (NoodleToken) null, index, this.name);
        List<NoodleToken> tokens = parameters.get(index);
        if (index >= tokens.size())
            throw new NoodleSyntaxException("Expected only one token at index %d for preprocessor command %s, but got %d.", (NoodleToken) null, index, this.name, tokens.size());
        NoodleToken token = tokens.get(0);
        if (tokenType != null && token.getTokenType() != tokenType)
            throw new NoodleSyntaxException("Preprocessor function '%s' expects a(n) '%s' token, but got '%s'.", token, this.name, tokenType, token);
        return token;
    }
}
