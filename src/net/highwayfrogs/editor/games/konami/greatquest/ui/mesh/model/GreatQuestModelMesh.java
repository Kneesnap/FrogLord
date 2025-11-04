package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcTrack;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcAnimState;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceModel;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceNamedHash;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton.kcNode;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTrack;
import net.highwayfrogs.editor.games.konami.greatquest.model.*;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcCActionSequence;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection;
import net.highwayfrogs.editor.system.math.Matrix4x4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a model mesh for Frogger The Great Quest.
 * The main mesh (this) does not actually contain any mesh data itself despite having the capacity for it, because in order to accurately render texture coordinates, we needed texture repeating, which meant no texture atlas.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestModelMesh extends DynamicMesh {
    @Getter private final kcModelWrapper modelWrapper;
    @Getter private final kcCResourceSkeleton skeleton;
    @Getter private final kcCResourceNamedHash actionSequenceTable;
    @Getter private final ObservableList<kcCResourceTrack> availableAnimations;
    @Getter private final ObservableList<kcCActionSequence> availableSequences;
    @Getter private final DynamicMeshCollection<GreatQuestModelMaterialMesh> actualMesh;
    @Getter private final boolean skeletonAxisRotationApplied;
    @Getter private final boolean environmentalMesh;
    @Getter private kcCResourceTrack activeAnimation;
    @Getter private boolean repeatAnimationOnFinish; // Whether the animation should repeat upon finish.
    @Getter private boolean playingAnimation; // Returns true while playing an animation.
    @Getter private double animationTick;
    @Getter private final GreatQuestModelSkeletonMesh skeletonMesh;

    // Used for
    private final kcAnimState tempAnimationState = new kcAnimState();
    private final List<Matrix4x4f> cachedBoneMatrices = new ArrayList<>();
    private final List<Matrix4x4f> cachedFinalBoneMatrices = new ArrayList<>();

    public static final int TICKS_PER_SECOND = 4800; // Obtained from CGreatQuest::Run()


    public GreatQuestModelMesh(kcCResourceModel resourceModel) {
        this(resourceModel != null ? resourceModel.getModelWrapper() : null, resourceModel != null ? resourceModel.getName() : "dummy");
    }

    public GreatQuestModelMesh(kcModelWrapper modelWrapper) {
        this(modelWrapper, modelWrapper != null ? modelWrapper.getExportName() : "dummy");
    }

    private GreatQuestModelMesh(kcModelWrapper modelWrapper, String meshName) {
        this(modelWrapper, null, null, null, meshName);
    }

    public GreatQuestModelMesh(kcModelWrapper modelWrapper, kcCResourceSkeleton skeleton, List<kcCResourceTrack> animations, kcCResourceNamedHash actionSequenceTable, String meshName) {
        super(null, DynamicMeshTextureQuality.LIT_BLURRY, meshName);
        this.modelWrapper = modelWrapper;
        this.skeleton = skeleton;
        this.actionSequenceTable = actionSequenceTable;
        this.availableAnimations = animations != null ? FXCollections.observableArrayList(animations) : FXCollections.observableArrayList();
        this.availableSequences = actionSequenceTable != null ? FXCollections.observableArrayList(actionSequenceTable.getSequences()) : FXCollections.observableArrayList();
        this.actualMesh = new DynamicMeshCollection<>(getMeshName());
        this.environmentalMesh = modelWrapper != null && GreatQuestEntityManager.isFileNameEnvironmentalMesh(modelWrapper.getFileName());
        this.skeletonMesh = skeleton != null ? new GreatQuestModelSkeletonMesh(this, meshName + "Skeleton") : null;
        if (!this.availableAnimations.isEmpty())
            this.availableAnimations.add(0, null);
        if (!this.availableSequences.isEmpty())
            this.availableSequences.add(0, null);

        // Setup actual mesh.
        kcModel model = modelWrapper != null ? modelWrapper.getModel() : null;
        this.skeletonAxisRotationApplied = (skeleton != null) || hasSkeletonAxisRotation(model);

        if (model != null) {
            List<kcModelPrim>[] modelPrimsByMaterial = getModelPrimsByMaterialIDs(model); // Use as few models as possible.

            // Only adds materials which are actually used.
            for (int i = 0; i < model.getMaterials().size(); i++) {
                List<kcModelPrim> modelPrims = modelPrimsByMaterial[i];
                if (modelPrims != null && !modelPrims.isEmpty())
                    this.actualMesh.addMesh(new GreatQuestModelMaterialMesh(this, model, meshName, model.getMaterials().get(i), modelPrims));
            }

            // Add unresolved materials.
            List<kcModelPrim> nullMaterialPrims = modelPrimsByMaterial[modelPrimsByMaterial.length - 1];
            if (nullMaterialPrims != null && !nullMaterialPrims.isEmpty())
                this.actualMesh.addMesh(new GreatQuestModelMaterialMesh(this, model, meshName, null, nullMaterialPrims));
        } else {
            // Setup placeholder.
            this.actualMesh.addMesh(new GreatQuestModelMaterialMesh(this, null, null));
        }
    }

    @SuppressWarnings("unchecked")
    private static List<kcModelPrim>[] getModelPrimsByMaterialIDs(kcModel model) {
        int nullMaterialID = model.getMaterials().size();
        List<kcModelPrim>[] results = new List[nullMaterialID + 1]; // Slot 0 is null material, and might be copied to other slots.

        List<kcMaterial> materials = model.getMaterials();
        for (kcModelPrim modelPrim : model.getPrimitives()) {
            int materialId = modelPrim.getMaterialId();
            kcMaterial material = materialId >= 0 && materials.size() > materialId ? materials.get(materialId) : null;
            if (material == null || !material.hasTexture() || material.getTexture() == null)
                materialId = nullMaterialID;

            List<kcModelPrim> list = results[materialId];
            if (list == null)
                results[materialId] = list = new ArrayList<>();

            list.add(modelPrim);
        }

        return results;
    }

    /**
     * Gets the model represented by this mesh.
     */
    public kcModel getModel() {
        return this.modelWrapper != null ? this.modelWrapper.getModel() : null;
    }

    /**
     * Changes which animation is currently displayed by the mesh
     * @param newAnimation the animation to display
     */
    public void setActiveAnimation(kcCResourceTrack newAnimation) {
        setActiveAnimation(newAnimation, false);
    }

    /**
     * Changes which animation is currently displayed by the mesh
     * @param newAnimation the animation to display
     * @param repeat if true, the animation will repeat upon completion.
     */
    public void setActiveAnimation(kcCResourceTrack newAnimation, boolean repeat) {
        if (newAnimation == this.activeAnimation)
            return; // No change.

        this.activeAnimation = newAnimation;
        this.repeatAnimationOnFinish = repeat;
        this.playingAnimation = true;
        this.animationTick = 0;
        updateMeshes();
    }

    /**
     * Applies the given number of animation ticks
     * @param deltaTimeSeconds the amount of time (in seconds) to increase the animation state by
     */
    public void tickAnimation(float deltaTimeSeconds) {
        if (this.playingAnimation)
            setAnimationTick(this.animationTick + ((double) deltaTimeSeconds * TICKS_PER_SECOND));
    }

    /**
     * Sets the current tick for the active animation, and update the mesh to reflect this new time.
     * @param animationTick the tick to apply
     */
    public void setAnimationTick(double animationTick) {
        if (!Double.isFinite(animationTick))
            throw new IllegalArgumentException("Invalid animation tick: " + animationTick);

        if (animationTick == this.animationTick)
            return; // No change.

        this.animationTick = animationTick;
        if (this.activeAnimation != null)
            updateMeshes();
    }

    private void updateMeshes() {
        recalculateBoneMatrices();
        if (this.skeletonMesh != null)
            this.skeletonMesh.updateVertices();

        for (int i = 0; i < this.actualMesh.getMeshes().size(); i++) {
            GreatQuestModelMaterialMesh mesh = this.actualMesh.getMeshes().get(i);
            mesh.updateVertices(); // TODO: We should also update texCoords to animate UVs, sWaterParam contains per-level water speed data.
        }
    }

    /**
     * Recalculates the bone matrices for all bones using the current animation state.
     */
    public void recalculateBoneMatrices() {
        kcCResourceSkeleton skeleton = getSkeleton();
        if (skeleton == null)
            return;

        recalculateBoneMatrices(skeleton.getRootNode());
    }

    /**
     * Recalculates the bone matrices for the current animation state.
     * @param startNode the node to calculate matrices for
     */
    public void recalculateBoneMatrices(kcNode startNode) {
        if (startNode == null)
            throw new NullPointerException("startNode");

        this.playingAnimation = false;
        List<kcNode> nodeQueue = new ArrayList<>();
        nodeQueue.add(startNode);
        while (nodeQueue.size() > 0) {
            kcNode tempNode = nodeQueue.remove(0);
            nodeQueue.addAll(tempNode.getChildren());

            // Ensure we have storage to store the matrices.
            while (tempNode.getTag() >= this.cachedBoneMatrices.size())
                this.cachedBoneMatrices.add(new Matrix4x4f());
            while (tempNode.getTag() >= this.cachedFinalBoneMatrices.size())
                this.cachedFinalBoneMatrices.add(new Matrix4x4f());

            // Get the bone's transform in local bone space.
            Matrix4x4f localTransform = this.cachedBoneMatrices.get(tempNode.getTag());
            if (this.activeAnimation != null) { // Apply animation.
                List<kcTrack> tracks = this.activeAnimation.getTracksByTag(tempNode.getTag());
                this.tempAnimationState.reset(tempNode);
                boolean moreAnimationLeft = this.tempAnimationState.evaluate(tempNode, this.animationTick, tracks);
                localTransform = this.tempAnimationState.getLocalOffsetMatrix(localTransform);
                if (moreAnimationLeft)
                    this.playingAnimation = true;
            } else { // Use default bone transform.
                localTransform = tempNode.getLocalOffsetMatrix(localTransform);
            }

            // Convert the bone transform into model space.
            Matrix4x4f globalTransform = localTransform;
            Matrix4x4f parentTransform = tempNode.getParent() != null ? this.cachedBoneMatrices.get(tempNode.getParent().getTag()) : null;
            if (parentTransform != null) // The OpenGL example may multiply in the opposite order, but this is what works, and I believe its done by the game, seen in EnumEval -> the second kcMtxStkMul().
                globalTransform = localTransform.multiply(parentTransform, globalTransform);

            // When multiplying a vertex, the vertex will be in model space, but globalTransform is in local space.
            Matrix4x4f finalTransform = this.cachedFinalBoneMatrices.get(tempNode.getTag());
            tempNode.getModelToBoneMatrix().multiply(globalTransform, finalTransform); // This is the same as multiplying a vector against the two matrices separately.
        }

        // Restart the animation if configured.
        if (!this.playingAnimation && this.repeatAnimationOnFinish) {
            this.playingAnimation = true;
            this.animationTick = 0;
        }
    }

    /**
     * Gets the bone transform for the given bone tag/id.
     * @param tag the bone tag to find the current transform by
     * @return boneTransform
     */
    public Matrix4x4f getBoneTransform(int tag) {
        if (tag >= this.cachedBoneMatrices.size())
            recalculateBoneMatrices();

        if (tag >= this.cachedBoneMatrices.size() || tag < 0)
            throw new IllegalArgumentException("Invalid bone tag: " + tag);

        return this.cachedBoneMatrices.get(tag);
    }

    /**
     * Gets the final bone transform for the given bone tag/id.
     * @param tag the bone tag to find the current transform by
     * @return boneTransform
     */
    public Matrix4x4f getFinalBoneTransform(int tag) {
        if (tag >= this.cachedFinalBoneMatrices.size())
            recalculateBoneMatrices();

        if (tag >= this.cachedFinalBoneMatrices.size() || tag < 0)
            throw new IllegalArgumentException("Invalid bone tag: " + tag);

        return this.cachedFinalBoneMatrices.get(tag);
    }

    private static boolean hasSkeletonAxisRotation(kcModel model) {
        if (model == null)
            return false;
        if (model.getBonesPerPrimitive() > 0 && model.hasBoneWeights())
            return true;

        for (int i = 0; i < model.getNodes().size(); i++) {
            kcModelNode modelNode = model.getNodes().get(i);
            if (modelNode.getNodeId() > 0)
                return true; // The Bone ID is non-zero! The model probably has weights.
        }

        return false;
    }
}