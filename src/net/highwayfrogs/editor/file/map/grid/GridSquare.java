package net.highwayfrogs.editor.file.map.grid;

import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXGPUPrimitive;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.PSXPolygon;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the GRID_SQUARE struct.
 * Created by Kneesnap on 8/27/2018.
 */
@Getter
public class GridSquare extends GameObject {
    private int flags;
    private PSXPolygon polygon;
    private transient MAPFile parent;

    public GridSquare(MAPFile parent) {
        this.parent = parent;
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readInt();
        int polyF4Pointer = reader.readInt();

        PSXGPUPrimitive prim = parent.getLoadPointerPolygonMap().get(polyF4Pointer);
        Utils.verify(prim != null, "GridSquare's Polygon Pointer does not point to a primitive! (%d)", polyF4Pointer);
        Utils.verify(prim instanceof PSXPolygon, "GridSquare's Polygon Pointer does not point to a polygon! (%d)", polyF4Pointer);
        this.polygon = (PSXPolygon) prim;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.flags);

        Integer polyPointer = parent.getSavePolygonPointerMap().get(polygon);
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
     * @param flag  The flag type.
     * @param state The state of the flag.
     */
    public void setFlag(GridSquareFlag flag, boolean state) {
        if (state) {
            this.flags |= flag.getFlag();
        } else {
            this.flags ^= flag.getFlag();
        }
    }
}
