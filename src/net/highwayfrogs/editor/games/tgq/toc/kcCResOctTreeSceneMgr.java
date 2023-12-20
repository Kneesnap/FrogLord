package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;
import net.highwayfrogs.editor.games.tgq.kcPlatform;
import net.highwayfrogs.editor.games.tgq.loading.kcLoadContext;
import net.highwayfrogs.editor.games.tgq.math.kcBox4;
import net.highwayfrogs.editor.games.tgq.math.kcVector4;
import net.highwayfrogs.editor.games.tgq.model.*;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the OTT chunk.
 * Helpful:
 * - <a href="https://github.com/Kneesnap/frogger-tgq-decomp/blob/main/export/projects/Frogger-PAL/GreatQuest/kcGameSystem/Src/kcCVtxBufList.h">kcCVtxBufList.h</a>
 * - <a href="https://github.com/Kneesnap/frogger-tgq-decomp/blob/main/export/projects/Frogger-PAL/kcTechGroup/KcLib/Src/kcMath/kcMath3D.h">kcMath3D.h</a>
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public class kcCResOctTreeSceneMgr extends kcCResource {
    private final List<kcVtxBufFileStruct> vertexBuffers = new ArrayList<>();
    private final List<kcMaterial> materials = new ArrayList<>();

    public static final int NAME_SIZE = 32;
    private static final int RESERVED_HEADER_FIELDS = 7;
    private static final int RESERVED_PRIM_HEADER_FIELDS = 3;

    public kcCResOctTreeSceneMgr(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.OCTTREESCENEMGR);
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
            System.out.println("OctTree file in '" + getParentFile().getDebugName() + "' said it had " + remainingDataInBytes + " remaining bytes, but it actually had " + reader.getRemaining() + ".");

        int version = reader.readInt();
        if (version != 1)
            System.out.println("OctTree file in '" + getParentFile().getDebugName() + "' identified as version " + version + ", but we only understand version 1.");

        int meshCount = reader.readInt(); // TODO: Used. 0x08
        int treeEntityDataSize = reader.readInt();
        int primCount = reader.readInt();
        int treeVisualDataSize = reader.readInt();
        int materialCount = reader.readInt(); // TODO: Used. 0x18
        int sourcePathMeshCount = reader.readInt(); // TODO: Used 0x1C
        int sourcePathVisualCount = reader.readInt(); // TODO: Used. 0x20
        for (int i = 0; i < RESERVED_HEADER_FIELDS; i++) {
            int zero = reader.readInt();
            if (zero != 0)
                throw new RuntimeException("Expected zero in reserved header field at " + reader.getIndex() + ", but got " + zero + " instead.");
        }

        // End of header.

        // Read oct tree data. TODO (Future): Implement actually reading this.
        reader.skipBytes(treeEntityDataSize);
        // This data is passed directly to kcCOctTree::Init(), to build the "entity tree"?

        // Read visual tree data. TODO (Future): Implement actually reading this.
        reader.skipBytes(treeVisualDataSize);
        // This data is passed directly to kcCOctTree::Init(), to build the "visual tree"?

        // Read primitives.
        this.vertexBuffers.clear();
        kcPlatform platform = getMainArchive().getPlatform();
        for (int i = 0; i < primCount; i++) {
            kcVtxBufFileStruct vtxBuf = new kcVtxBufFileStruct();
            vtxBuf.load(reader, platform);
            this.vertexBuffers.add(vtxBuf);
        }

        // Read materials.
        System.out.println("Reading materials from " + Utils.toHexString(reader.getIndex()) + " in " + getParentFile().getDebugName()); // TODO: TOSS
        for (int i = 0; i < materialCount; i++) {
            kcMaterial newMaterial = new kcMaterial();
            newMaterial.load(reader);
            this.materials.add(newMaterial);
        }

        // TODO: There's more data... Figure out what it is and implement it.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(getRawData()); // TODO: REPLACE
    }

    @Override
    public void afterLoad1(kcLoadContext context) {
        super.afterLoad1(context);

        // Apply texture file names.
        TGQChunkedFile parentFile = getParentFile();
        if (parentFile != null && parentFile.hasFilePath())
            context.getMaterialLoadContext().applyLevelTextureFileNames(parentFile, parentFile.getFilePath(), this.materials);
    }

    @Override
    public void afterLoad2(kcLoadContext context) {
        super.afterLoad2(context);

        // Resolves textures. Waits until after afterLoad1() when file names are resolved.
        context.getMaterialLoadContext().resolveMaterialTexturesInChunk(getParentFile(), this.materials);
    }

    @Getter
    public static class kcVtxBufFileStruct {
        // _OTAPrimHeader
        private long materialId;
        private float normalTolerance;
        private final kcVector4 normalAverage = new kcVector4();
        private final kcBox4 boundingBox = new kcBox4();

        // kcVtxBufFileStruct
        private long fvf;
        private kcVertexFormatComponent[] components;
        private int fvfStride;
        private kcPrimitiveType primitiveType;
        private final List<kcVertex> vertices = new ArrayList<>();

        /**
         * Set the FVF value for this vtxBuf.
         * @param newFvf   The new fvf value to include.
         * @param platform The platform to save the FVF on.
         */
        public void setFVF(long newFvf, kcPlatform platform) {
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
        public void load(DataReader reader, kcPlatform platform) {
            int startReadIndex = reader.getIndex();

            // _OTAPrimHeader from kcCVtxBufList.h
            long otaPrimHeaderSize = reader.readUnsignedIntAsLong();
            this.materialId = reader.readUnsignedIntAsLong();
            this.normalTolerance = reader.readFloat();
            int otaZero = reader.readInt();
            this.normalAverage.load(reader);
            this.boundingBox.load(reader);
            if (otaZero != 0)
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
                vertex.load(reader, this.components, this.fvf);
                this.vertices.add(vertex);
            }
        }

        /**
         * Save the data to the writer.
         * @param writer The writer to write data to.
         */
        public void save(DataWriter writer) {
            // _OTAPrimHeader from kcCVtxBufList.h
            int otaPrimHeaderSizeAddr = writer.writeNullPointer(); // otaPrimHeaderSize
            writer.writeUnsignedInt(this.materialId);
            writer.writeFloat(this.normalTolerance);
            writer.writeInt(0); // Verified to be zero. (Reserved)
            this.normalAverage.save(writer);
            this.boundingBox.save(writer);

            // _kcVtxBufFileStruct
            writer.writeUnsignedInt(this.fvf);
            writer.writeInt(this.fvfStride);
            writer.writeInt(this.primitiveType.ordinal());
            writer.writeInt(kcModel.calculatePrimCount(this.vertices.size(), this.primitiveType)); // primitiveCount
            int vtxByteLengthAddr = writer.writeNullPointer();
            for (int j = 0; j < RESERVED_PRIM_HEADER_FIELDS; j++)
                writer.writeInt(0); // These are known to be empty.

            // Write vertices.
            int vtxDataStart = writer.getIndex();
            for (int i = 0; i < this.vertices.size(); i++)
                this.vertices.get(i).save(writer, this.components, this.fvf);

            // Write lengths.
            int headerByteLength = (writer.getIndex() - otaPrimHeaderSizeAddr);
            int vtxByteLength = (writer.getIndex() - vtxDataStart);
            writer.writeAddressAt(otaPrimHeaderSizeAddr, headerByteLength + vtxByteLength);
            writer.writeAddressAt(vtxByteLengthAddr, vtxByteLength);
        }
    }
}