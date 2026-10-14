package net.highwayfrogs.editor.games.sony.shared.model;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.PSXMatrix;
import net.highwayfrogs.editor.games.psx.math.vector.IVector;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.SCGameObject.SCSharedGameObject;
import net.highwayfrogs.editor.games.sony.shared.math.PTQuaternion;
import net.highwayfrogs.editor.games.sony.shared.model.actionset.PTAction;
import net.highwayfrogs.editor.games.sony.shared.model.actionset.PTActionSet;
import net.highwayfrogs.editor.games.sony.shared.collprim.PTCollprim;
import net.highwayfrogs.editor.games.sony.shared.model.actionset.PTActionSetFile;
import net.highwayfrogs.editor.games.sony.shared.model.skeleton.PTSkeletonFile;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticFile;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticPart;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticPartCel;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a unified 3D model, containing a static mesh, and optionally, supplemental animation files.
 * Seems to be called 'PT_MODEL' in the game by looking at the game binaries.
 * Created by Kneesnap on 5/21/2024.
 */
public class PTModel extends SCSharedGameObject {
    @Getter private final PTStaticFile staticMeshFile;
    @Getter private final PTSkeletonFile skeletonFile;
    @Getter private final PTActionSetFile actionSetFile;
    @Getter private final List<PTTransformInstanceData> transformData;
    @Getter private final List<PTPartInstanceData> partData;
    @Getter private final PSXMatrix matrix = new PSXMatrix();

    public PTModel(PTStaticFile staticMeshFile, PTSkeletonFile skeletonFile, PTActionSetFile actionSetFile) {
        super(staticMeshFile.getGameInstance());
        this.staticMeshFile = staticMeshFile;
        this.skeletonFile = skeletonFile;
        this.actionSetFile = actionSetFile;

        // Create transform data.
        this.transformData = new ArrayList<>();
        for (int i = 0; i < getTransformCount(); i++)
            this.transformData.add(new PTTransformInstanceData(this, i));

        // Create part data.
        this.partData = new ArrayList<>();
        for (int i = 0; i < staticMeshFile.getParts().size(); i++)
            this.partData.add(new PTPartInstanceData(staticMeshFile.getParts().get(i)));
    }

    /**
     * Gets the name of the model.
     */
    public String getName() {
        String name = null;
        if (this.staticMeshFile != null)
            name = FileUtils.stripExtension(this.staticMeshFile.getFileDisplayName());
        if (this.actionSetFile != null && StringUtils.isNullOrWhiteSpace(name))
            name = FileUtils.stripExtension(this.actionSetFile.getFileDisplayName());
        if (this.skeletonFile != null && StringUtils.isNullOrWhiteSpace(name))
            name = FileUtils.stripExtension(this.skeletonFile.getFileDisplayName());

        return name;
    }

    /**
     * Gets the number of static mesh parts used in the model.
     */
    public int getPartCount() {
        return this.staticMeshFile.getParts().size();
    }

    /**
     * Gets the collprim of the static mesh.
     */
    public PTCollprim getStaticCollprim() {
        return this.staticMeshFile.getCollprim();
    }

    /**
     * Get the number of transforms.
     */
    public int getTransformCount() {
        return this.skeletonFile != null ? this.skeletonFile.getBones().size() : this.staticMeshFile.getParts().size();
    }


    /**
     * Initialize/update the animation preview.
     * TODO: Support animation selection, playback, and keyframe interpolation.
     */
    public void updateAnimation() {
        if (this.actionSetFile == null)
            return; // If there's no animation file, we can't update.

        for (PTActionSet actionSet : this.actionSetFile.getActionSets()) {
            for (PTAction action : actionSet.getActions()) {
                if (action.getKeyFrameCount() > 0 && action.getTransformCount() > 0
                        && action.getRootTransformIndex() + action.getTransformCount() <= getTransformCount()) {
                    applyKeyFrame(action, 0);
                    return;
                }
            }
        }
    }

    /**
     * Applies an action keyframe to the local transforms. Update the hierarchy and skin afterwards.
     */
    public void applyKeyFrame(PTAction action, int keyFrame) {
        if (keyFrame < 0 || keyFrame >= action.getKeyFrameCount())
            throw new IllegalArgumentException("Invalid keyframe: " + keyFrame);
        if (action.getRootTransformIndex() + action.getTransformCount() > getTransformCount())
            throw new IllegalArgumentException("The action has more transforms than the model.");

        for (int i = 0; i < action.getTransformCount(); i++) {
            PTTransformInstanceData transform = this.transformData.get(action.getRootTransformIndex() + i);
            int flags = transform.getFlags();
            int index = action.getTransformationIndices()[i][keyFrame] & PTAction.TRANSFORM_INDEX_MASK;
            PTQuaternion quaternion = (flags & PTTransformInstanceData.FLAG_USE_LOCAL_TRANSLATION) != 0
                    ? action.getActionSet().getRotationWithTranslations().get(index)
                    : action.getActionSet().getRotations().get(index);
            PSXMatrix pose = quaternion.createMatrix();
            if ((flags & (PTTransformInstanceData.FLAG_OVERRIDE_LOCAL_MATRIX | PTTransformInstanceData.FLAG_OVERRIDE_MATRIX)) == 0) {
                transform.getLocalMatrix().setMatrix(pose.getMatrix());
                // Preserve the exported quaternion's sign: skin interpolation depends on it.
                transform.getLocalTransform().setC(quaternion.getC());
                transform.getLocalTransform().setX(quaternion.getX());
                transform.getLocalTransform().setY(quaternion.getY());
                transform.getLocalTransform().setZ(quaternion.getZ());
            }
            if ((flags & PTTransformInstanceData.FLAG_USE_LOCAL_TRANSLATION) != 0
                    && (flags & (PTTransformInstanceData.FLAG_OVERRIDE_LOCAL_TRANSLATION | PTTransformInstanceData.FLAG_OVERRIDE_TRANSLATION)) == 0) {
                if ((flags & PTTransformInstanceData.FLAG_USE_OFFSET_AS_SCALE) != 0) {
                    SVector scale = transform.getBone().getJointOffset();
                    pose.getTransform()[0] = (pose.getTransform()[0] * scale.getX()) >> 12;
                    pose.getTransform()[1] = (pose.getTransform()[1] * scale.getY()) >> 12;
                    pose.getTransform()[2] = (pose.getTransform()[2] * scale.getZ()) >> 12;
                }
                System.arraycopy(pose.getTransform(), 0, transform.getLocalMatrix().getTransform(), 0, 3);
                for (int axis = 0; axis < 3; axis++)
                    transform.getLocalTransform().getTranslation()[axis] = (short) pose.getTransform()[axis];
            }
        }
    }

    /**
     * Update the static model.
     */
    public void updateStaticModel() {
        int transformCount = getTransformCount();
        for (int i = 0; i < transformCount; i++)
            this.transformData.get(i).update();
    }

    /**
     * Update mime and skin vector data.
     * TODO: Support interpolation.
     */
    public void updateMimeAndSkin() {
        for (int i = 0; i < getPartCount(); i++) {
            PTStaticPart part = this.staticMeshFile.getParts().get(i);
            PTPartInstanceData partData = this.partData.get(i);
            PTStaticPartCel partCel = part.getPartCels().get(partData.getCurrentPartCel());

            // MIME
            if ((part.getFlags() & PTStaticPart.FLAG_MIME_ENABLED) == PTStaticPart.FLAG_MIME_ENABLED) {
                if ((partData.getLastInterpolationProgress() != partData.getInterpolationProgress()) && (partData.getInterpolationProgress() != 0)) {
                    // PTStaticPartCel endPartCel = part.getPartCels().get(partData.getTargetPartCel());
                    for (int j = 0; j < part.getMimeVectors(); j++)
                        partData.getVectorBlock()[j].setValues(partCel.getMimeVectors().get(j));

                    partData.setLastInterpolationProgress(partData.getInterpolationProgress());
                }
            }

            // Skin
            if (partCel.getSkinVectorCount() != 0) {
                // Toolkit skinning rotates about the current joint; it does not blend world positions.
                PSXMatrix relativeRotation = new PSXMatrix();
                PTQuaternion relativeQuaternion = new PTQuaternion(getGameInstance());
                if (part.getTransformId() < this.transformData.size() && part.getConnectedTransformId() < this.transformData.size()) {
                    PTTransformInstanceData transform = this.transformData.get(part.getTransformId());
                    if (this.actionSetFile != null && transform.getBone() != null
                            && (transform.getFlags() & PTTransformInstanceData.FLAG_HAS_PREVIOUS_BONE) != 0
                            && transform.getBone().getPreviousBoneID() == part.getConnectedTransformId()
                            && (transform.getFlags() & (PTTransformInstanceData.FLAG_MASK_OVERRIDE_MATRICES | PTTransformInstanceData.FLAG_APPLY_LOCAL_MATRIX)) == 0) {
                        // The animated hierarchy path uses the conjugate of the original local quaternion.
                        // Converting the world matrices back to a quaternion would lose its sign.
                        PTQuaternion local = transform.getLocalTransform();
                        relativeQuaternion.setC(local.getC());
                        relativeQuaternion.setX((short) -local.getX());
                        relativeQuaternion.setY((short) -local.getY());
                        relativeQuaternion.setZ((short) -local.getZ());
                        for (int row = 0; row < 3; row++)
                            for (int col = 0; col < 3; col++)
                                relativeRotation.getMatrix()[row][col] = transform.getLocalMatrix().getMatrix()[col][row];
                    } else {
                        PSXMatrix current = transform.getMatrix();
                        PSXMatrix inverse = new PSXMatrix();
                        for (int row = 0; row < 3; row++)
                            for (int col = 0; col < 3; col++)
                                inverse.getMatrix()[row][col] = current.getMatrix()[col][row];
                        PSXMatrix.MRMulMatrixABC(inverse, this.transformData.get(part.getConnectedTransformId()).getMatrix(), relativeRotation);
                        relativeQuaternion.fromMatrix(relativeRotation);
                    }
                }
                int lastWeight = -1;
                PSXMatrix weightedRotation = relativeRotation;
                IVector result = new IVector();
                int skinVectorOffset = part.getMimeVectors() + part.getMimeSkinVectors();
                for (int j = 0; j < partCel.getSkinVectorCount(); j++) {
                    SVector source = partCel.getVectors().get(j);
                    SVector target = partData.getVectorBlock()[skinVectorOffset + j];
                    int weight = source.getUnsignedPadding();
                    if (weight != lastWeight) {
                        weightedRotation = createSkinRotation(relativeRotation, relativeQuaternion, weight);
                        lastWeight = weight;
                    }
                    PSXMatrix.MRApplyRotMatrix(weightedRotation, source, result);
                    target.setValues((short) result.getX(), (short) result.getY(), (short) result.getZ());
                    target.setPadding(source.getPadding());
                }
            }
        }
    }

    private PSXMatrix createSkinRotation(PSXMatrix relativeRotation, PTQuaternion quaternion, int weight) {
        if (weight == 0)
            return PSXMatrix.IDENTITY;
        if (weight == 4096)
            return relativeRotation;
        if (weight > 4096)
            throw new IllegalArgumentException("Invalid PT skin weight: " + weight);

        // vPTModelUpdateMIMEandSKIN interpolates identity -> relative rotation WITHOUT flipping
        // the quaternion sign to choose the shortest arc. This distinction matters near 180 degrees.
        double t = weight / 4096D;
        double cosine = Math.max(-1D, Math.min(1D, quaternion.getC() / 4096D));
        double startScale = 1D - t;
        double endScale = t;
        if (1D - cosine > 1D / 4096D) {
            double omega = Math.acos(cosine);
            endScale = Math.sin(t * omega) / Math.sin(omega);
            startScale = Math.cos(t * omega) - cosine * endScale;
        }
        PTQuaternion weighted = new PTQuaternion(getGameInstance());
        weighted.setC((short) Math.round(startScale * 4096D + endScale * quaternion.getC()));
        weighted.setX((short) Math.round(endScale * quaternion.getX()));
        weighted.setY((short) Math.round(endScale * quaternion.getY()));
        weighted.setZ((short) Math.round(endScale * quaternion.getZ()));
        return weighted.createMatrix();
    }
}