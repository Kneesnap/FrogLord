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
@Getter
public class FroggerScript extends GameObject {
    private List<ScriptCommand> commands = new ArrayList<>();
    public static final FroggerScript EMPTY_SCRIPT = new FroggerScript();

    @Override
    public void load(DataReader reader) {
        ScriptCommand lastCommand = null;
        while (lastCommand == null || !lastCommand.getCommandType().isFinalCommand()) {
            lastCommand = new ScriptCommand();
            lastCommand.load(reader);
            getCommands().add(lastCommand);
        }
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
}
