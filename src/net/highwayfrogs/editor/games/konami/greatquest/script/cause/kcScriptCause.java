package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.generic.data.GameObject;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;

import java.util.List;

/**
 * Represents a "cause", or a condition which causes a script to run.
 * Created by Kneesnap on 8/16/2023.
 */
@Getter
public abstract class kcScriptCause extends GameObject<GreatQuestInstance> {
    private final kcScriptCauseType type;
    private final int minimumArguments;
    @Setter(AccessLevel.PACKAGE) private kcScriptFunction parentFunction;

    public kcScriptCause(GreatQuestInstance instance, kcScriptCauseType type, int minimumArguments) {
        super(instance);
        this.type = type;
        this.minimumArguments = minimumArguments;
    }

    /**
     * Load data from the provided values.
     * @param subCauseType The sub-cause type value.
     * @param extraValues  Any optional additional values.
     */
    public abstract void load(int subCauseType, List<Integer> extraValues);

    /**
     * Save the cause data from this function to a list.
     * @param output The list to save to.
     */
    public abstract void save(List<Integer> output);

    /**
     * Test that we support the number of provided arguments.
     * @param argumentCount The amount of arguments to check.
     * @return If we support this many arguments.
     */
    public boolean validateArgumentCount(int argumentCount) {
        return argumentCount == this.minimumArguments;
    }

    /**
     * Gets a description of the condition required to fulfil the cause condition.
     * @param builder  The builder to write the description to.
     * @param settings The settings to use.
     */
    public abstract void toString(StringBuilder builder, kcScriptDisplaySettings settings);

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        this.toString(builder, kcScriptDisplaySettings.getDefaultSettings(getGameInstance(), this.parentFunction != null ? this.parentFunction.getChunkedFile() : null));
        return builder.toString();
    }

    /**
     * Reads a script's boolean value from an integer.
     * @param number      The number to read from.
     * @param description The description of what is being read.
     * @return The parsed boolean value
     */
    protected static boolean readBoolean(int number, String description) {
        if (number == 1) {
            return true;
        } else if (number == 0) {
            return false;
        } else {
            throw new RuntimeException("Tried to read " + number + " as a boolean for " + description + ".");
        }
    }

    /**
     * Writes a boolean value to the output list.
     * @param output The output list to write to.
     * @param value  The boolean value to write.
     */
    protected static void writeBoolean(List<Integer> output, boolean value) {
        output.add(value ? 1 : 0);
    }
}