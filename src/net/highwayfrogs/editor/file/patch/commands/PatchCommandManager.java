package net.highwayfrogs.editor.file.patch.commands;

import net.highwayfrogs.editor.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of commands.
 * Created by Kneesnap on 1/15/2020.
 */
public class PatchCommandManager {
    private static Map<String, PatchCommand> commandMap = new HashMap<>();

    /**
     * Gets a PatchCommand by its name.
     * @param commandName The name of the command to get.
     * @return patchCommand
     */
    public static PatchCommand getCommand(String commandName) {
        if (commandMap.size() == 0)
            setupCommands();
        return commandMap.get(commandName);
    }

    private static void setupCommands() {
        addCommand(new PatchCommandAdd());
        addCommand(new PatchCommandDivide());
        addCommand(new PatchCommandMultiply());
        addCommand(new PatchCommandPrint());
        addCommand(new PatchCommandSet());
        addCommand(new PatchCommandSubtract());
        addCommand(new PatchCommandWrite("writeb", (writer, value) -> writer.writeUnsignedByte((short) (value.getAsInteger() & 0xFF))));
        addCommand(new PatchCommandWrite("writes", (writer, value) -> writer.writeShort((short) value.getAsInteger())));
        addCommand(new PatchCommandWrite("writei", (writer, value) -> writer.writeInt(value.getAsInteger() & 0xFF)));
        addCommand(new PatchCommandWrite("writef", (writer, value) -> writer.writeFloat((float) value.getAsDecimal())));
        addCommand(new PatchCommandWrite("writecolor", (writer, value) -> writer.writeInt(Utils.toRGB(value.getAsColor()))));
        addCommand(new PatchCommandIf());
        addCommand(new PatchCommandRead("readb", (reader, value) -> value.setInteger(reader.readUnsignedByteAsShort())));
        addCommand(new PatchCommandRead("reads", (reader, value) -> value.setInteger(reader.readShort())));
        addCommand(new PatchCommandRead("readi", (reader, value) -> value.setInteger(reader.readInt())));
        addCommand(new PatchCommandRead("readf", (reader, value) -> value.setDecimal(reader.readFloat())));
        addCommand(new PatchCommandRead("readcolor", (reader, value) -> value.setColor(Utils.fromRGB(reader.readInt()))));
        addCommand(new PatchCommandHex());
    }

    private static void addCommand(PatchCommand command) {
        commandMap.put(command.getName(), command);
    }
}
