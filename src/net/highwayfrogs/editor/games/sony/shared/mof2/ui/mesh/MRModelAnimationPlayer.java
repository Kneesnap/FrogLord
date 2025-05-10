package net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMofModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMofXarAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.flipbook.MRMofFlipbookAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.flipbook.MRMofFlipbookAnimationList;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;

import java.util.List;

/**
 * Tracks the data necessary for MOF animation playback. Both static and XAR mofs are animated through this class.
 * In the official MR API, flipbook animations cannot be mixed with XAR animations, so this is not supported.
 * Created by Kneesnap on 2/25/2025.
 */
@Getter
public class MRModelAnimationPlayer {
    @NonNull private final MRModelMesh modelMesh;
    @Setter private boolean animationPlaying;
    @Setter private boolean textureAnimationEnabled;
    // There can be only one XAR animation at a time, and there must be no flipbook animation active.
    // Flipbook animations come from the animated XAR file/animated MOF file, and more specifically an individual modelSet/celSet.
    private int xarAnimationId = ANIMATION_ID_NONE;
    // There can be only one flipbook animation at a time, and there must be no XAR animation active.
    // Flipbook animations come from the static mof.
    private int flipbookAnimationId = ANIMATION_ID_NONE;
    // There can be any number of texture animations active at once.
    // Texture animations come from the static mof.
    private int animationTick; // we need it to be a single like that for the slider.

    public static final int ANIMATION_ID_NONE = -1;

    public MRModelAnimationPlayer(MRModelMesh modelMesh) {
        this.modelMesh = modelMesh;
    }

    /**
     * Gets the model which is animated by the player.
     */
    public MRModel getModel() {
        return this.modelMesh.getModel();
    }

    /**
     * Gets the active XAR animation, if there is one.
     * @return xarAnimation
     */
    public MRAnimatedMofXarAnimation getXarAnimation() {
        if (this.xarAnimationId < 0)
            return null;

        MRAnimatedMofModel mofModel = getModelMesh().getActiveMofModel();
        if (mofModel == null)
            return null;

        List<MRAnimatedMofXarAnimation> animations = mofModel.getParentModelSet().getCelSet().getAnimations();
        return animations.size() > this.xarAnimationId ? animations.get(this.xarAnimationId) : null;
    }

    /**
     * Gets the active flipbook animation, if there is one.
     * @return flipbookAnimation
     */
    public MRMofFlipbookAnimation getFlipbookAnimation() {
        if (this.flipbookAnimationId < 0)
            return null;

        MRStaticMof staticMof = getModelMesh().getActiveStaticMof();
        if (staticMof == null)
            return null;

        List<MRMofPart> mofParts = staticMof.getParts();
        if (mofParts.size() != 1) // In theory more can be supported I think, but until we ever see this in practice, an error will be our approach.
            throwException("Static Mof had " + mofParts.size() + " parts, but only one was expected.");

        MRMofFlipbookAnimationList animations = staticMof.getParts().get(0).getFlipbook();
        if (animations == null || this.flipbookAnimationId >= animations.getAnimations().size())
            return null;

        return animations.getAnimations().get(this.flipbookAnimationId);
    }

    private void throwException(String message) {
        this.animationPlaying = false; // Halt playback to avoid error spam.
        throw new RuntimeException(message);
    }

    /**
     * Updates the mesh.
     */
    public void updateMesh() {
        getModelMesh().pushBatchOperations();
        for (int i = 0; i < getModelMesh().getModelMeshPartNodes().size(); i++)
            getModelMesh().getModelMeshPartNodes().get(i).updateAnimatedPolygons();
        getModelMesh().popBatchOperations();
    }

    /**
     * Sets the animation tick currently active
     * @param newTick the animation tick to apply
     */
    public void setAnimationTick(int newTick) {
        if (this.animationTick == newTick)
            return;

        this.animationTick = newTick;
        updateMesh();
    }

    /**
     * Advance to the next animation tick.
     */
    public void applyNextAnimationTick() {
        this.animationTick++;
        updateMesh();
    }

    /**
     * Regress to the previous animation tick.
     */
    public void applyPreviousAnimationTick() {
        this.animationTick--;
        updateMesh();
    }

    /**
     * Remove all active animations.
     */
    public void setNoActiveAnimation() {
        boolean shouldUpdate = this.flipbookAnimationId != ANIMATION_ID_NONE || this.xarAnimationId != ANIMATION_ID_NONE;

        this.animationTick = 0;
        this.flipbookAnimationId = ANIMATION_ID_NONE;
        this.xarAnimationId = ANIMATION_ID_NONE;
        if (shouldUpdate)
            updateMesh();
    }

    /**
     * Apply the default animation. (What I suspect was probably shown in Mappy)
     */
    public void setDefaultAnimation() {
        this.animationTick = 0;

        MRStaticMof staticMof = getModelMesh().getActiveStaticMof();
        this.flipbookAnimationId = staticMof != null && staticMof.getFlipbookAnimationCount() > 0 ? 0 : ANIMATION_ID_NONE;

        MRAnimatedMofModel animatedMof = getModelMesh().getActiveMofModel();
        this.xarAnimationId = animatedMof != null && animatedMof.getParentModelSet().getCelSet().getAnimations().size() > 0 ? 0 : ANIMATION_ID_NONE;
        updateMesh();
    }

    /**
     * Set the flipbook animation id.
     * @param flipbookActionId The frame to use.
     */
    public void setFlipbookAnimationID(int flipbookActionId, boolean removeXarAnimation) {
        if (flipbookActionId < 0)
            flipbookActionId = ANIMATION_ID_NONE;
        if (flipbookActionId == this.flipbookAnimationId)
            return;
        if (flipbookActionId == ANIMATION_ID_NONE) {
            this.animationTick = 0;
            this.flipbookAnimationId = ANIMATION_ID_NONE;
            if (removeXarAnimation)
                this.xarAnimationId = ANIMATION_ID_NONE;

            updateMesh();
            return;
        }

        MRStaticMof staticMof = getModelMesh().getActiveStaticMof();
        if (staticMof == null) {
            throwException("The staticMof was null, so there are no flipbook animations available!");
            return; // Hides warning.
        }

        List<MRMofPart> mofParts = staticMof.getParts();
        if (mofParts.size() != 1) // In theory more can be supported I think, but until we ever see this in practice, an error will be our approach.
            throwException("Static Mof had " + mofParts.size() + " parts, but only one was expected.");

        MRMofFlipbookAnimationList animations = staticMof.getParts().get(0).getFlipbook();
        if (animations == null || this.flipbookAnimationId >= animations.getAnimations().size())
            throwException("The flipbook ID " + flipbookActionId + " does not appear to be valid.");

        this.animationTick = 0;
        this.flipbookAnimationId = flipbookActionId;
        if (removeXarAnimation)
            this.xarAnimationId = ANIMATION_ID_NONE;

        updateMesh();
    }

    /**
     * Set the xar animation id.
     * @param xarAnimationId The xar animation id.
     */
    public void setXarAnimationID(int xarAnimationId, boolean removeFlipbookAnimation) {
        if (xarAnimationId < 0)
            xarAnimationId = ANIMATION_ID_NONE;
        if (xarAnimationId == this.xarAnimationId)
            return;
        if (xarAnimationId == ANIMATION_ID_NONE) {
            this.animationTick = 0;
            this.xarAnimationId = ANIMATION_ID_NONE;
            if (removeFlipbookAnimation)
                this.flipbookAnimationId = ANIMATION_ID_NONE;

            updateMesh();
            return;
        }

        MRAnimatedMofModel mofModel = getModelMesh().getActiveMofModel();
        if (mofModel == null) {
            throwException("There is no MRAnimatedMofModel to lookup the XAR Animation ID (" + xarAnimationId + ") from.");
            return; // Hides warning.
        }

        List<MRAnimatedMofXarAnimation> animations = mofModel.getParentModelSet().getCelSet().getAnimations();
        if (xarAnimationId >= animations.size())
            throwException("There is no XAR Animation accessible with the ID (" + xarAnimationId + ").");

        this.animationTick = 0;
        this.xarAnimationId = xarAnimationId;
        if (removeFlipbookAnimation)
            this.flipbookAnimationId = ANIMATION_ID_NONE;

        updateMesh();
    }
}
