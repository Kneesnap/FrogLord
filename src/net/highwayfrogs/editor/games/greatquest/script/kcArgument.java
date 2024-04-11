package net.highwayfrogs.editor.games.greatquest.script;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Contains utilities for dealing with script effect arguments.
 * Created by Kneesnap on 8/23/2023.
 */
@Getter
@AllArgsConstructor
public class kcArgument {
    private final kcParamType type;
    private final String name;

    /**
     * Creates an array of script effect arguments with one element.
     * @param firstArgType The first argument type
     * @param firstArgName The first argument name
     * @return scriptEffectArgumentArray
     */
    public static kcArgument[] make(kcParamType firstArgType, String firstArgName) {
        return new kcArgument[]{new kcArgument(firstArgType, firstArgName)};
    }

    /**
     * Creates an array of script effect arguments with two elements.
     * @param firstArgType  The first argument type
     * @param firstArgName  The first argument name
     * @param secondArgType The second argument type
     * @param secondArgName The second argument name
     * @return scriptEffectArgumentArray
     */
    public static kcArgument[] make(kcParamType firstArgType, String firstArgName, kcParamType secondArgType, String secondArgName) {
        return new kcArgument[]{
                new kcArgument(firstArgType, firstArgName),
                new kcArgument(secondArgType, secondArgName)
        };
    }

    /**
     * Creates an array of script effect arguments with three elements.
     * @param firstArgType  The first argument type
     * @param firstArgName  The first argument name
     * @param secondArgType The second argument type
     * @param secondArgName The second argument name
     * @param thirdArgType  The third argument type
     * @param thirdArgName  The third argument name
     * @return scriptEffectArgumentArray
     */
    public static kcArgument[] make(kcParamType firstArgType, String firstArgName, kcParamType secondArgType, String secondArgName, kcParamType thirdArgType, String thirdArgName) {
        return new kcArgument[]{
                new kcArgument(firstArgType, firstArgName),
                new kcArgument(secondArgType, secondArgName),
                new kcArgument(thirdArgType, thirdArgName)
        };
    }

    /**
     * Creates an array of script effect arguments with four elements.
     * @param firstArgType  The first argument type
     * @param firstArgName  The first argument name
     * @param secondArgType The second argument type
     * @param secondArgName The second argument name
     * @param thirdArgType  The third argument type
     * @param thirdArgName  The third argument name
     * @param fourthArgType The fourth argument type
     * @param fourthArgName The fourth argument name
     * @return scriptEffectArgumentArray
     */
    public static kcArgument[] make(kcParamType firstArgType, String firstArgName, kcParamType secondArgType, String secondArgName, kcParamType thirdArgType, String thirdArgName, kcParamType fourthArgType, String fourthArgName) {
        return new kcArgument[]{
                new kcArgument(firstArgType, firstArgName),
                new kcArgument(secondArgType, secondArgName),
                new kcArgument(thirdArgType, thirdArgName),
                new kcArgument(fourthArgType, fourthArgName)
        };
    }
}