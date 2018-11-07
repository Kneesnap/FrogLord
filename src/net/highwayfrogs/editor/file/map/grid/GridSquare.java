package net.highwayfrogs.editor.file.map.grid;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXGPUPrimitive;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the GRID_SQUARE struct.
 * Created by Kneesnap on 8/27/2018.
 */
@Getter
public class GridSquare extends GameObject {
    private int flags;
    private PSXGPUPrimitive polygon;
    private MAPFile parent;

    public GridSquare(MAPFile parent) {
        this.parent = parent;
    }

    public static final int FLAG_USABLE = 1; // Frog can jump here.
    public static final int FLAG_SAFE = 1 << 1; // Standard land.
    public static final int FLAG_DEADLY = 1 << 2; // Frogger dies.
    public static final int FLAG_WATER = 1 << 3; // Frogger drowns here.
    public static final int FLAG_SLIPPY = 1 << 4; // Frogger slides around.
    public static final int FLAG_BOUNCY = 1 << 5; // Frogger bounces.
    public static final int FLAG_CHECKPOINT = 1 << 6; // Checkpoint here?
    public static final int FLAG_SLIPPY_CONTROL = 1 << 7; // Slippy but frogger can control.
    public static final int FLAG_SOFT_GROUND = 1 << 8; // Frog won't die from fall damage.
    public static final int FLAG_EXTEND_HOP_HEIGHT = 1 << 9; // Unused. Believe this was supposed to extend the height the frog can super jump at. But, it's not used.
    public static final int FLAG_SIMPLE_SLIPPY = 1 << 10; // Not sure how this differs from the first slippy flag.
    public static final int FLAG_CLIFF_DEATH = 1 << 11; // Kill the frog with a cliff death.
    public static final int FLAG_POP_DEATH = 1 << 12; // Frog does a polygon-pop death.

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readInt();
        int polyF4Pointer = reader.readInt();
        this.polygon = parent.getPointerPolygonMap().get(polyF4Pointer); //TODO This seems to sorta not work.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.flags);
        writer.writeInt(parent.getPolygonPointerMap().getOrDefault(polygon, 0));
    }
}
