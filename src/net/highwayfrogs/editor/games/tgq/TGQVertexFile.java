package net.highwayfrogs.editor.games.tgq;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.system.Tuple3;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles TGQ .VTX files
 * Created by Kneesnap on 8/24/2019.
 */
public class TGQVertexFile extends TGQFile {
    private List<Tuple3<Float, Float, Float>> vertices = new ArrayList<>();
    private List<Tuple2<Float, Float>> normals = new ArrayList<>();

    public static final String SIGNATURE = "6YTV";

    public TGQVertexFile(TGQBinFile mainArchive) {
        super(mainArchive);
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE);

        //TODO: The rest of the file.
        reader.setIndex(0x20);
        int vertexCount = reader.readInt();
        //System.out.println("Vertice Count: " + vertexCount);

        try {
            //System.out.println("Reading from: " + Utils.toHexString(reader.getSize() - (68 * vertexCount)));
            reader.jumpTemp(reader.getSize() - (68 * vertexCount));
            for (int i = 0; i < vertexCount; i++) {
                this.vertices.add(new Tuple3<>(reader.readFloat(), reader.readFloat(), reader.readFloat()));
                reader.skipBytes(36);
                this.normals.add(new Tuple2<>(reader.readFloat(), reader.readFloat()));
                reader.skipBytes(12);
            }

            //System.out.println("Left: " + reader.getRemaining());
        } catch (Exception ex) { //TODO: Lmao, actually parse the file.
            //System.out.println("Failure!");
        }
    }

    @Override
    public String getExtension() {
        return "vtx";
    }

    /**
     * Export this model to a .obj file.
     * @param outputFile The output file.
     */
    public void saveToFile(File outputFile) throws IOException {
        PrintWriter writer = new PrintWriter(outputFile);

        writer.write("# Exported by FrogLord" + Constants.NEWLINE);
        for (Tuple3<Float, Float, Float> vertex : this.vertices)
            writer.write("v " + vertex.getA() + " " + vertex.getB() + " " + vertex.getC() + Constants.NEWLINE);

        for (Tuple2<Float, Float> normal : this.normals)
            writer.write("vt " + normal.getA() + " " + normal.getB() + Constants.NEWLINE);

        writer.close();
    }
}
