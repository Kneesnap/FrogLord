package net.highwayfrogs.editor.games.konami.greatquest.chunks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GamePlatform;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTriMesh.kcCTriMesh;
import net.highwayfrogs.editor.games.konami.greatquest.loading.kcLoadContext;
import net.highwayfrogs.editor.games.konami.greatquest.map.octree.kcOctTree;
import net.highwayfrogs.editor.games.konami.greatquest.map.octree.kcOctTreeType;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcBox4;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;
import net.highwayfrogs.editor.games.konami.greatquest.model.*;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Handles the OTT chunk.
 * Helpful:
 * - <a href="https://github.com/Kneesnap/frogger-tgq-decomp/blob/main/export/projects/Frogger-PAL/GreatQuest/kcGameSystem/Src/kcCVtxBufList.h">kcCVtxBufList.h</a>
 * - <a href="https://github.com/Kneesnap/frogger-tgq-decomp/blob/main/export/projects/Frogger-PAL/kcTechGroup/KcLib/Src/kcMath/kcMath3D.h">kcMath3D.h</a>
 * Created by Kneesnap on 8/25/2019.
 */
public class kcCResOctTreeSceneMgr extends kcCResource implements IMultiLineInfoWriter {
    private final List<kcVtxBufFileStruct> vertexBuffers = new ArrayList<>(); // Automatically added to the kcCOctTree in-game on load via kcCOctTreeAtom::SetBoundingBox, called from kcCResOctTreeSceneMgr::Load.
    private final List<kcVtxBufFileStruct> immutableVertexBuffers = Collections.unmodifiableList(this.vertexBuffers);
    private final List<kcMaterial> materials = new ArrayList<>();
    private final List<kcMaterial> immutableMaterials = Collections.unmodifiableList(this.materials);
    private final List<kcCTriMesh> collisionMeshes = new ArrayList<>(); // Automatically added to the kcCOctTree in-game on load via kcCOctTreeAtom::SetBoundingBox, called from kcCResOctTreeSceneMgr::Load.
    private final List<kcCTriMesh> immutableCollisionMeshes = Collections.unmodifiableList(this.collisionMeshes);
    @Getter private final kcOctTree entityTree;
    @Getter private final kcOctTree visualTree;
    private final transient Map<kcMaterial, List<kcVtxBufFileStruct>> buffersPerMaterial = new HashMap<>(); // TODO: Update when the underlying materials/vertexBuffers lists change.

    private static final int SUPPORTED_VERSION = 1;
    private static final int RESERVED_HEADER_FIELDS = 7;
    private static final int RESERVED_PRIM_HEADER_FIELDS = 3;
    public static final String RESOURCE_NAME = "OctTreeSceneMgr";

    public kcCResOctTreeSceneMgr(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.OCTTREESCENEMGR);
        // All the levels seem to use these for their oct trees.
        this.visualTree = new kcOctTree(getLogger(), getGameInstance(), kcOctTreeType.VISUAL, 14, 10);
        this.entityTree = new kcOctTree(getLogger(), getGameInstance(), kcOctTreeType.COLLISION, 14, 14);
    }

    /**
     * Gets an immutable list containing vertex buffers.
     */
    public List<kcVtxBufFileStruct> getVertexBuffers() {
        return this.immutableVertexBuffers;
    }

    /**
     * Gets an immutable list containing materials.
     */
    public List<kcMaterial> getMaterials() {
        return this.immutableMaterials;
    }

    /**
     * Gets an immutable list containing collision meshes.
     */
    public List<kcCTriMesh> getCollisionMeshes() {
        return this.immutableCollisionMeshes;
    }

    /**
     * Gets vertex buffers used for the material
     * @param material the material provided
     * @return vertexBufferList
     */
    public List<kcVtxBufFileStruct> getVertexBuffersForMaterial(kcMaterial material) {
        return this.buffersPerMaterial.get(material);
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
            getLogger().warning("OctTree file in '%s' said it had %d remaining byte(s), but it actually had %d.", getParentFile().getDebugName(), remainingDataInBytes, reader.getRemaining());

        int version = reader.readInt();
        if (version != SUPPORTED_VERSION)
            getLogger().severe("OctTree file in '%s' identified as version %d, but we only understand version %d.", getParentFile().getDebugName(), version, SUPPORTED_VERSION);

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
        GamePlatform platform = getLoadPlatform();
        for (int i = 0; i < primCount; i++) {
            kcVtxBufFileStruct vtxBuf = new kcVtxBufFileStruct(this);
            this.vertexBuffers.add(vtxBuf);
            vtxBuf.load(reader, platform);
        }

        // Read materials.
        this.materials.clear();
        for (int i = 0; i < materialCount; i++) {
            kcMaterial newMaterial = new kcMaterial(getGameInstance());
            newMaterial.load(reader);
            this.materials.add(newMaterial);
        }

        // Create cache.
        updateMaterialVertexBufferListCache();

        // Read collision meshes.
        this.collisionMeshes.clear();
        for (int i = 0; i < meshCount; i++) {
            kcCTriMesh newMesh = new kcCTriMesh(getGameInstance());
            newMesh.load(reader);
            this.collisionMeshes.add(newMesh);
        }

        // There is code in the final game which can acknowledge these, although it doesn't do anything with this data.
        // This feature is also not actually used in any of the retail maps.
        // I think this feature was supposed to be for loading meshes (collision & visual) from other parts of the game.
        // However, I'm not really sure why we'd ever want this, since collision meshes aren't generally going to be shared.
        // So, we'll leave this unimplemented unless we see it actually used.
        if (sourcePathMeshCount != 0 || sourcePathVisualCount != 0)
            getLogger().severe("The sourcePath counts were not zero!! This feature was not added since it was not seen in any versions at the time of writing! (Source Path Mesh Count: %d, Source Path Visual Count: %d)", sourcePathMeshCount, sourcePathVisualCount);
    }

    private GamePlatform getLoadPlatform() {
        GamePlatform platform = getGameInstance().getPlatform();

        // River Town is in PC format in the prototype build even though it shouldn't be.
        if (platform == GamePlatform.PLAYSTATION_2 && getGameInstance().isPrototype() && "\\GameData\\Level04RiverTown\\Level\\PS2_Level04.dat".equalsIgnoreCase(getParentFile().getFilePath()))
            platform = GamePlatform.WINDOWS;

        return platform;
    }

    private void updateMaterialVertexBufferListCache() {
        this.buffersPerMaterial.clear();
        for (int i = 0; i < this.vertexBuffers.size(); i++) {
            kcVtxBufFileStruct vtxBuf = this.vertexBuffers.get(i);
            int materialId = vtxBuf.getMaterialId();
            kcMaterial material = materialId >= 0 && this.materials.size() > materialId ? this.materials.get(materialId) : null;
            this.buffersPerMaterial.computeIfAbsent(material, key -> new ArrayList<>()).add(vtxBuf);
        }
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
        private final kcCResOctTreeSceneMgr sceneManager;
        // _OTAPrimHeader (Applied to kcCOTAPrim in kcCOTAPrim::Init)
        // -1 is not a valid material ID, and will display warnings if this value is not changed before a save occurs.
        @Setter private int materialId = -1; // kcCOTAPrim::__ct()
        // The normal tolerance and normal heading are SUPPOSED to be used for culling purposes.
        // kcCOTAPrim::Render will ensure that dotProduct(this.normalAverage, globalRenderHeading) <= normalTolerance.
        // It's not very clear how exactly this would be a valid way to cull display lists, since it does not seem to have any way of accounting for camera position, just rotation.
        // Since the game does not have backface culling, it seems unclear how this could possibly cull anything (in the context of the model already passing the frustum culling check).
        // That's probably why rendering will still happen if normalTolerance >= 1.0, and that has been observed to be the case in almost all original game data.
        // The only exception was a single buffer in The Goblin Trail, which appears to be old terrain (located physically under terrain actually displayed), from before the level was textured. It's at the start of the level to the left, beyond some spikes and under 3 crates. View it from below.
        // So while the algorithm to generate these values is not known, leaving them as the default values is acceptable.
        private float normalTolerance = 2F; // kcCOTAPrim::__ct()
        private final kcVector4 normalAverage = new kcVector4(0, 0, 1, 0); // kcCOTAPrim::__ct()
        // This is the smallest box which contains all vertices. Generates the box bounding sphere from this, as well as oct tree collision.
        // Note that because the PS2 data has lower-precision for its floating point values, the values loaded from the PS2 version are slightly more accurate than the actual vertices.)
        // In other words, there's some precision loss if we recalculate this box on the PS2 version. BUT, it does not matter, since the actual vertices change too.
        private final kcBox4 boundingBox = new kcBox4();

        // kcVtxBufFileStruct
        private int fvf;
        private kcVertexFormatComponent[] components;
        private int fvfStride;
        // kcOTARenderCallbackBuffer::AddVtxBuffer on PS2 PAL will skip all non TRIANGLE_LIST types.
        // Therefore, it is only possible to use TRIANGLE_LIST here.
        private kcPrimitiveType primitiveType = kcPrimitiveType.TRIANGLE_LIST;
        private final List<kcVertex> vertices = new ArrayList<>();

        private static final ThreadLocal<kcBox4> TEMPORARY_BOUNDING_BOX = ThreadLocal.withInitial(kcBox4::new);
        private static final int PS2_FVF_VALUE = 0x4152;
        private static final int PC_FVF_VALUE = 0x152;

        public kcVtxBufFileStruct(kcCResOctTreeSceneMgr sceneManager) {
            this.sceneManager = sceneManager;

            GamePlatform platform = sceneManager.getGameInstance().getPlatform();
            switch (platform) {
                case WINDOWS:
                    setFVF(PC_FVF_VALUE, platform);
                    break;
                case PLAYSTATION_2:
                    setFVF(PS2_FVF_VALUE, platform);
                    break;
                default:
                    throw new UnsupportedOperationException("The platform " + platform + " is not supported by kcVtxBufFileStruct.");
            }
        }

        /**
         * Gets the local index of this vertex buffer within the list of vertex buffers.
         */
        public int getLocalIndex() {
            return this.sceneManager.getVertexBuffers().lastIndexOf(this);
        }

        /**
         * Set the FVF value for this vtxBuf.
         * @param newFvf   The new fvf value to include.
         * @param platform The platform to save the FVF on.
         */
        public void setFVF(int newFvf, GamePlatform platform) {
            if (platform == null)
                throw new NullPointerException("platform");

            this.fvf = newFvf;
            this.components = kcFvFUtil.getTerrainComponents(newFvf, platform);
            this.fvfStride = kcVertex.calculateStride(this.components, newFvf);

            int validationFvf = kcFvFUtil.calculateTerrainFvF(this.components, platform);
            if (this.fvf != validationFvf)
                throw new RuntimeException("The newFvf was " + NumberUtils.toHexString(newFvf) + ", which resulted in " + Arrays.toString(this.components) + ", with a non-matching FvF of " + NumberUtils.toHexString(validationFvf) + ".");
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
            if (this.normalTolerance < .95F) // See the explanation above for normalTolerance.
                this.sceneManager.getLogger().warning("kcVtxBufFileStruct[%d] has a normalTolerance of %f, which is thought to cause it to be intermittently invisible!", getLocalIndex(), this.normalTolerance);

            int otaZero = reader.readInt();
            this.normalAverage.load(reader);
            this.boundingBox.load(reader);
            if (otaZero != 0) // Written as zero by kcCOTAPrim::Store()
                throw new RuntimeException("The reserved field in the _OTAPrimHeader was supposed to be zero, but actually was " + otaZero + ".");

            // _kcVtxBufFileStruct
            setFVF(reader.readInt(), platform);
            int fvfStride = reader.readInt();
            if (this.fvfStride != fvfStride)
                throw new RuntimeException("The calculated fvfStride did not match the fvfStride provided in the vtxBuf! (FVF: " + this.fvf + ", Components: " + Arrays.toString(this.components) + ", Read Stride: " + fvfStride + ", Calculated Stride: " + this.fvfStride + ")");

            this.primitiveType = kcPrimitiveType.values()[reader.readInt()];
            int primCount = reader.readInt();
            int byteLength = reader.readInt();
            for (int j = 0; j < RESERVED_PRIM_HEADER_FIELDS; j++) {
                int zero = reader.readInt();
                if (zero != 0)
                    throw new RuntimeException("Expected zero in reserved prim header field at " + reader.getIndex() + ", but got " + zero + " instead.");
            }

            int numOfVertices = this.primitiveType.calculateVertexCount(primCount);
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
                vertex.load(reader, this.components, this.fvf, false, false);
                this.vertices.add(vertex);
            }

            // Validate bounding box seems right.
            float testThreshold = this.sceneManager.getGameInstance().isPS2() ? 0.1F : 0.01F; // PS2 has less accuracy.
            kcBox4 calculatedBoundingBox = calculateBoundingBox(TEMPORARY_BOUNDING_BOX.get());
            if (Math.abs(calculatedBoundingBox.getMin().getX() - this.boundingBox.getMin().getX()) > testThreshold
                    || Math.abs(calculatedBoundingBox.getMax().getX() - this.boundingBox.getMax().getX()) > testThreshold
                    || Math.abs(calculatedBoundingBox.getMin().getY() - this.boundingBox.getMin().getY()) > testThreshold
                    || Math.abs(calculatedBoundingBox.getMax().getY() - this.boundingBox.getMax().getY()) > testThreshold
                    || Math.abs(calculatedBoundingBox.getMin().getZ() - this.boundingBox.getMin().getZ()) > testThreshold
                    || Math.abs(calculatedBoundingBox.getMax().getZ() - this.boundingBox.getMax().getZ()) > testThreshold
                    || Math.abs(calculatedBoundingBox.getMin().getW() - this.boundingBox.getMin().getW()) > testThreshold
                    || Math.abs(calculatedBoundingBox.getMax().getW() - this.boundingBox.getMax().getW()) > testThreshold)
                this.sceneManager.getLogger().warning("Vertex Buffer[%d]%nReal Bounding Box: %s%nCalc Bounding Box: %s", getLocalIndex(), this.boundingBox, calculatedBoundingBox);
        }

        /**
         * Save the data to the writer.
         * @param writer The writer to write data to.
         */
        public void save(DataWriter writer) {
            boolean validMaterialID = true;
            if (this.materialId < 0 || this.materialId >= this.sceneManager.getMaterials().size()) {
                validMaterialID = false;
                this.sceneManager.getGameInstance().showWarning(this.sceneManager.getLogger(), "Invalid Material ID", "kcVtxBufFileStruct[%d] in %s cannot be saved with an invalid material ID of %d!", getLocalIndex(), this.sceneManager.getParentFile().getDebugName(), this.materialId);
            }

            // _OTAPrimHeader from kcCVtxBufList.h
            int otaPrimHeaderSizeAddress = writer.writeNullPointer(); // otaPrimHeaderSize
            writer.writeInt(validMaterialID ? this.materialId : 0);
            writer.writeFloat(this.normalTolerance);
            writer.writeInt(0); // Verified to be zero. (Reserved)
            this.normalAverage.save(writer);
            calculateBoundingBox(this.boundingBox).save(writer);

            // _kcVtxBufFileStruct
            writer.writeInt(this.fvf);
            writer.writeInt(this.fvfStride);
            writer.writeInt(this.primitiveType.ordinal());
            writer.writeInt(this.primitiveType.calculatePrimCount(this.vertices.size())); // primitiveCount
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

        /**
         * Calculates the bounding box for this list, applied to the
         * @param boundingBox the output storage for the bounding box, or null to create one
         * @return boundingBox
         */
        public kcBox4 calculateBoundingBox(kcBox4 boundingBox) {
            if (boundingBox == null)
                boundingBox = new kcBox4();

            // No vertices? Return an empty box.
            if (this.vertices.isEmpty()) {
                boundingBox.getMin().setXYZW(0, 0, 0, 1F);
                boundingBox.getMax().setXYZW(0, 0, 0, 1F);
                return boundingBox;
            }

            boundingBox.getMin().setXYZW(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 1F);
            boundingBox.getMax().setXYZW(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 1F);

            for (int i = 0; i < this.vertices.size(); i++) {
                kcVertex vertex = this.vertices.get(i);
                if (vertex.getX() < boundingBox.getMin().getX())
                    boundingBox.getMin().setX(vertex.getX());
                if (vertex.getX() > boundingBox.getMax().getX())
                    boundingBox.getMax().setX(vertex.getX());
                if (vertex.getY() < boundingBox.getMin().getY())
                    boundingBox.getMin().setY(vertex.getY());
                if (vertex.getY() > boundingBox.getMax().getY())
                    boundingBox.getMax().setY(vertex.getY());
                if (vertex.getZ() < boundingBox.getMin().getZ())
                    boundingBox.getMin().setZ(vertex.getZ());
                if (vertex.getZ() > boundingBox.getMax().getZ())
                    boundingBox.getMax().setZ(vertex.getZ());
            }

            return boundingBox;
        }
    }
}