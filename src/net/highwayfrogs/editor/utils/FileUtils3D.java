package net.highwayfrogs.editor.utils;

import javafx.scene.control.Alert.AlertType;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyFlat;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyGouraud;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyTexture;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.ShaderMode;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureTreeNode;
import net.highwayfrogs.editor.file.mof.*;
import net.highwayfrogs.editor.file.mof.animation.MOFAnimation;
import net.highwayfrogs.editor.file.mof.animation.MOFAnimationCels;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformObject;
import net.highwayfrogs.editor.file.mof.flipbook.MOFFlipbook;
import net.highwayfrogs.editor.file.mof.flipbook.MOFFlipbookAction;
import net.highwayfrogs.editor.file.mof.hilite.MOFHilite;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnim;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnimEntryList;
import net.highwayfrogs.editor.file.mof.prims.MOFPolyTexture;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.mof.prims.MOFPrimType;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.blocks.*;
import net.highwayfrogs.editor.system.mm3d.blocks.MMFrameAnimationsBlock.MMAnimationFrame;
import net.highwayfrogs.editor.system.mm3d.blocks.MMFrameAnimationsBlock.MMFloatVertex;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contains static 3d file utilities.
 * Created by Kneesnap on 2/28/2019.
 */
public class FileUtils3D {
    private static final String MATERIAL_NAME_PREFIX = "tex";

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
                    GameImage image = textureMap.computeIfAbsent((int) texture.getImageId(), polygon.getMWD()::getImageByTextureId);
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
        TextureMap textureMap = TextureMap.newTextureMap(map, ShaderMode.NO_SHADING);
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

        List<MAPPolygon> allPolygons = map.getAllPolygonsSafe();
        if (exportTextures)
            allPolygons.sort(Comparator.comparingInt(MAPPolygon::getOrderId)); // Groups textures together. Must happen before any other use of this.

        // Write Vertices.
        for (MAPPolygon poly : allPolygons) {
            for (int i = 0; i < poly.getVerticeCount(); i++) {
                SVector vtx = map.getVertexes().get(poly.getVertices()[poly.getVerticeCount() - i - 1]);
                String base = vtx.toOBJString();

                if (poly instanceof MAPPolyTexture) {
                    MAPPolyTexture polyTex = (MAPPolyTexture) poly;

                    int colorIndex = poly.getVerticeCount() - i - 1;
                    if (polyTex.getColors().length == 1) {
                        colorIndex = 0;
                    } else if (colorIndex == 3) {
                        colorIndex = 2;
                    } else if (colorIndex == 2 && poly.isQuadFace()) {
                        colorIndex = 3;
                    }

                    PSXColorVector color = polyTex.getColors()[colorIndex];
                    base += " " + (color.getRed() / 127D) + " " + (color.getGreen() / 127D) + " " + (color.getBlue() / 127D);
                }

                objWriter.write(base + Constants.NEWLINE);
            }
        }
        objWriter.write(Constants.NEWLINE);

        // Register textures.
        if (exportTextures) {
            objWriter.write("# Vertex Textures" + Constants.NEWLINE);

            DecimalFormat df = new DecimalFormat("#");
            df.setMaximumFractionDigits(8);

            for (MAPPolygon poly : allPolygons) {
                if (poly instanceof MAPPolyTexture) {
                    MAPPolyTexture polyTex = (MAPPolyTexture) poly;

                    TextureTreeNode node = polyTex.getTreeNode(textureMap);

                    for (int i = polyTex.getUvs().length - 1; i >= 0; i--) {
                        ByteUV uv = polyTex.getUvs()[i];
                        float u = uv.getFloatU();
                        float v = uv.getFloatV();

                        float fullU = (node.getMaxU() - node.getMinU());
                        float fullV = (node.getMaxV() - node.getMinV());

                        objWriter.write("vt " + df.format(node.getMinU() + (u * fullU)) + " " + df.format(1F - (node.getMinV() + (v * fullV))) + Constants.NEWLINE);
                    }
                } else if (poly instanceof MAPPolyGouraud || poly instanceof MAPPolyFlat) {
                    TextureTreeNode node = poly.getTreeNode(textureMap);
                    if (poly.isQuadFace()) {
                        objWriter.write("vt " + node.getMaxU() + " " + (1F - node.getMinV()) + Constants.NEWLINE); // TR
                        objWriter.write("vt " + node.getMaxU() + " " + (1F - node.getMaxV()) + Constants.NEWLINE); // TL
                        objWriter.write("vt " + node.getMinU() + " " + (1F - node.getMaxV()) + Constants.NEWLINE); // BR
                        objWriter.write("vt " + node.getMinU() + " " + (1F - node.getMinV()) + Constants.NEWLINE); // BL
                    } else {
                        objWriter.write("vt " + node.getMaxU() + " " + (1F - node.getMinV()) + Constants.NEWLINE); //BR
                        objWriter.write("vt " + node.getMinU() + " " + (1F - node.getMaxV()) + Constants.NEWLINE); //TL
                        objWriter.write("vt " + node.getMinU() + " " + (1F - node.getMinV()) + Constants.NEWLINE); //BL
                    }
                } else {
                    throw new RuntimeException("Don't know how to handle a " + poly + " poly.");
                }
            }

            objWriter.write(Constants.NEWLINE);
        }

        // Write Faces.
        objWriter.write("# Faces" + Constants.NEWLINE);
        if (exportTextures)
            objWriter.write("usemtl master" + Constants.NEWLINE);

        int count = 1;
        for (int i = 0; i < allPolygons.size(); i++) {
            MAPPolygon mapPoly = allPolygons.get(i);

            StringBuilder builder = new StringBuilder("f");
            for (int j = 0; j < mapPoly.getVerticeCount(); j++, count++) {
                builder.append(" ").append(count);
                if (exportTextures)
                    builder.append("/").append(count);
            }
            objWriter.write(builder.toString() + Constants.NEWLINE);
        }

        // Write MTL file and textures.
        if (exportTextures) {
            @Cleanup PrintWriter mtlWriter = new PrintWriter(new File(directory, mtlName));
            mtlWriter.write("newmtl master" + Constants.NEWLINE);
            mtlWriter.write("Kd 1 1 1" + Constants.NEWLINE); // Diffuse color.
            // "d 0.75" = Partially transparent, if we want to support this later.
            mtlWriter.write("map_Kd " + cleanName + ".png" + Constants.NEWLINE);
            mtlWriter.write(Constants.NEWLINE);
            mtlWriter.close();

            ImageIO.write(textureMap.getTextureTree().getImage(), "png", new File(directory, cleanName + ".png"));
        }

        System.out.println("Export complete.");
    }

    /**
     * Convert a MOF to a MisfitModel3D.
     * mm3d does not support:
     * - Texture animations. (Flipbook)
     * - Quads
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
                    animation.setFramesPerSecond(holder.getMWD().getFPS());
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
                        animation.setFramesPerSecond(holder.getMWD().getFPS());
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
        List<MOFPolygon> polygonList = staticMof.getAllPolygons();
        for (MOFPolygon poly : polygonList) {
            long faceIndex = model.getTriangleFaces().size();
            model.getTriangleFaces().addMofPolygon(poly);

            MOFTextureData data;
            if (poly instanceof MOFPolyTexture) {
                MOFPolyTexture polyTex = (MOFPolyTexture) poly;

                data = dataMap.computeIfAbsent(polyTex.getImageId(), key -> {
                    GameImage image = vloTable.getImageByTextureId(key);
                    int localId = image.getLocalImageID();
                    int texId = image.getTextureId();

                    // Create material.
                    int externalTextureId = model.getExternalTextures().size();
                    model.getExternalTextures().addTexture(localId + ".png"); // Add external texture.

                    int materialId = model.getMaterials().size();
                    MMMaterialsBlock material = model.getMaterials().addNewElement();
                    material.setTexture(externalTextureId);
                    material.setName(MATERIAL_NAME_PREFIX + texId);

                    // Create new group.
                    MMTriangleGroupsBlock group = model.getGroups().addNewElement();
                    group.setName("texGroup" + texId);
                    group.setMaterial(materialId);
                    return new MOFTextureData(image, externalTextureId, group, material);
                });

                model.getTextureCoordinates().addMofPolygon(faceIndex, polyTex); // Add UVs.
            } else {
                data = colorMap.computeIfAbsent(poly.getColor(), colorKey -> {
                    // Create material.
                    int materialId = model.getMaterials().size();
                    MMMaterialsBlock material = model.getMaterials().addNewElement();
                    material.setFlags(MMMaterialsBlock.FLAG_NO_TEXTURE);
                    material.setName("color" + materialId);
                    material.getDiffuse()[0] = Utils.unsignedByteToFloat(colorKey.getRed());
                    material.getDiffuse()[1] = Utils.unsignedByteToFloat(colorKey.getGreen());
                    material.getDiffuse()[2] = Utils.unsignedByteToFloat(colorKey.getBlue());

                    // Create new group.
                    MMTriangleGroupsBlock group = model.getGroups().addNewElement();
                    group.setName("colorGroup" + colorKey.toRGB());
                    group.setMaterial(materialId);
                    return new MOFTextureData(null, MMTriangleGroupsBlock.EMPTY_MATERIAL, group, material);
                });
            }

            // Link faces to texture group.
            List<Long> triangleIndices = data.getGroup().getTriangleIndices();
            triangleIndices.add(faceIndex);
            if (poly.isQuadFace())
                triangleIndices.add(faceIndex + 1);

            // Add normals, if there are any present.
            if (poly.getNormals().length > 0) {
                MOFPartcel partcel = poly.getParentPart().getStaticPartcel();
                MMTriangleNormalsBlock normal1 = model.getNormals().addNewElement();
                normal1.setIndex(faceIndex);

                if (poly.getNormals().length == 1) {
                    SVector normal = partcel.getNormals().get(poly.getNormals()[0]);
                    normal1.setV1Normals(vecToFloat(normal));
                    normal1.setV2Normals(vecToFloat(normal));
                    normal1.setV3Normals(vecToFloat(normal));

                    if (poly.isQuadFace()) {
                        MMTriangleNormalsBlock normal2 = model.getNormals().addNewElement();
                        normal2.setIndex(faceIndex + 1);
                        normal2.setV1Normals(vecToFloat(normal));
                        normal2.setV2Normals(vecToFloat(normal));
                        normal2.setV3Normals(vecToFloat(normal));
                    }
                } else if (poly.getNormals().length == 3) {
                    normal1.setV1Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[0])));
                    normal1.setV2Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[1])));
                    normal1.setV3Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[2])));
                } else if (poly.getNormals().length == 4) {
                    normal1.setV1Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[0])));
                    normal1.setV2Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[3])));
                    normal1.setV3Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[1])));

                    MMTriangleNormalsBlock normal2 = model.getNormals().addNewElement();
                    normal2.setIndex(faceIndex + 1);
                    normal2.setV1Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[1])));
                    normal2.setV2Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[3])));
                    normal2.setV3Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[2])));
                }
            }
        }

        return model;
    }

    private static float[] vecToFloat(SVector vec) {
        return new float[]{vec.getExportFloatX(), vec.getExportFloatY(), vec.getExportFloatZ()};
    }

    private static SVector floatToVec(float[] floatArray) {
        return new SVector(-floatArray[0], -floatArray[1], floatArray[2]);
    }

    @Getter
    @AllArgsConstructor
    private static final class MOFTextureData {
        private GameImage image;
        private long externalTextureIndex;
        private MMTriangleGroupsBlock group;
        private MMMaterialsBlock material;
    }

    // Other TODOs:
    // TODO: Import textures from imported models. [Requires a system to automatically put textures in vram safely. Also requires FrogLord to be able to handle multiple images with the same id.]

    /**
     * Load a MOF from a model.
     * Due to limits of the mm3d file format, we are unable to generate a MOFAnimation, however it should be able to make a MOFFile just fine.
     * @param model  The model to load from.
     * @param holder The data to overwrite.
     */
    public static void importMofFromModel(MisfitModel3DObject model, MOFHolder holder) {
        if (holder.isIncomplete()) {
            Utils.makePopUp("Importing over incomplete mofs is not currently supported.", AlertType.WARNING);
            return;
        }

        boolean isUnsafeReplacementArea = (holder.getTheme() == null || holder.getTheme() == MAPTheme.GENERAL);
        if (isUnsafeReplacementArea && holder.isAnimatedMOF()) { // Unfortunately, the game assumes these models are animated, and it will crap itself if we try to give it a static model.
            Utils.makePopUp("This XAR model cannot be overwritten by a mm3d model.", AlertType.ERROR);

            // TODO: For supporting parts.
            //  - Is it possible to have the same triangle in multiple groups? Maybe we could have groups for parts, parallel to textures.
            //  - For keeping XAR animation, we can use the whole skeletal animation system.
            //  - Yeah, this can be done.
            //  - Though, we'll need to fix the rest of the method to not work under the assumption that there's a single static part.

            return;
        }

        MOFFile staticMof = holder.isDummy() ? new MOFFile(holder) : holder.asStaticFile();
        holder.setStaticFile(staticMof);
        holder.setAnimatedFile(null);
        holder.setDummy(false);

        // Build Parts.
        List<MOFPart> oldParts = new ArrayList<>(staticMof.getParts());
        staticMof.getParts().clear();

        // Unfortunately, we can't actually discern part information, so we have to create it all under a single part.
        // I'd imagine this would cause lag if you tried to run this on hardware of the time, especially combined with the whole part thing.
        // However, since our target is the PC version on modern PCs, this is an acceptable solution for now.
        // If we make a blender plugin for editing models, that would also be an ample solution.

        MOFPart part = new MOFPart(staticMof);
        staticMof.getParts().add(part);

        // Build Polygon Data:

        MOFPartcel basePartcel = new MOFPartcel(part, 0, 0);
        Map<SVector, Short> baseNormalIndices = new HashMap<>();

        // Load Vertices.
        for (MMVerticeBlock block : model.getVertices().getDataBlockBodies())
            basePartcel.getVertices().add(new SVector(-block.getX(), -block.getY(), block.getZ()));

        // Load Normals.
        for (MMTriangleNormalsBlock block : model.getNormals().getDataBlockBodies()) {
            SVector n1 = new SVector(-block.getV1Normals()[0], -block.getV1Normals()[1], block.getV1Normals()[2]);
            SVector n2 = new SVector(-block.getV2Normals()[0], -block.getV2Normals()[1], block.getV2Normals()[2]);
            SVector n3 = new SVector(-block.getV3Normals()[0], -block.getV3Normals()[1], block.getV3Normals()[2]);

            short index = (short) basePartcel.getNormals().size();
            if (!baseNormalIndices.containsKey(n1)) {
                baseNormalIndices.put(n1, index++);
                basePartcel.getNormals().add(n1);
            }

            if (!baseNormalIndices.containsKey(n2)) {
                baseNormalIndices.put(n2, index++);
                basePartcel.getNormals().add(n2);
            }

            if (!baseNormalIndices.containsKey(n3)) {
                baseNormalIndices.put(n3, index);
                basePartcel.getNormals().add(n3);
            }
        }

        part.getPartcels().add(basePartcel);

        // Port data which isn't supposed to be overwritten by an import.

        if (oldParts.size() == 1 && oldParts.get(0).getMatrix() != null) // If there's more than one part, this would be a problem, since it would apply to parts it originally didn't apply to.
            part.setMatrix(oldParts.get(0).getMatrix());

        for (MOFPart oldPart : oldParts) {
            if (oldPart.getCollprim() != null) { // Transition collprim.
                MOFCollprim oldCollprim = oldPart.getCollprim();
                oldCollprim.setParent(part);
                part.setCollprim(oldCollprim);
            }

            for (MOFHilite oldHilite : oldPart.getHilites()) { // Transition Hilites.
                if (basePartcel.getVertices().contains(oldHilite.getVertex())) {
                    oldHilite.setParent(part);
                    part.getHilites().add(oldHilite);
                }
            }
        }

        // Add Flipbook animations.
        MOFFlipbook flipbook = new MOFFlipbook();
        for (MMFrameAnimationsBlock block : model.getFrameAnimations().getDataBlockBodies()) {
            int partcelIndex = part.getPartcels().size();

            for (MMAnimationFrame frame : block.getFrames()) {
                MOFPartcel newCel = new MOFPartcel(part, frame.getVertexPositions().size(), frame.getVertexPositions().size());
                for (MMFloatVertex vertex : frame.getVertexPositions())
                    newCel.getVertices().add(new SVector(-vertex.getX(), -vertex.getY(), vertex.getZ()));

                newCel.getNormals().addAll(basePartcel.getNormals()); //TODO: Calculate Normals instead. See if there's any easy way to do this. Do the partcels even have normals usually? As far as I'm aware, XAR animations don't use normals, so we should check that vertex normals are used as well. Check what XAR animation does, and if it's really unused.

                // Add new frame of animation.
                part.getPartcels().add(newCel);
            }

            flipbook.getActions().add(new MOFFlipbookAction(part.getPartcels().size() - partcelIndex, partcelIndex));
        }

        if (flipbook.getActions().size() > 0)
            part.setFlipbook(flipbook);

        // Load Polygons.
        Map<Integer, MMMaterialsBlock> materialMap = new HashMap<>();
        Map<Integer, MMTriangleNormalsBlock> normalMap = new HashMap<>();
        Map<Integer, MMTextureCoordinatesBlock> uvMap = new HashMap<>();

        // Build Maps.
        for (MMTextureCoordinatesBlock block : model.getTextureCoordinates().getDataBlockBodies())
            uvMap.put((int) block.getTriangle(), block);

        for (MMTriangleNormalsBlock block : model.getNormals().getDataBlockBodies())
            normalMap.put((int) block.getIndex(), block);

        for (MMTriangleGroupsBlock block : model.getGroups().getDataBlockBodies()) {
            MMMaterialsBlock material = block.getMaterial() != MMTriangleGroupsBlock.EMPTY_MATERIAL ? model.getMaterials().getBody((int) block.getMaterial()) : null;
            for (Long face : block.getTriangleIndices())
                materialMap.put((int) (long) face, material);
        }

        Set<Short> allowedTextureIds = new HashSet<>();
        for (VLOArchive vloArchive : holder.getMWD().getAllFiles(VLOArchive.class))
            for (GameImage gameImage : vloArchive.getImages())
                allowedTextureIds.add(gameImage.getTextureId());

        for (int i = 0; i < model.getTriangleFaces().size(); i++) {
            MMTriangleFaceBlock faceBlock = model.getTriangleFaces().getBody(i);
            MMMaterialsBlock material = materialMap.get(i);
            MMTriangleNormalsBlock normals = normalMap.get(i);
            MMTextureCoordinatesBlock texCoords = uvMap.get(i);

            MOFPart partOwner = part; //TODO: Get this from somewhere. If there's a joint, get it from that, otherwise use a static part.
            boolean isTextured = material != null && ((material.getFlags() & MMMaterialsBlock.FLAG_NO_TEXTURE) != MMMaterialsBlock.FLAG_NO_TEXTURE);
            boolean isGouraud = normals != null && Arrays.equals(normals.getV1Normals(), normals.getV2Normals()) && Arrays.equals(normals.getV1Normals(), normals.getV3Normals());

            // Build the polygon. (We always use Tris, since mm3d doesn't support quads.)
            MOFPrimType primType = isTextured
                    ? (isGouraud ? MOFPrimType.GT3 : MOFPrimType.FT3)
                    : (isGouraud ? MOFPrimType.G3 : MOFPrimType.F3);

            MOFPolygon poly = primType.makeNew(partOwner);
            if (isTextured && (poly instanceof MOFPolyTexture) && material.getName().startsWith(MATERIAL_NAME_PREFIX)) {
                MOFPolyTexture polyTex = (MOFPolyTexture) poly;
                String trailingNumber = material.getName().substring(MATERIAL_NAME_PREFIX.length()).split("-")[0];
                if (!Utils.isInteger(trailingNumber))
                    throw new RuntimeException("'" + trailingNumber + "' is not a numeric texture id. (Material Name)");

                int textureId = Integer.parseInt(trailingNumber);
                if (!allowedTextureIds.contains((short) textureId))
                    throw new RuntimeException(textureId + " does not map to real texture.");

                polyTex.setImageId((short) textureId);

                // Load UVs.
                if (texCoords != null)
                    for (int j = 0; j < polyTex.getUvs().length; j++)
                        polyTex.getUvs()[poly.getVerticeCount() - j - 1] = new ByteUV(texCoords.getXCoordinates()[j], texCoords.getYCoordinates()[j]);

                polyTex.getColor().fromRGB(0x7F7F7F); // TODO: Remember color. Can we use any material properties to make this work? If yes, we can have arbitrary suffix like tex123-color1 or tex123-user_message, otherwise it has to be tex123-7f7f7f. Another option is to analyze models and see if maybe it's not so unique, for instance if all colors are shared per texture, or something.
            }

            for (int j = 0; j < poly.getVerticeCount(); j++)
                poly.getVertices()[poly.getVerticeCount() - j - 1] = (int) faceBlock.getVertices()[j];

            if (normals != null) {
                if (poly.getNormals().length == 1) {
                    poly.getNormals()[0] = baseNormalIndices.get(floatToVec(normals.getV1Normals()));
                } else if (poly.getNormals().length == 3) {
                    poly.getNormals()[0] = baseNormalIndices.get(floatToVec(normals.getV1Normals()));
                    poly.getNormals()[1] = baseNormalIndices.get(floatToVec(normals.getV2Normals()));
                    poly.getNormals()[2] = baseNormalIndices.get(floatToVec(normals.getV3Normals()));
                }
            }

            if (!isTextured) { // Load the color.
                int red = (material != null ? ((int) (material.getDiffuse()[0] * 255)) & 0xFF : 255);
                int green = (material != null ? ((int) (material.getDiffuse()[1] * 255)) & 0xFF : 0);
                int blue = (material != null ? ((int) (material.getDiffuse()[2] * 255)) & 0xFF : 255);
                poly.getColor().fromRGB((red << 16) | (green << 8) | blue);
            }

            // Register the polygon.
            partOwner.getMofPolygons().computeIfAbsent(poly.getType(), key -> new ArrayList<>()).add(poly);
            partOwner.getOrderedByLoadPolygons().add(poly);
        }

        // Build Vertex Map.
        Map<SVector, Set<MOFPolygon>> polyMap = new HashMap<>();
        for (MOFPolygon poly : staticMof.getAllPolygons()) {
            MOFPartcel partCel = poly.getParentPart().getStaticPartcel();
            for (int i = 0; i < poly.getVerticeCount(); i++)
                polyMap.computeIfAbsent(partCel.getVertices().get(poly.getVertices()[i]), key -> new HashSet<>()).add(poly);
        }

        // Port Texture Animations to the new model.
        for (MOFPart oldPart : oldParts) {
            for (MOFPartPolyAnim partPolyAnim : oldPart.getPartPolyAnims()) {
                MOFPolygon oldPolygon = partPolyAnim.getMofPolygon();
                MOFPartcel oldPartcel = oldPart.getStaticPartcel();
                MOFPolygon newPolygon = getPolygonFromVertices(polyMap, oldPolygon, oldPartcel.getVertices().get(oldPolygon.getVertices()[0]), oldPartcel.getVertices().get(oldPolygon.getVertices()[1]), oldPartcel.getVertices().get(oldPolygon.getVertices()[oldPolygon.isQuadFace() ? 3 : 2]));
                if (newPolygon == null)
                    continue; // Couldn't find a new polygon which matched the old one, so skip.

                MOFPart polyPart = newPolygon.getParentPart();
                partPolyAnim.setPrimType(newPolygon.getType());
                partPolyAnim.setMofPolygon(newPolygon);
                partPolyAnim.setParentPart(polyPart);
                polyPart.getPartPolyAnims().add(partPolyAnim);

                MOFPartPolyAnimEntryList entryList = partPolyAnim.getEntryList();
                if (oldPolygon.isQuadFace() && !newPolygon.isQuadFace()) {
                    MOFPolygon otherNew = getPolygonFromVertices(polyMap, oldPolygon, oldPartcel.getVertices().get(oldPolygon.getVertices()[1]), oldPartcel.getVertices().get(oldPolygon.getVertices()[2]), oldPartcel.getVertices().get(oldPolygon.getVertices()[3]));
                    if (otherNew != null)
                        polyPart.getPartPolyAnims().add(new MOFPartPolyAnim(polyPart, otherNew, entryList));
                }

                if (entryList.getParent() != polyPart) { // Link the old entry list, unless it's already linked to the new part.
                    entryList.setParent(polyPart);
                    polyPart.getPartPolyAnimLists().add(entryList);
                }
            }
        }
    }

    private static MOFPolygon getPolygonFromVertices(Map<SVector, Set<MOFPolygon>> polyMap, MOFPolygon oldPoly, SVector... vertices) {
        short oldTextureId = (oldPoly instanceof MOFPolyTexture) ? ((MOFPolyTexture) oldPoly).getImageId() : (short) -1;

        Map<MOFPolygon, Integer> countMap = new HashMap<>();
        for (SVector vertex : vertices)
            for (MOFPolygon poly : polyMap.get(vertex))
                countMap.put(poly, countMap.getOrDefault(poly, 0) + 1);

        MOFPolygon bestMatch = null;
        for (MOFPolygon poly : countMap.keySet())
            if (countMap.get(poly) == vertices.length && (bestMatch == null || (poly instanceof MOFPolyTexture && ((MOFPolyTexture) poly).getImageId() == oldTextureId)))
                bestMatch = poly;

        return bestMatch;
    }
}