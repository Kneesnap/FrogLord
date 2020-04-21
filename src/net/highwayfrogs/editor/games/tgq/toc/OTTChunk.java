package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
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
 * Handles the OTT chunk.
 * Helpful: https://github.com/FrozenFish24/SH2MapTools/blob/master/Sh2Map.py
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public class OTTChunk extends TGQFileChunk {
    private String name;
    private List<OTTMesh> meshes = new ArrayList<>();

    public static final int NAME_SIZE = 32;

    public OTTChunk(TGQChunkedFile parentFile) {
        super(parentFile, TGQChunkType.OTT); //TODO
    }

    @Override
    public void load(DataReader reader) {
        this.name = reader.readTerminatedStringOfLength(NAME_SIZE);
        int unknownPointer = reader.readInt();
        int unknownCount = reader.readInt();
        int unknownPointer2 = reader.readInt();
        int meshData = reader.readInt();
        int meshCount = reader.readInt();
        int unknownPointer3 = reader.readInt();
        int unknownCount3 = reader.readInt();
        reader.setIndex(0x60); // End of header.

        // Skip
        reader.skipBytes(meshData + unknownPointer3);


        //TODO: Read textures (maybe)
        //TODO: Read materials.
        //TODO: Read Object Group?

        int matCount = (int) getParentFile().getChunks().stream().filter(chunk -> chunk instanceof TEXChunk).count();
        System.out.println("Mesh Count: " + meshCount + ", " + matCount); //TODO
        for (int i = 0; i < meshCount; i++) {
            reader.jumpTemp(reader.getIndex());
            reader.skipInt();
            int testMaterial = reader.readInt();
            if (testMaterial < 0 || testMaterial > matCount)
                System.out.println("Mesh Num BAD " + testMaterial);
            reader.jumpReturn();

            reader.skipBytes(0x44); // TODO: Read.
            int structSize = reader.readInt();
            if (structSize != 0x24)
                break; // Break! (For some reason) PS2 Only. TODO: Check if it's 0x20. If it is, then likely we should handle it normally just without reading the color value.

            int unknownAlways4 = reader.readInt(); // Unknown, seems to always be four.
            int count = reader.readInt();
            int unknownValue = reader.readInt(); // Unknown. It seems to be a relatively small value ranging from 0 - 30,000.
            reader.skipBytes(3 * Constants.INTEGER_SIZE); // These seem to always be zero.

            OTTMesh newMesh = new OTTMesh(count, testMaterial);
            for (int j = 0; j < count * 3; j++) {
                newMesh.getPositions().add(new Tuple3<>(reader.readFloat(), reader.readFloat(), reader.readFloat()));
                newMesh.getNormals().add(new Tuple3<>(reader.readFloat(), reader.readFloat(), reader.readFloat()));
                int color = reader.readInt(); // Used for static lighting. Alpha is always 0xFF.
                newMesh.getTexCoords().add(new Tuple2<>(reader.readFloat(), reader.readFloat()));
            }

            getMeshes().add(newMesh);
        }
    }

    /**
     * Export this data in .obj format.
     * @param folder   The folder to export to.
     * @param fileName The obj file name.
     */
    public void exportAsObj(File folder, String fileName) throws IOException {
        PrintWriter writer = new PrintWriter(new File(folder, fileName + ".obj"));
        writer.write("# FrogLord export." + Constants.NEWLINE);
        writer.write("# Mesh Count: " + getMeshes().size() + Constants.NEWLINE);
        writer.write("mtllib " + fileName + ".mtl" + Constants.NEWLINE);
        writer.write(Constants.NEWLINE);

        int vId = 1;
        for (int i = 0; i < getMeshes().size(); i++) {
            OTTMesh mesh = getMeshes().get(i);

            writer.write("g" + Constants.NEWLINE);

            // Vertices.
            for (Tuple3<Float, Float, Float> vertex : mesh.getPositions())
                writer.write("v " + vertex.getA() + " " + vertex.getB() + " " + vertex.getC() + Constants.NEWLINE);
            writer.write(Constants.NEWLINE);

            // Tex Coords.
            for (Tuple2<Float, Float> texCoord : mesh.getTexCoords())
                writer.write("vt " + texCoord.getA() + " " + texCoord.getB() + Constants.NEWLINE);
            writer.write(Constants.NEWLINE);

            // Normals.
            for (Tuple3<Float, Float, Float> normal : mesh.getNormals())
                writer.write("vn " + normal.getA() + " " + normal.getB() + " " + normal.getC() + Constants.NEWLINE);
            writer.write(Constants.NEWLINE);

            // Faces.
            writer.write("g Mesh_" + Utils.padNumberString(i, 4) + Constants.NEWLINE);
            writer.write("usemtl Mat_" + Utils.padNumberString(mesh.getMaterial(), 4) + Constants.NEWLINE);
            for (int j = 0; j < mesh.getCount(); j++) {
                writer.write("f");
                for (int k = 0; k < 3; k++) {
                    writer.write(" " + vId + "/" + vId + "/" + vId);
                    vId++;
                }
                writer.write(Constants.NEWLINE);
            }
            writer.write(Constants.NEWLINE);
        }
        writer.close();

        List<TEXChunk> chunks = new ArrayList<>();
        for (TGQFileChunk chunk : getParentFile().getChunks())
            if (chunk instanceof TEXChunk)
                chunks.add((TEXChunk) chunk);


        PrintWriter mtlWriter = new PrintWriter(new File(folder, fileName + ".mtl"));
        for (int i = 0; i < chunks.size(); i++) {
            mtlWriter.write("newmtl Mat_" + Utils.padNumberString(i, 4) + Constants.NEWLINE);
            mtlWriter.write("Kd 1 1 1" + Constants.NEWLINE);
            mtlWriter.write("map_Kd " + fileName + "/Textures/" + i + ".png" + Constants.NEWLINE);
            mtlWriter.write(Constants.NEWLINE);
        }
        mtlWriter.close();

        if (chunks.size() > 0) {
            File imgFolder = new File(folder, fileName + "/Textures/");
            if (!imgFolder.exists())
                imgFolder.mkdirs();

            int id = 0;
            for (TEXChunk texChunk : chunks) {
                TGQFile file = getParentFile().getMainArchive().getFileByName(texChunk.getPath());
                if (file instanceof TGQImageFile)
                    ((TGQImageFile) file).saveImageToFile(new File(imgFolder, id + ".png"));
                id++;
            }
        }

        //TODO: Write mtl, grab textures too.

    }

    @Override
    public void save(DataWriter writer) {
        //TODO
    }

    @Getter
    public class OTTMesh {
        private List<Tuple3<Float, Float, Float>> positions = new ArrayList<>();
        private List<Tuple3<Float, Float, Float>> normals = new ArrayList<>();
        private List<Tuple2<Float, Float>> texCoords = new ArrayList<>();
        private int material;
        private int count;

        public OTTMesh(int count, int material) {
            this.count = count;
            this.material = material;
        }
    }
}
