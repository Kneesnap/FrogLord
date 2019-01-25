package net.highwayfrogs.editor.file.map.grid;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the GRID_STACK struct.
 * Created by Kneesnap on 8/27/2018.
 */
@Getter
@Setter
public class GridStack extends GameObject {
    private short squareCount; // Either 0 or 1.
    private int index; // Index of first stack square in the GRID_SQUARE array.

    @Override
    public void load(DataReader reader) {
        this.squareCount = reader.readUnsignedByteAsShort();
        reader.readByte(); // Unused
        this.index = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte(this.squareCount);
        writer.writeByte(Constants.NULL_BYTE); // Unused.
        writer.writeUnsignedShort(this.index);
    }
}
