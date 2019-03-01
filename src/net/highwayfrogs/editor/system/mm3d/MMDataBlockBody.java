package net.highwayfrogs.editor.system.mm3d;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * A general data block body type.
 * Created by Kneesnap on 2/28/2019.
 */
public abstract class MMDataBlockBody extends GameObject {

    /**
     * Reads a float array from a reader.
     */
    public static void readFloatArray(DataReader reader, float[] readTo) {
        for (int i = 0; i < readTo.length; i++)
            readTo[i] = reader.readFloat();
    }

    /**
     * Writes a float array to a writer.
     */
    public static void writeFloatArray(DataWriter writer, float[] toWrite) {
        for (float value : toWrite)
            writer.writeFloat(value);
    }
}