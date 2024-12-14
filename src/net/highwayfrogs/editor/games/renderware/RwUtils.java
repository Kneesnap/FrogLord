package net.highwayfrogs.editor.games.renderware;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.NumberUtils;

/**
 * Contains utilities for working with RenderWare.
 * Created by Kneesnap on 8/12/2024.
 */
public class RwUtils {

    /**
     * Reads an RwBool from the reader.
     * @param reader the reader to read the RwBool from
     * @return rwBool
     */
    public static boolean readRwBool(DataReader reader) {
        int value = reader.readInt();
        if (value == 1) {
            return true;
        } else if (value == 0) {
            return false;
        } else {
            throw new IllegalArgumentException("Cannot interpret " + NumberUtils.toHexString(value) + " as an RwBool!");
        }
    }

    /**
     * Writes an RwBool to the writer.
     * @param writer the writer to write the RwBool to
     */
    public static void writeRwBool(DataWriter writer, boolean value) {
        writer.writeInt(value ? 1 : 0);
    }
}