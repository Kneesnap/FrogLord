package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;
import net.highwayfrogs.editor.games.tgq.TGQFile;
import net.highwayfrogs.editor.games.tgq.TGQImageFile;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.system.Tuple3;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a 3D model.
 * TODO: It seems there are other polygon modes or something, some of the models don't look right.
 * Created by Kneesnap on 3/23/2020.
 */
@Getter
public class TGQChunk3DModel extends kcCResource {
    private String referenceName; // If this is a reference.
    private String fullReferenceName;
    private byte[] toSave;

    // If this is a model.
    private List<TGQMaterial> materials = new ArrayList<>();
    private List<Tuple3<Float, Float, Float>> vertices = new ArrayList<>();
    private List<Tuple3<Float, Float, Float>> normals = new ArrayList<>();
    private List<Tuple2<Float, Float>> uvs = new ArrayList<>();
    private byte[] unknownBytes;
    private List<Integer> unknownIntegers = new ArrayList<>();
    @Setter private transient TGQChunkedFile environmentFile;
    //TODO: Could materials be applied to groups? What about the extra bytes in the material? Maybe it has ranges of faces, or just plain faces?
    //TODO: Color.
    //TODO: Bones, Animations? It seems like these are probably in a different chunk, and on a per-file basis. [Maybe this means the bone definitions are in the animation data.]
    //TODO: Materials are likely applied to whatever bone / grouping system being used.

    public static final int NAME_SIZE = 32;
    public static final int FULL_NAME_SIZE = 260;
    public static final byte FULL_NAME_PADDING = (byte) 0xCD;

    public TGQChunk3DModel(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.MODEL);
    }

    @Override
    public void load(DataReader reader) {
        reader.jumpTemp(reader.getIndex());
        this.toSave = reader.readBytes(reader.getRemaining()); //TODO
        reader.jumpReturn();

        if (!isRootChunk()) { // Reference.
            this.referenceName = reader.readTerminatedStringOfLength(NAME_SIZE);
            this.fullReferenceName = reader.readTerminatedStringOfLength(FULL_NAME_SIZE);
            return;
        }

        // Read Header. (TODO: Make sense of)
        int fvf = reader.readInt();
        int componentCount = reader.readInt(); // Different prim types?
        int materialCount = reader.readInt();
        int nodeCount = reader.readInt(); // A node is probably a bone. TODO
        int primitiveCount = reader.readInt();
        int bonesPerPrim = reader.readInt();
        int vertexCount = reader.readInt();

        int[] componentIds = new int[componentCount];
        for (int i = 0; i < componentIds.length; i++)
            componentIds[i] = reader.readInt();

        // 1. Read Materials.
        for (int i = 0; i < materialCount; i++) {
            TGQMaterial newMaterial = new TGQMaterial();
            newMaterial.load(reader);
            materials.add(newMaterial);
        }


        int count = 0;
        // 2. Unknown integers which resemble faces. TODO: I bet this data has something to do with what it applies to.
        while (reader.hasMore()) { // TODO: Read these as bone definitions? int material, int type, int vertexCount? The prim bytes could be an index into an array of these entries. Maybe?
            int temp = reader.readInt();
            if ((temp & 0xFFFF0000) != 0) {
                reader.setIndex(reader.getIndex() - Constants.INTEGER_SIZE);
                break;
            }
            this.unknownIntegers.add(temp);
        }
        //System.out.println("COUNT = " + count + ", Vertices = " + vertexCount);

        // 3. Read unknown bytes.
        this.unknownBytes = reader.readBytes(primitiveCount);
        if (reader.getIndex() % 4 != 0)
            reader.skipBytes(4 - (reader.getIndex() % 4)); // Pad.

        // kcModelRender
        // 0x11518C - s2 = Ptr to right after the material ends. s3=12 after s2. var2 = The start of the material block. Ie: Data right before the first material.

        //TODO: Get breakpoints to the things which we don't know about.

        // Read Vertices.
        //TODO: Is the first integer the count of faces in a mesh in certain cases?
        for (int i = 0; i < vertexCount; i++) {
            readPrim(reader, null, componentIds.length);
        }

        //if (reader.hasMore())
        //    System.out.println("More " + getParentFile().getMainArchive().getFiles().size() + ": " + reader.getRemaining() + ", " + Integer.toHexString(reader.getIndex()));
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(this.toSave); //TODO: Temporary.
    }

    /**
     * Export this model to a .obj file.
     * @param outputFile The output file.
     */
    public void saveToFile(File outputFile) throws IOException {
        PrintWriter writer = new PrintWriter(outputFile);

        writer.write("# Exported by FrogLord " + Constants.VERSION + Constants.NEWLINE);
        writer.write("mtllib " + Utils.stripExtension(outputFile.getName()) + ".mtl" + Constants.NEWLINE);
        writer.write("usemtl root" + Constants.NEWLINE);
        writer.write(Constants.NEWLINE);

        // Vertices.
        writer.write("# Vertices" + Constants.NEWLINE);
        for (Tuple3<Float, Float, Float> vertex : this.vertices)
            writer.write("v " + vertex.getA() + " " + vertex.getB() + " " + vertex.getC() + Constants.NEWLINE);
        writer.write(Constants.NEWLINE);

        writer.write("# Normals" + Constants.NEWLINE);
        for (Tuple3<Float, Float, Float> normal : this.normals)
            writer.write("vn " + normal.getA() + " " + normal.getB() + " " + normal.getC() + Constants.NEWLINE);
        writer.write(Constants.NEWLINE);

        writer.write("# Texture Coordinates" + Constants.NEWLINE);
        for (Tuple2<Float, Float> uv : this.uvs)
            writer.write("vt " + uv.getA() + " " + uv.getB() + Constants.NEWLINE);
        writer.write(Constants.NEWLINE);

        // Write Faces.
        writer.write("# Faces" + Constants.NEWLINE);
        for (int i = 1; i < this.vertices.size(); i += 3) //TODO: The faces don't always export right, example Gavin, or the dragon. I bet that block of integers above is face connections, we just have to figure out how to use them. (3761).
            writer.write("f " + i + "/" + i + "/" + i + " " + (i + 1) + "/" + (i + 1) + "/" + (i + 1) + " " + (i + 2) + "/" + (i + 2) + "/" + (i + 2) + Constants.NEWLINE);

        /*for (int i = 1; i < (this.vertices.size() - 2); i += 3) { //TODO: It's ++ for all faces, but borked. Fix this for other models later.
            int x = ((i % 3) != 1) ? i : (i + 2);
            int y = ((i % 3) != 1) ? (i + 1) : (i + 1);
            int z = ((i % 3) != 1) ? (i + 2) : i;
            writer.write("f " + x + "/" + x + "/" + x + " " + y + "/" + y + "/" + y + " " + z + "/" + z + "/" + z + Constants.NEWLINE);
        }*/

        writer.close();

        List<TGQChunkTextureReference> chunks = new ArrayList<>();
        if (getEnvironmentFile() != null) {
            for (kcCResource chunk : getEnvironmentFile().getChunks())
                if (chunk instanceof TGQChunkTextureReference)
                    chunks.add((TGQChunkTextureReference) chunk);

            PrintWriter mtlWriter = new PrintWriter(new File(outputFile.getParentFile(), outputFile.getName().split("\\.")[0] + ".mtl"));
            TGQChunkTextureReference goodChunk = null;
            for (int i = 0; i < getMaterials().size(); i++) {
                TGQMaterial material = getMaterials().get(i);
                if (material.getTextureFile().isEmpty())
                    continue;

                File folder = new File(outputFile.getParentFile(), "Textures/");
                if (!folder.exists())
                    folder.mkdirs();

                String stripped = material.getTextureFile().split("\\.")[0].toLowerCase();
                TGQChunkTextureReference foundChunk = null; // TODO: Get this the way the game gets it.
                for (TGQChunkTextureReference chunk : chunks)
                    if (chunk.getPath().toLowerCase().contains(stripped))
                        foundChunk = chunk;

                if (foundChunk != null) {
                    if (goodChunk == null)
                        goodChunk = foundChunk;

                    TGQFile file = getParentFile().getMainArchive().getFileByName(foundChunk.getPath());
                    if (file instanceof TGQImageFile) {
                        ((TGQImageFile) file).saveImageToFile(new File(folder, file.getExportName() + ".png"));
                        mtlWriter.write("newmtl " + (goodChunk == foundChunk ? "root" : foundChunk.getName()) + Constants.NEWLINE);
                        mtlWriter.write("Kd 1 1 1" + Constants.NEWLINE);
                        mtlWriter.write("map_Kd " + "Textures/" + file.getExportName() + ".png" + Constants.NEWLINE);
                        mtlWriter.write(Constants.NEWLINE);
                    }
                }
            }
            mtlWriter.close();
        }

        //TODO: Get Textures from TEX section. Apply everything to texture 0 for now.
    }

    private void readPrim(DataReader reader, TGQPrimType primType, int componentIds) {
        if (componentIds == 5) { // TODO: This is nonsense I'm pretty sure.
            /*this.uvs.add(new Tuple2<>(reader.readFloat(), reader.readFloat()));
            reader.skipInt(); // May be material.
            reader.skipInt();
            this.vertices.add(new Tuple3<>(reader.readFloat(), reader.readFloat(), reader.readFloat()));
            this.normals.add(new Tuple3<>(0F, 0F, 0F)); //TODO: ?
            reader.skipBytes(Constants.INTEGER_SIZE * 5); // Unsure, Seems to be 0x3F800000. TODO: Figure this out. It might be color.
            //this.uvs.add(new Tuple2<>(reader.readFloat(), reader.readFloat())); // TF? We already read uvs above. TODO: ???
            reader.skipBytes(2 * Constants.FLOAT_SIZE); //TODO ^^
            reader.skipInt();
            reader.skipInt(); // May be material.*/
        } else { // Mode 6.
            this.vertices.add(new Tuple3<>(reader.readFloat(), reader.readFloat(), reader.readFloat()));
            reader.readFloat(); // 1.0 as a float usually.
            this.normals.add(new Tuple3<>(reader.readFloat(), reader.readFloat(), reader.readFloat()));
            reader.skipBytes(Constants.FLOAT_SIZE * 5); // Unsure, Seems to be 0x3F800000. Usually seems to be 1.0
            this.uvs.add(new Tuple2<>(reader.readFloat(), reader.readFloat()));
            reader.skipBytes(3 * Constants.FLOAT_SIZE); // 3 Floats. In a few cases, the last one is not present, for the last entry in the file.
            // TODO: Middle skipped float could be material.
        }
    }

    public enum TGQPrimType {
        POINT_LIST,
        LINE_LIST,
        LINE_STRIP,
        TRIANGLE_LIST,
        TRIANGLE_STRIP,
        TRIANGLE_FAN
    }

    @Getter
    public static class TGQMaterial extends GameObject { // TODO: Export properties in .obj
        private String materialName;
        private String textureFile;
        private int flags = 1;
        private float xpVal = 0F;
        private float diffuseRed = 1F;
        private float diffuseGreen = 1F;
        private float diffuseBlue = 1F;
        private float diffuseAlpha = 1F;
        private float ambientRed = 1F;
        private float ambientGreen = 1F;
        private float ambientBlue = 1F;
        private float ambientAlpha = 1F;
        private float specularRed;
        private float specularGreen;
        private float specularBlue;
        private float specularAlpha;
        private float emissiveRed;
        private float emissiveGreen;
        private float emissiveBlue;
        private float emissiveAlpha = 1F;
        private float power = 1F;
        private int texture; // TODO: Find any materials where this is not zero.

        private static final int NAME_SIZE = 32;
        private static final int FILENAME_SIZE = 32;

        @Override
        public void load(DataReader reader) {
            this.materialName = reader.readTerminatedStringOfLength(NAME_SIZE);
            this.textureFile = reader.readTerminatedStringOfLength(FILENAME_SIZE);
            this.flags = reader.readInt();
            this.xpVal = reader.readFloat();
            this.diffuseRed = reader.readFloat();
            this.diffuseGreen = reader.readFloat();
            this.diffuseBlue = reader.readFloat();
            this.diffuseAlpha = reader.readFloat();
            this.ambientRed = reader.readFloat();
            this.ambientGreen = reader.readFloat();
            this.ambientBlue = reader.readFloat();
            this.ambientAlpha = reader.readFloat();
            this.specularRed = reader.readFloat();
            this.specularGreen = reader.readFloat();
            this.specularBlue = reader.readFloat();
            this.specularAlpha = reader.readFloat();
            this.emissiveRed = reader.readFloat();
            this.emissiveGreen = reader.readFloat();
            this.emissiveBlue = reader.readFloat();
            this.emissiveAlpha = reader.readFloat();
            this.power = reader.readFloat();
            this.texture = reader.readInt();
            if (this.texture != 0)
                System.out.println("NON-ZERO MATERIAL PTR!! " + Utils.toHexString(this.texture) + ", " + this.materialName + ", " + this.textureFile);
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeTerminatedStringOfLength(this.materialName, NAME_SIZE);
            writer.writeTerminatedStringOfLength(this.textureFile, FILENAME_SIZE);
            writer.writeInt(this.flags);
            writer.writeFloat(this.xpVal);
            writer.writeFloat(this.diffuseRed);
            writer.writeFloat(this.diffuseGreen);
            writer.writeFloat(this.diffuseBlue);
            writer.writeFloat(this.diffuseAlpha);
            writer.writeFloat(this.ambientRed);
            writer.writeFloat(this.ambientGreen);
            writer.writeFloat(this.ambientBlue);
            writer.writeFloat(this.ambientAlpha);
            writer.writeFloat(this.specularRed);
            writer.writeFloat(this.specularGreen);
            writer.writeFloat(this.specularBlue);
            writer.writeFloat(this.specularAlpha);
            writer.writeFloat(this.emissiveRed);
            writer.writeFloat(this.emissiveGreen);
            writer.writeFloat(this.emissiveBlue);
            writer.writeFloat(this.emissiveAlpha);
            writer.writeFloat(this.power);
            writer.writeInt(this.texture);
        }
    }
}
