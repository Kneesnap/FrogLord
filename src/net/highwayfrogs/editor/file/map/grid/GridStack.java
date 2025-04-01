package net.highwayfrogs.editor.file.map.grid;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitive;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.utils.DataUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the GRID_STACK struct.
 * Created by Kneesnap on 8/27/2018.
 */
@Getter
@Setter
public class GridStack extends GameObject {
    private List<GridSquare> gridSquares = new ArrayList<>();
    private short averageHeight; // This is only used for cliff deaths. However it gets calculated, I'm not sure, it's pretty complicated it seems.

    private transient int loadedSquareCount;
    private transient int tempIndex;

    @Override
    public void load(DataReader reader) {
        this.loadedSquareCount = reader.readUnsignedByteAsShort();
        this.averageHeight = reader.readUnsignedByteAsShort();
        this.tempIndex = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte((short) gridSquares.size());
        writer.writeUnsignedByte(this.averageHeight);
        writer.writeUnsignedShort(this.tempIndex);
    }

    /**
     * Gets the stack height.
     * @return height
     */
    public int getHeight() {
        return (this.averageHeight << 6);
    }

    /**
     * Set the height of this stack.
     * @param newHeight The new height.
     */
    public void setHeight(int newHeight) {
        this.averageHeight = (short) (newHeight >> 6);
    }

    /**
     * Load squares after they're loaded.
     * @param loadedGridSquares The list of squares to read from.
     */
    public void loadSquares(List<GridSquare> loadedGridSquares) {
        for (int i = 0; i < getLoadedSquareCount(); i++)
            gridSquares.add(loadedGridSquares.get(getTempIndex() + i));
    }

    /**
     * Calculates the world height of this stack.
     * @param map The map which this stack belongs to.
     * @return worldHeight
     */
    public float calculateWorldHeight(MAPFile map) {
        for (int i = this.gridSquares.size() - 1; i >= 0; i--) {
            GridSquare square = this.gridSquares.get(i);

            MAPPrimitive polygon = square.getPolygon();
            if (polygon != null) {
                float heightSum = 0;
                for (int j = 0; j < polygon.getVerticeCount(); j++)
                    heightSum += map.getVertexes().get(polygon.getVertices()[j]).getFloatY();

                return heightSum / polygon.getVerticeCount();
            }
        }

        // No usable grid squares.
        return -DataUtils.fixedPointIntToFloat4Bit(getHeight());
    }

    /**
     * Converts the grid stack from the old format to the new format.
     * @param newGridStack the new format object to convert to
     * @param convertedPolygons the converted polygon lookup table
     */
    public void convertToNewFormat(@NonNull FroggerGridStack newGridStack, @NonNull Map<MAPPrimitive, FroggerMapPolygon> convertedPolygons) {
        newGridStack.clear();
        newGridStack.setAverageWorldHeight(this.averageHeight << 6);
        for (GridSquare gridSquare : this.gridSquares)
            newGridStack.getGridSquares().add(gridSquare.convertToNewFormat(newGridStack, convertedPolygons));
    }
}
