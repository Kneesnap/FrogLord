package net.highwayfrogs.editor.games.tgq.model;

import lombok.Cleanup;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.io.*;
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

    public static final int FVF_FLAG_NORMALS_HAVE_W = Constants.BIT_FLAG_4; // 0x10
    public static final int FVF_FLAG_POSITIONS_HAVE_SIZE = Constants.BIT_FLAG_5; // 0x20
    public static final int FVF_FLAG_DIFFUSE_RGBA255 = Constants.BIT_FLAG_6; // 0x40
    public static final int FVF_FLAG_HAS_MATRIX = Constants.BIT_FLAG_12; // 0x1000, Also disables blend?
    public static final int FVF_FLAG_PS2_COMPRESSED = Constants.BIT_FLAG_14; // 0x4000
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
        @Cleanup PrintWriter objWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File(outputFolder, fileName + ".obj"))));
        @Cleanup PrintWriter mtlWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File(outputFolder, fileName + ".mtl"))));

        objWriter.write("# Exported by FrogLord " + Constants.VERSION + Constants.NEWLINE);
        if (mtlWriter != null)
            objWriter.write("mtllib " + fileName + ".mtl" + Constants.NEWLINE);

        objWriter.write(Constants.NEWLINE);

        kcModelObjWriter.writeMeshesToObj(objWriter, mtlWriter, this);
        objWriter.close();

        for (int i = 0; i < getMaterials().size(); i++) {
            kcMaterial material = getMaterials().get(i);
            if (material.getTexture() == null)
                continue;

            File folder = new File(outputFolder, "Textures/");
            Utils.makeDirectory(folder);

            String outputImageFileName = Utils.stripExtension(fileName) + "_" + Utils.stripExtension(material.getTextureFileName()) + ".png";
            material.getTexture().saveImageToFile(new File(folder, outputImageFileName));
            mtlWriter.write("newmtl " + material.getMaterialName() + Constants.NEWLINE);
            mtlWriter.write("Kd 1 1 1" + Constants.NEWLINE);
            mtlWriter.write("map_Kd " + "Textures/" + outputImageFileName + Constants.NEWLINE);
            mtlWriter.write(Constants.NEWLINE);

            mtlWriter.close();
        }

        // Write a raw binary version of the model too.
        /*File rawFile = new File(outputFolder, fileName + ".dat");
        DataWriter rawWriter = new DataWriter(new FileReceiver(rawFile));
        save(rawWriter);
        rawWriter.closeReceiver();*/
    }

    /**
     * Calculate the components and order from an fvf value.
     * This is a recreation of the function 'kcFVFVertexGetOrder' as found in the PS2 PAL version.
     * @param fvf The fvf value to calculate from.
     * @return orderedComponentList
     */
    public static kcVertexFormatComponent[] calculateOrder(long fvf) {
        List<kcVertexFormatComponent> components = new ArrayList<>(8);

        if ((fvf & FVF_FLAG_POSITIONS_HAVE_SIZE) == FVF_FLAG_POSITIONS_HAVE_SIZE) {
            components.add(kcVertexFormatComponent.NORMAL_XYZF);
            components.add(kcVertexFormatComponent.PSIZE);
        } else {
            components.add(kcVertexFormatComponent.NORMAL_XYZWF);
        }

        if ((fvf & FVF_FLAG_NORMALS_HAVE_W) == FVF_FLAG_NORMALS_HAVE_W)
            components.add(kcVertexFormatComponent.NORMAL_XYZWF);

        if ((fvf & FVF_FLAG_DIFFUSE_RGBA255) == FVF_FLAG_DIFFUSE_RGBA255)
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

        if ((fvf & FVF_FLAG_HAS_MATRIX) == FVF_FLAG_HAS_MATRIX)
            components.add(kcVertexFormatComponent.MATRIX_INDICES);

        return components.toArray(new kcVertexFormatComponent[0]);
    }

    /**
     * Calculates the stride of a vertex with the given FVF value.
     * Functionality matches 'kcFVFVertexGetSizePS2' from the PS2 PAL version.
     * @param fvf The fvf value to calculate the stride from.
     * @return calculatedStride
     */
    public static int calculateStride(long fvf) {
        boolean isCompressedPS2 = (fvf & FVF_FLAG_PS2_COMPRESSED) == FVF_FLAG_PS2_COMPRESSED;
        return calculateStride(calculateOrder(fvf), isCompressedPS2);
    }

    /**
     * Calculates the stride of a vertex with the given FVF value.
     * Functionality matches 'kcFVFVertexGetSizePS2' from the PS2 PAL version.
     * @param components The components to calculate the stride from.
     * @param fvf        The fvf value to calculate the stride from.
     * @return calculatedStride
     */
    public static int calculateStride(kcVertexFormatComponent[] components, long fvf) {
        return calculateStride(components, (fvf & FVF_FLAG_PS2_COMPRESSED) == FVF_FLAG_PS2_COMPRESSED);
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
            stride += isCompressedPS2 ? components[i].getPs2CompressedStride() : components[i].getStride();

        return stride;
    }
}
