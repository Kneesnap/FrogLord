package net.highwayfrogs.editor.file.config.script;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a Frogger script command and its data.
 * Created by Kneesnap on 8/1/2019.
 */
@Getter
public class ScriptCommand extends GameObject {
    private ScriptCommandType commandType;
    private int[] arguments;

    @Override
    public void load(DataReader reader) {
        this.commandType = ScriptCommandType.values()[reader.readInt()];
        this.arguments = new int[this.commandType.getArgumentCount()];
        for (int i = 0; i < this.arguments.length; i++)
            this.arguments[i] = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.commandType.ordinal());
        for (int i = 0; i < this.arguments.length; i++)
            writer.writeInt(this.arguments[i]);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getCommandType().name());
        for (int i = 0; i < this.arguments.length; i++)
            builder.append(" ").append(this.commandType.getFormatters()[i].numberToString(this.arguments[i]));
        return builder.toString();
    }

    /**
     * Changes the ScriptCommandType.
     * @param newType The new ScriptCommandType to use.
     */
    public void setCommandType(ScriptCommandType newType) {
        this.commandType = newType;
        this.arguments = new int[newType.getArgumentCount()];
    }

    /**
     * Parse a command from a string.
     * @param inputLine The string to parse.
     * @return command
     */
    public static ScriptCommand readCommandFromString(String inputLine) {
        inputLine = Utils.removeDuplicateSpaces(inputLine);
        if (inputLine.isEmpty())
            return null;

        String[] split = inputLine.split(" ");
        ScriptCommandType commandType = ScriptCommandType.getByName(split[0]);
        if (commandType == null)
            throw new ScriptParseException("Unknown command: '" + split[0] + "'.");
        if (commandType.getSize() != split.length)
            throw new ScriptParseException("Expected " + commandType.getArgumentCount() + " arguments for " + commandType.name() + ", got " + (split.length - 1) + ".");

        int[] arguments = new int[commandType.getArgumentCount()];
        for (int i = 0; i < arguments.length; i++)
            arguments[i] = commandType.getFormatters()[i].stringToNumber(split[i + 1]);

        ScriptCommand newCommand = new ScriptCommand();
        newCommand.commandType = commandType;
        newCommand.arguments = arguments;
        return newCommand;
    }
}
