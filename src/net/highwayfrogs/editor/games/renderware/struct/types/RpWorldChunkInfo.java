package net.highwayfrogs.editor.games.renderware.struct.types;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.RwUtils;
import net.highwayfrogs.editor.games.renderware.RwVersion;
import net.highwayfrogs.editor.games.renderware.chunks.RwGeometryChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwMaterialChunk.RwSurfaceProperties;
import net.highwayfrogs.editor.games.renderware.chunks.RwWorldChunk;
import net.highwayfrogs.editor.games.renderware.chunks.sector.RwAtomicSectorChunk;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Implemented from 'babinworld.h'
 * Created by Kneesnap on 8/12/2024.
 */
@Getter
public class RpWorldChunkInfo extends RwStruct {
    private boolean rootIsWorldSector;
    private final RwV3d invWorldOrigin;
    private final RwSurfaceProperties surfaceProperties; // Only used in older versions.
    private int numTriangles;
    private int numVertices;
    private int numPlaneSectors;
    private int numWorldSectors;
    private int colSectorSize;
    private int formatFlags; // Flags about the world. 0xggnn00gg, nn = num tex coords, gg = flags
    private final RwBBox boundingBox; // Null in early versions.

    public RpWorldChunkInfo(GameInstance instance) {
        super(instance, RwStructType.WORLD);
        this.invWorldOrigin = new RwV3d(instance);
        this.surfaceProperties = new RwSurfaceProperties(instance);
        this.boundingBox = new RwBBox(instance);
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        int readStartIndex = reader.getIndex();
        this.rootIsWorldSector = RwUtils.readRwBool(reader);
        this.invWorldOrigin.load(reader, version, byteLength - (reader.getIndex() - readStartIndex));
        if (hasSurfaceProperties(version))
            this.surfaceProperties.load(reader);

        this.numTriangles = reader.readInt();
        this.numVertices = reader.readInt();
        this.numPlaneSectors = reader.readInt();
        this.numWorldSectors = reader.readInt();
        this.colSectorSize = reader.readInt();
        this.formatFlags = reader.readInt();
        warnAboutInvalidBitFlags(this.formatFlags, RwGeometryChunk.FLAG_VALIDATION_MASK, "RpWorld formatFlags");

        if (RwVersion.isAtLeast(version, RwVersion.VERSION_3403)) // Version mentioned in comments.
            this.boundingBox.load(reader, version, byteLength - (reader.getIndex() - readStartIndex));

        if (this.colSectorSize > 0)
            getLogger().warning("The BSP has not been exported since RW3.03, and contains collision data that is not currently supported.");
    }

    @Override
    public void save(DataWriter writer, int version) {
        RwUtils.writeRwBool(writer, this.rootIsWorldSector);
        this.invWorldOrigin.save(writer, version);
        if (hasSurfaceProperties(version))
            this.surfaceProperties.save(writer);

        writer.writeInt(this.numTriangles);
        writer.writeInt(this.numVertices);
        writer.writeInt(this.numPlaneSectors);
        writer.writeInt(this.numWorldSectors);
        writer.writeInt(this.colSectorSize);
        writer.writeInt(this.formatFlags);

        if (RwVersion.isAtLeast(version, RwVersion.VERSION_3403)) // Version mentioned in comments.
            this.boundingBox.save(writer, version);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Root Is World Sector", this.rootIsWorldSector);
        propertyList.add("Inv World Origin", this.invWorldOrigin);
        propertyList.add("Triangles", this.numTriangles);
        propertyList.add("Vertices", this.numVertices);
        propertyList.add("Plane Sectors", this.numPlaneSectors);
        propertyList.add("Collision Sector Size", this.colSectorSize);
        propertyList.add("World Format Flags", Utils.toHexString(this.formatFlags));
        if (this.boundingBox != null)
            propertyList.add("Bounding Box", this.boundingBox);

        return propertyList;
    }

    public void applyDataFromWorld(RwWorldChunk worldChunk) {
        // Missing: invWorldOrigin, surfaceProperties, formatFlags.

        this.rootIsWorldSector = worldChunk.getRootSector().isWorldSector();

        this.numTriangles = 0;
        this.numVertices = 0;
        for (int i = 0; i < worldChunk.getWorldSectors().size(); i++) {
            RwAtomicSectorChunk chunk = worldChunk.getWorldSectors().get(i);
            this.numVertices += chunk.getVertices().size();
            this.numTriangles += chunk.getTriangles().size();
        }

        this.numPlaneSectors = worldChunk.getPlaneSectors().size();
        this.numWorldSectors = worldChunk.getWorldSectors().size();
        this.colSectorSize = 0;

        if (this.boundingBox != null) {
            this.boundingBox.getMinPosition().setX(Float.POSITIVE_INFINITY);
            this.boundingBox.getMinPosition().setY(Float.POSITIVE_INFINITY);
            this.boundingBox.getMinPosition().setZ(Float.POSITIVE_INFINITY);
            this.boundingBox.getMaxPosition().setX(Float.NEGATIVE_INFINITY);
            this.boundingBox.getMaxPosition().setY(Float.NEGATIVE_INFINITY);
            this.boundingBox.getMaxPosition().setZ(Float.NEGATIVE_INFINITY);
            for (int i = 0; i < worldChunk.getWorldSectors().size(); i++) {
                RwAtomicSectorChunk chunk = worldChunk.getWorldSectors().get(i);
                for (int j = 0; j < chunk.getVertices().size(); j++) {
                    RwV3d vertex = chunk.getVertices().get(j);
                    if (vertex.getX() < this.boundingBox.getMinPosition().getX())
                        this.boundingBox.getMinPosition().setX(vertex.getX());
                    if (vertex.getX() > this.boundingBox.getMaxPosition().getX())
                        this.boundingBox.getMaxPosition().setX(vertex.getX());
                    if (vertex.getY() < this.boundingBox.getMinPosition().getY())
                        this.boundingBox.getMinPosition().setY(vertex.getY());
                    if (vertex.getY() > this.boundingBox.getMaxPosition().getY())
                        this.boundingBox.getMaxPosition().setY(vertex.getY());
                    if (vertex.getZ() < this.boundingBox.getMinPosition().getZ())
                        this.boundingBox.getMinPosition().setZ(vertex.getZ());
                    if (vertex.getZ() > this.boundingBox.getMaxPosition().getZ())
                        this.boundingBox.getMaxPosition().setZ(vertex.getZ());
                }
            }
        }
    }

    /**
     * Returns true if surface properties are included in the data for the given version.
     * @param version the version to test
     * @return true iff the provided version has surface properties.
     */
    public boolean hasSurfaceProperties(int version) {
        // Surface Properties are seen in Frogger Beyond (3.3.0.2), but not Rescue (3.4.0.3).
        return !RwVersion.isAtLeast(version, RwVersion.VERSION_3403);
    }

    /**
     * Returns true if the provided flag bits are set.
     * @param bitFlagMask the bit mask to test
     * @return true if all bits in the mask are set
     */
    public boolean testFlagMask(int bitFlagMask) {
        return (this.formatFlags & bitFlagMask) == bitFlagMask;
    }

    /**
     * Gets the number of texture coordinate sets.
     */
    public int getTexCoordSetsCount() {
        return RwGeometryChunk.getTexCoordSetsCount(this.formatFlags);
    }
}