package net.highwayfrogs.editor.file.map.grid;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitive;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.standard.psx.PSXGPUPrimitive;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquare;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.Map;

/**
 * Represents the GRID_SQUARE struct.
 * Created by Kneesnap on 8/27/2018.
 */
@Getter
public class GridSquare extends GameObject {
    private int flags;
    @Setter private MAPPolygon polygon;
    private final transient MAPFile parent;

    public GridSquare(MAPFile parent) {
        this.parent = parent;
    }

    public GridSquare(MAPPolygon poly, MAPFile parent) {
        this(parent);
        this.polygon = poly;
    }

    public GridSquare(MAPPolygon poly, MAPFile parent, int flags) {
        this(poly, parent);
        this.flags = flags;
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readInt();
        int polyF4Pointer = reader.readInt();

        PSXGPUPrimitive prim = parent.getLoadPointerPolygonMap().get(polyF4Pointer);
        if (polyF4Pointer != 0) {
            Utils.verify(prim != null, "GridSquare's Polygon Pointer does not point to a primitive! (%d)", polyF4Pointer);
            Utils.verify(prim instanceof MAPPolygon, "GridSquare's Polygon Pointer does not point to a polygon! (%d)", polyF4Pointer);
        }

        this.polygon = (MAPPolygon) prim;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.flags);

        Integer polyPointer = parent.getSavePolygonPointerMap().get(this.polygon);
        if (this.polygon == null)
            polyPointer = 0; // Can write null.

        Utils.verify(polyPointer != null, "A GridSquare's polygon was not saved! This means this GridSquare likely should not be saved!");
        writer.writeInt(polyPointer);
    }

    /**
     * Test if a flag is present.
     * @param flag The flag to test.
     * @return isPresent
     */
    public boolean testFlag(GridSquareFlag flag) {
        return (getFlags() & flag.getFlag()) == flag.getFlag();
    }

    /**
     * Set the flag state.
     * @param flag     The flag type.
     * @param newState The new state of the flag.
     */
    public void setFlag(GridSquareFlag flag, boolean newState) {
        boolean oldState = testFlag(flag);
        if (oldState == newState)
            return; // Prevents the ^ operation from breaking the value.

        if (newState) {
            this.flags |= flag.getFlag();
        } else {
            this.flags ^= flag.getFlag();
        }
    }

    /**
     * Converts the grid square from the old format to the new format.
     * @param newGridStack the new format object to convert to
     * @param convertedPolygons the converted polygon lookup table
     * @return newGridSquare
     */
    public FroggerGridSquare convertToNewFormat(@NonNull FroggerGridStack newGridStack, @NonNull Map<MAPPrimitive, FroggerMapPolygon> convertedPolygons) {
        FroggerMapPolygon convertedPolygon = convertedPolygons.get(this.polygon);
        if (convertedPolygon == null)
            throw new RuntimeException("Could not find the polygon attached to the grid square. (Was it actually converted to the new format?)");

        return new FroggerGridSquare(newGridStack, convertedPolygon, this.flags);
    }
}
