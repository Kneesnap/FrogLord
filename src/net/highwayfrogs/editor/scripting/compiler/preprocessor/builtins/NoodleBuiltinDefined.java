package net.highwayfrogs.editor.scripting.compiler.preprocessor.builtins;

import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessorContext;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenNumber;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenString;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleTokenType;

import java.util.Collections;
import java.util.List;

/**
 * Allows testing if a given macro is defined.
 */
public class NoodleBuiltinDefined extends NoodleBuiltin {
    public NoodleBuiltinDefined() {
        super("defined", "macroName");
    }

    @Override
    public List<NoodleToken> execute(NoodlePreprocessorContext context, List<List<NoodleToken>> parameters) {
        NoodleTokenString identToken = (NoodleTokenString) getSingleNoodleToken(parameters, 0, NoodleTokenType.IDENTIFIER);
        String name = identToken.getStringData();

        boolean isDefined = context.getCompileContext().getMacros().hasByName(name)
                || context.getCompileContext().getEngine().getBuiltinManager().getBuiltins().hasByName(name);
        return Collections.singletonList(new NoodleTokenNumber(NoodleTokenType.NUMBER, identToken.getCodeLocation(), isDefined ? 1 : 0));
    }
}
