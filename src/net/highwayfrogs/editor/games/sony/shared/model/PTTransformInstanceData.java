package net.highwayfrogs.editor.games.sony.shared.model;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.IVector;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.psx.math.PSXMatrix;
import net.highwayfrogs.editor.games.sony.SCGameObject.SCSharedGameObject;
import net.highwayfrogs.editor.games.sony.shared.math.PTQuaternionTranslation;
import net.highwayfrogs.editor.games.sony.shared.model.skeleton.PTSkeletonBone;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticFile;

/**
 * Contains instanced data for transforms.
 * Created by Kneesnap on 5/22/2024.
 */
@Getter
public class PTTransformInstanceData extends SCSharedGameObject {
    private final PTModel model;
    private final int id;
    private int flags;
    private final PSXMatrix localMatrix = new PSXMatrix();
    private final PSXMatrix matrix = new PSXMatrix();
    private final PTQuaternionTranslation interpolationKeyFrameTransform; // Virtual keyframe for interpolating between actions.
    private final PTQuaternionTranslation localTransform; // The part local transform.

    // Skeleton bone flags appear valid here.
    public static final int FLAG_HAS_PREVIOUS_BONE = PTSkeletonBone.FLAG_HAS_PREVIOUS_BONE;
    public static final int FLAG_USE_LOCAL_TRANSLATION = PTSkeletonBone.FLAG_USE_LOCAL_TRANSLATION;
    public static final int FLAG_USE_OFFSET_AS_SCALE = PTSkeletonBone.FLAG_USE_OFFSET_AS_SCALE;

    // Other flags.
    public static final int FLAG_OVERRIDE_LOCAL_MATRIX = Constants.BIT_FLAG_3;
    public static final int FLAG_OVERRIDE_LOCAL_TRANSLATION = Constants.BIT_FLAG_4;
    public static final int FLAG_OVERRIDE_LOCAL_TRANSLATION_USE_BONE = Constants.BIT_FLAG_5;
    public static final int FLAG_OVERRIDE_MATRIX = Constants.BIT_FLAG_6;
    public static final int FLAG_OVERRIDE_TRANSLATION = Constants.BIT_FLAG_7;
    public static final int FLAG_INTERPOLATION_NEXT = Constants.BIT_FLAG_8;
    public static final int FLAG_INTERPOLATION_PREVIOUS = Constants.BIT_FLAG_9;
    public static final int FLAG_INTERPOLATION_TRIGGER = Constants.BIT_FLAG_10;
    public static final int FLAG_APPLY_LOCAL_MATRIX = Constants.BIT_FLAG_11;
    public static final int FLAG_DETACHED = Constants.BIT_FLAG_12;
    public static final int FLAG_MASK_NO_UPDATE = FLAG_OVERRIDE_MATRIX | FLAG_OVERRIDE_TRANSLATION;
    public static final int FLAG_MASK_NO_LOCAL_UPDATE = FLAG_OVERRIDE_LOCAL_MATRIX | FLAG_OVERRIDE_LOCAL_TRANSLATION;
    public static final int FLAG_MASK_INTERPOLATION = FLAG_INTERPOLATION_PREVIOUS | FLAG_INTERPOLATION_NEXT;
    public static final int FLAG_MASK_OVERRIDE_MATRICES = FLAG_OVERRIDE_LOCAL_MATRIX | FLAG_OVERRIDE_MATRIX;

    public PTTransformInstanceData(PTModel model, int id) {
        super(model.getGameInstance());
        this.model = model;
        this.id = id;
        this.interpolationKeyFrameTransform = new PTQuaternionTranslation(getGameInstance());
        this.localTransform = new PTQuaternionTranslation(getGameInstance());
    }

    /**
     * Gets the skeleton bone.
     */
    public PTSkeletonBone getBone() {
        return this.model.getSkeletonFile() != null ? this.model.getSkeletonFile().getBones().get(this.id) : null;
    }

    /**
     * Get the bit flags.
     */
    public int getFlags() {
        int boneFlags;
        if (this.model.getSkeletonFile() != null && (this.model.getStaticMeshFile().getFlags() & PTStaticFile.FLAG_HAS_SKELETON) == PTStaticFile.FLAG_HAS_SKELETON) {
            boneFlags = getBone().getFlags();
        } else {
            boneFlags = FLAG_USE_LOCAL_TRANSLATION;
        }

        return this.flags | boneFlags;
    }

    /**
     * Updates the transform.
     */
    public void update() {
        PTSkeletonBone bone = getBone();

        // No update should occur.
        int flags = getFlags();
        if ((flags & FLAG_MASK_NO_UPDATE) == FLAG_MASK_NO_UPDATE)
            return;

        // Determine current/previous transforms. Previous transform is the parent bone.
        PSXMatrix previousTransform;
        if ((flags & FLAG_HAS_PREVIOUS_BONE) == FLAG_HAS_PREVIOUS_BONE) {
            previousTransform = this.model.getTransformData().get(bone.getPreviousBoneID()).getMatrix();
        } else {
            // Relative to the model.
            previousTransform = this.model.getMatrix();
        }

        // Process skeletal animation transform.
        if ((flags & FLAG_OVERRIDE_MATRIX) == 0) {
            // TODO: This might not be valid.
            PSXMatrix.MRMulMatrixABC(this.localMatrix, previousTransform, this.matrix);
        }

        // Update the translation.
        if ((flags & FLAG_OVERRIDE_TRANSLATION) == 0) {
            // Copy the previous transform's position to the current one.
            System.arraycopy(previousTransform.getTransform(), 0, this.matrix.getTransform(), 0, this.matrix.getTransform().length);

            SVector inputVec;
            if ((flags & (FLAG_OVERRIDE_LOCAL_TRANSLATION | FLAG_USE_LOCAL_TRANSLATION)) == FLAG_USE_LOCAL_TRANSLATION) {
                inputVec = new SVector(this.localMatrix.getTransform()[0], this.localMatrix.getTransform()[1], this.localMatrix.getTransform()[2]);
            } else {
                inputVec = bone.getJointOffset();
            }

            // Apply the transform.
            IVector result = new IVector();
            PSXMatrix.MRApplyMatrix(this.matrix, inputVec, result); // TODO: Format could be wrong, not sure.
            this.matrix.getTransform()[0] = result.getX();
            this.matrix.getTransform()[1] = result.getY();
            this.matrix.getTransform()[2] = result.getZ();
        }
    }
}