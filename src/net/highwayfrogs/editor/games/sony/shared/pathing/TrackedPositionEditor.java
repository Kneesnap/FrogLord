package net.highwayfrogs.editor.games.sony.shared.pathing;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents something capable of changing MediEvil path positions.
 * Created by Kneesnap on 2/18/2026.
 */
@RequiredArgsConstructor
public abstract class TrackedPositionEditor {
    @Getter @NonNull protected final SCPositionTracker tracker;
    @NonNull protected final SVector targetPosition = new SVector(); // This must NOT be modified by any method other than setTargetPosition()
    @NonNull protected final SVector tempVector = new SVector();
    @NonNull private final List<ITrackedPosition> linkedPositions = new ArrayList<>();
    @NonNull protected final List<ITrackedPosition> immutableLinkedPositions = Collections.unmodifiableList(this.linkedPositions);
    @NonNull private final List<DisplayList> displayLists = new ArrayList<>();
    protected TrackedPositionEditor pendingSnapTarget; // The currently tracked snap target.

    /**
     * Gets the target position in a way which ensures the caller cannot directly modify the original vector.
     */
    public SVector getTargetPosition() {
        this.tempVector.setValues(this.targetPosition);
        return this.tempVector;
    }

    /**
     * Links a position to this position editor.
     * @param trackedPosition the position to link
     * @return true iff the position has been linked successfully
     */
    public boolean linkPosition(ITrackedPosition trackedPosition) {
        if (trackedPosition == null)
            throw new NullPointerException("editablePosition");

        SVector position = trackedPosition.getPositionVector();
        if (position == null)
            throw new IllegalArgumentException(Utils.getSimpleName(trackedPosition) + " returned a null position vector!");

        if (this.linkedPositions.isEmpty()) {
            this.targetPosition.setValues(position);
            updateDisplayPosition();
        } else if (!this.targetPosition.equals(position)) { // Wrong position!
            throw new IllegalArgumentException("The provided editablePosition (" + trackedPosition + ") is located at " + position + ", while the position editor targets " + this.targetPosition + ".");
        }

        return !this.linkedPositions.contains(trackedPosition) && this.linkedPositions.add(trackedPosition);
    }

    /**
     * Unlinks a position from this position editor.
     * @param editablePosition the position to unlink
     * @return true iff the position has been unlinked successfully
     */
    public boolean unlinkPosition(ITrackedPosition editablePosition) {
        if (editablePosition == null)
            throw new NullPointerException("editablePosition");

        boolean success = this.linkedPositions.remove(editablePosition);
        if (success && this.linkedPositions.isEmpty())
            if (this.tracker.removeEditor(this.targetPosition) != this)
                throw new IllegalStateException("Successfully unlinked the final position, but failed to unlink from the tracker!");

        return success;
    }

    /**
     * Gets the list of linked positions.
     */
    public List<ITrackedPosition> getLinkedPositions() {
        return this.immutableLinkedPositions;
    }

    /**
     * Called when the target position changes.
     */
    protected void onPositionChange(float newX, float newY, float newZ) {
        // Apply the new position.
        for (int i = 0; i < this.linkedPositions.size(); i++) {
            ITrackedPosition position = this.linkedPositions.get(i);
            position.getPositionVector().setValues(newX, newY, newZ);
            position.onPositionUpdate();
        }

        // Update preview hooks after the update has been applied.
        for (int i = 0; i < this.linkedPositions.size(); i++)
            this.linkedPositions.get(i).updatePreview();
    }

    private boolean trySetTargetPosition(float newX, float newY, float newZ) {
        short newFixedPosX = DataUtils.floatToFixedPointShort4Bit(newX);
        short newFixedPosY = DataUtils.floatToFixedPointShort4Bit(newY);
        short newFixedPosZ = DataUtils.floatToFixedPointShort4Bit(newZ);
        this.tempVector.setValues(newFixedPosX, newFixedPosY, newFixedPosZ);
        if (this.tempVector.equals(this.targetPosition))
            return false; // The position has not changed.

        TrackedPositionEditor snapTarget = this.tracker.getEditor(this.tempVector);
        if (snapTarget != null)
            return false; // A snap target already exists at this position, so it is not possible to place it at this new position.

        TrackedPositionEditor oldEditor = this.tracker.removeEditor(this.targetPosition);
        if (oldEditor != this)
            throw new IllegalStateException("Failed to remove the position from the tracker. (Has the position vector been directly modified?) (Removed Editor: " + oldEditor + ")");

        // Update the position and add to the editor.
        this.targetPosition.setValues(newX, newY, newZ);
        this.tracker.addEditor(this, null);

        // Call the position change hook.
        onPositionChange(newX, newY, newZ);
        return true;
    }

    /**
     * Updates the position at which the display is placed.
     */
    public abstract void updateDisplayPosition();

    /**
     * Sets the target position.
     * The resulting position may be slightly different from the one provided due to a mix of fixed point decimal precision,
     *  snapping to nearby position editors, and an underlying restriction that two different position editors must not occupy the same world space.
     * @param newPosX the new world X position to apply
     * @param newPosY the new world Y position to apply
     * @param newPosZ the new world Z position to apply
     * @return the editor which this will be snapped to when the position is confirmed
     */
    public TrackedPositionEditor setTargetPosition(float newPosX, float newPosY, float newPosZ) {
        // 1) Try calculating the pending snap.
        List<SVector> nearbyPositions = this.tracker.findTrackedPositionsNear(newPosX, newPosY, newPosZ, (float) TranslationGizmo.SNAPPING_THRESHOLD);
        this.pendingSnapTarget = null;
        double smallestDistanceSq = Double.MAX_VALUE;
        for (int i = 0; i < nearbyPositions.size(); i++) {
            SVector testPos = nearbyPositions.get(i);
            if (testPos.equals(this.targetPosition))
                continue; // Don't try snapping to the previous position, it would be impossible to move anything anywhere.

            double testDistanceSq = (newPosX - testPos.getFloatX()) * (newPosX - testPos.getFloatX())
                    + (newPosY - testPos.getFloatY()) * (newPosY - testPos.getFloatY())
                    + (newPosZ - testPos.getFloatZ()) * (newPosZ - testPos.getFloatZ());
            if (testDistanceSq >= smallestDistanceSq)
                continue; // Distance is closer than expected.

            TrackedPositionEditor otherEditor = this.tracker.getEditor(testPos);
            if (otherEditor == this)
                continue; // Shouldn't happen, but would prevent us from moving stuff.

            // Test merge/snapability.
            if (canSnapTo(otherEditor)) {
                this.pendingSnapTarget = otherEditor;
                smallestDistanceSq = testDistanceSq;
            }
        }

        // 2) Update positions.
        // If a pending snap target exists, we should not update the position until the snap actually occurs.
        // This is because the Octree and position editor data structures are not capable of holding two editors at the same position.
        // Therefore, we must keep our current position until the signal is received to snap to the target.
        if (this.pendingSnapTarget == null) {
            trySetTargetPosition(newPosX, newPosY, newPosZ); // If this returns false, we can safely ignore it. The target position thing will be ignored since there already exists another editor at this position, so we can't use it.
            return null;
        }

        return this.pendingSnapTarget;
    }

    /**
     * Attempts to snap to the pending snap target.
     */
    protected void trySnapToTarget() {
        if (this.pendingSnapTarget != null) {
            snapTo(this.pendingSnapTarget);
            this.pendingSnapTarget = null;
        }
    }

    /**
     * Test if it is possible to snap to another position editor, and merge to become one.
     * @param other the other position editor to test against
     */
    public abstract boolean canSnapTo(TrackedPositionEditor other);

    /**
     * Implements on snap behavior for the tracked position editor
     * @param other the other editor to snap to
     */
    protected abstract void onSnapTo(TrackedPositionEditor other);

    /**
     * Snaps to the other position editor
     * @param other the position editor to snap to.
     */
    protected void snapTo(TrackedPositionEditor other) {
        if (other == null)
            throw new NullPointerException("other");
        if (other == this)
            return; // Makes no sense to snap to self.

        // Remove self from tracker.
        TrackedPositionEditor oldEditor = this.tracker.removeEditor(this.targetPosition);
        if (oldEditor != this)
            throw new IllegalStateException("Failed to remove the position from the tracker. (Has the position vector been directly modified?) (Removed Editor: " + oldEditor + ")");

        // Run position updates to apply the new snapped position to the positions seen here.
        this.targetPosition.setValues(other.targetPosition);
        onPositionChange(other.targetPosition.getFloatX(), other.targetPosition.getFloatY(), other.targetPosition.getFloatZ());

        // Run snap hook.
        onSnapTo(other);

        // Move linked positions to the new editor.
        for (int i = 0; i < this.linkedPositions.size(); i++)
            other.linkPosition(this.linkedPositions.get(i));
        this.linkedPositions.clear();

        // Remove the nodes from all display lists.
        removeFromDisplayLists();
    }

    /**
     * Adds the display preview to the display list.
     * @param displayList the display list to add to
     */
    protected void addToDisplayList(DisplayList displayList) {
        if (this.displayLists.contains(displayList))
            return;

        this.displayLists.add(displayList);
        onAddToDisplayList(displayList);
    }

    /**
     * Adds the display preview to the display list.
     * @param displayList the display list to add to
     */
    protected abstract void onAddToDisplayList(DisplayList displayList);

    /**
     * Removes the display preview from the display lists.
     */
    protected void removeFromDisplayLists() {
        for (int i = this.displayLists.size() - 1; i >= 0; i--)
            onRemoveFromDisplayList(this.displayLists.remove(i--));
    }

    /**
     * Removes the display preview from the display list.
     * @param displayList the display list to remove from
     */
    protected abstract void onRemoveFromDisplayList(DisplayList displayList);

    /**
     * Sets if the position editor is visible or not
     * @param visible if it should be visible
     */
    public abstract void setVisible(boolean visible);

    /**
     * Creates the user interface for editing the position at this spot.
     * @param editorGrid the grid to add the position editor to
     */
    public void setupUI(String name, GUIEditorGrid editorGrid) {
        editorGrid.addFloatVector(name, getTargetPosition(),
                () -> onPositionChange(this.tempVector.getFloatX(), this.tempVector.getFloatY(), this.tempVector.getFloatZ()),
                this.tracker.getController());
    }
}
