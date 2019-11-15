package net.highwayfrogs.editor.utils;

import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Getter;
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
import net.highwayfrogs.editor.file.mof.animation.MOFAnimation;
import net.highwayfrogs.editor.file.mof.animation.MOFAnimationCels;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformObject;
import net.highwayfrogs.editor.file.mof.flipbook.MOFFlipbookAction;
import net.highwayfrogs.editor.file.mof.prims.MOFPolyTexture;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.gui.SelectionMenu;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.blocks.*;
import net.highwayfrogs.editor.system.mm3d.blocks.MMFrameAnimationsBlock.MMAnimationFrame;
import net.highwayfrogs.editor.system.mm3d.blocks.MMFrameAnimationsBlock.MMFloatVertex;

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
                    GameImage image = textureMap.computeIfAbsent((int) texture.getImageId(), vloTable::getImageByTextureId);
                    int newTextureId = image.getTextureId();

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
                mtlWriter.write("map_Kd " + image.getLocalImageID() + ".png" + Constants.NEWLINE);
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
        if (directory == null)
            return;

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
                mtlWriter.write("map_Kd " + image.getLocalImageID() + ".png" + Constants.NEWLINE);
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
     *
     * mm3d does not support:
     *  - Texture animations. (Flipbook)
     *  - Lighting.
     *  - Quads
     *
     * @param holder The mof to convert.
     * @return misfit3d
     */
    public static MisfitModel3DObject convertMofToMisfitModel(MOFHolder holder) {
        MisfitModel3DObject model = new MisfitModel3DObject();
        MOFFile staticMof = holder.asStaticFile();
        VLOArchive vloTable = holder.getVloFile();
        Utils.verify(vloTable != null, "Unknown VLO Table for %s!", holder.getFileEntry().getDisplayName());

        // Add Vertices.
        int verticeStart = 0;
        for (MOFPart part : staticMof.getParts()) {
            part.setTempVertexStart(verticeStart);
            MOFPartcel partcel = part.getStaticPartcel();
            verticeStart += partcel.getVertices().size();

            for (SVector vertex : partcel.getVertices()) {
                MMVerticeBlock mmVertice = model.getVertices().addNewElement();
                mmVertice.setFlags(MMVerticeBlock.FLAG_FREE_VERTEX);
                mmVertice.setX(vertex.getExportFloatX());
                mmVertice.setY(vertex.getExportFloatY());
                mmVertice.setZ(vertex.getExportFloatZ());
            }
        }

        // Add Flipbook animations.
        for (MOFPart part : staticMof.getParts()) {
            if (part.getFlipbook() == null)
                continue;

            for (int action = 0; action < part.getFlipbook().getActions().size(); action++) {
                MMFrameAnimationsBlock animation = model.getFrameAnimations().getBody(action);
                if (animation == null) {
                    animation = model.getFrameAnimations().addNewElement();
                    animation.setFramesPerSecond(20);
                    animation.setName(holder.getName(action));
                }

                MOFFlipbookAction flipbookAction = part.getFlipbook().getAction(action);
                for (int frame = 0; frame < flipbookAction.getFrameCount(); frame++) {
                    MOFPartcel partcel = part.getCel(action, frame);

                    if (frame >= animation.getFrames().size())
                        animation.getFrames().add(new MMAnimationFrame());

                    MMAnimationFrame animFrame = animation.getFrames().get(frame);
                    partcel.getVertices().forEach(vertice -> animFrame.getVertexPositions().add(new MMFloatVertex(vertice)));
                }
            }
        }

        // Add XAR animations.
        if (holder.isAnimatedMOF()) { // Maybe in the future we can use frame animation points, instead of using the final vertice location.
            MOFAnimation animatedMof = holder.getAnimatedFile();

            for (MOFPart part : staticMof.getParts()) {
                for (int action = 0; action < holder.getMaxAnimation(); action++) {
                    MMFrameAnimationsBlock animation = model.getFrameAnimations().getBody(action);
                    if (animation == null) {
                        animation = model.getFrameAnimations().addNewElement();
                        animation.setFramesPerSecond(20);
                        animation.setName(holder.getName(action));
                    }

                    MOFAnimationCels celAnimation = animatedMof.getAnimationById(action);
                    for (int frame = 0; frame < celAnimation.getFrameCount(); frame++) {
                        MOFPartcel partcel = part.getCel(action, frame);

                        if (frame >= animation.getFrames().size())
                            animation.getFrames().add(new MMAnimationFrame());

                        MMAnimationFrame animFrame = animation.getFrames().get(frame);
                        TransformObject transform = animatedMof.getTransform(part, action, frame);
                        partcel.getVertices().forEach(vertex ->
                                animFrame.getVertexPositions().add(new MMFloatVertex(PSXMatrix.MRApplyMatrix(transform.calculatePartTransform(), vertex, new IVector()))));
                    }
                }
            }
        }

        // Add Faces and Textures.
        Map<Short, MOFTextureData> dataMap = new HashMap<>();
        Map<PSXColorVector, MOFTextureData> colorMap = new HashMap<>();
        staticMof.forEachPolygon(poly -> {
            long faceIndex = model.getTriangleFaces().size();
            model.getTriangleFaces().addMofPolygon(poly);

            if (poly instanceof MOFPolyTexture) {
                MOFPolyTexture polyTex = (MOFPolyTexture) poly;

                MOFTextureData data = dataMap.computeIfAbsent(polyTex.getImageId(), key -> {
                    GameImage image = vloTable.getImageByTextureId(key);
                    int localId = image.getLocalImageID();
                    int texId = image.getTextureId();

                    // Create material.
                    int externalTextureId = model.getExternalTextures().size();
                    model.getExternalTextures().addTexture(localId + ".png"); // Add external texture.

                    int materialId = model.getMaterials().size();
                    MMMaterialsBlock material = model.getMaterials().addNewElement();
                    material.setTexture(externalTextureId);
                    material.setName("mat" + texId);

                    // Create new group.
                    MMTriangleGroupsBlock group = model.getGroups().addNewElement();
                    group.setName("group" + texId);
                    group.setMaterial(materialId);
                    return new MOFTextureData(image, externalTextureId, group, material);
                });

                model.getTextureCoordinates().addMofPolygon(faceIndex, polyTex); // Add UVs.

                // Link faces to texture group.
                List<Long> triangleIndices = data.getGroup().getTriangleIndices();
                triangleIndices.add(faceIndex);
                if (poly.isQuadFace())
                    triangleIndices.add(faceIndex + 1);
            } else {
                MOFTextureData colorData = colorMap.computeIfAbsent(poly.getColor(), colorKey -> {
                    // Create material.
                    int materialId = model.getMaterials().size();
                    MMMaterialsBlock material = model.getMaterials().addNewElement();
                    material.setFlags(MMMaterialsBlock.FLAG_NO_TEXTURE);
                    material.setName("colorMaterial" + materialId);
                    material.getDiffuse()[0] = Utils.unsignedByteToFloat(colorKey.getRed());
                    material.getDiffuse()[1] = Utils.unsignedByteToFloat(colorKey.getGreen());
                    material.getDiffuse()[2] = Utils.unsignedByteToFloat(colorKey.getBlue());

                    // Create new group.
                    MMTriangleGroupsBlock group = model.getGroups().addNewElement();
                    group.setName("colorGroup" + colorKey.toRGB());
                    group.setMaterial(materialId);
                    return new MOFTextureData(null, MMTriangleGroupsBlock.EMPTY_MATERIAL, group, material);
                });

                // Link faces to texture group.
                List<Long> triangleIndices = colorData.getGroup().getTriangleIndices();
                triangleIndices.add(faceIndex);
                if (poly.isQuadFace())
                    triangleIndices.add(faceIndex + 1);
            }
        });

        // Add normals.
        for (int i = 0; i < model.getTriangleFaces().size(); i++) {
            MMTriangleNormalsBlock normal = model.getNormals().addNewElement();
            normal.setIndex(i);
            //TODO: Add normal data.
        }

        return model;
    }

    @Getter
    @AllArgsConstructor
    private static final class MOFTextureData {
        private GameImage image;
        private long externalTextureIndex;
        private MMTriangleGroupsBlock group;
        private MMMaterialsBlock material;
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
