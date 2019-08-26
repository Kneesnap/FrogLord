package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQTOCFile;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.system.Tuple3;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the OTT chunk of a TOC file.
 * Created by Kneesnap on 8/25/2019.
 */
@Getter
public class OTTChunk extends TOCChunk {
    private List<OTTMesh> meshes = new ArrayList<>();

    public OTTChunk(TGQTOCFile parentFile) {
        super(parentFile, TOCChunkType.OTT);
    }

    @Override
    public void load(DataReader reader) {
        reader.skipBytes(0x2C); //TODO: Handle this data.
        int skipSize = reader.readInt(); //TODO: Read this.
        int meshCount = reader.readInt();
        skipSize += reader.readInt(); //TODO: This too.
        reader.skipBytes(skipSize + 0x28);
        System.out.println("Mesh Count: " + meshCount + ", Start: " + Utils.toHexString(reader.getIndex()));

        for (int i = 0; i < meshCount; i++) {
            reader.skipBytes(0x44); // TODO: Read.
            int structSize = reader.readInt();
            if (structSize != 0x24)
                break; // Break! (For some reason)

            reader.skipInt(); // TODO: What is it?
            int count = reader.readInt();
            reader.skipBytes(0x10); //TODO

            OTTMesh newMesh = new OTTMesh(count);
            for (int j = 0; j < count * 3; j++) {
                newMesh.getPositions().add(new Tuple3<>(reader.readFloat(), reader.readFloat(), reader.readFloat()));
                newMesh.getNormals().add(new Tuple3<>(reader.readFloat(), reader.readFloat(), reader.readFloat()));
                reader.skipInt(); //TODO: ?
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
            writer.write("usemtl " + fileName + "_" + Utils.padNumberString(i, 4) + Constants.NEWLINE);
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
        private int count;

        public OTTMesh(int count) {
            this.count = count;
        }
    }
}
