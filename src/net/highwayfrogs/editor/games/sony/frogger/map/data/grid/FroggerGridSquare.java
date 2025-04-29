package net.highwayfrogs.editor.games.sony.frogger.map.data.grid;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerMapGroup;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Represents the GRID_SQUARE struct.
 * Created by Kneesnap on 8/27/2018.
 */
@Getter
public class FroggerGridSquare extends SCGameData<FroggerGameInstance> {
    private final FroggerGridStack gridStack;
    @Setter private int flags;
    @Setter private FroggerMapPolygon polygon;

    public FroggerGridSquare(FroggerGridStack gridStack) {
        super(gridStack != null ? gridStack.getGameInstance() : null);
        this.gridStack = gridStack;
    }

    public FroggerGridSquare(FroggerGridStack gridStack, FroggerMapPolygon polygon) {
        this(gridStack);
        this.polygon = polygon;
    }

    public FroggerGridSquare(FroggerGridStack gridStack, FroggerMapPolygon polygon, int flags) {
        this(gridStack, polygon);
        this.flags = flags;
        warnAboutInvalidBitFlags(flags, FroggerGridSquareFlag.GRID_VALIDATION_BIT_MASK);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readInt();
        warnAboutInvalidBitFlags(this.flags, FroggerGridSquareFlag.GRID_VALIDATION_BIT_MASK);
        int polygonPointer = reader.readInt();

        // Find loaded polygon.
        List<FroggerMapPolygon> polygons = getMapFile().getPolygonPacket().getPolygons();
        int polygonIndex = Utils.binarySearch(polygons, polygonPointer, FroggerMapPolygon::getLastReadAddress);
        if (polygonIndex < 0)
            throw new RuntimeException("FroggerGridSquare's Polygon Pointer does not point to a valid polygon! (" + NumberUtils.toHexString(polygonPointer) + ")");

        this.polygon = polygons.get(polygonIndex);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.flags);

        // Write polygon pointer after validating the polygon is registered & was written.
        if (this.polygon == null || this.polygon.getLastWriteAddress() <= 0)
            throw new RuntimeException("A FroggerGridSquare's polygon was not saved! Most likely it was not registered to the map! (" + getLoggerInfo() + ")");

        // Ensure the polygon was written.
        FroggerMapGroup mapGroup = getMapFile().getGroupPacket().getMapGroup(this.polygon);
        if (Collections.binarySearch(mapGroup.getPolygonsByType(this.polygon.getPolygonType()), this.polygon, Comparator.comparingInt(FroggerMapPolygon::getLastWriteAddress)) < 0)
            throw new RuntimeException("A FroggerGridSquare's polygon was not saved! Most likely it was not registered to the map! (" + getLoggerInfo() + ")");

        writer.writeInt(this.polygon.getLastWriteAddress());
    }

    /**
     * Get the ID of the square within the stack that holds this square.
     */
    public int getLayerID() {
        return this.gridStack.getGridSquares().indexOf(this);
    }

    /**
     * Gets the logger information.
     */
    public String getLoggerInfo() {
        return this.gridStack != null ? (this.gridStack.getLoggerInfo() + ",Layer=" + getLayerID()) : Utils.getSimpleName(this);
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), FroggerGridSquare::getLoggerInfo, this);
    }

    /**
     * Gets the simplified reaction used to represent the grid square flags, if there is a simple reaction which can be used for the current flags.
     */
    public FroggerGridSquareReaction getReaction() {
        return FroggerGridSquareReaction.getReactionFromFlags(this.flags);
    }

    /**
     * Applies the reaction used to the grid square flags.
     * @param newReaction the reaction to apply
     */
    public void setReaction(FroggerGridSquareReaction newReaction) {
        if (newReaction == null)
            throw new NullPointerException("newReaction");

        FroggerGridSquareReaction oldReaction = getReaction();
        if (oldReaction == newReaction)
            return;

        this.flags &= ~FroggerGridSquareReaction.REACTION_BIT_MASK;
        this.flags |= newReaction.getGridSquareFlagBitMask();
    }

    /**
     * Gets the map file this square is used within.
     * @return mapFile
     */
    public FroggerMapFile getMapFile() {
        return this.gridStack != null ? this.gridStack.getMapFile() : (this.polygon != null ? this.polygon.getMapFile() : null);
    }

    /**
     * Test if a flag is present.
     * @param flag The flag to test.
     * @return isPresent
     */
    public boolean testFlag(FroggerGridSquareFlag flag) {
        if (flag == null)
            throw new NullPointerException("flag");
        if (!flag.isLandGridData())
            throw new IllegalArgumentException("Cannot test flag " + flag + " for FroggerGridSquare, as the flag is not applicable to FroggerGridSquare.");

        return (this.flags & flag.getBitFlagMask()) == flag.getBitFlagMask();
    }

    /**
     * Set the flag state.
     * @param flag     The flag type.
     * @param newState The new state of the flag.
     */
    public void setFlag(FroggerGridSquareFlag flag, boolean newState) {
        boolean oldState = testFlag(flag);
        if (oldState == newState)
            return; // Prevents the ^ operation from breaking the value.

        if (newState) {
            this.flags |= flag.getBitFlagMask();
        } else {
            this.flags &= ~flag.getBitFlagMask();
        }
    }

    /**
     * Calculates the average world height. Equivalent to GetGridSquareHeight()
     * @return averageWorldHeight
     */
    public int calculateAverageWorldHeight() {
        int worldHeight = 0;
        List<SVector> vertices = getMapFile().getVertexPacket().getVertices();
        int vertexCount = this.polygon.getVertexCount();
        for (int i = 0; i < vertexCount; i++)
            worldHeight += vertices.get(this.polygon.getVertices()[i]).getY();

        // Triangles are treated as quads for the purposes of grid calculations.
        // Using the last vertex is validated as good by the warnings that will display if the data doesn't match this pattern when read.
        if (vertexCount == 3) {
            worldHeight += vertices.get(this.polygon.getVertices()[vertexCount - 1]).getY();
            vertexCount++;
        }

        return worldHeight / vertexCount;
    }
}