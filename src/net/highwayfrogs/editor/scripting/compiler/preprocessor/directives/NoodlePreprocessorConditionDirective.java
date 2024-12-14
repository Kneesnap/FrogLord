package net.highwayfrogs.editor.scripting.compiler.preprocessor.directives;

import lombok.NonNull;
import net.highwayfrogs.editor.scripting.compiler.NoodleSyntaxException;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessor;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessorContext;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenType;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Base representation of the '#if', '#ifdef', '#else', and '#elseif' preprocessor directives.
 */
public abstract class NoodlePreprocessorConditionDirective extends NoodlePreprocessorDirective {
    private final List<NoodleToken> trueTokens = new ArrayList<>();
    protected NoodlePreprocessorDirective nextDirective;

    protected NoodlePreprocessorConditionDirective(@NonNull NoodlePreprocessorDirectiveType directiveType, NoodleCodeLocation location) {
        super(directiveType, location);
    }

    @Override
    public void parseTokens(NoodlePreprocessorContext context) {
        List<Boolean> conditions = new ArrayList<>();
        conditions.add(getDirectiveType() != NoodlePreprocessorDirectiveType.ELSE);

        while (context.hasMoreTokens() && conditions.size() > 0) {
            int currentTokenPos = context.getCurrentTokenIndex();

            // Get token.
            NoodleToken nextToken = context.getCurrentTokenIncrement();
            if (nextToken.getTokenType() != NoodleTokenType.POUND) {
                this.trueTokens.add(nextToken);
                continue;
            }

            // Read directives.
            boolean includeDirectiveTokens = true;
            NoodlePreprocessorDirective directive = NoodlePreprocessor.readPreprocessorDirective(context);
            switch (directive.getDirectiveType()) {
                case IF:
                case IFDEF:
                case IFNDEF:
                    conditions.add(Boolean.TRUE);
                    break;
                case ELSEIF:
                    // Do nothin, unless it's this one.
                    if (!conditions.get(conditions.size() - 1))
                        throw new NoodleSyntaxException("Cannot use #elseif here.", nextToken);

                    if (conditions.size() == 1) {
                        this.nextDirective = directive;
                        conditions.clear();
                        includeDirectiveTokens = false;
                    }
                    break;
                case ELSE:
                    if (!conditions.get(conditions.size() - 1))
                        throw new NoodleSyntaxException("Cannot use #else here.", nextToken);

                    conditions.set(conditions.size() - 1, Boolean.FALSE);
                    if (conditions.size() == 1) {
                        this.nextDirective = directive;
                        conditions.clear();
                        includeDirectiveTokens = false;
                    }
                    break;
                case ENDIF:
                    conditions.remove(conditions.size() - 1);
                    includeDirectiveTokens = conditions.size() > 0;
                    break;
            }

            if (includeDirectiveTokens) {
                // Write tokens from directive.
                int endIndex = context.getCurrentTokenIndex();
                for (int i = currentTokenPos; i < endIndex; i++)
                    this.trueTokens.add(context.getTokens().get(i));
            }
        }
    }

    @Override
    public List<NoodleToken> processAndWriteTokens(NoodlePreprocessorContext context) {
        if (shouldUseTrueBlock(context)) {
            return this.trueTokens;
        } else if (this.nextDirective != null) {
            return this.nextDirective.processAndWriteTokens(context);
        }

        return null;
    }

    /**
     * Whether the true block should be used.
     * @param context The context to test under.
     * @return Whether the true block should be used.
     */
    protected abstract boolean shouldUseTrueBlock(NoodlePreprocessorContext context);
}
