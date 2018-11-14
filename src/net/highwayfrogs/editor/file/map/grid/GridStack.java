package net.highwayfrogs.editor.file.map.grid;

import lombok.Getter;
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
    private short averageHeight; // Unused.
    private short index; // Index of first stack square in the GRID_SQUARE array.

    @Override
    public void load(DataReader reader) {
        this.squareCount = reader.readUnsignedByteAsShort();
        this.averageHeight = reader.readUnsignedByteAsShort();
        this.index = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte(this.squareCount);
        writer.writeUnsignedByte(this.averageHeight);
        writer.writeShort(this.index);
    }
}
