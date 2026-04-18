package net.highwayfrogs.editor.games.sony.shared.pathing;

import net.highwayfrogs.editor.games.psx.math.vector.SVector;

/**
 * Represents a path position which can be edited using a {@code TrackedPositionEditor}.
 * Created by Kneesnap on 2/20/2026.
 */
public interface ITrackedPosition {
    /**
     * Gets the underlying vector which may be updated/changed.
     */
    SVector getPositionVector();

    /**
     * Called when the position is updated/changed.
     */
    void onPositionUpdate();

    /**
     * Updates the preview of this position, if one exists.
     */
    void updatePreview();

    /**
     * Creates an editor object usable for this position
     * @param positionTracker the position to create
     * @return newEditor
     */
    TrackedPositionEditor createEditor(SCPositionTracker positionTracker);

    /**
     * Test if the position is capable of snapping to the given position editor.
     * TODO: Use this in addition to the abstract method in the edit one.
     * @param positionEditor the position editor to try snapping to
     * @return true iff the position can snap to/become part of the editor.
     */
    boolean canSnapTo(TrackedPositionEditor positionEditor);

    /**
     * Snaps to the given position editor.
     * TODO: Use this in addition to the abstract method in the edit one.
     * Throws an exception if snapping cannot occur.
     * @param positionEditor the position editor to snap to
     */
    void snapTo(TrackedPositionEditor positionEditor);
}
