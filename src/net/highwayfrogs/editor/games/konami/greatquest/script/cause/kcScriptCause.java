package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.games.generic.data.GameObject;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash.kcHashedResource;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionID;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.kcScriptEffect;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.kcScriptEffectAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptValidationData;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;
import net.highwayfrogs.editor.utils.objects.StringNode;

import java.util.List;

/**
 * Represents a "cause", or a condition which causes a script to run.
 * Created by Kneesnap on 8/16/2023.
 */
@Getter
public abstract class kcScriptCause extends GameObject<GreatQuestInstance> {
    private final kcScript script;
    private final kcScriptCauseType type;
    private final int minimumArguments;
    private final int gqsArgumentCount;
    @Setter private kcScriptFunction parentFunction;
    @Setter private boolean loadedFromGame; // True if the cause was loaded from the game, and was not loaded by the user.
    private int userLineNumber = -1; // The line number as imported by the user.
    private String userImportSource;
    private boolean unusedCauseAllowed; // If true, any warnings will be hidden about the triggering of the cause, as we expect it to be allowed from another file.

    public static final String ARGUMENT_NAME_ALLOW_UNUSED_CAUSE = "AllowUnused";

    public kcScriptCause(@NonNull kcScript script, kcScriptCauseType type, int minimumArguments, int gqsArgumentCount) {
        super(script.getGameInstance());
        this.script = script;
        this.type = type;
        this.minimumArguments = minimumArguments;
        this.gqsArgumentCount = gqsArgumentCount;
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
     * Loads the cause data from the arguments.
     * @param arguments the arguments to read from
     */
    public final void load(ILogger logger, OptionalArguments arguments, int lineNumber, String fileName) {
        if (!validateGqsArgumentCount(arguments.getRemainingArgumentCount()))
            throw new RuntimeException("Cannot load " + Utils.getSimpleName(this) + "[" + getType() + "] from '" + arguments + "' since " + getGqsArgumentCount() + " arguments were expected, but " + arguments.getRemainingArgumentCount() + " were found.");
        if (logger == null)
            logger = getLogger();

        this.loadedFromGame = false;
        this.userLineNumber = lineNumber;
        this.userImportSource = fileName;
        this.unusedCauseAllowed = arguments.useFlag(ARGUMENT_NAME_ALLOW_UNUSED_CAUSE);
        loadArguments(logger, arguments);
        arguments.warnAboutUnusedArguments(logger);
        printWarnings(logger);
        if (!validateGqsArgumentCount(arguments.getOrderedArgumentCount()))
            throw new RuntimeException("Cannot load " + Utils.getSimpleName(this) + "[" + getType() + "] from '" + arguments + "' since " + getGqsArgumentCount() + " arguments were expected, but " + arguments.getOrderedArgumentCount() + " were found.");
    }

    /**
     * Saves the cause data to the arguments object.
     * @param arguments the arguments to save to
     */
    public final void save(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        save(null, arguments, settings);
    }

    /**
     * Saves the cause data to the arguments object.
     * @param arguments the arguments to save to
     */
    public final void save(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        if (logger == null)
            logger = getLogger();

        arguments.createNext().setAsString(getType().getDisplayName(), false);
        int oldCount = arguments.getOrderedArgumentCount();

        saveArguments(logger, arguments, settings);

        int argumentCount = (arguments.getOrderedArgumentCount() - oldCount);
        if (!validateGqsArgumentCount(argumentCount))
            throw new RuntimeException("Expected '" + arguments + "' to have " + getGqsArgumentCount() + " arguments, but it actually had " + argumentCount + ".");
    }

    /**
     * Called when the cause is read from the game files.
     */
    public void onRead() {
        this.loadedFromGame = true;
        this.userLineNumber = -1;
        this.userImportSource = null;
    }

    /**
     * Load data from the provided arguments.
     * @param logger the logger to write warnings and other messages to
     * @param arguments The arguments to read from
     */
    protected abstract void loadArguments(ILogger logger, OptionalArguments arguments);

    /**
     * Save arguments to the provided object.
     * @param logger the logger to write warnings and other messages to
     * @param arguments the object to save arguments to
     * @param settings  the settings to display the cause with
     */
    protected abstract void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings);

    /**
     * Test that we support the number of provided arguments.
     * @param argumentCount The amount of arguments to check.
     * @return If we support this many arguments.
     */
    public boolean validateArgumentCount(int argumentCount) {
        return argumentCount == this.minimumArguments;
    }

    /**
     * Test that we support the number of provided GQS arguments.
     * @param argumentCount The amount of arguments to check.
     * @return If we support this many arguments.
     */
    public boolean validateGqsArgumentCount(int argumentCount) {
        return argumentCount >= getGqsArgumentCount();
    }

    /**
     * Gets a description of the condition required to fulfil the cause condition.
     * @param builder  The builder to write the description to.
     * @param settings The settings to use.
     */
    public abstract void toString(StringBuilder builder, kcScriptDisplaySettings settings);

    /**
     * Gets a description of the condition required to fulfil the cause condition.
     * @param settings The settings to use.
     */
    public final String toString(kcScriptDisplaySettings settings) {
        StringBuilder builder = new StringBuilder();
        this.toString(builder, settings);
        return builder.toString();
    }

    @Override
    public String toString() {
        return this.toString(createDisplaySettings());
    }

    @Override
    public int hashCode() {
        return this.type.ordinal() << 28;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof kcScriptCause) && ((kcScriptCause) obj).getType() == this.type
                && ((kcScriptCause) obj).getScript() == getScript();
    }

    /**
     * Gets this cause as a string in gqs format.
     */
    public String getAsGqsStatement() {
        OptionalArguments arguments = new OptionalArguments();
        save(arguments, createDisplaySettings());
        return arguments.toString();
    }

    /**
     * Prints warnings about this script cause to the logger.
     * @param logger the logger to print warnings to
     */
    public void printWarnings(ILogger logger) {
        this.type.getMinimumEntityGroup().logEntityTypeWarnings(logger, this, this.type.getDisplayName());
    }

    /**
     * Prints warnings about the cause which could cause it to behave in undesired ways.
     * These warnings are "advanced" because they need information about other script actions/causes.
     * @param data The data to use for warning lookups.
     */
    public void printAdvancedWarnings(kcScriptValidationData data) {
        // By default, there are none.
    }

    /**
     * Prints a warning.
     * @param logger the logger to print the warning to
     * @param warning the warning to print
     */
    public final void printWarning(ILogger logger, String warning) {
        if (!this.loadedFromGame)
            logger.warning("The cause '%s' %s%swill never occur because %s%s",
                    getAsGqsStatement(),
                    (this.userImportSource != null ? "in '" + this.userImportSource + "' " : ""),
                    (this.userLineNumber > 0 ? "on line " + this.userLineNumber + " " : ""),
                    warning,
                    (warning.endsWith(".") || warning.endsWith("!") || warning.endsWith(")") ? "" : "."));
    }

    private kcScriptDisplaySettings createDisplaySettings() {
        return kcScriptDisplaySettings.getDefaultSettings(getGameInstance(), this.parentFunction != null ? this.parentFunction.getChunkedFile() : null);
    }

    /**
     * Gets the chunk file containing the script.
     */
    public GreatQuestChunkedFile getChunkFile() {
        return this.script != null ? this.script.getScriptList().getParentFile() : null;
    }

    /**
     * Gets the entity which this script operates on.
     */
    public kcCResourceEntityInst getScriptEntity() {
        return this.script != null ? this.script.getEntity() : null;
    }

    /**
     * Resolves a resource from a config node.
     * @param node the node to resolve the resource from
     * @param resourceClass the type of resource to resolve
     * @param hashObj the hash object to apply the result to
     * @param <TResource> the type of resource to resolve
     */
    protected <TResource extends kcHashedResource> void resolveResource(ILogger logger, StringNode node, Class<TResource> resourceClass, GreatQuestHash<TResource> hashObj) {
        GreatQuestChunkedFile chunkedFile = this.parentFunction.getChunkedFile();
        GreatQuestUtils.resolveLevelResource(logger, node, resourceClass, chunkedFile, this, hashObj, true);
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

    /**
     * Attempts to parse a script effect from a line of text in the FrogLord TGQ script syntax.
     * Throws an exception if it cannot be parsed.
     * @param line The line of text to parse
     * @return the parsed script effect
     */
    public static kcScriptCause parseScriptCause(ILogger logger, kcScriptFunction function, String line, int lineNumber, String fileName) {
        if (function == null)
            throw new NullPointerException("function");
        if (line == null)
            throw new NullPointerException("line");
        if (line.trim().isEmpty())
            throw new NullPointerException("Cannot interpret '" + line + "' as a script cause!");

        OptionalArguments arguments = OptionalArguments.parse(line);
        String causeName = arguments.useNext().getAsString();
        kcScriptCauseType causeType = kcScriptCauseType.getCauseType(causeName);
        if (causeType == null)
            throw new RuntimeException("The cause name '" + causeName + "' seems invalid, no kcScriptCauseType could be found for it.");

        kcScriptCause newCause = causeType.createNew(function.getScript());
        newCause.setParentFunction(function);

        try {
            newCause.load(logger, arguments, lineNumber, fileName);
        } catch (Throwable th) {
            throw new RuntimeException("Failed to parse '" + line + "' in '" + fileName + "' on line " + lineNumber + " as a script effect.", th);
        }

        return newCause;
    }

    /**
     * Test if an entity is terminated when the given action runs.
     * @param action the action to test
     * @return isEntityTerminatedOnRun
     */
    public static boolean isEntityTerminated(kcAction action) {
        if (action.getExecutor() instanceof kcScriptEffect) {
            kcScriptEffect effect = (kcScriptEffect) action.getExecutor();
            kcScriptFunction function = effect.getParentFunction();
            if (function != null) {
                kcCResourceEntityInst targetEntity = effect.getTargetEntityRef().getResource();
                if (function.getCause() instanceof kcScriptCausePlayer
                        && (targetEntity != null && function.getCause().getScriptEntity() == targetEntity)
                        && ((kcScriptCausePlayer) function.getCause()).getAction() == kcScriptCauseEntityAction.PICKUP_ITEM)
                    return true; // Picking up an item causes the item entity to terminate, so it would not work.

                // Test for termination effect.
                for (int i = 0; i < function.getEffects().size(); i++) {
                    kcScriptEffect testEffect = function.getEffects().get(i);
                    if (!(testEffect instanceof kcScriptEffectAction) || (targetEntity == null || testEffect.getTargetEntityRef().getResource() != targetEntity))
                        continue;

                    kcAction testAction = ((kcScriptEffectAction) testEffect).getAction();
                    if (testAction.getActionID() == kcActionID.TERMINATE)
                        return true;
                }
            }
        }

        // It's probably OK.
        return false;
    }
}