package net.highwayfrogs.editor.utils;

import lombok.Cleanup;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyFlat;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyGouraud;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyTexture;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.mof.MOFPartcel;
import net.highwayfrogs.editor.file.mof.prims.MOFPolyTexture;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.gui.SelectionMenu;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.blocks.MMTriangleNormalsBlock;
import net.highwayfrogs.editor.system.mm3d.blocks.MMVerticeBlock;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Contains static 3d file utilities.
 * Created by Kneesnap on 2/28/2019.
 */
public class FileUtils3D {
    private static final String META_KEY_MOF_THEME = "frogMofTheme";

    /**
     * Export a MOF file to Wavefront OBJ.
     * Not the cleanest thing in the world, but it doesn't need to be.
     * @param staticMof The mof to export.
     * @param folder    The folder to export into.
     * @param vloTable  The image pack to use textures from.
     */
    @SneakyThrows
    public static void exportMofToObj(MOFFile staticMof, File folder, VLOArchive vloTable) {
        boolean exportTextures = vloTable != null;

        String mofName = staticMof.getFileEntry().getDisplayName();
        String cleanName = Utils.stripExtension(mofName);
        String mtlName = cleanName + ".mtl";
        @Cleanup PrintWriter objWriter = new PrintWriter(new File(folder, cleanName + ".obj"));

        objWriter.write("# FrogLord MOF Export" + Constants.NEWLINE);
        objWriter.write("# Exported: " + Calendar.getInstance().getTime().toString() + Constants.NEWLINE);
        objWriter.write("# MOF Name: " + mofName + Constants.NEWLINE);
        objWriter.write(Constants.NEWLINE);

        if (exportTextures) {
            objWriter.write("mtllib " + mtlName + Constants.NEWLINE);
            objWriter.write(Constants.NEWLINE);
        }

        // Write Vertices.
        int partCount = 0;
        int verticeStart = 0;
        for (MOFPart part : staticMof.getParts()) {
            part.setTempVertexStart(verticeStart);
            objWriter.write("# Part " + (partCount++) + ":" + Constants.NEWLINE);
            MOFPartcel partcel = part.getStaticPartcel(); // 0 is the animation frame.
            verticeStart += partcel.getVertices().size();

            for (SVector vertex : partcel.getVertices())
                objWriter.write(vertex.toOBJString() + Constants.NEWLINE);
        }

        objWriter.write(Constants.NEWLINE);

        // Write Faces.
        List<MOFPolygon> allPolygons = new ArrayList<>();
        staticMof.getParts().forEach(part -> part.getMofPolygons().values().forEach(allPolygons::addAll));

        // Register textures.
        if (exportTextures) {
            allPolygons.sort(Comparator.comparingInt(MOFPolygon::getOrderId));
            objWriter.write("# Vertex Textures" + Constants.NEWLINE);

            for (MOFPolygon poly : allPolygons) {
                if (poly instanceof MOFPolyTexture) {
                    MOFPolyTexture mofTex = (MOFPolyTexture) poly;
                    for (int i = mofTex.getUvs().length - 1; i >= 0; i--)
                        objWriter.write(mofTex.getObjUVString(i) + Constants.NEWLINE);
                }
            }
        }

        objWriter.write("# Faces" + Constants.NEWLINE);

        AtomicInteger textureId = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger counter = new AtomicInteger();

        Map<Integer, GameImage> textureMap = new HashMap<>();
        List<PSXColorVector> faceColors = new ArrayList<>();
        Map<PSXColorVector, List<MOFPolygon>> facesWithColors = new HashMap<>();

        allPolygons.forEach(polygon -> {
            if (!(polygon instanceof MOFPolyTexture)) {
                PSXColorVector color = polygon.getColor();
                if (!faceColors.contains(color))
                    faceColors.add(color);
                facesWithColors.computeIfAbsent(color, key -> new ArrayList<>()).add(polygon);
            } else {
                MOFPolyTexture texture = (MOFPolyTexture) polygon;

                if (exportTextures) {
                    int newTextureId = texture.getImageId();

                    GameImage image = textureMap.computeIfAbsent(newTextureId, key ->
                            vloTable.getImageByTextureId(texture.getImageId()));
                    newTextureId = image.getTextureId();

                    if (newTextureId != textureId.get()) { // It's time to change the texture.
                        textureId.set(newTextureId);
                        objWriter.write(Constants.NEWLINE);
                        objWriter.write("usemtl tex" + newTextureId + Constants.NEWLINE);
                    }
                }

                objWriter.write(polygon.toObjFaceCommand(exportTextures, counter) + Constants.NEWLINE);
            }
        });

        objWriter.append(Constants.NEWLINE);
        objWriter.append("# Faces without textures.").append(Constants.NEWLINE);
        for (Entry<PSXColorVector, List<MOFPolygon>> mapEntry : facesWithColors.entrySet()) {
            objWriter.write("usemtl color" + faceColors.indexOf(mapEntry.getKey()) + Constants.NEWLINE);
            mapEntry.getValue().forEach(poly -> objWriter.write(poly.toObjFaceCommand(exportTextures, null) + Constants.NEWLINE));
        }


        // Write MTL file.
        if (exportTextures) {
            @Cleanup PrintWriter mtlWriter = new PrintWriter(new File(folder, mtlName));

            for (GameImage image : textureMap.values()) {
                mtlWriter.write("newmtl tex" + image.getTextureId() + Constants.NEWLINE);
                mtlWriter.write("Kd 1 1 1" + Constants.NEWLINE); // Diffuse color.
                // "d 0.75" = Partially transparent, if we want to support this later.
                mtlWriter.write("map_Kd " + vloTable.getImages().indexOf(image) + ".png" + Constants.NEWLINE);
                mtlWriter.write(Constants.NEWLINE);
            }

            for (int i = 0; i < faceColors.size(); i++) {
                PSXColorVector color = faceColors.get(i);
                mtlWriter.write("newmtl color" + i + Constants.NEWLINE);
                if (i == 0)
                    mtlWriter.write("d 1" + Constants.NEWLINE); // All further textures should be completely solid.
                mtlWriter.write("Kd " + Utils.unsignedByteToFloat(color.getRed()) + " " + Utils.unsignedByteToFloat(color.getGreen()) + " " + Utils.unsignedByteToFloat(color.getBlue()) + Constants.NEWLINE); // Diffuse color.
                mtlWriter.write(Constants.NEWLINE);
            }
        }

        System.out.println(mofName + " Exported.");
    }

    /**
     * Exports a map file to wavefront obj.
     * It's a little nasty, but it works.
     * @param map       The map to export.
     * @param directory The directory to export it to.
     */
    @SneakyThrows
    public static void exportMapToObj(MAPFile map, File directory) {
        FileEntry entry = map.getFileEntry();
        VLOArchive vloArchive = map.getVlo();
        List<Short> remapTable = map.isQB() ? Collections.emptyList() : map.getConfig().getRemapTable(entry);
        String cleanName = Utils.getRawFileName(entry.getDisplayName());
        boolean exportTextures = vloArchive != null;

        System.out.println("Exporting " + cleanName + ".");

        String mtlName = cleanName + ".mtl";
        @Cleanup PrintWriter objWriter = new PrintWriter(new File(directory, cleanName + ".obj"));

        objWriter.write("# FrogLord Map Export" + Constants.NEWLINE);
        objWriter.write("# Exported: " + Calendar.getInstance().getTime().toString() + Constants.NEWLINE);
        objWriter.write("# Map Name: " + entry.getDisplayName() + Constants.NEWLINE);
        objWriter.write(Constants.NEWLINE);

        if (exportTextures) {
            objWriter.write("mtllib " + mtlName + Constants.NEWLINE);
            objWriter.write(Constants.NEWLINE);
        }

        // Write Vertexes.
        map.getVertexes().forEach(vertex -> objWriter.write(vertex.toOBJString() + Constants.NEWLINE));
        objWriter.write(Constants.NEWLINE);

        // Write Faces.
        List<MAPPolygon> allPolygons = new ArrayList<>();
        map.forEachPrimitive(prim -> {
            if (prim instanceof MAPPolygon)
                allPolygons.add((MAPPolygon) prim);
        });

        // Register textures.
        if (exportTextures) {
            allPolygons.sort(Comparator.comparingInt(MAPPolygon::getOrderId));
            objWriter.write("# Vertex Textures" + Constants.NEWLINE);

            for (MAPPolygon poly : allPolygons) {
                if (poly instanceof MAPPolyTexture) {
                    MAPPolyTexture polyTex = (MAPPolyTexture) poly;
                    for (int i = polyTex.getUvs().length - 1; i >= 0; i--)
                        objWriter.write(polyTex.getObjUVString(i) + Constants.NEWLINE);
                }
            }

            objWriter.write(Constants.NEWLINE);
        }

        objWriter.write("# Faces" + Constants.NEWLINE);

        AtomicInteger textureId = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger counter = new AtomicInteger();

        Map<Integer, GameImage> textureMap = new HashMap<>();
        List<PSXColorVector> faceColors = new ArrayList<>();
        Map<PSXColorVector, List<MAPPolygon>> facesWithColors = new HashMap<>();

        allPolygons.forEach(polygon -> {
            if (polygon instanceof MAPPolyTexture) {
                MAPPolyTexture texture = (MAPPolyTexture) polygon;

                if (exportTextures) {
                    int newTextureId = texture.getTextureId();

                    if (remapTable != null) { // Apply remap.
                        GameImage image = textureMap.computeIfAbsent(newTextureId, key -> vloArchive.getImageByTextureId(remapTable.get(key)));
                        newTextureId = image.getTextureId();
                    }

                    if (newTextureId != textureId.get()) { // It's time to change the texture.
                        textureId.set(newTextureId);
                        objWriter.write(Constants.NEWLINE);
                        objWriter.write("usemtl tex" + newTextureId + Constants.NEWLINE);
                    }
                }

                objWriter.write(polygon.toObjFaceCommand(exportTextures, counter) + Constants.NEWLINE);
            } else {
                PSXColorVector color = (polygon instanceof MAPPolyFlat) ? ((MAPPolyFlat) polygon).getColor() : ((MAPPolyGouraud) polygon).getColors()[0];
                if (!faceColors.contains(color))
                    faceColors.add(color);
                facesWithColors.computeIfAbsent(color, key -> new ArrayList<>()).add(polygon);
            }
        });

        objWriter.append(Constants.NEWLINE);
        objWriter.append("# Faces without textures.").append(Constants.NEWLINE);
        for (Entry<PSXColorVector, List<MAPPolygon>> mapEntry : facesWithColors.entrySet()) {
            objWriter.write("usemtl color" + faceColors.indexOf(mapEntry.getKey()) + Constants.NEWLINE);
            mapEntry.getValue().forEach(poly -> objWriter.write(poly.toObjFaceCommand(exportTextures, null) + Constants.NEWLINE));
        }


        // Write MTL file.
        if (exportTextures) {
            @Cleanup PrintWriter mtlWriter = new PrintWriter(new File(directory, mtlName));

            for (GameImage image : textureMap.values()) {
                mtlWriter.write("newmtl tex" + image.getTextureId() + Constants.NEWLINE);
                mtlWriter.write("Kd 1 1 1" + Constants.NEWLINE); // Diffuse color.
                // "d 0.75" = Partially transparent, if we want to support this later.
                mtlWriter.write("map_Kd " + vloArchive.getImages().indexOf(image) + ".png" + Constants.NEWLINE);
                mtlWriter.write(Constants.NEWLINE);
            }

            for (int i = 0; i < faceColors.size(); i++) {
                PSXColorVector color = faceColors.get(i);
                mtlWriter.write("newmtl color" + i + Constants.NEWLINE);
                if (i == 0)
                    mtlWriter.write("d 1" + Constants.NEWLINE); // All further textures should be completely solid.
                mtlWriter.write("Kd " + Utils.unsignedByteToFloat(color.getRed()) + " " + Utils.unsignedByteToFloat(color.getGreen()) + " " + Utils.unsignedByteToFloat(color.getBlue()) + Constants.NEWLINE); // Diffuse color.
                mtlWriter.write(Constants.NEWLINE);
            }
        }

        System.out.println("Export complete.");
    }

    /**
     * Convert a MOF to a MisfitModel3D.
     * TODO: Faces
     * TODO: Support textures. (and uvs)
     * TODO: Support normals.
     * TODO: Support texture animations.
     * TODO: Support lighting.
     * TODO: Support bounding box.
     * TODO: Support flipbook animation.
     * TODO: Other missing things like collprim and matrix.
     * @param holder The mof to convert.
     * @return misfit3d
     */
    public static MisfitModel3DObject convertMofToMisfitModel(MOFHolder holder) {
        MisfitModel3DObject model = new MisfitModel3DObject();
        MOFFile staticMof = holder.asStaticFile();
        VLOArchive vloTable = holder.getVloFile();
        Utils.verify(vloTable != null, "Unknown VLO Table for %s!", holder.getFileEntry().getDisplayName());

        // Add Vertices.
        for (MOFPart part : staticMof.getParts()) {
            MOFPartcel partcel = part.getStaticPartcel();
            for (SVector vertex : partcel.getVertices()) {
                MMVerticeBlock mmVertice = model.getVertices().addNewElement();
                mmVertice.setFlags(MMVerticeBlock.FLAG_FREE_VERTEX);
                mmVertice.setX(vertex.getExportFloatX());
                mmVertice.setY(vertex.getExportFloatY());
                mmVertice.setZ(vertex.getExportFloatZ());
            }
        }

        // Add Faces.
        staticMof.forEachPolygon(model.getTriangleFaces()::addMofPolygon);

        // Add normals.
        for (int i = 0; i < model.getTriangleFaces().size(); i++) {
            MMTriangleNormalsBlock normal = model.getNormals().addNewElement();
            normal.setIndex(i);
        }

        /* UVs
        for (MOFPolygon poly : allPolygons) {
            if (poly instanceof MOFPolyTexture) {
                MOFPolyTexture mofTex = (MOFPolyTexture) poly;
                for (int i = mofTex.getUvs().length - 1; i >= 0; i--)
                    objWriter.write(mofTex.getObjUVString(i) + Constants.NEWLINE);
            }
        }
        */


        return model;
    }

    /**
     * Load a MOF from a model.
     * @param model The model to load from.
     */
    public static void importMofFromModel(FroggerEXEInfo config, MisfitModel3DObject model, Consumer<MOFHolder> handler) {
        String themeValue = model.getMetadata(META_KEY_MOF_THEME);
        if (themeValue != null) {
            handler.accept(importMofFromModel(model, MAPTheme.valueOf(themeValue)));
            return;
        }

        SelectionMenu.promptThemeSelection(config, theme -> handler.accept(importMofFromModel(model, theme)), true);
    }

    /**
     * Load a MOF from a model.
     * @param model The model to load from.
     * @return importedMof
     */
    public static MOFHolder importMofFromModel(MisfitModel3DObject model, MAPTheme mofTheme) {
        MOFHolder holder = new MOFHolder(mofTheme, null);
        //TODO: Import data.
        return holder;
    }
}
