package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.*;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.games.renderware.struct.types.*;
import net.highwayfrogs.editor.games.renderware.struct.types.RpTriangle.IRwGeometryMesh;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents 'RpGeometry'.
 * Created by Kneesnap on 8/25/2024.
 */
@Getter
public class RwGeometryChunk extends RwStreamChunk implements IRwGeometryMesh {
    private int formatFlags;
    private int vertexCount;
    private final List<RwColorRGBA> preLitColors = new ArrayList<>();
    private final List<RpTriangle> triangles = new ArrayList<>();
    private final List<List<RwTexCoords>> texCoordSets = new ArrayList<>();
    private final List<RpMorphTarget> morphTargets = new ArrayList<>();
    private final RwMaterialListChunk materialList;

    public static final int FLAG_TRISTRIP = Constants.BIT_FLAG_0; // Can be rendered as strips.
    public static final int FLAG_POSITIONS = Constants.BIT_FLAG_1; // This mesh has positions. Is this even used?
    public static final int FLAG_TEXTURED = Constants.BIT_FLAG_2; // This geometry has only one set of texture coordinates.
    public static final int FLAG_PRELIT = Constants.BIT_FLAG_3; // Mesh has pre-light baked colors (on a per-vertex basis)
    public static final int FLAG_NORMALS = Constants.BIT_FLAG_4; // Mesh has normals (on a per-vertex basis)
    public static final int FLAG_LIGHT = Constants.BIT_FLAG_5; // Mesh should have lighting applied to it.
    public static final int FLAG_MODULATE_MATERIAL_COLOR = Constants.BIT_FLAG_6; // Modulate material color with vertex colors (both pre-lit and lit).
    public static final int FLAG_TEXTURED2 = Constants.BIT_FLAG_7; // Has at least 2 sets of texture coordinates.

    public static final int FLAG_NATIVE = Constants.BIT_FLAG_24;
    public static final int FLAG_NATIVE_INSTANCE = Constants.BIT_FLAG_25;
    public static final int FLAG_SECTORS_OVERLAP = Constants.BIT_FLAG_30;
    public static final int FLAG_VALIDATION_MASK = 0b01000011_11111111_00000000_11111111; // 0xggnn00gg, nn = num tex coords, gg = flags

    public RwGeometryChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.GEOMETRY, version, parentChunk);
        this.materialList = new RwMaterialListChunk(streamFile, version, this);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        readStruct(reader, new RpGeometryChunkInfoExtension(this));
        readChunk(reader, this.materialList);
        readOptionalExtensionData(reader);
    }

    private void loadStructExtensionData(DataReader reader, int version, int dataLength, RpGeometryChunkInfoExtension sectorInfo) {
        // Apply data from the struct.
        this.vertexCount = sectorInfo.getVertexCount();
        this.formatFlags = sectorInfo.getFormatFlags();
        warnAboutInvalidBitFlags(this.formatFlags, FLAG_VALIDATION_MASK);

        // Read data.
        this.triangles.clear();
        this.preLitColors.clear();
        this.texCoordSets.clear();
        if (!testFlagMask(RwGeometryChunk.FLAG_NATIVE)) {
            // Read vertices.
            if (sectorInfo.getVertexCount() > 0) {
                // Read vertex pre-light colors.
                if (testFlagMask(FLAG_PRELIT)) {
                    for (int i = 0; i < sectorInfo.getVertexCount(); i++) {
                        RwColorRGBA newColor = new RwColorRGBA(getGameInstance());
                        newColor.load(reader, version);
                        this.preLitColors.add(newColor);
                    }
                }

                // Read texture coordinate sets.
                int texCoordSets = getTexCoordSetsCount();
                if (texCoordSets > 0) {
                    for (int i = 0; i < texCoordSets; i++) {
                        List<RwTexCoords> texCoordSet = new ArrayList<>();
                        for (int j = 0; j < sectorInfo.getVertexCount(); j++) {
                            RwTexCoords newTexCoords = new RwTexCoords(getGameInstance());
                            newTexCoords.load(reader, version, dataLength);
                            texCoordSet.add(newTexCoords);
                        }

                        this.texCoordSets.add(texCoordSet);
                    }
                }

                // Read triangles.
                for (int i = 0; i < sectorInfo.getTriangleCount(); i++) {
                    RpTriangle newTriangle = new RpTriangle(this);
                    newTriangle.loadGeometryFormat(reader);
                    this.triangles.add(newTriangle);
                }
            }
        }

        // Read Morph Targets (Containing vertices & normals). (Doesn't seem to exist in Frogger Beyond, but does exist in Rescue)
        this.morphTargets.clear();
        if (RwVersion.isAtLeast(getVersion(), RwVersion.VERSION_3403)) {
            for (int i = 0; i < sectorInfo.getMorphTargetCount(); i++) {
                RpMorphTarget newMorphTarget = new RpMorphTarget(this);
                newMorphTarget.load(reader, version, dataLength);
                this.morphTargets.add(newMorphTarget);
            }
        }
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writeStruct(writer, new RpGeometryChunkInfoExtension(this));
        writeChunk(writer, this.materialList); // Write material list.
        writeOptionalExtensionData(writer);
    }

    private void saveStructExtensionData(DataWriter writer) {
        if (!testFlagMask(RwGeometryChunk.FLAG_NATIVE)) {
            // Write vertex pre-light colors.
            if (testFlagMask(FLAG_PRELIT)) {
                if (this.preLitColors.size() != this.vertexCount)
                    throw new IllegalStateException("Cannot save " + this.preLitColors.size() + " vertex colors, when there are a different number of vertices. (" + this.vertexCount + ")");

                for (int i = 0; i < this.preLitColors.size(); i++)
                    this.preLitColors.get(i).save(writer, getVersion());
            }

            // Write texture coordinate sets.
            for (int i = 0; i < this.texCoordSets.size(); i++) {
                List<RwTexCoords> texCoords = this.texCoordSets.get(i);
                if (texCoords.size() != this.vertexCount)
                    throw new IllegalStateException("Cannot save texCoordSet " + i + " with " + texCoords.size() + " entries, when there are a different number of vertices. (" + this.vertexCount + ")");

                for (int j = 0; j < texCoords.size(); j++)
                    texCoords.get(j).save(writer, getVersion());
            }

            // Write triangles.
            for (int i = 0; i < this.triangles.size(); i++)
                this.triangles.get(i).saveGeometryFormat(writer);
        }

        // Write morph targets. (Doesn't seem to exist in Frogger Beyond?)
        if (RwVersion.isAtLeast(getVersion(), RwVersion.VERSION_3403)) {
            for (int i = 0; i < this.morphTargets.size(); i++)
                this.morphTargets.get(i).save(writer, getVersion());
        }
    }

    @Override
    public List<RwV3d> getVertices() {
        // TODO: Perhaps not in the interface.
        return this.morphTargets.size() > 0 ? this.morphTargets.get(0).getVertices() : null;
    }

    @Override
    public List<RpVertexNormal> getNormals() {
        return null;
    }

    @Override
    public RwMaterialChunk getMaterial(RpTriangle triangle) {
        List<RwMaterialChunk> materialList = this.materialList.getMaterials();
        int materialIndex = triangle.getMaterialIndex();
        return materialList.size() > materialIndex ? materialList.get(materialIndex) : null;
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Format Flags", NumberUtils.toHexString(this.formatFlags));
        propertyList.add("Vertex Count", this.vertexCount);
        propertyList.add("Triangle Count", this.triangles.size());
        propertyList.add("Morph Target Set Count", this.morphTargets.size());
        propertyList.add("# of TexCoord sets", this.texCoordSets.size());
        return propertyList;
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

    /**
     * Gets the number of texture coordinates
     * @param format the format flags to get the texCoordSets from
     * @return texCoordSetsCount
     */
    public static int getTexCoordSetsCount(int format) {
        if ((format & 0xFF0000) != 0) {
            return (format & 0xFF0000) >> 16;
        } else if ((format & FLAG_TEXTURED2) == FLAG_TEXTURED2) {
            return 2;
        } else if ((format & FLAG_TEXTURED) == FLAG_TEXTURED) {
            return 1;
        } else {
            return 0;
        }
    }

    // The geometry data isn't part of the struct definition, but is part of the data written to the struct chunk section.
    // We use this to read/write the data as struct data.
    private static class RpGeometryChunkInfoExtension extends RpGeometryChunkInfo {
        private final RwGeometryChunk geometryChunk;

        public RpGeometryChunkInfoExtension(RwGeometryChunk geometryChunk) {
            super(geometryChunk);
            this.geometryChunk = geometryChunk;
        }

        @Override
        public void load(DataReader reader, int version, int byteLength) {
            int dataStartIndex = reader.getIndex();
            super.load(reader, version, byteLength);
            this.geometryChunk.loadStructExtensionData(reader, version, byteLength - (reader.getIndex() - dataStartIndex), this);
        }

        @Override
        public void save(DataWriter writer, int version) {
            super.save(writer, version);
            this.geometryChunk.saveStructExtensionData(writer);
        }
    }

    /**
     * Represents '_rpGeometry' as defined in bageomet.h.
     */
    @Getter
    public static class RpGeometryChunkInfo extends RwStruct {
        private int formatFlags;
        private int triangleCount;
        private int vertexCount;
        private int morphTargetCount;

        public RpGeometryChunkInfo(GameInstance instance) {
            super(instance, RwStructType.GEOMETRY_CHUNK_INFO);
        }

        public RpGeometryChunkInfo(RwGeometryChunk geometry) {
            this(geometry.getGameInstance());
            this.formatFlags = geometry.getFormatFlags();
            this.triangleCount = geometry.getTriangles().size();
            this.vertexCount = geometry.getVertexCount();
            this.morphTargetCount = geometry.getMorphTargets().size();
        }

        @Override
        public void load(DataReader reader, int version, int byteLength) {
            this.formatFlags = reader.readInt(); // Validated in main area.
            this.triangleCount = reader.readInt();
            this.vertexCount = reader.readInt();
            this.morphTargetCount = reader.readInt();
        }

        @Override
        public void save(DataWriter writer, int version) {
            writer.writeInt(this.formatFlags);
            writer.writeInt(this.triangleCount);
            writer.writeInt(this.vertexCount);
            writer.writeInt(this.morphTargetCount);
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList = super.addToPropertyList(propertyList);
            propertyList.add("Format Flags", NumberUtils.toHexString(this.formatFlags));
            propertyList.add("Triangle Count", this.triangleCount);
            propertyList.add("Vertex Count", this.vertexCount);
            propertyList.add("Morph Target Count", this.morphTargetCount);
            return propertyList;
        }

        @Override
        public String toString() {
            return "RpGeometryChunkInfo{formatFlags=" + NumberUtils.toHexString(this.formatFlags)
                    + ",triCount" + this.triangleCount + ",vtxCount=" + this.vertexCount
                    + ",morphTargetCount=" + this.morphTargetCount + "}";
        }
    }

    /**
     * Represents '_rpMorphTarget' as defined in bageomet.c
     */
    @Getter
    public static class RpMorphTarget extends RwStruct {
        private final RwGeometryChunk geometry;
        private final RwSphere boundingSphere;
        private boolean pointsPresent;
        private boolean normalsPresent;
        private final List<RwV3d> vertices = new ArrayList<>();
        private final List<RwV3d> normals = new ArrayList<>();

        public RpMorphTarget(RwGeometryChunk geometry) {
            super(geometry.getGameInstance(), RwStructType.MORPH_TARGET);
            this.geometry = geometry;
            this.boundingSphere = new RwSphere(geometry.getGameInstance());
        }

        @Override
        public void load(DataReader reader, int version, int byteLength) {
            this.boundingSphere.load(reader, version, byteLength);
            this.pointsPresent = RwUtils.readRwBool(reader);
            this.normalsPresent = RwUtils.readRwBool(reader);

            // Read vertices.
            this.vertices.clear();
            if (this.pointsPresent) {
                for (int i = 0; i < this.geometry.getVertexCount(); i++) {
                    RwV3d newVertex = new RwV3d(getGameInstance());
                    newVertex.load(reader, version, byteLength);
                    this.vertices.add(newVertex);
                }
            }

            // Read normals.
            this.normals.clear();
            if (this.normalsPresent) {
                for (int i = 0; i < this.geometry.getVertexCount(); i++) {
                    RwV3d newNormal = new RwV3d(getGameInstance());
                    newNormal.load(reader, version, byteLength);
                    this.normals.add(newNormal);
                }
            }
        }

        @Override
        public void save(DataWriter writer, int version) {
            this.pointsPresent = (!this.geometry.testFlagMask(RwGeometryChunk.FLAG_NATIVE) && this.vertices.size() > 0);
            this.normalsPresent = (!this.geometry.testFlagMask(RwGeometryChunk.FLAG_NATIVE) && this.normals.size() > 0);
            if (this.pointsPresent && this.vertices.size() != this.geometry.getVertexCount())
                throw new IllegalStateException("Cannot save " + this.vertices.size() + " vertices when the geometry reports having a different number of vertices. (" + this.geometry.getVertexCount() + ")");
            if (this.normalsPresent && this.normals.size() != this.vertices.size())
                throw new IllegalStateException("Cannot save " + this.normals.size() + " vertex normals, when there are a different number of vertices. (" + this.vertices.size() + ")");

            this.boundingSphere.save(writer, version);
            RwUtils.writeRwBool(writer, this.pointsPresent);
            RwUtils.writeRwBool(writer, this.normalsPresent);

            // Write vertices.
            if (this.pointsPresent)
                for (int i = 0; i < this.vertices.size(); i++)
                    this.vertices.get(i).save(writer, version);

            // Write normals.
            if (this.normalsPresent)
                for (int i = 0; i < this.normals.size(); i++)
                    this.normals.get(i).save(writer, version);
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList = super.addToPropertyList(propertyList);
            propertyList.add("Bounding Sphere", this.boundingSphere);
            propertyList.add("Points Present", this.pointsPresent);
            propertyList.add("Normals Present", this.normalsPresent);
            return propertyList;
        }

        @Override
        public String toString() {
            return "RpMorphTarget{pointsPresent=" + this.pointsPresent + ",normalsPresent=" + this.normalsPresent
                    + ",boundingSphere=" + this.boundingSphere + "}";
        }
    }
}