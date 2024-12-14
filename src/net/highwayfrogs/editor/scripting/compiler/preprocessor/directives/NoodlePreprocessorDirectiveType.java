package net.highwayfrogs.editor.scripting.compiler.preprocessor.directives;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.function.Function;

/**
 * A registry of different preprocessor directives.
 */
@Getter
public enum NoodlePreprocessorDirectiveType {
    INCLUDE(NoodlePreprocessorDirectiveInclude::new, true),
    DEFINE(NoodlePreprocessorDirectiveDefine::new, true),
    IF(NoodlePreprocessorDirectiveIf::new, true), // (ALLOW expressions, but not statements?)
    IFDEF(NoodlePreprocessorDirectiveIfDef::new, true),
    IFNDEF(NoodlePreprocessorDirectiveIfNotDef::new, true),
    ELSE(NoodlePreprocessorDirectiveElse::new, false),
    ELSEIF(NoodlePreprocessorDirectiveElseIf::new, false),
    ENDIF(NoodlePreprocessorDirectiveEndIf::new, false),
    ARGUMENTS(NoodlePreprocessorDirectiveArguments::new, true),
    ERROR(NoodlePreprocessorDirectiveError::new, true);

    private final String mnemonic;
    private final Function<NoodleCodeLocation, NoodlePreprocessorDirective> directiveMaker;
    private final boolean allowTopLevel;

    NoodlePreprocessorDirectiveType(Function<NoodleCodeLocation, NoodlePreprocessorDirective> directiveMaker, boolean allowTopLevel) {
        this.mnemonic = name().toLowerCase();
        this.directiveMaker = directiveMaker;
        this.allowTopLevel = allowTopLevel;
    }

    /**
     * Makes a directive of this type.
     * @param location The location of the '#' symbol which the directive was created from.
     * @return codeLocation
     */
    public NoodlePreprocessorDirective makeDirective(NoodleCodeLocation location) {
        return this.directiveMaker.apply(location);
    }

    /**
     * Get the preprocessor directive type by its name.
     * @param directiveName The name of the directive.
     */
    public static NoodlePreprocessorDirectiveType getByName(String directiveName) {
        for (int i = 0; i < values().length; i++)
            if (values()[i].getMnemonic().equalsIgnoreCase(directiveName))
                return values()[i];
        return null;
    }
}
