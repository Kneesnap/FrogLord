package net.highwayfrogs.editor.file.map.grid;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the GRID_STACK struct.
 * Created by Kneesnap on 8/27/2018.
 */
@Getter
@Setter
public class GridStack extends GameObject {
    private List<GridSquare> gridSquares = new ArrayList<>();

    private transient int loadedSquareCount;
    private transient int tempIndex;

    @Override
    public void load(DataReader reader) {
        this.loadedSquareCount = reader.readUnsignedByteAsShort();
        reader.readByte(); // Unused
        this.tempIndex = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte((short) gridSquares.size());
        writer.writeByte(Constants.NULL_BYTE); // Unused.
        writer.writeUnsignedShort(this.tempIndex);
    }

    /**
     * Load squares after they're loaded.
     * @param file The file to read the squares from.
     */
    public void loadSquares(MAPFile file) {
        for (int i = 0; i < getLoadedSquareCount(); i++)
            gridSquares.add(file.getLoadGridSquares().get(getTempIndex() + i));
    }
}
