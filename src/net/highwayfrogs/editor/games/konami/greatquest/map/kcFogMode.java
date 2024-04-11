package net.highwayfrogs.editor.games.konami.greatquest.map;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the '_kcFogMode' enum.
 * Created by Kneesnap on 8/1/2023.
 */
public enum kcFogMode {
    NONE,
    EXP,
    EXP2,
    LINEAR;

    /**
     * Reads the fog mode from the reader.
     * @param reader The reader to read from.
     * @return kcFogMode
     */
    public static kcFogMode readFogMode(DataReader reader) {
        int value = reader.readInt();
        if (value == 0)
            return null;

        if (value > values().length || value < 0)
            throw new RuntimeException("The number " + value + " is not a valid kcFogMode.");

        return values()[value - 1];
    }

    /**
     * Writes the fog mode to the reader.
     * @param writer The writer to write to.
     * @param mode   The fog mode to write.
     */
    public static void writeFogMode(DataWriter writer, kcFogMode mode) {
        if (mode != null) {
            writer.writeInt(mode.ordinal() + 1);
        } else {
            writer.writeInt(0);
        }
    }
}