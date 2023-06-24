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

    // TODO: Do we allow calculating this from the FVF?
    // - _kcBlendMode blendMode;
    // - int bIndexBlend;
    // FVF:
    // Bits (1, 2, 3)|0xE = blend settings, weird combinations seem to work except 0xC which is 'unsupported'
    // Bit 12|0x1000 = Disable Blend
    // Bit 14|0x4000 = Compression Enabled (kcVtxBufGetVertex)
    // Max transforms always appears to be 6?

    @Override
    public void load(DataReader reader) {
        // 1. Read Header.
        this.fvf = reader.readUnsignedIntAsLong(); // TODO: Let's figure out what every bit is.
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
        for (int i = 0; i < this.primitives.size(); i++)
            vertexCount -= this.primitives.get(i).loadVertices(reader);

        if (reader.hasMore() || vertexCount != 0)
            System.out.println("3D Model had unread data! (Remaining: " + reader.getRemaining() + ", Position: " + Integer.toHexString(reader.getIndex()) + ", Remaining Vertices: " + vertexCount + ")");
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
        writer.writeUnsignedInt(this.primitives.stream().mapToLong(kcModelPrim::getVertexCount).sum());

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
}
