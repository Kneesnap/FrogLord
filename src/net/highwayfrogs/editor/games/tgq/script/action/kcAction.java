package net.highwayfrogs.editor.games.tgq.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.tgq.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.tgq.script.kcArgument;
import net.highwayfrogs.editor.games.tgq.script.kcParam;
import net.highwayfrogs.editor.games.tgq.script.kcParamType;
import net.highwayfrogs.editor.games.tgq.script.kcScriptDisplaySettings;

/**
 * Represents an action.
 * Created by Kneesnap on 6/26/2023.
 */
@Getter
public abstract class kcAction extends GameObject {
    private final kcActionID actionID;
    private kcParam[] unhandledArguments;

    private static final int ARGUMENT_COUNT = 4;

    public kcAction(kcActionID action) {
        this.actionID = action;
    }

    /**
     * Get the arguments definitions for the action, assuming the given arguments are given.
     * @param arguments The arguments to the action. If null is supplied, the default parameters are returned.
     * @return parameters
     */
    public abstract kcArgument[] getArgumentTemplate(kcParam[] arguments);

    @Override
    public void load(DataReader reader) {
        kcParam[] arguments = new kcParam[ARGUMENT_COUNT];
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
     * Get the kcParam arguments to this action as an array.
     * @param requireNonNull If null arguments should be created and set to zero.
     * @return arguments
     */
    public kcParam[] getArguments(boolean requireNonNull) {
        kcParam[] output = new kcParam[ARGUMENT_COUNT];
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
        this.toString(builder, kcScriptDisplaySettings.DEFAULT_SETTINGS);
        return builder.toString();
    }

    /**
     * Writes the kcAction with its parameters to a StringBuilder.
     * @param builder    The builder to write to.
     * @param parameters The parameters to the action.
     * @param settings   The settings used to build the output
     */
    public void writeAction(StringBuilder builder, kcParam[] parameters, kcScriptDisplaySettings settings) {
        writeAction(builder, this.actionID.name(), getArgumentTemplate(parameters), parameters, settings);
    }

    /**
     * Writes the kcAction with its parameters to a StringBuilder.
     * @param builder          The builder to write to.
     * @param mnemonic         The name of the instruction to display.
     * @param argumentTemplate The template of arguments to write.
     * @param parameters       The parameters to the action.
     * @param settings         The settings used to build the output
     */
    public static void writeAction(StringBuilder builder, String mnemonic, kcArgument[] argumentTemplate, kcParam[] parameters, kcScriptDisplaySettings settings) {
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
                parameters[i].toString(builder, parameterType, settings);
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
    public static kcAction readAction(DataReader reader) {
        kcActionID id = kcActionID.getActionByOpcode(reader.readInt());
        kcAction action = id.newInstance();
        action.load(reader);
        return action;
    }
}
