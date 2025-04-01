package net.highwayfrogs.editor.file.map.form;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerMapForm;

/**
 * Reads the "FORM" struct.
 * Appears to be entity collision info, like being able to walk on logs or birds.
 * Created by Kneesnap on 8/23/2018.
 */
@Getter
@Setter
public class Form extends GameObject {
    private short xGridSquareCount; // Number of x grid squares in this form.
    private short zGridSquareCount; // Number of z grid squares in this form.
    private short xOffset; // Offset to bottom left or grid from entity origin.
    private short zOffset; // Offset to bottom left or grid from entity origin.
    private FormData data; // Null is allowed.

    public static final int GRID_PIXELS = 20;

    @Override
    public void load(DataReader reader) {
        int dataCount = reader.readUnsignedShortAsInt();
        reader.skipShort(); // Max Y, Runtime variable.
        this.xGridSquareCount = reader.readShort();
        this.zGridSquareCount = reader.readShort();
        this.xOffset = reader.readShort();
        this.zOffset = reader.readShort();

        if (dataCount == 0) {
            this.xGridSquareCount = -1;
            this.zGridSquareCount = -1;
            this.xOffset = -1;
            this.zOffset = -1;
            return; // There is no form data.
        }

        //Utils.verify(dataCount == 1, "Invalid Form Data Count: " + dataCount); // The game only supports 1 form data in the retail build even if it has a count for more. It appears more than 1 was supported in build 20 though.

        reader.jumpTemp(reader.readInt()); // Form Data Pointer.
        this.data = new FormData(this);
        data.load(reader);
        reader.jumpReturn();

        // TODO: Support this? This just reads the data, it doesn't actually store it.
        for (int i = 1; i < dataCount; i++) {
            reader.jumpTemp(reader.readInt());
            FormData newData = new FormData(this);
            newData.load(reader);
            reader.jumpReturn();
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(hasData() ? 1 : 0);
        writer.writeShort((short) 0);
        writer.writeShort(this.xGridSquareCount);
        writer.writeShort(this.zGridSquareCount);
        writer.writeShort(this.xOffset);
        writer.writeShort(this.zOffset);

        if (hasData()) { // Save the form data if there's any.
            int dataPointer = writer.writeNullPointer();
            writer.writeAddressTo(dataPointer); // We don't write this if we don't have data. I forget why
            getData().save(writer);
        }
    }

    /**
     * Test if this form has form data.
     * @return hasFormData
     */
    public boolean hasData() {
        return getData() != null;
    }

    /**
     * Changes the X size of this form.
     * @param newXCount New size.
     */
    public void setXGridSquareCount(short newXCount) {
        if (hasData()) {
            int[] oldFlags = getData().getGridFlags();
            int[] newFlags = new int[newXCount * getZGridSquareCount()];

            for (int i = 0; i < oldFlags.length; i++) {
                int x = getXFromIndex(i);
                if (x >= newXCount)
                    continue;

                newFlags[getIndex(x, getZFromIndex(i), newXCount)] = oldFlags[i];
            }

            getData().setGridFlags(newFlags);
        }

        this.xGridSquareCount = newXCount;
    }

    /**
     * Changes the Z size of this form.
     * @param newZCount New size.
     */
    public void setZGridSquareCount(short newZCount) {
        if (hasData()) {
            int[] oldFlags = getData().getGridFlags();
            int[] newFlags = new int[getXGridSquareCount() * newZCount];

            for (int i = 0; i < oldFlags.length; i++) {
                int z = getZFromIndex(i);
                if (z >= newZCount)
                    continue;

                newFlags[getIndex(getXFromIndex(i), z, getXGridSquareCount())] = oldFlags[i];
            }

            getData().setGridFlags(newFlags);
        }

        this.zGridSquareCount = newZCount;
    }

    /**
     * Get the X coordinate from the flag index.
     * @param index The index to use.
     * @return x
     */
    public int getXFromIndex(int index) {
        return (index % getXGridSquareCount());
    }

    /**
     * Get the Z coordinate from the flag index.
     * @param index The index to use.
     * @return z
     */
    public int getZFromIndex(int index) {
        return (index / getXGridSquareCount());
    }

    /**
     * Calculates the index of a grid flag.
     * @return index
     */
    public static int getIndex(int x, int z, int xCount) {
        return (z * xCount) + x;
    }

    /**
     * Converts the form to the new map format.
     */
    public FroggerMapForm convertToNewFormat(FroggerMapFile mapFile) {
        return MAPFile.copyToNewViaBytes(this, new FroggerMapForm(mapFile));
    }
}