package net.highwayfrogs.editor.games.tgq.script;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 6/26/2023.
 */
@Getter
public class kcAction extends GameObject {
    private kcActionID actionID;
    private final kcParam[] params = new kcParam[4];

    @Override
    public void load(DataReader reader) {
        this.actionID = kcActionID.values()[reader.readInt() - 1];
        for (int i = 0; i < this.params.length; i++)
            this.params[i] = kcParam.readParam(reader);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.actionID.ordinal());
        for (int i = 0; i < this.params.length; i++)
            writer.writeBytes(this.params[i].getBytes());
    }

    /**
     * Writes the action to a string builder.
     * @param builder  The builder to write the script to.
     * @param settings The settings used to build the output.
     */
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        writeAction(builder, this.actionID, this.params, settings);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        this.toString(builder, new kcScriptDisplaySettings(null, true, true));
        return builder.toString();
    }

    /**
     * Writes a kcActionID with its parameters to a StringBuilder.
     * @param builder    The builder to write to.
     * @param action     The action to write.
     * @param parameters The parameters to the action.
     * @param settings   The settings used to build the output
     */
    public static void writeAction(StringBuilder builder, kcActionID action, kcParam[] parameters, kcScriptDisplaySettings settings) {
        builder.append(action.name());

        for (int i = 0; i < action.getParameterCount(); i++) {
            String parameterName = action.getParameterNames()[i];
            kcParamType parameterType = action.getParameterTypes()[i];

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
        for (int i = action.getParameterCount(); i < parameters.length && !anyMissingData; i++)
            if (parameters[i].getAsInteger() != 0)
                anyMissingData = true;

        if (!anyMissingData)
            return;

        builder.append(" /* Unused: */");
        for (int i = action.getParameterCount(); i < parameters.length; i++) {
            builder.append(' ');
            parameters[i].toString(builder, kcParamType.ANY, settings);
        }
    }
}
