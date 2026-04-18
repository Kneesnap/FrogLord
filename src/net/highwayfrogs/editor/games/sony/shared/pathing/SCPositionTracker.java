package net.highwayfrogs.editor.games.sony.shared.pathing;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.shared.pointcloud.PointCloudOctree;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks points in 3D space.
 * Created by Kneesnap on 2/23/2026.
 */
@RequiredArgsConstructor
public class SCPositionTracker {
    @Getter @NonNull private final MeshViewController<?> controller;
    private final Map<SVector, TrackedPositionEditor> positionMap = new HashMap<>();
    private final PointCloudOctree octree = new PointCloudOctree();

    /**
     * Obtains the position editor for the given position, if an editor exists for the given position.
     * @param position the position to obtain the editor for
     * @return editor, or null if none is present
     */
    public TrackedPositionEditor getEditor(SVector position) {
        return this.positionMap.get(position);
    }

    /**
     * Adds a position tracker, creating the corresponding editor if necessary
     * @param trackedPosition the tracked position to add
     */
    public boolean addPositionTracker(ITrackedPosition trackedPosition, DisplayList displayList) {
        if (trackedPosition == null)
            throw new NullPointerException("positionEditor");

        SVector position = trackedPosition.getPositionVector();
        if (position == null)
            throw new NullPointerException("trackedPosition.getPositionVector()");

        TrackedPositionEditor editor = this.positionMap.get(position);
        boolean createEditor = (editor == null);
        if (createEditor)
            editor = trackedPosition.createEditor(this);

        if (!trackedPosition.canSnapTo(editor))
            throw new IllegalArgumentException("Cannot add the vector at " + trackedPosition + " to the incompatible editor: " + Utils.getSimpleName(editor) + ".");

        boolean success = editor.linkPosition(trackedPosition);
        if (success && createEditor) // Adds the editor.
            addEditor(editor, displayList);

        return success;
    }

    /**
     * Adds an editor.
     * @param positionEditor the editor to add
     */
    public void addEditor(TrackedPositionEditor positionEditor, DisplayList displayList) {
        if (positionEditor == null)
            throw new NullPointerException("positionEditor");
        if (positionEditor.immutableLinkedPositions.isEmpty())
            throw new IllegalArgumentException("Cannot track a position editor which does not have any positions linked.");

        SVector clonedPosition = positionEditor.targetPosition.clone(); // Changes to the original position should not be reflected in the Octree or hashmap keys.
        TrackedPositionEditor oldEditor = this.positionMap.putIfAbsent(clonedPosition, positionEditor);
        if (oldEditor != null)
            throw new IllegalArgumentException("Position " + clonedPosition + " already has an editor registered.");

        if (!this.octree.insert(clonedPosition))
            throw new IllegalStateException("The PointCloudOctree already had the position " + clonedPosition + " tracked, which should not be possible!");

        if (displayList != null)
            positionEditor.addToDisplayList(displayList);
    }

    /**
     * Removes an editor from the tracker.
     * @param position the position to remove the editor for
     * @return the removed editor, or null if no editor was removed
     */
    public TrackedPositionEditor removeEditor(SVector position) {
        if (position == null)
            throw new NullPointerException("position");

        TrackedPositionEditor editor = this.positionMap.remove(position);
        if (editor == null)
            return null;

        if (!this.octree.remove(position))
            throw new IllegalStateException("The PointCloudOctree did not track the position " + position + " tracked, which should not be possible!");

        return editor;
    }

    /**
     * Find a list of all tracked positions near the given position
     * @param posX the x position at the center of the search sphere area
     * @param posY the y position at the center of the search sphere area
     * @param posZ the z position at the center of the search sphere area
     * @param radius the radius of the search sphere area
     * @return list of tracked positions
     */
    public List<SVector> findTrackedPositionsNear(float posX, float posY, float posZ, float radius) {
        return this.octree.find(posX, posY, posZ, radius);
    }
}
