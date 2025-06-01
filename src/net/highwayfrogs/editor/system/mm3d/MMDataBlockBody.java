package net.highwayfrogs.editor.system.mm3d;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;

/**
 * A general data block body type.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
@AllArgsConstructor
public abstract class MMDataBlockBody implements IBinarySerializable {
    private OffsetType bodyType;
    private MisfitModel3DObject parent;

    /**
     * Gets the index of this block body data.
     * @return blockIndex
     */
    public int getBlockIndex() {
        return getBodyType().getFinder().apply(getParent()).getBlocks().indexOf(this);
    }

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

    /**
     * Reads an int array from a reader.
     */
    public static void readIntArray(DataReader reader, int[] readTo) {
        for (int i = 0; i < readTo.length; i++)
            readTo[i] = reader.readInt();
    }

    /**
     * Writes an int array to a writer.
     */
    public static void writeIntArray(DataWriter writer, int[] toWrite) {
        for (int value : toWrite)
            writer.writeInt(value);
    }
}
