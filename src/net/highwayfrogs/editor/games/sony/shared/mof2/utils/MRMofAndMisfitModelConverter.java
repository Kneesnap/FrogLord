package net.highwayfrogs.editor.games.sony.shared.mof2.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.psx.math.vector.CVector;
import net.highwayfrogs.editor.games.psx.math.PSXMatrix;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.*;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.flipbook.MRMofFlipbookAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.flipbook.MRMofFlipbookAnimationList;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform.MRAnimatedMofTransform;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform.MRAnimatedMofTransformType;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.*;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.system.IntList;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.blocks.*;
import net.highwayfrogs.editor.system.mm3d.blocks.MMFrameAnimationsBlock.MMAnimationFrame;
import net.highwayfrogs.editor.system.mm3d.blocks.MMFrameAnimationsBlock.MMFloatVertex;
import net.highwayfrogs.editor.system.mm3d.blocks.MMSkeletalAnimationBlock.MMAnimationKeyframeType;
import net.highwayfrogs.editor.system.mm3d.blocks.MMSkeletalAnimationBlock.MMSkeletalAnimationFrame;
import net.highwayfrogs.editor.system.mm3d.blocks.MMWeightedInfluencesBlock.MMWeightedInfluenceType;
import net.highwayfrogs.editor.system.mm3d.blocks.MMWeightedInfluencesBlock.MMWeightedPositionType;
import net.highwayfrogs.editor.system.mm3d.holders.MMTriangleFaceHolder;
import net.highwayfrogs.editor.utils.*;
import net.highwayfrogs.editor.utils.logging.ILogger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Contains static 3d file utilities.
 * Created by Kneesnap on 2/28/2019.
 */
public class MRMofAndMisfitModelConverter {
    private static final String JOINT_PART_NAME_PREFIX = "part";
    private static final String MATERIAL_NAME_PREFIX = "tex";
    private static final String METADATA_KEY_TRANSFORM_TYPE = "transformType";
    private static final String METADATA_KEY_MODEL_INDEX = "localMofModelIndex";
    private static final int MOF_EXPORT_FILTER = VloImage.DEFAULT_IMAGE_NO_PADDING_EXPORT_SETTINGS;

    /**
     * Convert a MOF to a MisfitModel3D.
     * Notes: Misfit Model 3D has a bug in it regarding texture management, where it will use the wrong material, for instance on the underwater portion of SUB_LOG in SUB1.
     * It seems using Maverick Model 3D instead fixes this problem.
     * @param staticMof The static mof to convert. (Xar animations will be included, if applicable)
     * @param textureExportFolder if not null, any required textures will be exported to this folder
     * @return misfit3d
     */
    public static MisfitModel3DObject convertMofToMisfitModel(MRStaticMof staticMof, String texturePathPrefix, File textureExportFolder) {
        if (staticMof == null)
            throw new NullPointerException("staticMof");

        MRModel model = staticMof.getModel();
        MisfitModel3DObject output = new MisfitModel3DObject();

        // Add TransformType.
        MRAnimatedMof animatedMof = model.getAnimatedMof();
        if (animatedMof != null)
            output.getMetadata().addMetadataValue(METADATA_KEY_TRANSFORM_TYPE, animatedMof.getTransformType().name());

        // Add model ID.
        int mofIndex = staticMof.getStaticMofIndex();
        if (mofIndex >= 0)
            output.getMetadata().addMetadataValue(METADATA_KEY_MODEL_INDEX, String.valueOf(mofIndex));

        // Add Joints.
        for (int i = 0; i < staticMof.getParts().size(); i++) {
            MMJointsBlock joint = output.getJoints().addNewElement();
            joint.setName(JOINT_PART_NAME_PREFIX + i);
        }

        // Add Vertices.
        int[] vertexStartsByPart = new int[staticMof.getParts().size()];
        int tempVertexStart = 0;
        for (int i = 0; i < staticMof.getParts().size(); i++) {
            MRMofPart part = staticMof.getParts().get(i);
            MRMofPartCel partCel = part.getStaticPartcel();
            vertexStartsByPart[i] = tempVertexStart;
            tempVertexStart += partCel.getVertices().size();

            for (SVector vertex : partCel.getVertices()) {
                int vtxId = output.getVertices().size();
                MMVerticeBlock mmVertice = output.getVertices().addNewElement();
                mmVertice.setFlags(MMVerticeBlock.FLAG_FREE_VERTEX);
                mmVertice.setX(-vertex.getFloatX());
                mmVertice.setY(-vertex.getFloatY());
                mmVertice.setZ(vertex.getFloatZ());

                // Linked vertices to joints.
                MMWeightedInfluencesBlock influence = output.getWeightedInfluences().addNewElement();
                influence.setPositionType(MMWeightedPositionType.VERTEX);
                influence.setPositionIndex(vtxId);
                influence.setBoneJointIndex(i);
                influence.setInfluenceType(MMWeightedInfluenceType.AUTOMATIC);
                influence.setInfluenceWeight(MMWeightedInfluencesBlock.MAX_INFLUENCE_WEIGHT);

                MMJointVerticesBlock jointVertices = output.getJointVertices().addNewElement();
                jointVertices.setJointIndex(i);
                jointVertices.setVertexIndex(vtxId);
            }
        }

        // Add Flipbook animations.
        for (MRMofPart part : staticMof.getParts()) {
            if (part.getFlipbook() == null)
                continue;

            for (int action = 0; action < part.getFlipbook().getAnimations().size(); action++) {
                MMFrameAnimationsBlock animation = output.getFrameAnimations().getBody(action);
                if (animation == null) {
                    animation = output.getFrameAnimations().addNewElement();
                    animation.setFramesPerSecond(model.getGameInstance().getFPS());
                    animation.setName(getAnimationName(model, action));
                    animation.setFlags(MMFrameAnimationsBlock.FLAG_LOOPING);
                }

                MRMofFlipbookAnimation flipbookAction = part.getFlipbook().getAction(action);
                for (int frame = 0; frame < flipbookAction.getFrameCount(); frame++) {
                    MRMofPartCel partCel = part.getPartCel(action, frame);
                    if (frame >= animation.getFrames().size())
                        animation.getFrames().add(new MMAnimationFrame());

                    MMAnimationFrame animFrame = animation.getFrames().get(frame);
                    partCel.getVertices().forEach(vertice -> animFrame.getVertexPositions().add(new MMFloatVertex(vertice)));
                }
            }
        }

        // Add XAR animations.
        MRAnimatedMofModel animatedModel = staticMof.getAnimatedMofModel();
        if (animatedModel != null) {
            MRAnimatedMofModelSet modelSet = animatedModel.getParentModelSet();

            for (int action = 0; action < modelSet.getCelSet().getAnimations().size(); action++) {
                MRAnimatedMofXarAnimation celAction = modelSet.getCelSet().getAnimations().get(action);

                MMSkeletalAnimationBlock skeletalAnimation = output.getSkeletalAnimations().addNewElement();
                skeletalAnimation.setName(getAnimationName(model, action));
                skeletalAnimation.setFps(model.getGameInstance().getFPS());
                skeletalAnimation.setFlags(MMSkeletalAnimationBlock.FLAG_LOOPING);

                for (int frame = 0; frame < celAction.getFrameCount(); frame++) {
                    List<MMSkeletalAnimationFrame> keyframes = new ArrayList<>();

                    for (int i = 0; i < staticMof.getParts().size(); i++) { // Each part has its own information.
                        MRMofPart part = staticMof.getParts().get(i);
                        PSXMatrix matrix = modelSet.getParentMof().getTransformMatrix(part, celAction, frame);
                        keyframes.add(new MMSkeletalAnimationFrame(i, MMAnimationKeyframeType.ROTATION, (float) -matrix.getPitchAngle(), (float) -matrix.getYawAngle(), (float) matrix.getRollAngle()));
                        keyframes.add(new MMSkeletalAnimationFrame(i, MMAnimationKeyframeType.TRANSLATION, -DataUtils.fixedPointIntToFloat4Bit(matrix.getTransform()[0]), -DataUtils.fixedPointIntToFloat4Bit(matrix.getTransform()[1]), DataUtils.fixedPointIntToFloat4Bit(matrix.getTransform()[2])));
                    }

                    skeletalAnimation.getFrames().add(keyframes);
                }
            }
        }

        // Add Faces and Textures.
        Map<ColorKey, MRMofMMMaterialData> dataMap = new HashMap<>();
        Map<Integer, Integer> exTexMap = new HashMap<>();
        Map<CVector, MRMofMMMaterialData> colorMap = new HashMap<>();
        for (int i = 0; i < staticMof.getParts().size(); i++) {
            MRMofPart mofPart = staticMof.getParts().get(i);
            int vertexStartAt = vertexStartsByPart[i];
            for (MRMofPolygon polygon : mofPart.getOrderedPolygons()) {
                int faceIndex = addMofPolygon(output, vertexStartAt, polygon);

                MRMofMMMaterialData data;
                if (polygon.getPolygonType().isTextured()) {
                    data = dataMap.computeIfAbsent(new ColorKey(polygon.getTextureId(), polygon.getColor()), key -> {
                        VloImage image = polygon.getDefaultTexture();
                        int localId = image.getLocalImageID();
                        int texId = image.getTextureId();

                        // Create material.
                        if (!exTexMap.containsKey(localId)) {
                            exTexMap.put(localId, output.getExternalTextures().size());

                            // Export texture file if necessary.
                            String textureFileName = texId + ".png";
                            if (textureExportFolder != null) {
                                File textureExportFile = new File(textureExportFolder, textureFileName);
                                if (!textureExportFile.exists()) {
                                    try {
                                        ImageIO.write(image.toBufferedImage(MOF_EXPORT_FILTER), "png", textureExportFile);
                                    } catch (IOException ex) {
                                        throw new RuntimeException("Failed to save " + textureExportFile.getName(), ex);
                                    }
                                }
                            }

                            String relativeTexturePath = textureFileName;
                            if (!StringUtils.isNullOrWhiteSpace(texturePathPrefix))
                                relativeTexturePath = texturePathPrefix + textureFileName;

                            output.getExternalTextures().addTexture(relativeTexturePath); // Add external texture.
                        }

                        int externalTextureId = exTexMap.get(localId);
                        int materialId = output.getMaterials().size();
                        MMMaterialsBlock material = output.getMaterials().addNewElement();
                        material.setName(MATERIAL_NAME_PREFIX + texId + "_" + NumberUtils.to0PrefixedHexString(key.getColor().toRGB()));
                        material.setTextureIndex(externalTextureId);
                        material.setFlags((short) (MMMaterialsBlock.FLAG_CLAMP_S | MMMaterialsBlock.FLAG_CLAMP_T)); // PSX textures are not capable of repeating/tiling.
                        material.getAmbient()[0] = (key.getColor().getRedShort() / 128F);
                        material.getAmbient()[1] = (key.getColor().getGreenShort() / 128F);
                        material.getAmbient()[2] = (key.getColor().getBlueShort() / 128F);
                        material.getAmbient()[3] = 1F;
                        Arrays.fill(material.getDiffuse(), 0);

                        // Create new group.
                        MMTriangleGroupsBlock group = output.getGroups().addNewElement();
                        group.setName("texGroup" + texId + "_" + NumberUtils.to0PrefixedHexString(key.getColor().toRGB()));
                        group.setMaterial(materialId);
                        return new MRMofMMMaterialData(image, externalTextureId, group, material);
                    });
                } else {
                    data = colorMap.computeIfAbsent(polygon.getColor(), colorKey -> {
                        // Create material.
                        int materialId = output.getMaterials().size();
                        MMMaterialsBlock material = output.getMaterials().addNewElement();
                        material.setFlags(MMMaterialsBlock.FLAG_NO_TEXTURE);
                        material.setName("color_" + NumberUtils.to0PrefixedHexString(colorKey.toRGB()));
                        material.getAmbient()[0] = DataUtils.unsignedByteToFloat(colorKey.getRed());
                        material.getAmbient()[1] = DataUtils.unsignedByteToFloat(colorKey.getGreen());
                        material.getAmbient()[2] = DataUtils.unsignedByteToFloat(colorKey.getBlue());
                        material.getAmbient()[3] = 1F;
                        Arrays.fill(material.getDiffuse(), 0);

                        // Create new group.
                        MMTriangleGroupsBlock group = output.getGroups().addNewElement();
                        group.setName("colorGroup_" + NumberUtils.to0PrefixedHexString(colorKey.toRGB()));
                        group.setMaterial(materialId);
                        return new MRMofMMMaterialData(null, MMTriangleGroupsBlock.EMPTY_MATERIAL, group, material);
                    });
                }

                // Link faces to texture group.
                List<Integer> triangleIndices = data.getGroup().getTriangleIndices();
                triangleIndices.add(faceIndex);
                if (polygon.getVertexCount() == 4)
                    triangleIndices.add(faceIndex + 1);

                addNormals(output, polygon, faceIndex);
            }
        }

        return output;
    }

    /**
     * Adds a mof polygon face.
     * @param polygon mofPolygon
     */
    private static int addMofPolygon(MisfitModel3DObject output, int partVertexStartAt, MRMofPolygon polygon) {
        int newFaceIndex = output.getTriangleFaces().size();
        VloImage image = polygon.getDefaultTexture();
        short flags = polygon.getMofPart().isHiddenByConfiguration() ? MMTriangleFaceBlock.FLAG_HIDDEN : 0;
        if (polygon.getVertexCount() == 4) {
            output.getTriangleFaces().addTriangle(partVertexStartAt + polygon.getVertices()[2],
                    partVertexStartAt + polygon.getVertices()[1], partVertexStartAt + polygon.getVertices()[0], flags);
            addTriangleUvs(output, newFaceIndex, polygon, image, 2, 1, 0);

            output.getTriangleFaces().addTriangle(partVertexStartAt + polygon.getVertices()[1],
                    partVertexStartAt + polygon.getVertices()[2], partVertexStartAt + polygon.getVertices()[3], flags);
            addTriangleUvs(output, newFaceIndex + 1, polygon, image, 1, 2, 3);
        } else if (polygon.getVertexCount() == 3) {
            output.getTriangleFaces().addTriangle(partVertexStartAt + polygon.getVertices()[2],
                    partVertexStartAt + polygon.getVertices()[1], partVertexStartAt + polygon.getVertices()[0], flags);
            addTriangleUvs(output, newFaceIndex, polygon, image, 2, 1, 0);
        } else {
            throw new RuntimeException("Failed to add MOF Face.");
        }

        return newFaceIndex;
    }

    private static void addNormals(MisfitModel3DObject output, MRMofPolygon polygon, int faceIndex) {
        // Add normals, if there are any present.
        if (polygon.getNormals().length > 0) {
            MRMofPartCel partCel = polygon.getMofPart().getStaticPartcel();
            MMTriangleNormalsBlock normal1 = output.getNormals().addNewElement();
            normal1.setTriangleIndex(faceIndex);

            if (polygon.getNormals().length == 1) {
                SVector normal = partCel.getNormals().get(polygon.getNormals()[0]);
                vecToFloat(normal, normal1.getV1Normals());
                vecToFloat(normal, normal1.getV2Normals());
                vecToFloat(normal, normal1.getV3Normals());

                if (polygon.getVertexCount() == 4) {
                    MMTriangleNormalsBlock normal2 = output.getNormals().addNewElement();
                    normal2.setTriangleIndex(faceIndex + 1);
                    vecToFloat(normal, normal2.getV1Normals());
                    vecToFloat(normal, normal2.getV2Normals());
                    vecToFloat(normal, normal2.getV3Normals());
                }
            } else if (polygon.getNormals().length == 3) {
                vecToFloat(partCel.getNormals().get(polygon.getNormals()[2]), normal1.getV1Normals());
                vecToFloat(partCel.getNormals().get(polygon.getNormals()[1]), normal1.getV2Normals());
                vecToFloat(partCel.getNormals().get(polygon.getNormals()[0]), normal1.getV3Normals());
            } else if (polygon.getNormals().length == 4) {
                vecToFloat(partCel.getNormals().get(polygon.getNormals()[2]), normal1.getV1Normals());
                vecToFloat(partCel.getNormals().get(polygon.getNormals()[1]), normal1.getV2Normals());
                vecToFloat(partCel.getNormals().get(polygon.getNormals()[0]), normal1.getV3Normals());

                MMTriangleNormalsBlock normal2 = output.getNormals().addNewElement();
                normal2.setTriangleIndex(faceIndex + 1);
                vecToFloat(partCel.getNormals().get(polygon.getNormals()[1]), normal2.getV1Normals());
                vecToFloat(partCel.getNormals().get(polygon.getNormals()[2]), normal2.getV2Normals());
                vecToFloat(partCel.getNormals().get(polygon.getNormals()[3]), normal2.getV3Normals());
            }
        }
    }

    private static MMTextureCoordinatesBlock addTriangleUvs(MisfitModel3DObject output, int faceIndex, MRMofPolygon polygon, VloImage image, int uvIndex0, int uvIndex1, int uvIndex2) {
        if (!polygon.getPolygonType().isTextured())
            return null;

        MMTextureCoordinatesBlock block = output.getTextureCoordinates().addNewElement();
        block.setTriangleIndex(faceIndex);
        block.getXCoordinates()[0] = polygon.getTextureUvs()[uvIndex0].getSnappedFloatU(image);
        block.getYCoordinates()[0] = 1F - polygon.getTextureUvs()[uvIndex0].getSnappedFloatV(image);
        block.getXCoordinates()[1] = polygon.getTextureUvs()[uvIndex1].getSnappedFloatU(image);
        block.getYCoordinates()[1] = 1F - polygon.getTextureUvs()[uvIndex1].getSnappedFloatV(image);
        block.getXCoordinates()[2] = polygon.getTextureUvs()[uvIndex2].getSnappedFloatU(image);
        block.getYCoordinates()[2] = 1F - polygon.getTextureUvs()[uvIndex2].getSnappedFloatV(image);
        return block;
    }

    private static void vecToFloat(SVector vec, float[] output) {
        output[0] = -vec.getFloatX();
        output[1] = -vec.getFloatY();
        output[2] = vec.getFloatZ();
    }

    private static String getAnimationName(MRModel model, int animationId) {
        String name = model.getConfiguredAnimationName(animationId);
        if (name != null)
            return name;

        return "Animation #" + (animationId + 1);
    }

    @Getter
    @AllArgsConstructor
    private static class ColorKey {
        private final short imageId;
        private final CVector color;

        @Override
        public int hashCode() {
            return (this.color.getRed() << 16) | (this.color.getBlue() << 8) | this.color.getGreen() | ((this.imageId & 0xFF) << 24);
        }

        @Override
        public boolean equals(Object other) {
            return (other instanceof ColorKey) && this.color.equals(((ColorKey) other).getColor()) && this.imageId == ((ColorKey) other).getImageId();
        }
    }

    @Getter
    @AllArgsConstructor
    private static final class MRMofMMMaterialData {
        private final VloImage image;
        private final long externalTextureIndex;
        private final MMTriangleGroupsBlock group;
        private final MMMaterialsBlock material;
    }

    /**
     * Load a MOF from a model.
     * Due to limits of the mm3d file format, we are unable to generate a MOFAnimation, however it should be able to make a MOFFile just fine.
     * @param logger The logger to write information to.
     * @param misfitModel The model to load from.
     * @param model The data to overwrite.
     */
    public static void importMofFromModel(ILogger logger, MisfitModel3DObject misfitModel, MRModel model) {
        if (logger == null)
            throw new NullPointerException("logger");
        if (misfitModel == null)
            throw new NullPointerException("misfitModel");
        if (model == null)
            throw new NullPointerException("model");

        String staticMofIndexStr = misfitModel.getFirstMetadataValue(METADATA_KEY_MODEL_INDEX);
        int staticMofIndex = NumberUtils.isInteger(staticMofIndexStr) ? Integer.parseInt(staticMofIndexStr) : 0;
        if (staticMofIndex < 0 || staticMofIndex >= model.getStaticMofs().size())
            throw new RuntimeException("Could not resolve the staticMofIndex of " + staticMofIndex + " to an actual staticMof in the MRModel. (staticMofs: " + model.getStaticMofs().size() + ")");

        // Import the MOF.
        MRStaticMof staticMof = model.getStaticMofs().get(staticMofIndex);
        loadXarAnimations(logger, misfitModel, staticMof);
        loadNewMofParts(logger, misfitModel, staticMof);
    }

    private static void loadXarAnimations(ILogger logger, MisfitModel3DObject misfitModel, MRStaticMof staticMof) {
        MRAnimatedMof animatedMof = staticMof.getModel().getAnimatedMof();
        if (animatedMof == null) {
            if (misfitModel.getSkeletalAnimations().size() > 0)
                logger.severe("Skipping %d skeletal animation(s) (XAR animations), because the game would likely crash if FrogLord changed the model type from STATIC to ANIMATED.", misfitModel.getSkeletalAnimations().size());

            return;
        }

        // Resolve celSet.
        MRAnimatedMofModel mofModel = staticMof.getAnimatedMofModel();
        MRAnimatedMofCelSet mofCelSet = mofModel != null ? mofModel.getParentModelSet().getCelSet() : null;
        if (mofCelSet == null) {
            logger.severe("Skipping %d animation(s) as FrogLord was unable to resolve the celSet.", misfitModel.getSkeletalAnimations().size());
            return;
        }

        // Consider it an error if there are too few animations.
        int oldXarAnimationCount = mofCelSet.getAnimations().size();
        if (oldXarAnimationCount > misfitModel.getSkeletalAnimations().size())
            logger.severe("The model had %d skeletal/XAR animation(s), but the newly imported model data only has %d.%nThe game may crash when it tries to play the now-missing animation(s).", oldXarAnimationCount, misfitModel.getSkeletalAnimations().size());

        // Clear animation data.
        mofCelSet.getAnimations().clear();
        if (misfitModel.getSkeletalAnimations().size() == 0)
            return; // No animations to setup.

        String transformType = misfitModel.getFirstMetadataValue(METADATA_KEY_TRANSFORM_TYPE);
        if (transformType != null)
            animatedMof.setTransformType(MRAnimatedMofTransformType.valueOf(transformType));

        animatedMof.setStartAtFrameZero(true); // The import logic expects starting at frame zero.

        int newPartCount = misfitModel.getJoints().size();
        for (MMSkeletalAnimationBlock skeletalAnimation : misfitModel.getSkeletalAnimations().getBlocks()) {
            MRAnimatedMofXarAnimation loadedAnimation = new MRAnimatedMofXarAnimation(mofCelSet);
            loadedAnimation.setStaticMofPartCount(newPartCount);

            for (int frame = 0; frame < skeletalAnimation.getFrames().size(); frame++) {
                // Figure out which frames apply to each joint/part.
                MMSkeletalAnimationFrame[] positionFrames = new MMSkeletalAnimationFrame[newPartCount];
                MMSkeletalAnimationFrame[] rotationFrames = new MMSkeletalAnimationFrame[newPartCount];
                for (MMSkeletalAnimationFrame frameInfo : skeletalAnimation.getFrames().get(frame)) {
                    if (frameInfo.getKeyframeType() == MMAnimationKeyframeType.ROTATION) {
                        rotationFrames[frameInfo.getJointIndex()] = frameInfo;
                    } else if (frameInfo.getKeyframeType() == MMAnimationKeyframeType.TRANSLATION) {
                        positionFrames[frameInfo.getJointIndex()] = frameInfo;
                    }
                }

                for (int i = 0; i < newPartCount; i++) {
                    MMSkeletalAnimationFrame rotFrame = rotationFrames[i];
                    MMSkeletalAnimationFrame posFrame = positionFrames[i];

                    // Create rotation matrix.
                    PSXMatrix matrix = new PSXMatrix();
                    if (posFrame != null) {
                        matrix.getTransform()[0] = DataUtils.floatToFixedPointInt4Bit(-posFrame.getX());
                        matrix.getTransform()[1] = DataUtils.floatToFixedPointInt4Bit(-posFrame.getY());
                        matrix.getTransform()[2] = DataUtils.floatToFixedPointInt4Bit(posFrame.getZ());
                    }
                    matrix.updateMatrix(rotFrame != null ? -rotFrame.getX() : 0D, rotFrame != null ? -rotFrame.getY() : 0D, rotFrame != null ? rotFrame.getZ() : 0D);
                    MRAnimatedMofTransform newTransform = animatedMof.getTransformType().makeTransform(matrix); // Create transform from matrix.
                    loadedAnimation.getTransformIds().add((short) animatedMof.getCommonData().getTransforms().size());
                    animatedMof.getCommonData().getTransforms().add(newTransform);
                }

                loadedAnimation.getCelNumbers().add(frame);
            }

            // celNumbers = entry for each frame.
            // indices = each frame has an index for each part, unless the frame exactly matches another frame.
            mofCelSet.getAnimations().add(loadedAnimation);
        }

        MRMofOptimizer.removeDuplicateAnimationTransforms(animatedMof);
    }

    private static List<MRMofPart> getOldMofParts(MisfitModel3DObject misfitModel, MRStaticMof staticMof) {
        int oldPartCount = staticMof.getParts().size();
        int newPartCount = misfitModel.getJoints().size();
        if (oldPartCount == newPartCount)
            return new ArrayList<>(staticMof.getParts());

        List<MRMofPart> oldParts = new ArrayList<>(newPartCount);
        for (int i = 0; i < newPartCount; i++)
            oldParts.add(null);

        // Figure out which parts correspond to what based on name.
        for (MMJointsBlock block : misfitModel.getJoints().getBlocks()) {
            if (block.getName() == null || !block.getName().toLowerCase().startsWith(JOINT_PART_NAME_PREFIX))
                continue;

            String partNumber = block.getName().substring(JOINT_PART_NAME_PREFIX.length());
            if (!NumberUtils.isNumber(partNumber))
                continue;

            int partIndex = Integer.parseInt(partNumber);
            if (partIndex >= 0 && partIndex < oldPartCount)
                oldParts.set(partIndex, staticMof.getParts().get(partIndex));
        }

        return oldParts;
    }

    @SuppressWarnings("ExtractMethodRecommender")
    private static void loadNewMofParts(ILogger logger, MisfitModel3DObject misfitModel, MRStaticMof staticMof) {
        List<MRMofPart> oldParts = getOldMofParts(misfitModel, staticMof);
        int oldFlipbookAnimationCount = staticMof.getFlipbookAnimationCount();
        staticMof.getParts().clear();

        if (oldFlipbookAnimationCount > misfitModel.getFrameAnimations().size()) {
            logger.severe("The model had %d flipbook animation(s), but the newly imported model data only has %d.%nThe game may crash when it tries to play the now-missing flipbook animation(s).", oldFlipbookAnimationCount, misfitModel.getFrameAnimations().size());
        } else if (misfitModel.getFrameAnimations().size() > 0) {
            if (staticMof.getModel().isAnimatedMof())
                logger.severe("Flipbook animations are untested/may not work on ANIMATED models, so please check the %d flipbook animation(s) imported.", misfitModel.getFrameAnimations().size());
        }

        // Calculate the joint mapping of which vertices are mapped to each joint.
        Map<Integer, IntList> vertexIdsPerJointId = new HashMap<>();
        if (misfitModel.getWeightedInfluences().size() > 0) {
            for (MMWeightedInfluencesBlock influence : misfitModel.getWeightedInfluences().getBlocks())
                if (influence.getPositionType() == MMWeightedPositionType.VERTEX)
                    vertexIdsPerJointId.computeIfAbsent(influence.getBoneJointIndex(), boneIndex -> new IntList()).add(influence.getPositionIndex());
        } else if (misfitModel.getJointVertices().size() > 0) { // Fallback to joint vertice block if weighted influences don't exist, but joint vertices do. It likely means the user is using mm3d 1.4.
            for (MMJointVerticesBlock jointVertices : misfitModel.getJointVertices().getBlocks())
                vertexIdsPerJointId.computeIfAbsent(jointVertices.getJointIndex(), boneIndex -> new IntList()).add(jointVertices.getVertexIndex());
        }

        // Prepare to build polygons by gathering data on a per-polygon accessible basis.
        MMTriangleFaceHolder faceHolder = misfitModel.getTriangleFaces();
        Map<MMTriangleFaceBlock, MMTextureCoordinatesBlock> texCoordsPerFace = new HashMap<>();
        for (MMTextureCoordinatesBlock block : misfitModel.getTextureCoordinates().getBlocks())
            texCoordsPerFace.put(faceHolder.getBlocks().get(block.getTriangleIndex()), block);

        Map<MMTriangleFaceBlock, MMTriangleNormalsBlock> normalsPerFace = new HashMap<>();
        for (MMTriangleNormalsBlock block : misfitModel.getNormals().getBlocks())
            normalsPerFace.put(faceHolder.getBlocks().get(block.getTriangleIndex()), block);

        Map<MMTriangleFaceBlock, MMMaterialsBlock> materialsPerFace = new HashMap<>();
        for (MMTriangleGroupsBlock block : misfitModel.getGroups().getBlocks()) {
            MMMaterialsBlock material = block.getMaterial() != MMTriangleGroupsBlock.EMPTY_MATERIAL ? misfitModel.getMaterials().getBody(block.getMaterial()) : null;
            for (int i = 0; i < block.getTriangleIndices().size(); i++) {
                int triangleIndex = block.getTriangleIndices().get(i);
                materialsPerFace.put(faceHolder.getBlocks().get(triangleIndex), material);
            }
        }

        // Determine the texture IDs for each material.
        Map<MMMaterialsBlock, Short> textureIdsPerMaterial = new HashMap<>();
        for (MMMaterialsBlock material : misfitModel.getMaterials().getBlocks()) {
            short textureId = getTextureIdFromMaterial(logger, misfitModel, material);
            if (textureId >= 0)
                textureIdsPerMaterial.put(material, textureId);
        }

        // Figure out which textures are allowed. (All of them) Maybe later we can put extra restrictions to figure out which ones are loaded when the model is loaded.
        Set<Short> allowedTextureIds = new HashSet<>();
        for (VloFile vloArchive : staticMof.getArchive().getAllFiles(VloFile.class))
            for (VloImage gameImage : vloArchive.getImages())
                allowedTextureIds.add(gameImage.getTextureId());

        // Convert each joint to a new MRMofPart.
        Map<MMVerticeBlock, Integer> partCelVertexIdsByVertexBlock = new HashMap<>();
        Map<MMTriangleNormalsBlock, int[]> partCelNormalIdsByNormalBlock = new HashMap<>();
        Map<Integer, List<MMTriangleFaceBlock>> facesPerJoint = getFacesPerJoint(logger, misfitModel, vertexIdsPerJointId);
        Map<Integer, List<MMTriangleNormalsBlock>> normalsPerJoint = getNormalsPerJoint(misfitModel, facesPerJoint);
        for (int partId = 0; partId < misfitModel.getJoints().size(); partId++) {
            MRMofPart oldPart = oldParts.get(partId);
            MRMofPart newPart = new MRMofPart(staticMof);
            staticMof.getParts().add(newPart);

            MRMofPartCel staticPartCel = new MRMofPartCel(newPart);
            newPart.getPartCels().add(staticPartCel);

            // Load Vertices.
            IntList vertexIds = vertexIdsPerJointId.get(partId);
            for (int i = 0; i < vertexIds.size(); i++) {
                MMVerticeBlock block = misfitModel.getVertices().getBody(vertexIds.get(i));
                partCelVertexIdsByVertexBlock.put(block, staticPartCel.getVertices().size());
                staticPartCel.getVertices().add(new SVector(-block.getX(), -block.getY(), block.getZ()));
            }

            // Load Normals.
            List<MMTriangleFaceBlock> jointFaces = facesPerJoint.get(partId);
            List<MMTriangleNormalsBlock> jointNormals = normalsPerJoint.get(partId);
            Map<SVector, Integer> normalIndices = new HashMap<>();
            Integer temp;
            for (MMTriangleNormalsBlock block : jointNormals) {
                int[] trackedIndices = new int[3];

                SVector n1 = new SVector(-block.getV1Normals()[0], -block.getV1Normals()[1], block.getV1Normals()[2]);
                if ((temp = normalIndices.get(n1)) == null) {
                    normalIndices.put(n1, temp = staticPartCel.getNormals().size());
                    staticPartCel.getNormals().add(n1);
                }
                trackedIndices[0] = temp;

                SVector n2 = new SVector(-block.getV2Normals()[0], -block.getV2Normals()[1], block.getV2Normals()[2]);
                if ((temp = normalIndices.get(n2)) == null) {
                    normalIndices.put(n2, temp = staticPartCel.getNormals().size());
                    staticPartCel.getNormals().add(n2);
                }
                trackedIndices[1] = temp;

                SVector n3 = new SVector(-block.getV3Normals()[0], -block.getV3Normals()[1], block.getV3Normals()[2]);
                if ((temp = normalIndices.get(n3)) == null) {
                    normalIndices.put(n3, temp = staticPartCel.getNormals().size());
                    staticPartCel.getNormals().add(n3);
                }
                trackedIndices[2] = temp;

                partCelNormalIdsByNormalBlock.put(block, trackedIndices);
            }

            // Load flipbook animations.
            if (misfitModel.getFrameAnimations().size() > 0) {
                MRMofFlipbookAnimationList flipbook = newPart.getFlipbook();
                for (MMFrameAnimationsBlock block : misfitModel.getFrameAnimations().getBlocks()) {
                    int partCelStartIndex = newPart.getPartCels().size();

                    for (MMAnimationFrame frame : block.getFrames()) {
                        MRMofPartCel newCel = new MRMofPartCel(newPart); // Create new frame of animation.
                        newPart.getPartCels().add(newCel);
                        newCel.getNormals().addAll(staticPartCel.getNormals()); // Frogger always seems to share the same normals across all partcels too.
                        for (MMFloatVertex vertex : frame.getVertexPositions())
                            newCel.getVertices().add(new SVector(-vertex.getX(), -vertex.getY(), vertex.getZ()));
                    }

                    flipbook.getAnimations().add(new MRMofFlipbookAnimation(newPart.getPartCels().size() - partCelStartIndex, partCelStartIndex));
                }
            }

            // Build polygons.
            MRMofPartPolygonBuilder faceBuilder = new MRMofPartPolygonBuilder(newPart);
            for (MMTriangleFaceBlock face : jointFaces) {
                MMMaterialsBlock material = materialsPerFace.get(face);
                MMTriangleNormalsBlock normals = normalsPerFace.get(face);
                MMTextureCoordinatesBlock texCoords = texCoordsPerFace.get(face);

                boolean isTextured = material != null && material.hasTexture();
                boolean hasUniqueNormals = normals != null && (!Arrays.equals(normals.getV1Normals(), normals.getV2Normals()) || !Arrays.equals(normals.getV1Normals(), normals.getV3Normals()));

                // Determine what kind of polygon this will be.
                // We always use Tris, since mm3d doesn't support quads.
                MRMofPolygonType polygonType = isTextured
                        ? (hasUniqueNormals ? MRMofPolygonType.GT3 : MRMofPolygonType.FT3)
                        : (hasUniqueNormals ? MRMofPolygonType.G3 : MRMofPolygonType.F3);

                // Create polygon.
                MRMofPolygon newPolygon = new MRMofPolygon(newPart, polygonType);
                for (int j = 0; j < newPolygon.getVertexCount(); j++) {
                    int vertexIndex = face.getVertices()[j];
                    MMVerticeBlock vertex = misfitModel.getVertices().getBody(vertexIndex);
                    newPolygon.getVertices()[newPolygon.getVertexCount() - j - 1] = partCelVertexIdsByVertexBlock.get(vertex);
                }

                // Apply Normals.
                int[] faceNormalIndices = partCelNormalIdsByNormalBlock.get(normals);
                if (faceNormalIndices != null)
                    for (int j = 0; j < newPolygon.getNormals().length; j++)
                        newPolygon.getNormals()[j] = (short) faceNormalIndices[j];

                // Apply texture data.
                Short textureId = isTextured ? textureIdsPerMaterial.get(material) : null;
                if (isTextured) {
                    if (allowedTextureIds.contains(textureId)) {
                        newPolygon.setTextureId(textureId);
                    } else {
                        logger.severe("Tried to use texture ID %s, but no texture was found using that ID!", textureId);
                    }

                    // Load UVs.
                    if (texCoords != null)
                        for (int j = 0; j < newPolygon.getTextureUvs().length; j++)
                            newPolygon.getTextureUvs()[newPolygon.getVertexCount() - j - 1] = loadTextureUv(logger, newPolygon, texCoords, j);

                    byte red = toByte(material.getAmbient()[0] * 128);
                    byte green = toByte(material.getAmbient()[1] * 128);
                    byte blue = toByte(material.getAmbient()[2] * 128);
                    newPolygon.getColor().fromRGB(ColorUtils.toRGB(red, green, blue));
                } else if (material != null) { // Load the color.
                    byte red = toByte(material.getAmbient()[0] * 255);
                    byte green = toByte(material.getAmbient()[1] * 255);
                    byte blue = toByte(material.getAmbient()[2] * 255);
                    newPolygon.getColor().fromRGB(ColorUtils.toRGB(red, green, blue));
                } else {
                    newPolygon.getColor().fromRGB(0xFFFFFFFF);
                }

                // Register the polygon.
                faceBuilder.addPolygon(newPolygon);
            }

            // Apply polygon data to the mof part.
            faceBuilder.applyPolygonsToPart();

            // Finish part.
            if (oldPart != null)
                MRModelImportUtils.copyNonMeshDataFromOldPartToNewPart(logger, oldPart, newPart);
        }
    }

    private static Map<Integer, List<MMTriangleFaceBlock>> getFacesPerJoint(ILogger logger, MisfitModel3DObject misfitModel, Map<Integer, IntList> vertexIdsPerJointId) {
        // Create a cache of which joint each vertex belongs to, to speed up the second step.
        Map<Integer, Integer> jointIdsPerVertexId = new HashMap<>();
        for (Entry<Integer, IntList> jointVerticeEntry : vertexIdsPerJointId.entrySet()) {
            int jointId = jointVerticeEntry.getKey();
            IntList vertexIds = jointVerticeEntry.getValue();
            for (int i = 0; i < vertexIds.size(); i++) {
                int vertexId = vertexIds.get(i);
                if (jointIdsPerVertexId.put(vertexId, jointId) != null)
                    throw new RuntimeException("The vertexId " + vertexId + " was somehow assigned to multiple joints! (" + jointId + ")");
            }
        }

        // Determine the joints which each face belongs to.
        int failCount = 0;
        Map<Integer, List<MMTriangleFaceBlock>> facesPerJoint = new HashMap<>();
        for (MMTriangleFaceBlock face : misfitModel.getTriangleFaces().getBlocks()) {
            boolean foundMatch = true;
            Integer firstJointId = jointIdsPerVertexId.get(face.getVertices()[0]);
            for (int i = 1; i < face.getVertices().length; i++) {
                if (!Objects.equals(jointIdsPerVertexId.get(face.getVertices()[i]), firstJointId)) {
                    foundMatch = false;
                    break;
                }
            }

            if (foundMatch) {
                facesPerJoint.computeIfAbsent(firstJointId, key -> new ArrayList<>()).add(face);
            } else {
                failCount++;
            }
        }

        if (failCount > 0)
            logger.severe("%d face(s) will be ignored because they were spread across multiple joints.", failCount);

        return facesPerJoint;
    }

    private static Map<Integer, List<MMTriangleNormalsBlock>> getNormalsPerJoint(MisfitModel3DObject misfitModel, Map<Integer, List<MMTriangleFaceBlock>> facesPerJoint) {
        // Create a cache of which joint each vertex belongs to, to speed up the second step.
        Map<MMTriangleFaceBlock, Integer> jointIdsPerFace = new HashMap<>();
        for (Entry<Integer, List<MMTriangleFaceBlock>> entry : facesPerJoint.entrySet()) {
            int jointId = entry.getKey();
            List<MMTriangleFaceBlock> faces = entry.getValue();
            for (MMTriangleFaceBlock face : faces)
                if (jointIdsPerFace.put(face, jointId) != null)
                    throw new RuntimeException("Face ID " + face.getBlockIndex() + " was somehow assigned to multiple joints! (" + jointId + ")");
        }

        // Determine the joints which each face belongs to.
        Map<Integer, List<MMTriangleNormalsBlock>> normalsPerJoint = new HashMap<>();
        for (MMTriangleNormalsBlock normal : misfitModel.getNormals().getBlocks()) {
            MMTriangleFaceBlock face = misfitModel.getTriangleFaces().getBody(normal.getTriangleIndex());
            if (face == null)
                throw new RuntimeException("Triangle Normal #" + normal.getBlockIndex() +  " linked to a non-existent face.");

            Integer jointId = jointIdsPerFace.get(face);
            if (jointId == null)
                throw new RuntimeException("Face #" + face.getBlockIndex() + " was not linked to a joint.");

            normalsPerJoint.computeIfAbsent(jointId, key -> new ArrayList<>()).add(normal);
        }

        return normalsPerJoint;
    }


    private static SCByteTextureUV loadTextureUv(ILogger logger, MRMofPolygon polygon, MMTextureCoordinatesBlock texCoords, int localIndex) {
        float u = texCoords.getXCoordinates()[localIndex];
        float v = texCoords.getYCoordinates()[localIndex];
        if (u <= -.0001 || u >= 1.0001 || v <= -.0001 || v >= 1.0001)
            logger.warning("Loaded out-of-range (invalid) texture coordinates! [%f, %f].", u, v);

        // Clamp U
        if (u < 0) {
            u = 0;
        } else if (u > 1) {
            u = 1;
        }

        // Clamp V
        if (v < 0) {
            v = 0;
        } else if (v > 1) {
            v = 1;
        }

        v = 1F - v; // V must be inverted.

        // Create option UV.
        SCByteTextureUV newUv = new SCByteTextureUV();
        newUv.setSnappedFloatUV(polygon.getDefaultTexture(), u, v);
        return newUv;
    }


    private static byte toByte(float colorValue) {
        return (byte) Math.max(0, Math.min(255, (int) colorValue));
    }

    private static short getTextureIdFromMaterial(ILogger logger, MisfitModel3DObject misfitModel, MMMaterialsBlock material) {
        if (material == null)
            return -1;

        MMExternalTexturesBlock texture = misfitModel.getExternalTextures().getBody(material.getTextureIndex());
        if (texture == null)
            return -1;

        // Extract the file name.
        String strippedFileName = FileUtils.stripExtension(texture.getFileName());

        StringBuilder digitBuilder = new StringBuilder();
        boolean readingDigits = false;
        for (int i = 0; i < strippedFileName.length(); i++) {
            char temp = strippedFileName.charAt(i);
            if (Character.isDigit(temp)) {
                if (!readingDigits) {
                    digitBuilder.setLength(0);
                    readingDigits = true;
                }

                digitBuilder.append(temp);
            } else {
                readingDigits = false;
            }
        }

        // Read it from the file name.
        if (digitBuilder.length() > 0 && digitBuilder.length() <= 5)
            return Short.parseShort(digitBuilder.toString());

        logger.severe("Could not determine the texture ID from the file name '%s'.", texture.getRelativeFilePath());
        return -1;
    }
}