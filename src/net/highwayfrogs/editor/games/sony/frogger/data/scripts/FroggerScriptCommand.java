package net.highwayfrogs.editor.games.sony.frogger.data.scripts;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a Frogger script command and its data.
 * Created by Kneesnap on 8/1/2019.
 */
@Getter
public class FroggerScriptCommand extends SCGameData<FroggerGameInstance> {
    private FroggerScriptCommandType commandType;
    private int[] arguments;

    public FroggerScriptCommand(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.commandType = FroggerScriptCommandType.values()[reader.readInt()];
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
            builder.append(" ").append(this.commandType.getFormatters()[i].numberToString(getGameInstance(), this.arguments[i]));
        return builder.toString();
    }

    /**
     * Changes the FroggerScriptCommandType.
     * @param newType The new FroggerScriptCommandType to use.
     */
    public void setCommandType(FroggerScriptCommandType newType) {
        this.commandType = newType;
        this.arguments = new int[newType.getArgumentCount()];
    }

    /**
     * Parse a command from a string.
     * @param inputLine The string to parse.
     * @return command
     */
    public static FroggerScriptCommand readCommandFromString(FroggerGameInstance instance, String inputLine) {
        inputLine = StringUtils.removeDuplicateSpaces(inputLine);
        if (inputLine.isEmpty())
            return null;

        String[] split = inputLine.split(" ");
        FroggerScriptCommandType commandType = FroggerScriptCommandType.getByName(split[0]);
        if (commandType == null)
            throw new FroggerScriptParseException("Unknown command: '" + split[0] + "'.");
        if (commandType.getSize() != split.length)
            throw new FroggerScriptParseException("Expected " + commandType.getArgumentCount() + " arguments for " + commandType.name() + ", got " + (split.length - 1) + ".");

        int[] arguments = new int[commandType.getArgumentCount()];
        for (int i = 0; i < arguments.length; i++)
            arguments[i] = commandType.getFormatters()[i].stringToNumber(instance, split[i + 1]);

        FroggerScriptCommand newCommand = new FroggerScriptCommand(instance);
        newCommand.commandType = commandType;
        newCommand.arguments = arguments;
        return newCommand;
    }
}