package net.highwayfrogs.editor.games.greatquest.model;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.greatquest.kcPlatform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a 3D model in the kcGameSystem.
 * Animations are stored separately.
 * Created by Kneesnap on 6/22/2023.
 */
@Getter
public class kcModel extends GameObject {
    private final List<kcMaterial> materials = new ArrayList<>();
    private final List<kcModelNode> nodes = new ArrayList<>();
    private final List<kcModelPrim> primitives = new ArrayList<>();
    private long fvf;
    private kcVertexFormatComponent[] components;
    private int bonesPerPrimitive = 1;

    // FVF:
    // - _kcBlendMode blendMode;
    // - int bIndexBlend;
    // Bits (1, 2, 3)|0xE = blend settings, weird combinations seem to work except 0xC which is 'unsupported'
    // Bit 0|0x1 = ? (kcGraphicsSetVertexShader)
    // Bit 12|0x1000 = Disable Blend

    // PS2 FVF Flags:
    public static final int FVF_FLAG_PS2_NORMALS_HAVE_W = Constants.BIT_FLAG_4; // 0x10
    public static final int FVF_FLAG_PS2_POSITIONS_HAVE_SIZE = Constants.BIT_FLAG_5; // 0x20
    public static final int FVF_FLAG_PS2_DIFFUSE_RGBA255 = Constants.BIT_FLAG_6; // 0x40
    public static final int FVF_FLAG_PS2_HAS_MATRIX = Constants.BIT_FLAG_12; // 0x1000, Also disables blend?

    // PC FVF Flags:
    public static final int FVF_FLAG_PC_NORMALS = Constants.BIT_FLAG_4; // 0x10
    public static final int FVF_FLAG_PC_PSIZE = Constants.BIT_FLAG_5; // 0x20
    public static final int FVF_FLAG_PC_DIFFUSE_RGBAI = Constants.BIT_FLAG_6; // 0x40
    public static final int FVF_FLAG_PC_SPECULAR_RGBAI = Constants.BIT_FLAG_7; // 0x80

    // Platform Independent:
    public static final int FVF_FLAG_COMPRESSED = Constants.BIT_FLAG_14; // 0x4000
    public static final int FVF_MASK_WEIGHTS = 0b1110;
    public static final int FVF_MASK_TEXTURE_START = 8;
    public static final int FVF_MASK_TEXTURE = 0b1111 << FVF_MASK_TEXTURE_START;

    /**
     * Get the number of vertices in the model.
     */
    public long getVertexCount() {
        return this.primitives.stream().mapToLong(kcModelPrim::getVertexCount).sum();
    }

    @Override
    public void load(DataReader reader) {
        // 1. Read Header.
        this.fvf = reader.readUnsignedIntAsLong();
        int componentCount = reader.readInt();
        int materialCount = reader.readInt();
        int nodeCount = reader.readInt();
        int primitiveCount = reader.readInt();
        this.bonesPerPrimitive = reader.readInt();
        int vertexCount = reader.readInt();

        // 2. Read vertex format components.
        this.components = new kcVertexFormatComponent[componentCount - 1];
        for (int i = 0; i < this.components.length; i++)
            this.components[i] = kcVertexFormatComponent.values()[reader.readInt()];

        int lastComponentId = reader.readInt();
        if (lastComponentId != kcVertexFormatComponent.NULL.ordinal())
            throw new RuntimeException("The last component ID was supposed to be null, but was " + lastComponentId + ".");

        // 3. Read Materials.
        this.materials.clear();
        for (int i = 0; i < materialCount; i++) {
            kcMaterial newMaterial = new kcMaterial();
            newMaterial.load(reader);
            newMaterial.applyModelMaterialInfo(); // Some of the data should be destroyed / overwritten.
            this.materials.add(newMaterial);
        }

        // 4. Read nodes.
        this.nodes.clear();
        for (int i = 0; i < nodeCount; i++) {
            kcModelNode node = new kcModelNode();
            node.load(reader);
            this.nodes.add(node);
        }

        // 5. Read primitives.
        this.primitives.clear();
        for (int i = 0; i < primitiveCount; i++) {
            kcModelPrim prim = new kcModelPrim(this);
            prim.load(reader);
            this.primitives.add(prim);
        }

        // 6. Read Bone Bytes.
        for (kcModelPrim prim : this.primitives) {
            short[] boneIds = new short[this.bonesPerPrimitive];
            for (int i = 0; i < boneIds.length; i++)
                boneIds[i] = reader.readUnsignedByteAsShort();
            prim.setBoneIds(boneIds);
        }

        // 7. Align4.
        if (reader.getIndex() % 4 != 0)
            reader.skipBytes(4 - (reader.getIndex() % 4)); // Pad.

        // 8. Read vertex buffers.
        int configuredStride = calculateStride(this.components, this.fvf);
        int realStrideRemainder = (reader.getRemaining() % vertexCount);
        int realStride = (reader.getRemaining() / vertexCount);

        if (realStrideRemainder != 0 || realStride != configuredStride) {
            System.out.println("3D model had " + vertexCount + " vertices with a stride calculated to be " + configuredStride + ", but the actual stride of remaining data was " + realStride + ". (Remainder: " + realStrideRemainder + ")");

            if (this.components[this.components.length - 1] == kcVertexFormatComponent.WEIGHT1F && realStride == 24 && configuredStride == 26) {
                // An example of an afflicted file, file 3594 on the PS2 PAL release.
                // The current hypothesis for these files is that the game itself wouldn't even load them properly.
                // TODO: This fix doesn't seem to give proper UV values. Investigate that once we fix PS2 texture exporting.

                kcVertexFormatComponent[] newComponents = new kcVertexFormatComponent[this.components.length - 1];
                System.arraycopy(this.components, 0, newComponents, 0, newComponents.length);
                this.components = newComponents;
                System.out.println(" - However, a hack was applied to fix the model.");
            } else {
                reader.skipBytes(reader.getRemaining());
                return;
            }
        }

        int loadedVertices = 0;
        for (int i = 0; i < this.primitives.size(); i++)
            loadedVertices += this.primitives.get(i).loadVertices(reader);

        if (reader.hasMore() || vertexCount != loadedVertices)
            System.out.println("3D Model had unread data! (Remaining: " + reader.getRemaining() + ", Position: " + Integer.toHexString(reader.getIndex()) + ", Vertices: " + loadedVertices + "/" + vertexCount + ")");
    }

    @Override
    public void save(DataWriter writer) {
        // 1. Write header.
        writer.writeUnsignedInt(this.fvf);
        writer.writeUnsignedInt(this.components != null ? this.components.length + 1 : 1); // Add 1 for the terminator.
        writer.writeUnsignedInt(this.materials.size());
        writer.writeUnsignedInt(this.nodes.size());
        writer.writeUnsignedInt(this.primitives.size());
        writer.writeUnsignedInt(this.bonesPerPrimitive);
        writer.writeUnsignedInt(getVertexCount());

        // 2. Write vertex format components.
        if (this.components != null)
            for (int i = 0; i < this.components.length; i++)
                writer.writeInt(this.components[i].ordinal());
        writer.writeInt(kcVertexFormatComponent.NULL.ordinal());

        // 3. Write materials.
        for (int i = 0; i < this.materials.size(); i++)
            this.materials.get(i).save(writer);

        // 4. Write nodes.
        for (int i = 0; i < this.nodes.size(); i++)
            this.nodes.get(i).save(writer);

        // 5. Write primitives.
        for (int i = 0; i < this.primitives.size(); i++)
            this.primitives.get(i).save(writer);

        // 6. Write Bone IDs.
        for (int i = 0; i < this.primitives.size(); i++) {
            kcModelPrim prim = this.primitives.get(i);
            int boneIdCount = (prim.getBoneIds() != null ? prim.getBoneIds().length : 0);
            if (prim.getBoneIds() == null || boneIdCount != this.bonesPerPrimitive)
                throw new RuntimeException("kcModel is configured to have " + this.bonesPerPrimitive + " bones per primitive, but the primitive at index " + i + " has " + boneIdCount + ".");

            for (int j = 0; j < prim.getBoneIds().length; j++)
                writer.writeUnsignedByte(prim.getBoneIds()[j]);
        }

        // 7. Align4.
        if (writer.getIndex() % 4 != 0)
            writer.writeNull(4 - (writer.getIndex() % 4)); // Pad.

        // 8. Write vertex buffers.
        for (int i = 0; i < this.primitives.size(); i++)
            this.primitives.get(i).saveVertices(writer);
    }

    /**
     * Export this model to a .obj file.
     * @param outputFolder The output folder.
     */
    public void saveToFile(File outputFolder, String fileName) throws IOException {
        kcModelObjWriter.writeMeshesToObj(outputFolder, fileName, this);
    }

    /**
     * Calculate the components and order from the fvf value.
     * This is a recreation of the function 'kcFVFVertexGetOrder'.
     * @param fvf      The fvf value to calculate from.
     * @param platform The platform to calculate the order for.
     * @return orderedComponentList
     */
    public static kcVertexFormatComponent[] calculateOrder(long fvf, kcPlatform platform) {
        switch (platform) {
            case PC:
                return calculateOrderPC(fvf);
            case PS2:
                return calculateOrderPS2(fvf);
            default:
                throw new RuntimeException("Cannot calculate vertex component FVF order for the platform: " + platform);
        }
    }

    /**
     * Calculate the components and order from an fvf value.
     * This is a recreation of the function 'kcFVFVertexGetOrder' as found in the PS2 PAL version.
     * @param fvf The fvf value to calculate from.
     * @return orderedComponentList
     */
    public static kcVertexFormatComponent[] calculateOrderPS2(long fvf) {
        List<kcVertexFormatComponent> components = new ArrayList<>(8);

        if ((fvf & FVF_FLAG_PS2_POSITIONS_HAVE_SIZE) == FVF_FLAG_PS2_POSITIONS_HAVE_SIZE) {
            components.add(kcVertexFormatComponent.POSITION_XYZF);
            components.add(kcVertexFormatComponent.PSIZE);
        } else {
            components.add(kcVertexFormatComponent.POSITION_XYZWF);
        }

        if ((fvf & FVF_FLAG_PS2_NORMALS_HAVE_W) == FVF_FLAG_PS2_NORMALS_HAVE_W)
            components.add(kcVertexFormatComponent.NORMAL_XYZWF);

        if ((fvf & FVF_FLAG_PS2_DIFFUSE_RGBA255) == FVF_FLAG_PS2_DIFFUSE_RGBA255)
            components.add(kcVertexFormatComponent.DIFFUSE_RGBA255F);

        long textureBits = (fvf & FVF_MASK_TEXTURE) >> FVF_MASK_TEXTURE_START;
        if (textureBits == 1) {
            components.add(kcVertexFormatComponent.TEX1_STQP);
        } else if (textureBits == 2) {
            components.add(kcVertexFormatComponent.TEX2F);
        }

        long weightBits = (fvf & FVF_MASK_WEIGHTS) >> 1;
        if (weightBits == 0b110 || weightBits == 0b101 || weightBits == 0b100 || weightBits == 0b011)
            components.add(kcVertexFormatComponent.WEIGHT4F);

        if ((fvf & FVF_FLAG_PS2_HAS_MATRIX) == FVF_FLAG_PS2_HAS_MATRIX)
            components.add(kcVertexFormatComponent.MATRIX_INDICES);

        return components.toArray(new kcVertexFormatComponent[0]);
    }

    /**
     * Calculate the components and order from the fvf value.
     * This is a recreation of the function 'kcFVFVertexGetOrder' as found in the PC version.
     * @param fvf The fvf value to calculate from.
     * @return orderedComponentList
     */
    public static kcVertexFormatComponent[] calculateOrderPC(long fvf) {
        List<kcVertexFormatComponent> components = new ArrayList<>(8);

        int weightBits = (int) ((fvf & FVF_MASK_WEIGHTS) >>> 1);
        if (weightBits == 2) {
            components.add(kcVertexFormatComponent.POSITION_XYZWF);
        } else if (weightBits != 0) {
            components.add(kcVertexFormatComponent.POSITION_XYZF);
            switch (weightBits - 2) {
                case 1:
                    components.add(kcVertexFormatComponent.WEIGHT1F);
                    break;
                case 2:
                    components.add(kcVertexFormatComponent.WEIGHT2F);
                    break;
                case 3:
                    components.add(kcVertexFormatComponent.WEIGHT3F);
                    break;
                case 4:
                    components.add(kcVertexFormatComponent.WEIGHT4F);
                    break;
            }
        }

        if ((fvf & FVF_FLAG_PC_NORMALS) == FVF_FLAG_PC_NORMALS)
            components.add(kcVertexFormatComponent.NORMAL_XYZF);
        if ((fvf & FVF_FLAG_PC_PSIZE) == FVF_FLAG_PC_PSIZE)
            components.add(kcVertexFormatComponent.PSIZE);
        if ((fvf & FVF_FLAG_PC_DIFFUSE_RGBAI) == FVF_FLAG_PC_DIFFUSE_RGBAI)
            components.add(kcVertexFormatComponent.DIFFUSE_RGBAI);
        if ((fvf & FVF_FLAG_PC_SPECULAR_RGBAI) == FVF_FLAG_PC_SPECULAR_RGBAI)
            components.add(kcVertexFormatComponent.SPECULAR_RGBAI);

        long textureBits = (fvf & FVF_MASK_TEXTURE) >> FVF_MASK_TEXTURE_START;
        if (textureBits == 2) {
            components.add(kcVertexFormatComponent.TEX2F);
        } else if (textureBits == 1) {
            components.add(kcVertexFormatComponent.TEX1F);
        }

        return components.toArray(new kcVertexFormatComponent[0]);
    }

    /**
     * Calculates the stride of a vertex with the given FVF value.
     * @param fvf      The fvf value to calculate the stride from.
     * @param platform The platform to calculate the stride for.
     * @return calculatedStride
     */
    public static int calculateStride(long fvf, kcPlatform platform) {
        kcVertexFormatComponent[] components = calculateOrder(fvf, platform);
        return calculateStride(components, fvf);
    }

    /**
     * Calculates the stride of a vertex with the given FVF value.
     * Functionality matches 'kcFVFVertexGetSizePS2' from the PS2 PAL version.
     * @param components The components to calculate the stride from.
     * @param fvf        The fvf value to calculate the stride from.
     * @return calculatedStride
     */
    public static int calculateStride(kcVertexFormatComponent[] components, long fvf) {
        return calculateStride(components, (fvf & FVF_FLAG_COMPRESSED) == FVF_FLAG_COMPRESSED);
    }

    /**
     * Calculates the stride of a vertex with the given FVF value.
     * Functionality matches 'kcFVFVertexGetSizePS2' from the PS2 PAL version.
     * @param components      The components to calculate the stride from.
     * @param isCompressedPS2 If the values are "compressed PS2" values.
     * @return calculatedStride
     */
    public static int calculateStride(kcVertexFormatComponent[] components, boolean isCompressedPS2) {
        int stride = 0;
        for (int i = 0; i < components.length; i++)
            stride += isCompressedPS2 ? components[i].getCompressedStride() : components[i].getStride();

        return stride;
    }

    /**
     * Calculate the number of primitives of the given type formed from a provided number of vertices.
     * @param numberOfVertices The number of vertices.
     * @param primitiveType    The primitive type.
     * @return primitive count
     */
    public static int calculatePrimCount(int numberOfVertices, kcPrimitiveType primitiveType) {
        switch (primitiveType) {
            case POINT_LIST:
                return numberOfVertices;
            case LINE_LIST:
                return numberOfVertices / 2;
            case LINE_STRIP:
                return numberOfVertices - 1;
            case TRIANGLE_LIST:
                return numberOfVertices / 3;
            case TRIANGLE_STRIP:
            case TRIANGLE_FAN:
                return numberOfVertices - 2;
            default:
                throw new RuntimeException("Cannot calculate the prim count for kcPrimitiveType: " + primitiveType);
        }
    }

    /**
     * Calculates the number of vertices required to form a number of prims of the given type.
     * @param primCount     The number of prims to require.
     * @param primitiveType The primitive type.
     * @return Number of vertices required
     */
    public static int calculateVertexCount(int primCount, kcPrimitiveType primitiveType) {
        switch (primitiveType) {
            case POINT_LIST:
                return primCount;
            case LINE_LIST:
                return primCount * 2;
            case LINE_STRIP:
                return primCount + 1;
            case TRIANGLE_LIST:
                return primCount * 3;
            case TRIANGLE_STRIP:
            case TRIANGLE_FAN:
                return primCount + 2;
            default:
                throw new RuntimeException("Cannot calculate the vertex count for kcPrimitiveType: " + primitiveType);
        }
    }
}