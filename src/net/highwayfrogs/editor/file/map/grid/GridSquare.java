package net.highwayfrogs.editor.file.map.grid;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
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

    public static final int FLAG_USABLE = Constants.BIT_FLAG_0; // Frog can jump here.
    public static final int FLAG_SAFE = Constants.BIT_FLAG_1; // Standard land.
    public static final int FLAG_DEADLY = Constants.BIT_FLAG_2; // Frogger dies.
    public static final int FLAG_WATER = Constants.BIT_FLAG_3; // Frogger drowns here.
    public static final int FLAG_SLIPPY = Constants.BIT_FLAG_4; // Frogger slides around.
    public static final int FLAG_BOUNCY = Constants.BIT_FLAG_5; // Frogger bounces.
    public static final int FLAG_CHECKPOINT = Constants.BIT_FLAG_6; // Checkpoint here?
    public static final int FLAG_SLIPPY_CONTROL = Constants.BIT_FLAG_7; // Slippy but frogger can control.
    public static final int FLAG_SOFT_GROUND = Constants.BIT_FLAG_8; // Frog won't die from fall damage.
    public static final int FLAG_EXTEND_HOP_HEIGHT = Constants.BIT_FLAG_9; // Unused. Believe this was supposed to extend the height the frog can super jump at. But, it's not used.
    public static final int FLAG_SIMPLE_SLIPPY = Constants.BIT_FLAG_10; // Not sure how this differs from the first slippy flag.
    public static final int FLAG_CLIFF_DEATH = Constants.BIT_FLAG_11; // Kill the frog with a cliff death.
    public static final int FLAG_POP_DEATH = Constants.BIT_FLAG_12; // Frog does a polygon-pop death.

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
    public boolean testFlag(int flag) {
        return (getFlags() & flag) == flag;
    }
}
