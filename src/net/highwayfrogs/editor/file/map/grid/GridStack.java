package net.highwayfrogs.editor.file.map.grid;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the GRID_STACK struct.
 * Created by Kneesnap on 8/27/2018.
 */
@Getter
public class GridStack extends GameObject {
    private short squareCount;
    private byte averageHeight; // Unused.
    private short index; // Index of first stack square in the GRID_SQUARE array.

    @Override
    public void load(DataReader reader) {
        this.squareCount = Utils.byteToUnsignedShort(reader.readByte());
        this.averageHeight = reader.readByte();
        this.index = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte(Utils.unsignedShortToByte(this.squareCount));
        writer.writeByte(this.averageHeight);
        writer.writeShort(this.index);
    }
}
