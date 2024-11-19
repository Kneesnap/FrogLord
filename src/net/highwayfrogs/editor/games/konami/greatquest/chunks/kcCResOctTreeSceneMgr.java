package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GamePlatform;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTriMesh.kcCTriMesh;
import net.highwayfrogs.editor.games.konami.greatquest.loading.kcLoadContext;
import net.highwayfrogs.editor.games.konami.greatquest.map.octree.kcOctTree;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcBox4;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.games.konami.greatquest.model.*;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles the OTT chunk.
 * Helpful:
 * - <a href="https://github.com/Kneesnap/frogger-tgq-decomp/blob/main/export/projects/Frogger-PAL/GreatQuest/kcGameSystem/Src/kcCVtxBufList.h">kcCVtxBufList.h</a>
 * - <a href="https://github.com/Kneesnap/frogger-tgq-decomp/blob/main/export/projects/Frogger-PAL/kcTechGroup/KcLib/Src/kcMath/kcMath3D.h">kcMath3D.h</a>
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public class kcCResOctTreeSceneMgr extends kcCResource implements IMultiLineInfoWriter {
    private final List<kcVtxBufFileStruct> vertexBuffers = new ArrayList<>(); // Automatically added to the kcCOctTree in-game on load via kcCOctTreeAtom::SetBoundingBox, called from kcCResOctTreeSceneMgr::Load.
    private final List<kcMaterial> materials = new ArrayList<>();
    private final List<kcCTriMesh> collisionMeshes = new ArrayList<>(); // Automatically added to the kcCOctTree in-game on load via kcCOctTreeAtom::SetBoundingBox, called from kcCResOctTreeSceneMgr::Load.
    private final kcOctTree entityTree;
    private final kcOctTree visualTree;

    public static final int NAME_SIZE = 32;
    private static final int SUPPORTED_VERSION = 1;
    private static final int RESERVED_HEADER_FIELDS = 7;
    private static final int RESERVED_PRIM_HEADER_FIELDS = 3;
    public static final int LEVEL_RESOURCE_HASH = GreatQuestUtils.hash("OctTreeSceneMgr", true);

    public kcCResOctTreeSceneMgr(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.OCTTREESCENEMGR);
        // All the levels seem to use these for their oct trees.
        this.entityTree = new kcOctTree(getGameInstance(), 14, 14);
        this.visualTree = new kcOctTree(getGameInstance(), 14, 10);
    }

    /**
     * Export this data in .obj format.
     * @param folder   The folder to export to.
     * @param fileName The obj file name.
     */
    public void exportAsObj(File folder, String fileName) throws IOException {
        kcModelObjWriter.writeMapToObj(folder, fileName, this);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        int remainingDataInBytes = reader.readInt() - Constants.INTEGER_SIZE;
        if (reader.getRemaining() != remainingDataInBytes)
            getLogger().warning("OctTree file in '" + getParentFile().getDebugName() + "' said it had " + remainingDataInBytes + " remaining bytes, but it actually had " + reader.getRemaining() + ".");

        int version = reader.readInt();
        if (version != SUPPORTED_VERSION)
            getLogger().severe("OctTree file in '" + getParentFile().getDebugName() + "' identified as version " + version + ", but we only understand version " + SUPPORTED_VERSION + ".");

        int meshCount = reader.readInt();
        int treeEntityDataSize = reader.readInt();
        int primCount = reader.readInt();
        int treeVisualDataSize = reader.readInt();
        int materialCount = reader.readInt();
        int sourcePathMeshCount = reader.readInt();
        int sourcePathVisualCount = reader.readInt();
        reader.skipBytesRequireEmpty(RESERVED_HEADER_FIELDS * Constants.INTEGER_SIZE);

        // End of header.

        // Read oct tree data.
        int treeEntityEndIndex = reader.getIndex() + treeEntityDataSize;
        this.entityTree.load(reader);
        requireReaderIndex(reader, treeEntityEndIndex, "Expected visual kcOctTree");

        // Read visual tree data.
        int treeVisualEndIndex = reader.getIndex() + treeVisualDataSize;
        this.visualTree.load(reader);
        requireReaderIndex(reader, treeVisualEndIndex, "Expected primitive buffers");

        // Read primitives.
        this.vertexBuffers.clear();
        GamePlatform platform = getGameInstance().getPlatform();
        for (int i = 0; i < primCount; i++) {
            kcVtxBufFileStruct vtxBuf = new kcVtxBufFileStruct();
            vtxBuf.load(reader, platform);
            this.vertexBuffers.add(vtxBuf);
        }

        // Read materials.
        for (int i = 0; i < materialCount; i++) {
            kcMaterial newMaterial = new kcMaterial(getGameInstance());
            newMaterial.load(reader);
            this.materials.add(newMaterial);
        }

        // Read collision meshes.
        this.collisionMeshes.clear();
        for (int i = 0; i < meshCount; i++) {
            kcCTriMesh newMesh = new kcCTriMesh(getGameInstance());
            newMesh.load(reader);
            this.collisionMeshes.add(newMesh);
        }

        // There IS code in the final game which can read these.
        // However, this feature is not actually used in any of the retail maps.
        // I think this feature is capable of loading meshes (collision & visual) from other parts of the game.
        // However, I'm not really sure why we'd ever want this, since collision meshes aren't generally going to be shared.
        // So, we'll leave this unimplemented unless we see it actually used.
        if (sourcePathMeshCount != 0 || sourcePathVisualCount != 0)
            getLogger().severe("The sourcePath counts were not zero!! This feature was not added since it was not seen in any versions at the time of writing! (Source Path Mesh Count: " + sourcePathMeshCount + ", Source Path Visual Count: " + sourcePathVisualCount + ")");
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);

        // Header
        int fullSizeAddress = writer.writeNullPointer();
        writer.writeInt(SUPPORTED_VERSION);
        writer.writeInt(this.collisionMeshes.size()); // Mesh count.
        int treeEntityDataSizeAddress = writer.writeNullPointer();
        writer.writeInt(this.vertexBuffers.size()); // Prim count.
        int treeVisualDataSizeAddress = writer.writeNullPointer();
        writer.writeInt(this.materials.size());
        writer.writeInt(0); // sourcePathMeshCount
        writer.writeInt(0); // sourcePathVisualCount
        writer.writeNull(RESERVED_HEADER_FIELDS * Constants.INTEGER_SIZE);

        // Write oct tree data.
        int treeEntityStartIndex = writer.getIndex();
        this.entityTree.save(writer);
        writer.writeIntAtPos(treeEntityDataSizeAddress, writer.getIndex() - treeEntityStartIndex);

        // Write visual tree data.
        int treeVisualStartIndex = writer.getIndex();
        this.visualTree.save(writer);
        writer.writeIntAtPos(treeVisualDataSizeAddress, writer.getIndex() - treeVisualStartIndex);

        // Write primitives.
        for (int i = 0; i < this.vertexBuffers.size(); i++)
            this.vertexBuffers.get(i).save(writer);

        // Write materials.
        for (int i = 0; i < this.materials.size(); i++)
            this.materials.get(i).save(writer);

        // Write collision meshes.
        for (int i = 0; i < this.collisionMeshes.size(); i++)
            this.collisionMeshes.get(i).save(writer);

        writer.writeIntAtPos(fullSizeAddress, writer.getIndex() - fullSizeAddress);
    }

    @Override
    public void afterLoad1(kcLoadContext context) {
        super.afterLoad1(context);

        // Apply texture file names.
        GreatQuestChunkedFile parentFile = getParentFile();
        if (parentFile != null && parentFile.hasFilePath())
            context.getMaterialLoadContext().applyLevelTextureFileNames(parentFile, parentFile.getFilePath(), this.materials);
    }

    @Override
    public void afterLoad2(kcLoadContext context) {
        super.afterLoad2(context);

        // Resolves textures. Waits until after afterLoad1() when file names are resolved.
        context.getMaterialLoadContext().resolveMaterialTexturesInChunk(getParentFile(), this.materials);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        String newPadding = padding + " ";

        // Write vertex buffer data.
        builder.append(padding).append("Vertex Buffers [").append(this.vertexBuffers.size()).append("]:").append(Constants.NEWLINE);
        for (int i = 0; i < this.vertexBuffers.size(); i++)
            this.vertexBuffers.get(i).writeMultiLineInfo(builder, newPadding);
        builder.append(Constants.NEWLINE);

        // Write material data.
        builder.append(padding).append("Materials [").append(this.materials.size()).append("]:").append(Constants.NEWLINE);
        for (int i = 0; i < this.materials.size(); i++)
            this.materials.get(i).writeMultiLineInfo(builder, newPadding);
        builder.append(Constants.NEWLINE);

        // Write collision mesh data.
        builder.append(padding).append("Collision Meshes [").append(this.collisionMeshes.size()).append("]:").append(Constants.NEWLINE);
        for (int i = 0; i < this.collisionMeshes.size(); i++)
            this.collisionMeshes.get(i).writeMultiLineInfo(builder, newPadding);
        builder.append(Constants.NEWLINE);

        // Save entity tree.
        builder.append(padding).append("Entity Tree:").append(Constants.NEWLINE);
        this.entityTree.writeMultiLineInfo(builder, newPadding);
        builder.append(Constants.NEWLINE);

        // Save visual tree.
        builder.append(padding).append("Visual Tree:").append(Constants.NEWLINE);
        this.visualTree.writeMultiLineInfo(builder, newPadding);
        builder.append(Constants.NEWLINE);
    }

    @Override
    public void handleDoubleClick() {
        getParentFile().openMeshViewer();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Vertex Buffers", this.vertexBuffers.size());
        propertyList.add("Materials", this.materials.size());
        propertyList.add("Collision Meshes", this.collisionMeshes.size());
        propertyList.add("Entity Tree", "");
        propertyList = this.entityTree.addToPropertyList(propertyList);
        propertyList.add("Visual Tree", "");
        propertyList = this.visualTree.addToPropertyList(propertyList);
        return propertyList;
    }

    @Getter
    public static class kcVtxBufFileStruct implements IMultiLineInfoWriter {
        // _OTAPrimHeader (Applied to kcCOTAPrim in kcCOTAPrim::Init)
        @Setter private int materialId; // kcCOTAPrim::__ct()
        @Setter private float normalTolerance = 2F; // kcCOTAPrim::__ct()
        private final kcVector4 normalAverage = new kcVector4(0, 0, 1, 0); // kcCOTAPrim::__ct()
        private final kcBox4 boundingBox = new kcBox4(); // Generates the box bounding sphere from this, as well as oct tree collision.

        // kcVtxBufFileStruct
        private long fvf;
        private kcVertexFormatComponent[] components;
        private int fvfStride;
        @Setter private kcPrimitiveType primitiveType;
        private final List<kcVertex> vertices = new ArrayList<>();

        /**
         * Set the FVF value for this vtxBuf.
         * @param newFvf   The new fvf value to include.
         * @param platform The platform to save the FVF on.
         */
        public void setFVF(long newFvf, GamePlatform platform) {
            this.fvf = newFvf;
            this.components = kcModel.calculateOrder(newFvf, platform);
            this.fvfStride = kcModel.calculateStride(this.components, newFvf);
        }

        /**
         * Get the number of vertices tracked in the buffer.
         */
        public int getVertexCount() {
            return this.vertices.size();
        }

        /**
         * Load the data from the reader.
         * @param reader   The reader to read data from.
         * @param platform The platform to read data for.
         */
        public void load(DataReader reader, GamePlatform platform) {
            int startReadIndex = reader.getIndex();

            // _OTAPrimHeader from kcCVtxBufList.h
            int otaPrimHeaderSize = reader.readInt();
            this.materialId = reader.readInt();
            this.normalTolerance = reader.readFloat();
            int otaZero = reader.readInt();
            this.normalAverage.load(reader);
            this.boundingBox.load(reader);
            if (otaZero != 0) // Written as zero by kcCOTAPrim::Store()
                throw new RuntimeException("The reserved field in the _OTAPrimHeader was supposed to be zero, but actually was " + otaZero + ".");

            // _kcVtxBufFileStruct
            setFVF(reader.readUnsignedIntAsLong(), platform);
            int fvfStride = reader.readInt();
            if (this.fvfStride != fvfStride)
                throw new RuntimeException("The calculated fvfStride did not match the fvfStride provided in the vtxBuf! (FVF: " + this.fvf + ", Read: " + fvfStride + ", Calculated: " + this.fvfStride + ")");

            this.primitiveType = kcPrimitiveType.values()[reader.readInt()];
            int primCount = reader.readInt();
            int byteLength = reader.readInt();
            for (int j = 0; j < RESERVED_PRIM_HEADER_FIELDS; j++) {
                int zero = reader.readInt();
                if (zero != 0)
                    throw new RuntimeException("Expected zero in reserved prim header field at " + reader.getIndex() + ", but got " + zero + " instead.");
            }

            int numOfVertices = kcModel.calculateVertexCount(primCount, this.primitiveType);
            if (byteLength != (fvfStride * numOfVertices))
                throw new RuntimeException("The byte length (" + byteLength + ") for the prim didn't match the calculated fvfStride * numOfVertices (" + (fvfStride * numOfVertices) + ", " + this.primitiveType + ").");
            if (byteLength != (otaPrimHeaderSize - (reader.getIndex() - startReadIndex)))
                throw new RuntimeException("The byte length (" + byteLength + ") for the prim didn't match the byte size shown in the header (" + (otaPrimHeaderSize - (reader.getIndex() - startReadIndex)) + ").");

            // Read vertices.
            this.vertices.clear();
            if (this.primitiveType != kcPrimitiveType.TRIANGLE_LIST) // This never occurs in any known version of the game.
                throw new RuntimeException("Cannot read mesh with " + this.primitiveType + " yet.");

            for (int i = 0; i < numOfVertices; i++) {
                kcVertex vertex = new kcVertex();
                vertex.load(reader, this.components, this.fvf, false);
                this.vertices.add(vertex);
            }
        }

        /**
         * Save the data to the writer.
         * @param writer The writer to write data to.
         */
        public void save(DataWriter writer) {
            // _OTAPrimHeader from kcCVtxBufList.h
            int otaPrimHeaderSizeAddress = writer.writeNullPointer(); // otaPrimHeaderSize
            writer.writeInt(this.materialId);
            writer.writeFloat(this.normalTolerance);
            writer.writeInt(0); // Verified to be zero. (Reserved)
            this.normalAverage.save(writer);
            this.boundingBox.save(writer);

            // _kcVtxBufFileStruct
            writer.writeUnsignedInt(this.fvf);
            writer.writeInt(this.fvfStride);
            writer.writeInt(this.primitiveType.ordinal());
            writer.writeInt(kcModel.calculatePrimCount(this.vertices.size(), this.primitiveType)); // primitiveCount
            int vtxByteLengthAddress = writer.writeNullPointer();
            for (int j = 0; j < RESERVED_PRIM_HEADER_FIELDS; j++)
                writer.writeInt(0); // These are known to be empty.

            // Write vertices.
            for (int i = 0; i < this.vertices.size(); i++) {
                int vertexWriteStartIndex = writer.getIndex();
                this.vertices.get(i).save(writer, this.components, this.fvf, false);
                int vertexBytesWritten = writer.getIndex() - vertexWriteStartIndex;
                if (vertexBytesWritten != this.fvfStride)
                    throw new RuntimeException("The fvfStride expected us to write " + this.fvfStride + " per vertex, but we actually wrote " + vertexBytesWritten + ".");
            }

            // Write lengths.
            writer.writeIntAtPos(otaPrimHeaderSizeAddress, (writer.getIndex() - otaPrimHeaderSizeAddress));
            writer.writeIntAtPos(vtxByteLengthAddress, (writer.getIndex() - vtxByteLengthAddress - ((RESERVED_PRIM_HEADER_FIELDS + 1) * Constants.INTEGER_SIZE)));
        }

        @Override
        public void writeMultiLineInfo(StringBuilder builder, String padding) {
            builder.append(padding).append("Material ID: ").append(this.materialId).append(Constants.NEWLINE);
            builder.append(padding).append("Normal Tolerance: ").append(this.normalTolerance).append(Constants.NEWLINE);
            this.normalAverage.writePrefixedInfoLine(builder, "Normal Average", padding);
            String newPadding = padding + " ";
            this.boundingBox.writePrefixedMultiLineInfo(builder, "Bounding Box", padding, newPadding);
            builder.append(padding).append("FVF: ").append(NumberUtils.toHexString(this.fvf)).append(Constants.NEWLINE);
            builder.append(padding).append("Components: ").append(Arrays.toString(this.components)).append(Constants.NEWLINE);
            builder.append(padding).append("FVF Stride: ").append(this.fvfStride).append(Constants.NEWLINE);
            builder.append(padding).append("Primitive Type: ").append(this.primitiveType).append(Constants.NEWLINE);
            builder.append(padding).append("Vertices (").append(this.vertices.size()).append("):").append(Constants.NEWLINE);
            for (int i = 0; i < this.vertices.size(); i++)
                this.vertices.get(i).writePrefixedInfoLine(builder, "", newPadding);
        }
    }
}