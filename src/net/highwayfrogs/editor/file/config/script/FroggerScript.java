package net.highwayfrogs.editor.file.config.script;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * A frogger script.
 * Created by Kneesnap on 8/1/2019.
 */
public class FroggerScript extends GameObject {
    @Getter private final List<ScriptCommand> commands = new ArrayList<>();
    private int maxSize;
    public static final FroggerScript EMPTY_SCRIPT = new FroggerScript();

    @Override
    public void load(DataReader reader) {
        ScriptCommand lastCommand = null;
        while (lastCommand == null || !lastCommand.getCommandType().isFinalCommand()) {
            reader.jumpTemp(reader.getIndex());
            int scriptCommandId = reader.readInt();
            reader.jumpReturn();
            if (scriptCommandId < 0 || scriptCommandId >= ScriptCommandType.values().length) {
                System.out.println("Reached unexpected end of script in '" + getName() + "'.");
                break;
            }

            lastCommand = new ScriptCommand();
            lastCommand.load(reader);
            getCommands().add(lastCommand);
        }

        this.maxSize = getSize();
    }

    @Override
    public void save(DataWriter writer) {
        for (ScriptCommand command : getCommands())
            command.save(writer);
    }

    @Override
    public String toString() {
        StringBuilder scriptBuilder = new StringBuilder();
        for (ScriptCommand command : getCommands())
            scriptBuilder.append(command.toString()).append(Constants.NEWLINE);
        return scriptBuilder.toString();
    }

    /**
     * Gets the name of this script.
     * @return name
     */
    public String getName() {
        int index = getConfig().getScripts().indexOf(this);
        return index != -1 ? getConfig().getScriptBank().getName(index) : "Unnamed Script";
    }

    /**
     * Gets the total amount of integers this script takes up.
     * @return size
     */
    public int getSize() {
        int totalSize = 0;
        for (int i = 0; i < getCommands().size(); i++)
            totalSize += getCommands().get(i).getCommandType().getSize();
        return totalSize;
    }

    /**
     * Test if the size of this script is larger than the max allowed size.
     * @return isTooLarge
     */
    public boolean isTooLarge() {
        return getSize() > this.maxSize;
    }
}