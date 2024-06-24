package net.highwayfrogs.editor.utils;

import javafx.scene.control.Alert.AlertType;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.mof.*;
import net.highwayfrogs.editor.file.mof.animation.MOFAnimation;
import net.highwayfrogs.editor.file.mof.animation.MOFAnimationCels;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformObject;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformType;
import net.highwayfrogs.editor.file.mof.flipbook.MOFFlipbook;
import net.highwayfrogs.editor.file.mof.flipbook.MOFFlipbookAction;
import net.highwayfrogs.editor.file.mof.hilite.MOFHilite;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnim;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnimEntryList;
import net.highwayfrogs.editor.file.mof.prims.MOFPolyTexture;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.mof.prims.MOFPrimType;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.blocks.*;
import net.highwayfrogs.editor.system.mm3d.blocks.MMFrameAnimationsBlock.MMAnimationFrame;
import net.highwayfrogs.editor.system.mm3d.blocks.MMFrameAnimationsBlock.MMFloatVertex;
import net.highwayfrogs.editor.system.mm3d.blocks.MMSkeletalAnimationBlock.MMAnimationKeyframeType;
import net.highwayfrogs.editor.system.mm3d.blocks.MMSkeletalAnimationBlock.MMSkeletalAnimationFrame;
import net.highwayfrogs.editor.system.mm3d.blocks.MMWeightedInfluencesBlock.MMWeightedInfluenceType;
import net.highwayfrogs.editor.system.mm3d.blocks.MMWeightedInfluencesBlock.MMWeightedPositionType;

import java.io.File;
import java.io.PrintWriter;
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
        objWriter.write("# Exported: " + Calendar.getInstance().getTime() + Constants.NEWLINE);
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
                    GameImage image = textureMap.computeIfAbsent((int) texture.getImageId(), vloTable::getGlobalTexture);
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
    public static void exportMapToObj(FroggerMapFile map, File directory) {
        /*if (directory == null)
            return;

        FileEntry entry = map.getIndexEntry();
        VLOArchive vloArchive = map.getVloFile();
        TextureMap textureMap = TextureMap.newTextureMap(map, ShadingMode.NO_SHADING);
        String cleanName = Utils.getRawFileName(entry.getDisplayName());
        boolean exportTextures = vloArchive != null;

        System.out.println("Exporting " + cleanName + ".");

        String mtlName = cleanName + ".mtl";
        @Cleanup PrintWriter objWriter = new PrintWriter(new File(directory, cleanName + ".obj"));

        objWriter.write("# FrogLord Map Export" + Constants.NEWLINE);
        objWriter.write("# Exported: " + Calendar.getInstance().getTime() + Constants.NEWLINE);
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
                    base += " " + (color.getShadingRed() / 128D) + " " + (color.getShadingGreen() / 128D) + " " + (color.getShadingBlue() / 128D);
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
            objWriter.write(builder + Constants.NEWLINE);
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

        System.out.println("Export complete.");*/ // TODO: ENABLE LATER.
    }

    /**
     * Convert a MOF to a MisfitModel3D.
     * Notes: Misfit Model 3D has a bug in it regarding texture management, where it will use the wrong material, for instance on the underwater portion of SUB_LOG in SUB1.
     * It seems using Maverick Model 3D instead fixes this problem.
     * @param holder The mof to convert.
     * @return misfit3d
     */
    public static MisfitModel3DObject convertMofToMisfitModel(MOFHolder holder) {
        MisfitModel3DObject model = new MisfitModel3DObject();
        MOFFile staticMof = holder.asStaticFile();
        VLOArchive vloTable = holder.getVloFile();
        Utils.verify(vloTable != null, "Unknown VLO Table for %s!", holder.getFileDisplayName());

        // Add TransformType.
        if (holder.isAnimatedMOF())
            model.getMetadata().addMetadataValue("transformType", holder.getAnimatedFile().getTransformType().name());

        // Add Joints.
        for (int i = 0; i < staticMof.getParts().size(); i++) {
            MMJointsBlock joint = model.getJoints().addNewElement();
            joint.setName("part" + i);
        }

        // Add Vertices.
        int verticeStart = 0;
        for (int i = 0; i < staticMof.getParts().size(); i++) {
            MOFPart part = staticMof.getParts().get(i);
            part.setTempVertexStart(verticeStart);
            MOFPartcel partcel = part.getStaticPartcel();
            verticeStart += partcel.getVertices().size();

            for (SVector vertex : partcel.getVertices()) {
                int vtxId = model.getVertices().size();
                MMVerticeBlock mmVertice = model.getVertices().addNewElement();
                mmVertice.setFlags(MMVerticeBlock.FLAG_FREE_VERTEX);
                mmVertice.setX(vertex.getExportFloatX());
                mmVertice.setY(vertex.getExportFloatY());
                mmVertice.setZ(vertex.getExportFloatZ());

                // Linked vertices to joints.
                MMWeightedInfluencesBlock influence = model.getWeightedInfluences().addNewElement();
                influence.setPositionType(MMWeightedPositionType.VERTEX);
                influence.setPositionIndex(vtxId);
                influence.setBoneJointIndex(i);
                influence.setInfluenceType(MMWeightedInfluenceType.AUTOMATIC);
                influence.setInfluenceWeight(MMWeightedInfluencesBlock.MAX_INFLUENCE_WEIGHT);

                MMJointVerticesBlock jointVertices = model.getJointVertices().addNewElement();
                jointVertices.setJointIndex(i);
                jointVertices.setVertexIndex(vtxId);
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
                    animation.setFramesPerSecond(holder.getGameInstance().getFPS());
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
        if (holder.isAnimatedMOF()) {
            MOFAnimation animatedMof = holder.getAnimatedFile();

            for (int action = 0; action < animatedMof.getModelSet().getCelSet().getCels().size(); action++) {
                MOFAnimationCels celAction = animatedMof.getModelSet().getCelSet().getCels().get(action);

                MMSkeletalAnimationBlock skeletalAnimation = model.getSkeletalAnimations().addNewElement();
                skeletalAnimation.setName(holder.getName(action));
                skeletalAnimation.setFps(holder.getGameInstance().getFPS());

                for (int frame = 0; frame < celAction.getFrameCount(); frame++) {
                    List<MMSkeletalAnimationFrame> keyframes = new ArrayList<>();

                    for (int i = 0; i < staticMof.getParts().size(); i++) { // Each part has its own information.
                        MOFPart part = staticMof.getParts().get(i);
                        TransformObject transform = animatedMof.getTransform(part, action, frame);
                        PSXMatrix matrix = transform.createMatrix();
                        keyframes.add(new MMSkeletalAnimationFrame(i, MMAnimationKeyframeType.ROTATION, (float) -matrix.getRollAngle(), (float) -matrix.getPitchAngle(), (float) matrix.getYawAngle()));
                        keyframes.add(new MMSkeletalAnimationFrame(i, MMAnimationKeyframeType.TRANSLATION, -Utils.fixedPointIntToFloat4Bit(matrix.getTransform()[0]), -Utils.fixedPointIntToFloat4Bit(matrix.getTransform()[1]), Utils.fixedPointIntToFloat4Bit(matrix.getTransform()[2])));
                    }

                    skeletalAnimation.getFrames().add(keyframes);
                }
            }
        }

        // Add Faces and Textures.
        Map<ColorKey, MOFTextureData> dataMap = new HashMap<>();
        Map<Integer, Integer> exTexMap = new HashMap<>();
        Map<PSXColorVector, MOFTextureData> colorMap = new HashMap<>();
        List<MOFPolygon> polygonList = staticMof.getAllPolygons();
        for (MOFPolygon poly : polygonList) {
            int faceIndex = model.getTriangleFaces().size();
            model.getTriangleFaces().addMofPolygon(poly);

            MOFTextureData data;
            if (poly instanceof MOFPolyTexture) {
                MOFPolyTexture polyTex = (MOFPolyTexture) poly;

                data = dataMap.computeIfAbsent(new ColorKey(polyTex.getImageId(), polyTex.getColor()), key -> {
                    GameImage image = vloTable.getImageByTextureId(key.getImageId());
                    int localId = image.getLocalImageID();
                    int texId = image.getTextureId();

                    // Create material.
                    if (!exTexMap.containsKey(localId)) {
                        exTexMap.put(localId, model.getExternalTextures().size());
                        model.getExternalTextures().addTexture(localId + ".png"); // Add external texture.
                    }

                    int externalTextureId = exTexMap.get(localId);
                    int materialId = model.getMaterials().size();
                    MMMaterialsBlock material = model.getMaterials().addNewElement();
                    material.setTexture(externalTextureId);
                    material.setName(MATERIAL_NAME_PREFIX + texId + "-" + Integer.toHexString(key.getColor().toRGB()));
                    material.getDiffuse()[0] = (key.getColor().getShadingRed() / 128F);
                    material.getDiffuse()[1] = (key.getColor().getShadingGreen() / 128F);
                    material.getDiffuse()[2] = (key.getColor().getShadingBlue() / 128F);

                    // Create new group.
                    MMTriangleGroupsBlock group = model.getGroups().addNewElement();
                    group.setName("texGroup" + texId + "-" + Integer.toHexString(key.getColor().toRGB()));
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
            List<Integer> triangleIndices = data.getGroup().getTriangleIndices();
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
                    normal1.setV1Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[1])));
                    normal1.setV2Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[3])));
                    normal1.setV3Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[0])));

                    MMTriangleNormalsBlock normal2 = model.getNormals().addNewElement();
                    normal2.setIndex(faceIndex + 1);
                    normal2.setV1Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[2])));
                    normal2.setV2Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[3])));
                    normal2.setV3Normals(vecToFloat(partcel.getNormals().get(poly.getNormals()[1])));
                }
            }
        }

        return model;
    }

    private static float[] vecToFloat(SVector vec) {
        return new float[]{vec.getExportFloatX(), vec.getExportFloatY(), vec.getExportFloatZ()};
    }

    @Getter
    @AllArgsConstructor
    private static class ColorKey {
        private final short imageId;
        private final PSXColorVector color;

        @Override
        public int hashCode() {
            return ((color.getRed() | 0b111111) << 10) | ((color.getBlue() | 0b11111) << 5) | (color.getGreen() | 0b11111) | (this.imageId << 15);
        }

        @Override
        public boolean equals(Object other) {
            return (other instanceof ColorKey) && this.color.equals(((ColorKey) other).getColor()) && this.imageId == ((ColorKey) other).getImageId();
        }
    }

    @Getter
    @AllArgsConstructor
    private static final class MOFTextureData {
        private final GameImage image;
        private final long externalTextureIndex;
        private final MMTriangleGroupsBlock group;
        private final MMMaterialsBlock material;
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

        boolean isReplacementAnimated = model.getSkeletalAnimations().size() > 0;
        boolean isUnsafeReplacementArea = (holder.getTheme() == null || holder.getTheme() == FroggerMapTheme.GENERAL);
        if (isUnsafeReplacementArea && (isReplacementAnimated != holder.isAnimatedMOF())) { // Any model which is accessed directly by the game code via hardcoded ids is assumed to be a certain type by the code, and we should not change this type.
            Utils.makePopUp("This " + (holder.isAnimatedMOF() ? "animated" : "static") + " model cannot be overwritten by a " + (isReplacementAnimated ? "animated" : "static") + " model.", AlertType.ERROR);
            return;
        }

        if (model.getFrameAnimationPoints().size() > 0)
            Utils.makePopUp("Frame Point animations are not supported. " + model.getFrameAnimationPoints().size() + " frame point animations will be skipped.", AlertType.WARNING);

        MOFFile staticMof = holder.isDummy() ? new MOFFile(holder.getGameInstance(), holder) : holder.asStaticFile();
        MOFAnimation animatedMof = isReplacementAnimated ? new MOFAnimation(holder.getGameInstance(), holder, staticMof) : null;

        // Pre-Build Data Maps.
        int newPartCount = model.getJoints().size();
        Map<Integer, MMMaterialsBlock> materialMap = new HashMap<>();
        Map<Integer, MMTriangleNormalsBlock> normalMap = new HashMap<>();
        Map<Integer, MMTextureCoordinatesBlock> uvMap = new HashMap<>();
        Map<Integer, List<Integer>> jointVerticeMap = new HashMap<>();
        Map<Integer, List<MMTriangleFaceBlock>> jointFaceMap = new HashMap<>();
        Map<Integer, Integer> modelVtxToPartcelVtxMap = new HashMap<>();

        // Build Maps.
        for (MMTextureCoordinatesBlock block : model.getTextureCoordinates().getBlocks())
            uvMap.put(block.getTriangle(), block);

        for (MMTriangleNormalsBlock block : model.getNormals().getBlocks())
            normalMap.put(block.getIndex(), block);

        for (MMTriangleGroupsBlock block : model.getGroups().getBlocks()) {
            MMMaterialsBlock material = block.getMaterial() != MMTriangleGroupsBlock.EMPTY_MATERIAL ? model.getMaterials().getBody(block.getMaterial()) : null;
            for (int face : block.getTriangleIndices())
                materialMap.put(face, material);
        }

        // Build Joint Map.
        if (model.getWeightedInfluences().size() > 0) {
            for (MMWeightedInfluencesBlock influence : model.getWeightedInfluences().getBlocks())
                if (influence.getPositionType() == MMWeightedPositionType.VERTEX)
                    jointVerticeMap.computeIfAbsent(influence.getBoneJointIndex(), boneIndex -> new ArrayList<>()).add(influence.getPositionIndex());
        } else if (model.getJointVertices().size() > 0) { // Fallback to joint vertice block if weighted influences don't exist, but joint vertices do. It likely means the user is using mm3d 1.4.
            for (MMJointVerticesBlock jointVertices : model.getJointVertices().getBlocks())
                jointVerticeMap.computeIfAbsent(jointVertices.getJointIndex(), boneIndex -> new ArrayList<>()).add(jointVertices.getVertexIndex());
        }

        int failCount = 0;
        for (MMTriangleFaceBlock face : model.getTriangleFaces().getBlocks()) {
            boolean foundMatch = false;
            for (Entry<Integer, List<Integer>> jointVerticeEntry : jointVerticeMap.entrySet()) {
                boolean matchVertices = true;
                for (int i = 0; i < face.getVertices().length; i++) {
                    if (!jointVerticeEntry.getValue().contains(face.getVertices()[i])) {
                        matchVertices = false;
                        break;
                    }
                }

                if (matchVertices) {
                    foundMatch = true;
                    jointFaceMap.computeIfAbsent(jointVerticeEntry.getKey(), key -> new ArrayList<>()).add(face);
                    break;
                }
            }

            if (!foundMatch)
                failCount++;
        }

        if (failCount > 0)
            Utils.makePopUp(failCount + " could not be linked to a part / joint. They will be ignored.", AlertType.WARNING);

        // Figure out which textures are allowed. (All of them) Maybe later we can put extra restrictions to figure out which ones are loaded when the model is loaded.
        Set<Short> allowedTextureIds = new HashSet<>();
        for (VLOArchive vloArchive : holder.getArchive().getAllFiles(VLOArchive.class))
            for (GameImage gameImage : vloArchive.getImages())
                allowedTextureIds.add(gameImage.getTextureId());

        // Build Parts.
        List<MOFPart> oldParts = new ArrayList<>(staticMof.getParts());
        staticMof.getParts().clear();

        if (model.getSkeletalAnimations().size() > 0) { // This is an animated MOF.
            String transformType = model.getFirstMetadataValue("transformType");
            if (transformType != null)
                animatedMof.setTransformType(TransformType.valueOf(transformType));

            Map<TransformObject, Integer> indexMap = new HashMap<>();
            for (MMSkeletalAnimationBlock skeletalAnimation : model.getSkeletalAnimations().getBlocks()) {
                MOFAnimationCels loadedAnimation = new MOFAnimationCels(animatedMof);

                int uniqueFrames = 0;
                for (int frame = 0; frame < skeletalAnimation.getFrames().size(); frame++) {
                    List<MMSkeletalAnimationFrame> frameData = skeletalAnimation.getFrames().get(frame);

                    MMSkeletalAnimationFrame[][] frames = new MMSkeletalAnimationFrame[newPartCount][2];
                    for (MMSkeletalAnimationFrame frameInfo : frameData) {
                        if (frameInfo.getKeyframeType() == MMAnimationKeyframeType.ROTATION) {
                            frames[frameInfo.getJointIndex()][0] = frameInfo;
                        } else if (frameInfo.getKeyframeType() == MMAnimationKeyframeType.TRANSLATION) {
                            frames[frameInfo.getJointIndex()][1] = frameInfo;
                        }
                    }

                    short[] frameIndices = new short[newPartCount];

                    for (int i = 0; i < newPartCount; i++) {
                        MMSkeletalAnimationFrame rotFrame = frames[i][0];
                        MMSkeletalAnimationFrame posFrame = frames[i][1];

                        // Create rotation matrix.
                        PSXMatrix matrix = new PSXMatrix();
                        if (posFrame != null) {
                            matrix.getTransform()[0] = Utils.floatToFixedPointInt4Bit(-posFrame.getPosX());
                            matrix.getTransform()[1] = Utils.floatToFixedPointInt4Bit(-posFrame.getPosY());
                            matrix.getTransform()[2] = Utils.floatToFixedPointInt4Bit(posFrame.getPosZ());
                        }
                        matrix.updateMatrix(rotFrame != null ? rotFrame.getPosZ() : 0D, rotFrame != null ? -rotFrame.getPosY() : 0D, rotFrame != null ? -rotFrame.getPosX() : 0D);
                        TransformObject newTransform = animatedMof.getTransformType().makeTransform(matrix); // Create transform from matrix.

                        int index = indexMap.computeIfAbsent(newTransform, key -> {
                            int size = animatedMof.getCommonData().getTransforms().size();
                            animatedMof.getCommonData().getTransforms().add(key);
                            return size;
                        });

                        frameIndices[i] = (short) index;
                    }

                    int duplicateFrame = -1;
                    for (int i = 0; i < uniqueFrames; i++) { // Find a duplicate frame.
                        int startIndex = (i * newPartCount);
                        boolean match = true;
                        for (int j = 0; j < newPartCount; j++) { // Vanilla discrepancy. It appears the vanilla files only will duplicate the previous frame, but ours will duplicate any frame that matches.
                            if (frameIndices[j] != loadedAnimation.getIndices().get(startIndex + j)) {
                                match = false;
                                break;
                            }
                        }

                        if (match) {
                            duplicateFrame = i;
                            break;
                        }
                    }

                    if (duplicateFrame != -1) { // This frame is duplicate of another, so add an index to that instead.
                        loadedAnimation.getCelNumbers().add(duplicateFrame);
                    } else { // The frame is new, add its data.
                        for (int j = 0; j < frameIndices.length; j++)
                            loadedAnimation.getIndices().add(frameIndices[j]);
                        loadedAnimation.getCelNumbers().add(uniqueFrames);
                        uniqueFrames++;
                    }
                }

                // celNumbers = entry for each frame.
                // indices = each frame has an index for each part, unless the frame exactly matches another frame.
                animatedMof.getModelSet().getCelSet().getCels().add(loadedAnimation);
            }


            // Apply animation data to the model.
            holder.setStaticFile(null);
            holder.setAnimatedFile(animatedMof);
        }

        for (int partId = 0; partId < model.getJoints().size(); partId++) {
            MOFPart oldPart = oldParts.size() > partId ? oldParts.get(partId) : null;
            MOFPart part = new MOFPart(staticMof);
            staticMof.getParts().add(part);

            MOFPartcel basePartcel = new MOFPartcel(part, 0, 0);
            Map<SVector, Short> baseNormalIndices = new HashMap<>();
            List<Integer> vertexIdList = jointVerticeMap.get(partId);

            // Load Vertices.
            for (int id : vertexIdList) {
                MMVerticeBlock block = model.getVertices().getBody(id);
                modelVtxToPartcelVtxMap.put(id, basePartcel.getVertices().size());
                basePartcel.getVertices().add(new SVector(-block.getX(), -block.getY(), block.getZ()));
            }

            // Load Normals.
            Map<Integer, int[]> normalIndexMap = new HashMap<>(); // <Face, Normal Indices>
            for (MMTriangleNormalsBlock block : model.getNormals().getBlocks()) {
                if (!jointFaceMap.get(partId).contains(model.getTriangleFaces().getBody(block.getIndex())))
                    continue; // If the face these normals are for aren't in this joint, skip it.

                SVector n1 = new SVector(-block.getV1Normals()[0], -block.getV1Normals()[1], block.getV1Normals()[2]);
                SVector n2 = new SVector(-block.getV2Normals()[0], -block.getV2Normals()[1], block.getV2Normals()[2]);
                SVector n3 = new SVector(-block.getV3Normals()[0], -block.getV3Normals()[1], block.getV3Normals()[2]);

                int[] normalIndices = new int[3];
                short index = (short) basePartcel.getNormals().size();
                if (baseNormalIndices.containsKey(n1)) {
                    normalIndices[0] = baseNormalIndices.get(n1);
                } else {
                    normalIndices[0] = index;
                    baseNormalIndices.put(n1, index++);
                    basePartcel.getNormals().add(n1);
                }

                if (baseNormalIndices.containsKey(n2)) {
                    normalIndices[1] = baseNormalIndices.get(n2);
                } else {
                    normalIndices[1] = index;
                    baseNormalIndices.put(n2, index++);
                    basePartcel.getNormals().add(n2);
                }

                if (baseNormalIndices.containsKey(n3)) {
                    normalIndices[2] = baseNormalIndices.get(n3);
                } else {
                    normalIndices[2] = index;
                    baseNormalIndices.put(n3, index);
                    basePartcel.getNormals().add(n3);
                }

                normalIndexMap.put(block.getIndex(), normalIndices);
            }

            part.getPartcels().add(basePartcel);

            // Port data which isn't supposed to be overwritten by an import.
            if (oldPart != null) {
                if (oldPart.getMatrices().size() > 0) // Transition matrix.
                    part.getMatrices().addAll(oldPart.getMatrices());

                if (oldPart.getCollprims() != null) { // Transition collprim.
                    for (MOFCollprim oldCollprim : oldPart.getCollprims()) {
                        oldCollprim.setParentPart(part);
                        part.getCollprims().add(oldCollprim);
                    }
                }

                for (MOFHilite oldHilite : oldPart.getHilites()) { // Transition Hilites.
                    if (basePartcel.getVertices().contains(oldHilite.getVertex())) {
                        oldHilite.setParent(part);
                        part.getHilites().add(oldHilite);
                    }
                }
            }

            // Add Flipbook animations.
            if (!isReplacementAnimated && newPartCount == 1) {
                MOFFlipbook flipbook = new MOFFlipbook();
                for (MMFrameAnimationsBlock block : model.getFrameAnimations().getBlocks()) {
                    int partcelIndex = part.getPartcels().size();

                    for (MMAnimationFrame frame : block.getFrames()) {
                        MOFPartcel newCel = new MOFPartcel(part, frame.getVertexPositions().size(), frame.getVertexPositions().size());
                        for (MMFloatVertex vertex : frame.getVertexPositions())
                            newCel.getVertices().add(new SVector(-vertex.getX(), -vertex.getY(), vertex.getZ()));

                        newCel.getNormals().addAll(basePartcel.getNormals()); // There is no difference between these and the static partcel normals, at least not in any of Frogger's releases.
                        part.getPartcels().add(newCel); // Add new frame of animation.
                    }

                    flipbook.getActions().add(new MOFFlipbookAction(part.getPartcels().size() - partcelIndex, partcelIndex));
                }

                if (flipbook.getActions().size() > 0)
                    part.setFlipbook(flipbook);
            } else if (model.getFrameAnimations().size() > 0) {
                Utils.makePopUp(model.getFrameAnimations().size() + " flipbook (frame) animations have been skipped, since this is either XAR or has more than one part.", AlertType.WARNING);
            }

            // Build Polygons.
            for (MMTriangleFaceBlock faceBlock : jointFaceMap.get(partId)) {
                int i = faceBlock.getBlockIndex();

                MMMaterialsBlock material = materialMap.get(i);
                MMTriangleNormalsBlock normals = normalMap.get(i);
                MMTextureCoordinatesBlock texCoords = uvMap.get(i);

                boolean isTextured = material != null && material.hasTexture();
                boolean isGouraud = normals != null && !(Arrays.equals(normals.getV1Normals(), normals.getV2Normals()) && Arrays.equals(normals.getV1Normals(), normals.getV3Normals()));

                // Build the polygon. (We always use Tris, since mm3d doesn't support quads.) We could theoretically support quads by checking the vertices on each partcel (no need to check skeletal animation), but that might be more trouble than it's worth.
                MOFPrimType primType = isTextured
                        ? (isGouraud ? MOFPrimType.GT3 : MOFPrimType.FT3)
                        : (isGouraud ? MOFPrimType.G3 : MOFPrimType.F3);

                MOFPolygon poly = primType.makeNew(part);
                if (isTextured && (poly instanceof MOFPolyTexture) && material.getName().startsWith(MATERIAL_NAME_PREFIX)) {
                    MOFPolyTexture polyTex = (MOFPolyTexture) poly;
                    String trailingNumber = material.getName().substring(MATERIAL_NAME_PREFIX.length()).split("-")[0];  //TODO: Eventually switch this to check the texture filename, not the material name.
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

                    byte red = (byte) (int) (material.getDiffuse()[0] * 128);
                    byte green = (byte) (int) (material.getDiffuse()[1] * 128);
                    byte blue = (byte) (int) (material.getDiffuse()[2] * 128);
                    polyTex.getColor().fromRGB(Utils.toRGB(red, green, blue));
                }

                // Apply Vertices.
                for (int j = 0; j < poly.getVerticeCount(); j++)
                    poly.getVertices()[poly.getVerticeCount() - j - 1] = modelVtxToPartcelVtxMap.get(faceBlock.getVertices()[j]);

                // Apply Normals.
                int[] normalIndices = normalIndexMap.get(i);
                if (normalIndices != null) //TODO: If there are no normals, we should try to generate them. (Probably when the partcel is loaded, actually.)
                    for (int j = 0; j < poly.getNormals().length; j++)
                        poly.getNormals()[j] = (short) normalIndices[j];

                if (!isTextured) { // Load the color.
                    int red = (material != null ? ((int) (material.getDiffuse()[0] * 255)) & 0xFF : 255);
                    int green = (material != null ? ((int) (material.getDiffuse()[1] * 255)) & 0xFF : 0);
                    int blue = (material != null ? ((int) (material.getDiffuse()[2] * 255)) & 0xFF : 255);
                    poly.getColor().fromRGB((red << 16) | (green << 8) | blue);
                }

                // Register the polygon.
                part.getMofPolygons().computeIfAbsent(poly.getType(), key -> new ArrayList<>()).add(poly);
                part.getOrderedByLoadPolygons().add(poly);
            }
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

        // Finish Up, apply data.
        holder.setStaticFile(animatedMof != null ? null : staticMof);
        holder.setAnimatedFile(animatedMof);
        holder.setDummy(false);
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