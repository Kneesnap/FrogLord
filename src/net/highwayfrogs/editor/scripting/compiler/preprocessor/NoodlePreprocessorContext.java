package net.highwayfrogs.editor.scripting.compiler.preprocessor;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.scripting.compiler.NoodleCompileContext;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenType;

import java.util.List;

/**
 * State used for preprocessor token management.
 */
@Getter
public class NoodlePreprocessorContext {
    private final NoodleCompileContext compileContext;
    private final List<NoodleToken> tokens;
    @Setter private int currentTokenIndex;
    @Setter private int lastTokenStartIndex = -1;
    @Setter private int lastTokenEndIndex = -1;

    private static final NoodleToken EOF_TOKEN = new NoodleToken(NoodleTokenType.EOF, null);

    public NoodlePreprocessorContext(NoodleCompileContext context, List<NoodleToken> tokens) {
        this.compileContext = context;
        this.tokens = tokens;
    }

    /**
     * Gets the preprocessor.
     */
    public NoodlePreprocessor getPreprocessor() {
        return this.compileContext.getPreprocessor();
    }

    /**
     * Decrements to the previous token.
     */
    public void decrementToken() {
        this.currentTokenIndex--;
    }

    /**
     * Increment to the next token.
     */
    public int incrementToken() {
        return this.currentTokenIndex++;
    }

    /**
     * Gets the current token.
     */
    public NoodleToken getCurrentToken() {
        return this.currentTokenIndex == this.tokens.size()
                ? EOF_TOKEN
                : this.tokens.get(this.currentTokenIndex);
    }

    /**
     * Gets the current token, then increments to the next token.
     */
    public NoodleToken getCurrentTokenIncrement() {
        NoodleToken token = getCurrentToken();
        incrementToken();
        return token;
    }

    /**
     * Whether there are more tokens to read.
     * @return hasMoreTokens
     */
    public boolean hasMoreTokens() {
        if (this.currentTokenIndex >= this.tokens.size())
            return false; // No more tokens.

        NoodleToken currentToken = this.tokens.get(this.currentTokenIndex);
        return currentToken.getTokenType() != NoodleTokenType.EOF; // Allow reading further unless this is EOF.
    }
}
