package net.highwayfrogs.editor.scripting.compiler.preprocessor.directives;

import net.highwayfrogs.editor.scripting.compiler.NoodleSyntaxException;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessorContext;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenString;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenType;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the '#arguments' preprocessor directive.
 * This directive allows setting the variable names of any arguments passed into the script. This way, we do not need to access them with keywords like 'argument0'.
 */
public class NoodlePreprocessorDirectiveArguments extends NoodlePreprocessorDirective {
    private final List<String> argumentNames = new ArrayList<>();

    public NoodlePreprocessorDirectiveArguments(NoodleCodeLocation location) {
        super(NoodlePreprocessorDirectiveType.ARGUMENTS, location);
    }

    @Override
    public void parseTokens(NoodlePreprocessorContext context) {
        if (context.getCompileContext().getMainArgumentNames().size() > 0)
            throw new NoodleSyntaxException("Arguments have already been defined for this script.", context.getCurrentToken());

        int line = context.getCurrentToken().getLineNumber();
        boolean lastComma = true;
        while (context.hasMoreTokens()) {
            NoodleToken nextToken = context.getCurrentToken();
            if (nextToken.getLineNumber() > line)
                break; // Done!

            if (!lastComma && nextToken.getTokenType() == NoodleTokenType.COMMA) {
                lastComma = true;
            } else if (nextToken.getTokenType() == NoodleTokenType.IDENTIFIER) {
                this.argumentNames.add(((NoodleTokenString) nextToken).getStringData());
                lastComma = false;
            } else {
                throw new NoodleSyntaxException("Unexpected token in #arguments: '%s'.", nextToken, nextToken);
            }

            context.incrementToken();
        }
    }

    @Override
    public List<NoodleToken> processAndWriteTokens(NoodlePreprocessorContext context) {
        context.getCompileContext().getMainArgumentNames().clear();
        context.getCompileContext().getMainArgumentNames().addAll(this.argumentNames);
        return null;
    }

    @Override
    public void toString(StringBuilder builder) {
        super.toString(builder);
        for (int i = 0; i < this.argumentNames.size(); i++)
            builder.append(' ').append(this.argumentNames.get(i));
    }
}