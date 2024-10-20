package net.highwayfrogs.editor.games.konami.greatquest.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResOctTreeSceneMgr;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResOctTreeSceneMgr.kcVtxBufFileStruct;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * A utility for converting kcModel objects into wavefront .obj text data.
 * Created by Kneesnap on 6/22/2023.
 */
public class kcModelObjWriter {
    /**
     * Write the meshes in the model to .obj text.
     * @param outputFolder The folder to export the model to
     * @param fileName The name of the file to write
     * @param model The model to write mesh data from
     */
    public static void writeMeshesToObj(File outputFolder, String fileName, kcModel model) {
        File objFile = new File(outputFolder, fileName + ".obj");
        File mtlFile = new File(outputFolder, fileName + ".mtl");
        if (objFile.exists() && mtlFile.exists())
            return;

        StringBuilder objWriter = new StringBuilder();
        StringBuilder mtlWriter = new StringBuilder();

        writeMeshesToObj(outputFolder, fileName, objWriter, mtlWriter, model);

        try {
            Files.write(objFile.toPath(), Arrays.asList(objWriter.toString().split(Constants.NEWLINE)));
            Files.write(mtlFile.toPath(), Arrays.asList(mtlWriter.toString().split(Constants.NEWLINE)));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to export map to '" + objFile + "'.", ex);
        }
    }

    /**
     * Write the meshes in the model to .obj text.
     * @param outputFolder the folder to export the model to
     * @param objWriter the writer to write the obj text to
     * @param mtlWriter the writer to write the obj text to
     * @param model the model to write mesh data from
     */
    public static void writeMeshesToObj(File outputFolder, String fileName, StringBuilder objWriter, StringBuilder mtlWriter, kcModel model) {
        ModelObjContext context = new ModelObjContext(model, outputFolder, fileName, objWriter, mtlWriter);
        if (!setupContext(context, model.getComponents()))
            throw new RuntimeException("Cannot export model which doesn't have any position data.");

        // Write header.
        objWriter.append("# Exported by FrogLord " + Constants.VERSION).append(Constants.NEWLINE);
        if (mtlWriter != null)
            objWriter.append("mtllib ").append(fileName).append(".mtl").append(Constants.NEWLINE);
        objWriter.append(Constants.NEWLINE);

        // Write body.
        writePrims(context);
        writeMaterialsToMtl(context, model.getMaterials());
    }

    /**
     * Write the map mesh to .obj text.
     * @param outputFolder the folder to write the object to
     * @param mapMesh the map file to write data from
     */
    public static void writeMapToObj(File outputFolder, String fileName, kcCResOctTreeSceneMgr mapMesh) throws IOException {
        File objFile = new File(outputFolder, fileName + ".obj");
        File mtlFile = new File(outputFolder, fileName + ".mtl");

        StringBuilder objWriter = new StringBuilder();
        StringBuilder mtlWriter = new StringBuilder();

        writeMapToObj(outputFolder, fileName, objWriter, mtlWriter, mapMesh);

        try {
            Files.write(objFile.toPath(), Arrays.asList(objWriter.toString().split(Constants.NEWLINE)));
            Files.write(mtlFile.toPath(), Arrays.asList(mtlWriter.toString().split(Constants.NEWLINE)));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to export map to '" + objFile + "'.", ex);
        }
    }

    /**
     * Write the map mesh to .obj text.
     * @param outputFolder the folder to write the object to
     * @param fileName the name of the file to export to
     * @param objWriter the writer to write the obj text to
     * @param mtlWriter the writer to write the obj text to
     * @param mapMesh the map file to write data from
     */
    public static void writeMapToObj(File outputFolder, String fileName, StringBuilder objWriter, StringBuilder mtlWriter, kcCResOctTreeSceneMgr mapMesh) {
        MapObjContext context = new MapObjContext(mapMesh, outputFolder, fileName, objWriter, mtlWriter);

        // Write header.
        objWriter.append("# Exported by FrogLord " + Constants.VERSION).append(Constants.NEWLINE);
        if (mtlWriter != null)
            objWriter.append("mtllib ").append(fileName).append(".mtl").append(Constants.NEWLINE);
        objWriter.append(Constants.NEWLINE);

        // Write body.
        writePrims(context);
        writeMaterialsToMtl(context, mapMesh.getMaterials());
    }

    private static void writeMaterialsToMtl(ObjWriterContext context, List<kcMaterial> materials) {
        if (context == null || context.getMtlWriter() == null || materials == null)
            return;

        for (int i = 0; i < materials.size(); i++) {
            kcMaterial material = materials.get(i);
            if (material.getTexture() == null)
                continue;

            String outputImagePrefix = Utils.stripExtension(context.getFileName()) + "_";
            if (context.getOutputFolder() != null) {
                File texFolder = new File(context.getOutputFolder(), "Textures/");
                Utils.makeDirectory(texFolder);

                String outputImageFileName = outputImagePrefix + Utils.stripExtension(material.getTextureFileName()) + ".png";
                try {
                    material.getTexture().saveImageToFile(new File(texFolder, outputImageFileName));
                } catch (IOException ex) {
                    throw new RuntimeException("Failed to export texture '" + outputImageFileName + "' for .obj export.", ex);
                }
            }

            // Write material.
            StringBuilder builder = new StringBuilder();
            material.writeWavefrontObjMaterial(builder, "Textures/" + outputImagePrefix, true, true);
            context.getMtlWriter().append(builder.append(Constants.NEWLINE));
        }
    }

    private static void writePrims(ModelObjContext context) {
        StringBuilder objWriter = context.getObjWriter();
        StringBuilder mtlWriter = context.getMtlWriter();

        long lastMaterialId = -1;
        List<kcModelPrim> primitives = context.getModel().getPrimitives();
        for (int i = 0; i < primitives.size(); i++) {
            kcModelPrim prim = primitives.get(i);

            // Write new primitive ID.
            objWriter.append("# Primitive #");
            objWriter.append(i + 1);
            objWriter.append(":");
            objWriter.append(Constants.NEWLINE);

            // Write new material.
            if (lastMaterialId != prim.getMaterialId() && mtlWriter != null) {
                if (context.getModel().getMaterials().size() <= prim.getMaterialId()) {
                    // TODO: !
                    context.getLogger().warning("Got material ID " + prim.getMaterialId() + ", but... there are only " + context.getModel().getMaterials().size() + " material(s) available in the model.");
                } else {
                    kcMaterial material = context.getModel().getMaterials().get((int) prim.getMaterialId());
                    objWriter.append("usemtl ");
                    objWriter.append(material.getMaterialName());
                    objWriter.append(Constants.NEWLINE);

                    lastMaterialId = prim.getMaterialId();
                }
            }

            // Write vertices.
            for (int j = 0; j < prim.getVertexCount(); j++)
                writeVertex(context, prim.getVertices().get(j));
            objWriter.append(Constants.NEWLINE);

            // Write normals.
            if (context.hasNormals) {
                for (int j = 0; j < prim.getVertexCount(); j++)
                    writeVertexNormal(context, prim.getVertices().get(j));

                objWriter.append(Constants.NEWLINE);
            }

            // Write texture coordinates.
            if (context.hasTexCoords) {
                for (int j = 0; j < prim.getVertexCount(); j++)
                    writeTexCoord(context, prim.getVertices().get(j), false);

                objWriter.append(Constants.NEWLINE);
            }

            // Write faces.
            writePrim(context, prim);
            objWriter.append(Constants.NEWLINE);
        }
    }

    private static void writePrims(MapObjContext context) {
        StringBuilder objWriter = context.getObjWriter();
        StringBuilder mtlWriter = context.getMtlWriter();

        long lastMaterialId = -1;
        for (int i = 0; i < context.getMap().getVertexBuffers().size(); i++) {
            kcVtxBufFileStruct vtxBuf = context.getMap().getVertexBuffers().get(i);

            // Write new primitive ID.
            objWriter.append("# Vtx Buf #");
            objWriter.append(i + 1);
            objWriter.append(":");
            objWriter.append(Constants.NEWLINE);

            // Write new material.
            if (lastMaterialId != vtxBuf.getMaterialId() && mtlWriter != null) {
                if (context.getMap().getMaterials().size() <= vtxBuf.getMaterialId()) {
                    // TODO: !
                    context.getLogger().warning("Got material ID " + vtxBuf.getMaterialId() + ", but... there are only " + context.getMap().getMaterials().size() + " material(s) available in the model.");
                } else {
                    kcMaterial material = context.getMap().getMaterials().get((int) vtxBuf.getMaterialId());
                    objWriter.append("usemtl ");
                    objWriter.append(material.getMaterialName());
                    objWriter.append(Constants.NEWLINE);

                    lastMaterialId = vtxBuf.getMaterialId();
                }
            }

            if (!setupContext(context, vtxBuf.getComponents())) {
                context.getLogger().warning("The map mesh contained a primitive which had no position data."); // Shouldn't happen.
                objWriter.append("# Skipping because there was no position data...?");
                objWriter.append(Constants.NEWLINE);
                continue;
            }

            // Write vertices.
            for (int j = 0; j < vtxBuf.getVertexCount(); j++)
                writeVertex(context, vtxBuf.getVertices().get(j));
            objWriter.append(Constants.NEWLINE);

            // Write normals.
            if (context.hasNormals) {
                for (int j = 0; j < vtxBuf.getVertexCount(); j++)
                    writeVertexNormal(context, vtxBuf.getVertices().get(j));

                objWriter.append(Constants.NEWLINE);
            }

            // Write texture coordinates.
            if (context.hasTexCoords) {
                for (int j = 0; j < vtxBuf.getVertexCount(); j++)
                    writeTexCoord(context, vtxBuf.getVertices().get(j), false);

                objWriter.append(Constants.NEWLINE);
            }

            // Write faces.
            writePrim(context, vtxBuf);
            objWriter.append(Constants.NEWLINE);
        }
    }

    private static boolean setupContext(ObjWriterContext context, kcVertexFormatComponent[] components) {
        if (components == null)
            return false;

        // Calculate has / has nots.
        boolean hasPosition = false;
        boolean hasNormals = false;
        boolean hasTexCoords = false;
        for (int i = 0; i < components.length; i++) {
            switch (components[i]) {
                case POSITION_XYZF:
                case POSITION_XYZWF:
                    hasPosition = true;
                    break;
                case NORMAL_XYZF:
                case NORMAL_XYZWF:
                    hasNormals = true;
                    break;
                case TEX1F:
                case TEX2F:
                case TEX1_STQP:
                    hasTexCoords = true;
                    break;
            }
        }

        if (!hasPosition) // Can't do anything without position.
            return false;

        context.hasNormals = hasNormals;
        context.hasTexCoords = hasTexCoords;
        return true;
    }

    private static void writePrim(ModelObjContext context, kcModelPrim prim) {
        switch (prim.getPrimitiveType()) {
            case TRIANGLE_LIST:
                writeTriangleList(context, (int) prim.getVertexCount());
                break;
            case TRIANGLE_STRIP:
                writeTriangleStrip(context, (int) prim.getVertexCount());
                break;
            default:
                context.getLogger().warning("kcModel had a prim of type '" + prim.getPrimitiveType() + "', which was supposed because it was unsupported.");
        }
    }

    private static void writePrim(MapObjContext context, kcVtxBufFileStruct vtxBuf) {
        switch (vtxBuf.getPrimitiveType()) {
            case TRIANGLE_LIST:
                writeTriangleList(context, vtxBuf.getVertexCount());
                break;
            case TRIANGLE_STRIP:
                writeTriangleStrip(context, vtxBuf.getVertexCount());
                break;
            default:
                context.getLogger().warning("kcCResOctTreeSceneMgr had a prim of type '" + vtxBuf.getPrimitiveType() + "', which was supposed because it was unsupported.");
        }
    }

    private static void writeTriangleList(ObjWriterContext context, int vertexCount) {
        StringBuilder objWriter = context.getObjWriter();

        for (int i = 0; i < vertexCount; i += 3) {
            objWriter.append('f');
            writeVertexValue(context, 0, true);
            writeVertexValue(context, 0, true);
            writeVertexValue(context, 0, true);
            objWriter.append(Constants.NEWLINE);
        }
    }

    private static void writeTriangleStrip(ObjWriterContext context, int vertexCount) {
        StringBuilder objWriter = context.getObjWriter();

        for (int i = 0; i < vertexCount - 2; i++) {
            objWriter.append('f');
            if (i % 2 > 0) { // Alternate the indices so faces always orient consistently
                writeVertexValue(context, 1, false);
                writeVertexValue(context, 0, false);
            } else {
                writeVertexValue(context, 0, false);
                writeVertexValue(context, 1, false);
            }
            writeVertexValue(context, 2, true);
            objWriter.append(Constants.NEWLINE);
        }
    }

    private static void writeVertexValue(ObjWriterContext context, int offset, boolean increaseValues) {
        writeVertexValue(context, context.baseVertex + offset, context.baseNormal + offset, context.baseTexCoord + offset);

        if (increaseValues) {
            context.baseVertex++;
            if (context.hasNormals)
                context.baseNormal++;
            if (context.hasTexCoords)
                context.baseTexCoord++;
        }
    }

    private static void writeVertexValue(ObjWriterContext context, int vertexId, int normalId, int texCoordId) {
        StringBuilder objWriter = context.getObjWriter();
        objWriter.append(' ');
        objWriter.append(vertexId);
        if (context.hasNormals || context.hasTexCoords)
            objWriter.append('/');
        if (context.hasTexCoords)
            objWriter.append(texCoordId);
        if (context.hasNormals) {
            objWriter.append('/');
            objWriter.append(normalId);
        }
    }

    private static void writeVertex(ObjWriterContext context, kcVertex vertex) {
        StringBuilder objWriter = context.getObjWriter();
        objWriter.append("v ");
        objWriter.append(vertex.getX());
        objWriter.append(" ");
        objWriter.append(vertex.getY());
        objWriter.append(" ");
        objWriter.append(vertex.getZ());
        objWriter.append(Constants.NEWLINE);
    }

    private static void writeVertexNormal(ObjWriterContext context, kcVertex vertex) {
        StringBuilder objWriter = context.getObjWriter();
        objWriter.append("vn ");
        objWriter.append(vertex.getNormalX());
        objWriter.append(" ");
        objWriter.append(vertex.getNormalY());
        objWriter.append(" ");
        objWriter.append(vertex.getNormalZ());
        objWriter.append(Constants.NEWLINE);
    }

    private static void writeTexCoord(ObjWriterContext context, kcVertex vertex, boolean secondTex) {
        StringBuilder objWriter = context.getObjWriter();
        objWriter.append("vt ");
        objWriter.append(secondTex ? vertex.getU1() : vertex.getU0());
        objWriter.append(" ");
        objWriter.append(secondTex ? vertex.getV1() : vertex.getV0());
        objWriter.append(Constants.NEWLINE);
    }

    @RequiredArgsConstructor
    public static class ObjWriterContext {
        @Getter private final File outputFolder;
        @Getter private final String fileName;
        @Getter private final StringBuilder objWriter;
        @Getter private final StringBuilder mtlWriter;
        private Logger cachedLogger;
        public boolean hasNormals;
        public boolean hasTexCoords;
        public int baseVertex = 1;
        public int baseNormal = 1;
        public int baseTexCoord = 1;

        /**
         * Gets the logger.
         */
        public Logger getLogger() {
            if (this.cachedLogger == null)
                this.cachedLogger = Logger.getLogger(Utils.getSimpleName(this));

            return this.cachedLogger;
        }
    }

    private static class ModelObjContext extends ObjWriterContext {
        @Getter private final kcModel model;

        public ModelObjContext(kcModel model, File outputFolder, String fileName, StringBuilder objWriter, StringBuilder mtlWriter) {
            super(outputFolder, fileName, objWriter, mtlWriter);
            this.model = model;
        }
    }

    private static class MapObjContext extends ObjWriterContext {
        @Getter private final kcCResOctTreeSceneMgr map;

        public MapObjContext(kcCResOctTreeSceneMgr map, File outputFolder, String fileName, StringBuilder objWriter, StringBuilder mtlWriter) {
            super(outputFolder, fileName, objWriter, mtlWriter);
            this.map = map;
        }
    }
}