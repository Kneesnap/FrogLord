package net.highwayfrogs.editor.games.sony.frogger.data.scripts;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Frogger entity script.
 * These scripts are hardcoded into the game executable, meaning in order to edit them, we cannot exceed their original size.
 * Created by Kneesnap on 8/1/2019.
 */
public class FroggerScript extends SCGameData<FroggerGameInstance> {
    @Getter private final List<FroggerScriptCommand> commands = new ArrayList<>();
    private int maxSize;
    public static final FroggerScript EMPTY_SCRIPT = new FroggerScript(null);

    public FroggerScript(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    @Override
    public void load(DataReader reader) {
        FroggerScriptCommand lastCommand = null;
        while (lastCommand == null || !lastCommand.getCommandType().isFinalCommand()) {
            reader.jumpTemp(reader.getIndex());
            int scriptCommandId = reader.readInt();
            reader.jumpReturn();

            // Unexpected end of script!
            if (scriptCommandId < 0 || scriptCommandId >= FroggerScriptCommandType.values().length) {
                getLogger().severe("Reached unexpected end of script in '%s' at %08X.", getName(), reader.getIndex());
                break;
            }

            lastCommand = new FroggerScriptCommand(getGameInstance());
            lastCommand.load(reader);
            getCommands().add(lastCommand);
        }

        this.maxSize = getSize();
    }

    @Override
    public void save(DataWriter writer) {
        if (isTooLarge())
            throw new IllegalStateException("The script is too large to be saved back to the game! It will cause memory corruption!");

        for (int i = 0; i < this.commands.size(); i++)
            this.commands.get(i).save(writer);
    }

    @Override
    public String toString() {
        StringBuilder scriptBuilder = new StringBuilder();
        for (FroggerScriptCommand command : getCommands())
            scriptBuilder.append(command.toString()).append(Constants.NEWLINE);
        return scriptBuilder.toString();
    }

    /**
     * Gets the name of this script.
     * @return name
     */
    public String getName() {
        if (getGameInstance() == null)
            return "SCRIPT_NONE";

        int index = getGameInstance().getScripts().indexOf(this);
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