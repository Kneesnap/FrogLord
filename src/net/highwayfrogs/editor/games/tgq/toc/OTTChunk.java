package net.highwayfrogs.editor.games.tgq.toc;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQChunkedFile;
import net.highwayfrogs.editor.games.tgq.TGQFile;
import net.highwayfrogs.editor.games.tgq.TGQImageFile;
import net.highwayfrogs.editor.games.tgq.loading.kcLoadContext;
import net.highwayfrogs.editor.games.tgq.model.kcMaterial;
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
public class OTTChunk extends kcCResource {
    private final List<OTTMesh> meshes = new ArrayList<>();
    private final List<kcMaterial> materials = new ArrayList<>();

    public static final int NAME_SIZE = 32;

    public OTTChunk(TGQChunkedFile parentFile) {
        super(parentFile, KCResourceID.OCTTREESCENEMGR);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int unknownPointer = reader.readInt();
        int unknownCount = reader.readInt();
        int unknownPointer2 = reader.readInt();
        int meshData = reader.readInt();
        int meshCount = reader.readInt();
        int unknownPointer3 = reader.readInt();
        int materialCount = reader.readInt();
        reader.setIndex(0x60); // End of header.

        // Skip
        reader.skipBytes(meshData + unknownPointer3);

        //TODO: Read Object Group?

        int matCount = (int) getParentFile().getChunks().stream().filter(chunk -> chunk instanceof TGQChunkTextureReference).count();
        for (int i = 0; i < meshCount; i++) {
            reader.jumpTemp(reader.getIndex());
            reader.skipInt();
            int materialId = reader.readInt();
            reader.jumpReturn();
            //TODO: Ranges.

            reader.skipBytes(0x44); // TODO: Read.
            int structSize = reader.readInt();
            if (structSize == 0x24) {
                int unknownAlways4 = reader.readInt(); // Unknown, seems to always be four.
                int count = reader.readInt();
                int unknownValue = reader.readInt(); // Unknown. It seems to be a relatively small value ranging from 0 - 30,000.
                reader.skipBytes(3 * Constants.INTEGER_SIZE); // These seem to always be zero.

                OTTMesh newMesh = new OTTMesh(count, materialId);
                for (int j = 0; j < count * 3; j++) {
                    newMesh.getPositions().add(new Tuple3<>(reader.readFloat(), reader.readFloat(), reader.readFloat()));
                    newMesh.getNormals().add(new Tuple3<>(reader.readFloat(), reader.readFloat(), reader.readFloat()));
                    int color = reader.readInt(); // Either flags or used for static lighting. (Maybe flags are actually per mesh.)
                    newMesh.getTexCoords().add(new Tuple2<>(reader.readFloat(), reader.readFloat()));
                }

                getMeshes().add(newMesh);
            } else if (structSize == 0x20) {
                int unknownAlways4 = reader.readInt(); // Unknown, seems to always be four.
                int count = reader.readInt();
                int unknownValue = reader.readInt(); // Unknown. It seems to be a relatively small value ranging from 0 - 30,000.
                reader.skipBytes(3 * Constants.INTEGER_SIZE); // These seem to always be zero.

                OTTMesh newMesh = new OTTMesh(count, materialId);
                for (int j = 0; j < count * 3; j++) {
                    newMesh.getPositions().add(new Tuple3<>(reader.readFloat(), reader.readFloat(), reader.readFloat()));
                    newMesh.getNormals().add(new Tuple3<>(reader.readFloat(), reader.readFloat(), reader.readFloat()));
                    //int color = reader.readInt(); // Either flags or used for static lighting. (Maybe flags are actually per mesh.)
                    newMesh.getTexCoords().add(new Tuple2<>(reader.readFloat(), reader.readFloat()));
                }

                getMeshes().add(newMesh);
            }
        }

        for (int i = 0; i < materialCount; i++) {
            kcMaterial newMaterial = new kcMaterial();
            newMaterial.load(reader);
            this.materials.add(newMaterial);
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

        List<TGQChunkTextureReference> chunks = new ArrayList<>();
        for (kcCResource chunk : getParentFile().getChunks())
            if (chunk instanceof TGQChunkTextureReference)
                chunks.add((TGQChunkTextureReference) chunk);

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
            writer.write("usemtl Mat_" + getMaterials().get(mesh.getMaterial()).getMaterialName().replace(' ', '_') + Constants.NEWLINE);
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

        PrintWriter mtlWriter = new PrintWriter(new File(folder, fileName + ".mtl"));
        for (kcMaterial material : getMaterials()) {
            mtlWriter.write("newmtl Mat_" + material.getMaterialName().replace(' ', '_') + Constants.NEWLINE);
            mtlWriter.write("Kd 1 1 1" + Constants.NEWLINE);
            mtlWriter.write("map_Kd " + fileName + "/Textures/" + material.getTextureFileName().replace(".img", "") + ".png" + Constants.NEWLINE);
            mtlWriter.write(Constants.NEWLINE);
        }
        mtlWriter.close();

        if (chunks.size() > 0) {
            File imgFolder = new File(folder, fileName + "/Textures/");
            if (!imgFolder.exists())
                imgFolder.mkdirs();

            for (TGQChunkTextureReference TGQChunkTextureReference : chunks) {
                TGQFile file = getFileByName(TGQChunkTextureReference.getPath());
                if (file instanceof TGQImageFile)
                    ((TGQImageFile) file).saveImageToFile(new File(imgFolder, file.getExportName().replace(".img", "") + ".png"));
            }
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeBytes(getRawData()); // TODO: REPLACE
    }

    @Override
    public void afterLoad2(kcLoadContext context) {
        super.afterLoad2(context);
        // Resolves textures. Waits until after afterLoad1() when file names are resolved.
        context.getMaterialLoadContext().resolveMaterialTexturesInChunk(getParentFile(), this.materials);
    }

    @Getter
    public class OTTMesh {
        private final List<Tuple3<Float, Float, Float>> positions = new ArrayList<>();
        private final List<Tuple3<Float, Float, Float>> normals = new ArrayList<>();
        private final List<Tuple2<Float, Float>> texCoords = new ArrayList<>();
        private final int material;
        private final int count;

        public OTTMesh(int count, int material) {
            this.count = count;
            this.material = material;
        }
    }
}