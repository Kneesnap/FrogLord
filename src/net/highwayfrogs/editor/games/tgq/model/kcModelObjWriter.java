package net.highwayfrogs.editor.games.tgq.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.Constants;

import java.io.PrintWriter;

/**
 * A utility for converting kcModel objects into wavefront .obj text data.
 * Created by Kneesnap on 6/22/2023.
 */
public class kcModelObjWriter {
    /**
     * Write the meshes in the model to .obj text.
     * @param objWriter The writer to write the obj text to.
     * @param mtlWriter The writer to write the obj text to.
     * @param model     The model to write mesh data from.
     */
    public static void writeMeshesToObj(PrintWriter objWriter, PrintWriter mtlWriter, kcModel model) {

        // Calculate has / has nots.
        boolean hasPosition = false;
        boolean hasNormals = false;
        boolean hasTexCoords = false;
        if (model.getComponents() != null) {
            for (int i = 0; i < model.getComponents().length; i++) {
                switch (model.getComponents()[i]) {
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
        }

        if (hasPosition) { // Can't do anything without position.
            ObjContext context = new ObjContext(model, objWriter, mtlWriter);
            context.hasNormals = hasNormals;
            context.hasTexCoords = hasTexCoords;
            writePrims(context);
        }
    }

    private static void writePrims(ObjContext context) {
        PrintWriter objWriter = context.getObjWriter();
        PrintWriter mtlWriter = context.getMtlWriter();

        long lastMaterialId = -1;
        for (int i = 0; i < context.getModel().getPrimitives().size(); i++) {
            kcModelPrim prim = context.getModel().getPrimitives().get(i);

            // Write new primitive ID.
            objWriter.write("# Primitive #");
            objWriter.write(String.valueOf(i + 1));
            objWriter.write(":");
            objWriter.write(Constants.NEWLINE);

            // Write new material.
            if (lastMaterialId != prim.getMaterialId() && mtlWriter != null) {
                if (context.getModel().getMaterials().size() <= prim.getMaterialId()) {
                    // TODO: !
                    System.out.println("Got material ID " + prim.getMaterialId() + ", but... there are only " + context.getModel().getMaterials().size() + " material(s) available in the model.");
                } else {
                    kcMaterial material = context.getModel().getMaterials().get((int) prim.getMaterialId());
                    objWriter.write("usemtl ");
                    objWriter.write(material.getMaterialName());
                    objWriter.append(Constants.NEWLINE);

                    lastMaterialId = prim.getMaterialId();
                }
            }

            // Write vertices.
            for (int j = 0; j < prim.getVertexCount(); j++)
                writeVertex(context, prim.getVertices().get(j));
            objWriter.write(Constants.NEWLINE);

            // Write normals.
            if (context.hasNormals) {
                for (int j = 0; j < prim.getVertexCount(); j++)
                    writeVertexNormal(context, prim.getVertices().get(j));

                objWriter.write(Constants.NEWLINE);
            }

            // Write texture coordinates.
            if (context.hasTexCoords) {
                for (int j = 0; j < prim.getVertexCount(); j++)
                    writeTexCoord(context, prim.getVertices().get(j), false);

                objWriter.write(Constants.NEWLINE);
            }

            // Write faces.
            writePrim(context, prim);
            objWriter.write(Constants.NEWLINE);
        }
    }

    private static void writePrim(ObjContext context, kcModelPrim prim) {
        switch (prim.getPrimType()) {
            case TRIANGLE_LIST:
                writeTriangleList(context, prim);
                break;
            case TRIANGLE_STRIP:
                writeTriangleStrip(context, prim);
                break;
            default:
                System.out.println("kcModel had a prim of type '" + prim.getPrimType() + "', which was supposed because it was unsupported.");
        }
    }

    private static void writeTriangleList(ObjContext context, kcModelPrim prim) {
        PrintWriter objWriter = context.getObjWriter();

        for (int i = 0; i < prim.getVertexCount(); i += 3) {
            objWriter.write('f');
            writeVertexValue(context, 0, true);
            writeVertexValue(context, 0, true);
            writeVertexValue(context, 0, true);
            objWriter.write(Constants.NEWLINE);
        }
    }

    private static void writeTriangleStrip(ObjContext context, kcModelPrim prim) {
        PrintWriter objWriter = context.getObjWriter();

        for (int i = 0; i < prim.getVertexCount() - 2; i++) {
            objWriter.write('f');
            if (i % 2 > 0) { // Alternate the indices so faces always orient consistently
                writeVertexValue(context, 1, false);
                writeVertexValue(context, 0, false);
            } else {
                writeVertexValue(context, 0, false);
                writeVertexValue(context, 1, false);
            }
            writeVertexValue(context, 2, true);
            objWriter.write(Constants.NEWLINE);
        }
    }

    private static void writeVertexValue(ObjContext context, int offset, boolean increaseValues) {
        writeVertexValue(context, context.baseVertex + offset, context.baseNormal + offset, context.baseTexCoord + offset);

        if (increaseValues) {
            context.baseVertex++;
            if (context.hasNormals)
                context.baseNormal++;
            if (context.hasTexCoords)
                context.baseTexCoord++;
        }
    }

    private static void writeVertexValue(ObjContext context, int vertexId, int normalId, int texCoordId) {
        PrintWriter objWriter = context.getObjWriter();
        objWriter.write(' ');
        objWriter.write(String.valueOf(vertexId));
        if (context.hasNormals || context.hasTexCoords)
            objWriter.write('/');
        if (context.hasTexCoords)
            objWriter.write(String.valueOf(texCoordId));
        if (context.hasNormals) {
            objWriter.write('/');
            objWriter.write(String.valueOf(normalId));
        }
    }

    private static void writeVertex(ObjContext context, kcVertex vertex) {
        PrintWriter objWriter = context.getObjWriter();
        objWriter.write("v ");
        objWriter.write(String.valueOf(vertex.getX()));
        objWriter.write(" ");
        objWriter.write(String.valueOf(vertex.getY()));
        objWriter.write(" ");
        objWriter.write(String.valueOf(vertex.getZ()));
        objWriter.write(Constants.NEWLINE);
    }

    private static void writeVertexNormal(ObjContext context, kcVertex vertex) {
        PrintWriter objWriter = context.getObjWriter();
        objWriter.write("vn ");
        objWriter.write(String.valueOf(vertex.getNormalX()));
        objWriter.write(" ");
        objWriter.write(String.valueOf(vertex.getNormalY()));
        objWriter.write(" ");
        objWriter.write(String.valueOf(vertex.getNormalZ()));
        objWriter.write(Constants.NEWLINE);
    }

    private static void writeTexCoord(ObjContext context, kcVertex vertex, boolean secondTex) {
        PrintWriter objWriter = context.getObjWriter();
        objWriter.write("vt ");
        objWriter.write(String.valueOf(secondTex ? vertex.getU1() : vertex.getU0()));
        objWriter.write(" ");
        objWriter.write(String.valueOf(secondTex ? vertex.getV1() : vertex.getV0()));
        objWriter.write(Constants.NEWLINE);
    }

    @RequiredArgsConstructor
    private static class ObjContext {
        @Getter private final kcModel model;
        @Getter private final PrintWriter objWriter;
        @Getter private final PrintWriter mtlWriter;
        public boolean hasNormals;
        public boolean hasTexCoords;
        public int baseVertex = 1;
        public int baseNormal = 1;
        public int baseTexCoord = 1;
    }
}
