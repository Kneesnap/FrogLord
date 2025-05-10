package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInheritanceGroup;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.kcScriptEffectActor;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents an action.
 * Created by Kneesnap on 6/26/2023.
 */
@Getter
public abstract class kcAction extends GameData<GreatQuestInstance> {
    private final kcActionExecutor executor;
    private final kcActionID actionID;
    private kcParam[] unhandledArguments;
    private boolean loadedFromGame = true; // Reports whether the action was loaded from the game.
    private int userLineNumber = -1; // The line number as imported by the user.
    private String userImportSource;

    public static final int MAX_ARGUMENT_COUNT = 4;

    public kcAction(kcActionExecutor executor, kcActionID action) {
        super(executor != null ? executor.getGameInstance() : null);
        this.executor = executor;
        this.actionID = action;
    }

    /**
     * Gets the chunked file which this action is defined somewhere within.
     */
    public GreatQuestChunkedFile getChunkedFile() {
        return this.executor != null ? this.executor.getChunkedFile() : null;
    }

    /**
     * Get the arguments definitions for the action, assuming the given arguments are given.
     * @param arguments The arguments to the action. If null is supplied, the default parameters are returned.
     * @return parameters
     */
    public abstract kcArgument[] getArgumentTemplate(kcParam[] arguments);

    @Override
    public void load(DataReader reader) {
        this.loadedFromGame = true;
        this.userLineNumber = -1;
        this.userImportSource = null;
        kcParam[] arguments = new kcParam[MAX_ARGUMENT_COUNT];
        for (int i = 0; i < arguments.length; i++)
            arguments[i] = kcParam.readParam(reader);

        kcParamReader paramReader = new kcParamReader(arguments);
        this.load(paramReader);

        // Find unused arguments.
        int lastReadArgument = paramReader.getCurrentIndex();
        int lastUnusedArgument = -1;
        while (paramReader.hasMore()) {
            kcParam param = paramReader.next();
            if (param.getAsInteger() != 0)
                lastUnusedArgument = paramReader.getCurrentIndex();
        }

        // Store unused arguments.
        if (lastUnusedArgument >= 0) {
            kcParam[] unhandledParameters = new kcParam[lastUnusedArgument - lastReadArgument];
            paramReader.setCurrentIndex(lastReadArgument);
            for (int i = 0; i < unhandledParameters.length; i++)
                unhandledParameters[i] = paramReader.next();

            this.unhandledArguments = unhandledParameters;
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.actionID.getOpcode() & 0xFF);

        // Save arguments.
        kcParam[] arguments = getArguments(true);
        for (int i = 0; i < arguments.length; i++)
            writer.writeBytes(arguments[i].getBytes());
    }

    /**
     * Reads kcParam action data.
     * @param reader The source of kcParam values.
     */
    public abstract void load(kcParamReader reader);

    /**
     * Writes kcParam values to the output list.
     * @param writer The destination to write kcParam values to.
     */
    public abstract void save(kcParamWriter writer);

    /**
     * Gets the number of arguments expected (excluding the action name) when parsing the action in GQS format on the current template.
     */
    public int getGqsArgumentCount(kcArgument[] argumentTemplates) {
        return argumentTemplates.length;
    }

    /**
     * Gets the action as a gqs statement string.
     */
    public String getAsGqsStatement() {
        OptionalArguments arguments = new OptionalArguments();
        arguments.createNext().setAsString(this.actionID.getFrogLordName(), false);
        GreatQuestChunkedFile chunkedFile = getChunkedFile();
        this.save(arguments, chunkedFile != null ? chunkedFile.createScriptDisplaySettings() : null);
        return arguments.toString();
    }

    /**
     * Loads this kcAction data from an OptionalArguments object.
     * @param arguments The arguments to load the data from
     */
    public final void load(OptionalArguments arguments, int lineNumber, String fileName) {
        kcArgument[] argumentTemplates = getArgumentTemplate(null);
        int expectedArgumentCount = getGqsArgumentCount(argumentTemplates);
        if (expectedArgumentCount > arguments.getRemainingArgumentCount())
            throw new RuntimeException("Could not load '" + arguments + "' as kcAction[" + getActionID() + "], as it did not have " + expectedArgumentCount + " arguments.");

        this.loadedFromGame = false; // User-supplied.
        this.userLineNumber = lineNumber;
        this.userImportSource = fileName;
        loadArguments(arguments);
    }

    /**
     * Saves this kcAction data to an OptionalArguments object.
     * @param arguments The arguments to save the data to
     * @param settings The context settings necessary for turning a script into text.
     */
    public final void save(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        int startArgumentCount = arguments.getOrderedArgumentCount();
        saveArguments(arguments, settings);

        // If there were unhandled arguments, warn about it.
        if (this.unhandledArguments != null && this.unhandledArguments.length > 0)
            getLogger().warning("There were unhandled arguments present for '" + arguments + "'.");

        // Ensure it looks ok.
        kcArgument[] argumentTemplates = getArgumentTemplate(null);
        int gqsArgumentCount = getGqsArgumentCount(argumentTemplates); // Do not include the name of the action, sometimes a different name is provided.
        if (gqsArgumentCount > arguments.getOrderedArgumentCount() - startArgumentCount)
            throw new RuntimeException("Could not save kcAction[" + getActionID() + "] to arguments. We got '" + arguments + "', but were looking for at least " + gqsArgumentCount + " arguments.");
    }

    /**
     * Gets a string representing the end of line comment for the action.
     * Usually this will return null, meaning there is no end of line comment.
     */
    public String getEndOfLineComment() {
        return null;
    }

    /**
     * Loads the action arguments from the arguments provided.
     * @param arguments the arguments to load from
     */
    protected abstract void loadArguments(OptionalArguments arguments);

    /**
     * Save the arguments of the action to the object.
     * @param arguments The object to store the action arguments within
     * @param settings settings to use to save the arguments as strings
     */
    protected abstract void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings);

    /**
     * Get the kcParam arguments to this action as an array.
     * @param requireNonNull If null arguments should be created and set to zero.
     * @return arguments
     */
    public kcParam[] getArguments(boolean requireNonNull) {
        kcParam[] output = new kcParam[MAX_ARGUMENT_COUNT];
        kcParamWriter writer = new kcParamWriter(output);
        this.save(writer);

        // Include unhandled arguments.
        if (this.unhandledArguments != null)
            for (int i = 0; i < this.unhandledArguments.length && !writer.isFull(); i++)
                writer.write(this.unhandledArguments[i]);

        // Ensure remaining arguments aren't null.
        if (requireNonNull)
            writer.clearRemaining();

        return output;
    }

    /**
     * Prints warnings about the action which could cause it to behave in undesired ways.
     * @param logger The logger to print the warnings to.
     */
    public void printWarnings(ILogger logger) {
        kcEntityInheritanceGroup targetType = this.actionID.getActionTargetType();
        if (targetType != null)
            targetType.logEntityTypeWarnings(logger, this, this.actionID.getFrogLordName());
        if (targetType == null && this.actionID.isEnableForActionSequences() && !(getExecutor() instanceof kcCActionSequence)) {
            printWarning(logger, "'" + this.actionID + "' is only usable in an action sequence.");
        } else if (getExecutor() instanceof kcCActionSequence && !this.actionID.isEnableForActionSequences()) {
            printWarning(logger, "'" + this.actionID.getFrogLordName() + "' cannot be used in an action sequence.");
        } else if (getExecutor() instanceof kcScriptEffectActor && this.actionID.isScriptMappingMissing()) {
            printWarning(logger, "'" + this.actionID.getFrogLordName() + "' is not mapped by the kcCScriptMgr. (There's probably an alias which should be used instead)");
        }
    }

    /**
     * Prints warnings about the action which could cause it to behave in undesired ways.
     * These warnings are "advanced" because they need information about other script actions.
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
    public void printWarning(ILogger logger, String warning) {
        logger.warning("The action '%s' %s%swill be skipped by the game, since %s%s",
                getAsGqsStatement(),
                (this.userImportSource != null ? "in '" + this.userImportSource + "' " : ""),
                (this.userLineNumber > 0 ? "on line " + this.userLineNumber + " " : ""),
                warning,
                (warning.endsWith(".") || warning.endsWith("!") || warning.endsWith(")") ? "" : "."));
    }

    /**
     * Writes the action to a string builder.
     * @param builder  The builder to write the script to.
     * @param settings The settings used to build the output.
     */
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        writeAction(builder, getArguments(false), settings);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        this.toString(builder, kcScriptDisplaySettings.getDefaultSettings(getGameInstance(), getChunkedFile()));
        return builder.toString();
    }

    /**
     * Writes the kcAction with its parameters to a StringBuilder.
     * @param builder    The builder to write to.
     * @param parameters The parameters to the action.
     * @param settings   The settings used to build the output
     */
    public void writeAction(StringBuilder builder, kcParam[] parameters, kcScriptDisplaySettings settings) {
        writeAction(this.executor, builder, this.actionID.name(), getArgumentTemplate(parameters), parameters, settings);
    }

    /**
     * Writes the kcAction with its parameters to a StringBuilder.
     * @param builder          The builder to write to.
     * @param mnemonic         The name of the instruction to display.
     * @param argumentTemplate The template of arguments to write.
     * @param parameters       The parameters to the action.
     * @param settings         The settings used to build the output
     */
    public static void writeAction(kcActionExecutor executor, StringBuilder builder, String mnemonic, kcArgument[] argumentTemplate, kcParam[] parameters, kcScriptDisplaySettings settings) {
        builder.append(mnemonic);

        for (int i = 0; i < argumentTemplate.length; i++) {
            String parameterName = argumentTemplate[i].getName();
            kcParamType parameterType = argumentTemplate[i].getType();

            if (settings.isShowLabels()) {
                builder.append(" /* ")
                        .append(parameterType.name())
                        .append(' ')
                        .append(parameterName)
                        .append(": */ ");
            } else {
                builder.append(' ');
            }

            if (parameters != null && parameters.length > i) {
                parameters[i].toString(executor, builder, parameterType, settings);
            } else {
                builder.append("<MISSING_DATA>");
            }
        }

        if (!settings.isShowUnusedValues() || parameters == null)
            return;

        boolean anyMissingData = false;
        for (int i = argumentTemplate.length; i < parameters.length && !anyMissingData; i++)
            if (parameters[i] != null && parameters[i].getAsInteger() != 0)
                anyMissingData = true;

        if (!anyMissingData)
            return;

        builder.append(" // Unused Arguments: ");
        for (int i = argumentTemplate.length; i < parameters.length; i++) {
            builder.append(' ');
            builder.append(kcScriptDisplaySettings.getHashDisplay(settings, parameters[i].getAsInteger(), false));
        }
    }


    /**
     * Read a kcAction from the DataReader.
     * @param reader The reader to read from.
     * @return kcAction
     */
    public static kcAction readAction(DataReader reader, kcActionExecutor executor) {
        kcActionID id = kcActionID.getActionByOpcode(reader.readInt());
        kcAction action = id.newInstance(executor);
        action.load(reader);
        return action;
    }
}