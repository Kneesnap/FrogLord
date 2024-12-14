package net.highwayfrogs.editor.games.renderware.chunks.sector;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.*;
import net.highwayfrogs.editor.games.renderware.chunks.RwGeometryChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwMaterialChunk;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.games.renderware.struct.types.*;
import net.highwayfrogs.editor.games.renderware.struct.types.RpTriangle.IRwGeometryMesh;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements an atomic world sector like WorldSectorStreamRead/babinwor.c
 * TODO: type = rwSECTORATOMIC
 * In this case, atomic means it does not have any children.
 * Created by Kneesnap on 8/16/2024.
 */
@Getter
public class RwAtomicSectorChunk extends RwSectorBase implements IRwGeometryMesh {
    private int materialListBaseIndex;
    private final List<RwV3d> vertices = new ArrayList<>();
    private final List<RpVertexNormal> normals = new ArrayList<>();
    private final List<RwColorRGBA> preLitColors = new ArrayList<>();
    private final List<RpTriangle> triangles = new ArrayList<>();
    private final List<List<RwTexCoords>> texCoordSets = new ArrayList<>();

    public RwAtomicSectorChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.ATOMIC_SECTOR, version, parentChunk);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        if (!getWorld().getWorldSectors().contains(this))
            getWorld().getWorldSectors().add(this);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        RpWorldSectorInfoExtension sectorInfo = readStruct(reader, new RpWorldSectorInfoExtension(this));
        this.materialListBaseIndex = sectorInfo.getMaterialListWindowBase();
        readOptionalExtensionData(reader);
    }

    private void loadStructExtensionData(DataReader reader, int version, int dataLength, RpWorldSectorInfo sectorInfo) {
        RpWorldChunkInfo world = getWorld().getWorldInfo();
        this.vertices.clear();
        this.normals.clear();
        this.preLitColors.clear();
        this.texCoordSets.clear();
        if (world.testFlagMask(RwGeometryChunk.FLAG_NATIVE))
            return;

        // Read vertices.
        if (sectorInfo.getNumVertices() > 0) {
            for (int i = 0; i < sectorInfo.getNumVertices(); i++) {
                RwV3d newVertex = new RwV3d(getGameInstance());
                newVertex.load(reader, version, dataLength);
                this.vertices.add(newVertex);
            }

            // Read normals.
            if (world.testFlagMask(RwGeometryChunk.FLAG_NORMALS)) {
                for (int i = 0; i < sectorInfo.getNumVertices(); i++) {
                    RpVertexNormal newNormal = new RpVertexNormal(getGameInstance());
                    newNormal.load(reader, version, dataLength);
                    this.normals.add(newNormal);
                }
            }

            // Read vertex pre-light colors.
            if (world.testFlagMask(RwGeometryChunk.FLAG_PRELIT)) {
                for (int i = 0; i < sectorInfo.getNumVertices(); i++) {
                    RwColorRGBA newColor = new RwColorRGBA(getGameInstance());
                    newColor.load(reader, version);
                    this.preLitColors.add(newColor);
                }
            }

            // Read texture coordinate sets.
            int texCoordSets = world.getTexCoordSetsCount();
            if (texCoordSets > 0) {
                for (int i = 0; i < texCoordSets; i++) {
                    List<RwTexCoords> texCoordSet = new ArrayList<>();
                    for (int j = 0; j < sectorInfo.getNumVertices(); j++) {
                        RwTexCoords newTexCoords = new RwTexCoords(getGameInstance());
                        newTexCoords.load(reader, version, dataLength);
                        texCoordSet.add(newTexCoords);
                    }

                    this.texCoordSets.add(texCoordSet);
                }
            }
        }

        // Read triangles.
        this.triangles.clear();
        for (int i = 0; i < sectorInfo.getNumTriangles(); i++) {
            RpTriangle newTriangle = new RpTriangle(this);
            newTriangle.load(reader, version, dataLength);
            this.triangles.add(newTriangle);
        }

        // Read coll section.
        if (sectorInfo.isCollisionSectorPresent()) {
            getLogger().warning("Reading/Writing RpCollSection is not implemented, FrogLord will need to be updated to support it.");
            reader.skipBytes(3 * RpCollSector.MAX_COLLISION_SECTOR_COUNT);
        }

        // Extension chunk is read by the struct chunk.
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        RpWorldSectorInfoExtension sectorInfo = new RpWorldSectorInfoExtension(this);
        sectorInfo.applyDataFromSector(this);
        writeStruct(writer, sectorInfo);
        writeOptionalExtensionData(writer);
    }

    private void saveStructExtensionData(DataWriter writer, RpWorldSectorInfo sectorInfo) {
        RpWorldChunkInfo world = getWorld().getWorldInfo();
        if (world.testFlagMask(RwGeometryChunk.FLAG_NATIVE))
            return;

        // Write vertices.
        for (int i = 0; i < this.vertices.size(); i++)
            this.vertices.get(i).save(writer, getVersion());

        // Write normals.
        if (world.testFlagMask(RwGeometryChunk.FLAG_NORMALS)) {
            if (this.normals.size() != this.vertices.size())
                throw new IllegalStateException("Cannot save " + this.normals.size() + " vertex normals, when there are a different number of vertices. (" + this.vertices.size() + ")");

            for (int i = 0; i < this.normals.size(); i++)
                this.normals.get(i).save(writer, getVersion());
        }

        // Write vertex pre-light colors.
        if (world.testFlagMask(RwGeometryChunk.FLAG_PRELIT)) {
            if (this.preLitColors.size() != this.vertices.size())
                throw new IllegalStateException("Cannot save " + this.preLitColors.size() + " vertex colors, when there are a different number of vertices. (" + this.vertices.size() + ")");

            for (int i = 0; i < this.preLitColors.size(); i++)
                this.preLitColors.get(i).save(writer, getVersion());
        }

        // Write texture coordinate sets.
        for (int i = 0; i < this.texCoordSets.size(); i++) {
            List<RwTexCoords> texCoords = this.texCoordSets.get(i);
            if (texCoords.size() != this.vertices.size())
                throw new IllegalStateException("Cannot save texCoordSet " + i + " with " + texCoords.size() + " entries, when there are a different number of vertices. (" + this.vertices.size() + ")");

            for (int j = 0; j < texCoords.size(); j++)
                texCoords.get(j).save(writer, getVersion());
        }

        // Write triangles.
        for (int i = 0; i < this.triangles.size(); i++)
            this.triangles.get(i).save(writer, getVersion());

        // Later, if we ever encounter a game with it, write coll section.

        // Extension chunk is written by the struct chunk.
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Triangle Count", this.triangles.size());
        propertyList.add("Vertex Count", this.vertices.size());
        propertyList.add("Material List Window Base", this.materialListBaseIndex);
        propertyList.add("# of TexCoord sets", this.texCoordSets.size());
        propertyList.add("Minimum Position", calculateMinPosition(new RwV3d(getGameInstance())));
        propertyList.add("Maximum Position", calculateMaxPosition(new RwV3d(getGameInstance())));
        // TODO: Include flags in world.
        return propertyList;
    }

    /**
     * Gets the material used by a particular triangle.
     * @param triangle the triangle to resolve the material from
     * @return material, if one is found
     */
    public RwMaterialChunk getMaterial(RpTriangle triangle) {
        List<RwMaterialChunk> materialList = getWorld().getMaterialList().getMaterials();
        int worldMaterialIndex = this.materialListBaseIndex + triangle.getMaterialIndex();
        return materialList.size() > worldMaterialIndex ? materialList.get(worldMaterialIndex) : null;
    }

    /**
     * Calculates the minimum vertex position.
     * TODO: Problem, this fails when there are no vertices.
     * @param result the object to store the minimum position within.
     * @return minPosition
     */
    public RwV3d calculateMinPosition(RwV3d result) {
        result.setX(Float.POSITIVE_INFINITY);
        result.setY(Float.POSITIVE_INFINITY);
        result.setZ(Float.POSITIVE_INFINITY);
        for (int i = 0; i < this.vertices.size(); i++) {
            RwV3d vertex = this.vertices.get(i);
            if (vertex.getX() <= result.getX())
                result.setX(vertex.getX());
            if (vertex.getY() <= result.getY())
                result.setY(vertex.getY());
            if (vertex.getZ() <= result.getZ())
                result.setZ(vertex.getZ());
        }

        return result;
    }

    /**
     * Calculates the maximum vertex position.
     * @param result the object to store the maximum position within.
     * @return maxPosition
     */
    public RwV3d calculateMaxPosition(RwV3d result) {
        result.setX(Float.NEGATIVE_INFINITY);
        result.setY(Float.NEGATIVE_INFINITY);
        result.setZ(Float.NEGATIVE_INFINITY);
        for (int i = 0; i < this.vertices.size(); i++) {
            RwV3d vertex = this.vertices.get(i);
            if (vertex.getX() >= result.getX())
                result.setX(vertex.getX());
            if (vertex.getY() >= result.getY())
                result.setY(vertex.getY());
            if (vertex.getZ() >= result.getZ())
                result.setZ(vertex.getZ());

        }

        return result;
    }

    @Override
    public String getLoggerInfo() {
        return super.getLoggerInfo() + ",triCount=" + this.triangles.size() + ",vtxCount=" + this.vertices.size();
    }

    @Override
    public boolean isWorldSector() {
        return true;
    }

    // The geometry data isn't part of the struct definition, but is part of the data written to the struct chunk section.
    // We use this to read/write the data as struct data.
    private static class RpWorldSectorInfoExtension extends RpWorldSectorInfo { // TODO: WorldSectorIsCorrectlySorted() should give info.
        private final RwAtomicSectorChunk sectorChunk;

        public RpWorldSectorInfoExtension(RwAtomicSectorChunk sectorChunk) {
            super(sectorChunk.getGameInstance());
            this.sectorChunk = sectorChunk;
        }

        @Override
        public void load(DataReader reader, int version, int byteLength) {
            int dataStartIndex = reader.getIndex();
            super.load(reader, version, byteLength);
            this.sectorChunk.loadStructExtensionData(reader, version, byteLength - (reader.getIndex() - dataStartIndex), this);
        }

        @Override
        public void save(DataWriter writer, int version) {
            super.save(writer, version);
            this.sectorChunk.saveStructExtensionData(writer, this);
        }
    }

    @Getter
    public static class RpWorldSectorInfo extends RwStruct {
        private int materialListWindowBase;
        private int numTriangles;
        private int numVertices;
        private final RwV3d minPosition; // Infimum
        private final RwV3d maxPosition; // Supremum
        private boolean collisionSectorPresent;

        public RpWorldSectorInfo(GameInstance instance) {
            super(instance, RwStructType.WORLD_CHUNK_INFO_SECTOR);
            this.minPosition = new RwV3d(instance);
            this.maxPosition = new RwV3d(instance);
        }

        /**
         * Apply the data from the sector to this struct.
         * @param chunk the chunk to apply the data from
         */
        public void applyDataFromSector(RwAtomicSectorChunk chunk) {
            this.materialListWindowBase = chunk.getMaterialListBaseIndex();
            this.numTriangles = chunk.getTriangles().size();
            this.numVertices = chunk.getVertices().size();
            chunk.calculateMinPosition(this.minPosition);
            chunk.calculateMaxPosition(this.maxPosition);
            this.collisionSectorPresent = false;
        }

        @Override
        public void load(DataReader reader, int version, int byteLength) {
            this.materialListWindowBase = reader.readInt();
            this.numTriangles = reader.readInt();
            this.numVertices = reader.readInt();
            this.minPosition.load(reader, version, byteLength);
            this.maxPosition.load(reader, version, byteLength);
            if (RwVersion.isAtLeast(version, RwVersion.VERSION_3403)) { // NOTE: The exact version is not known when this changed, but it seems it was after Frogger Beyond (3.3.0.2), and at/before Frogger Rescue (3.4.0.3).
                this.collisionSectorPresent = false;
                reader.skipInt(); // Garbage data, no longer used, but still has data.
            } else {
                this.collisionSectorPresent = RwUtils.readRwBool(reader);
            }
            reader.skipInt(); // unused.
        }

        @Override
        public void save(DataWriter writer, int version) {
            writer.writeInt(this.materialListWindowBase);
            writer.writeInt(this.numTriangles);
            writer.writeInt(this.numVertices);
            this.minPosition.save(writer, version);
            this.maxPosition.save(writer, version);
            if (RwVersion.isAtLeast(version, RwVersion.VERSION_3403)) { // NOTE: The exact version is not known when this chamged, but it seems it was before 3.4.
                writer.writeInt(0);
            } else {
                RwUtils.writeRwBool(writer, this.collisionSectorPresent);
            }
            writer.writeInt(0); // unused
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList = super.addToPropertyList(propertyList);
            propertyList.add("Material List Window Base", this.materialListWindowBase);
            propertyList.add("Triangle Count", this.numTriangles);
            propertyList.add("Vertex Count", this.numVertices);
            propertyList.add("Min Position", this.minPosition);
            propertyList.add("Max Position", this.maxPosition);
            if (this.collisionSectorPresent)
                propertyList.add("Collision Sector Present?", true);
            return propertyList;
        }

        @Override
        public String toString() {
            return "RpWorldSectorInfo{materialListWindowBase=" + this.materialListWindowBase
                    + ",triCount=" + this.numTriangles + ",vtxCount="  + this.numVertices
                    + ",minPos=" + this.minPosition + ",maxPos=" + this.maxPosition + "}";
        }
    }

    @Getter
    public static class RpCollSector extends RwStruct {
        private byte cType; // binary tppaaaaa where t = type (1 = plane, 0 = polygons), p = plane, a = amount ON plane
        private short vertexIndex; // vertex index used for the split
        private short startPolygon;

        public static final int MAX_COLLISION_CUTS = 7;
        public static final int MAX_COLLISION_SECTOR_COUNT = 1 << MAX_COLLISION_CUTS;

        public RpCollSector(GameInstance instance) {
            super(instance, RwStructType.COLLISION_SECTOR);
        }

        @Override
        public void load(DataReader reader, int version, int byteLength) {
            this.cType = reader.readByte();
            this.vertexIndex = reader.readUnsignedByteAsShort();
            this.startPolygon = reader.readUnsignedByteAsShort();
        }

        @Override
        public void save(DataWriter writer, int version) {
            writer.writeByte(this.cType);
            writer.writeUnsignedByte(this.vertexIndex);
            writer.writeUnsignedByte(this.startPolygon);
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList = super.addToPropertyList(propertyList);
            propertyList.add("cType", NumberUtils.toHexString(this.cType & 0xFF));
            propertyList.add("Vertex Index", this.vertexIndex);
            propertyList.add("Start Polygon", this.startPolygon);
            return propertyList;
        }

        @Override
        public String toString() {
            return "RpCollSector{cType=" + NumberUtils.toHexString(this.cType & 0xFF)
                    + ",vtxIndex=" + this.vertexIndex + ",startPolygon="  + this.startPolygon + "}";
        }
    }

}