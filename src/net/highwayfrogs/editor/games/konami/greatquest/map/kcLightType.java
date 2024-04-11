package net.highwayfrogs.editor.games.konami.greatquest.map;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the possible light types.
 * Created by Kneesnap on 8/1/2023.
 */
public enum kcLightType {
    POINT,
    SPOT,
    DIRECTIONAL;

    /**
     * Reads a light type from the reader.
     * @param reader The reader to read from.
     * @return kcLightType
     */
    public static kcLightType readLightType(DataReader reader) {
        int value = reader.readInt();
        if (value == 0)
            return null;

        if (value > values().length || value < 0)
            throw new RuntimeException("The number " + value + " is not a valid kcLightType.");

        return values()[value - 1];
    }

    /**
     * Writes the fog mode to the reader.
     * @param writer The writer to write to.
     * @param type   The light type to write.
     */
    public static void writeLightType(DataWriter writer, kcLightType type) {
        if (type != null) {
            writer.writeInt(type.ordinal() + 1);
        } else {
            writer.writeInt(0);
        }
    }
}