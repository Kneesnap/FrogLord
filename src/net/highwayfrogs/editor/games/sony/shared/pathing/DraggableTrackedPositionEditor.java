package net.highwayfrogs.editor.games.sony.shared.pathing;

import javafx.geometry.Point3D;
import javafx.scene.shape.Box;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.fxobject.FX3DDragController;
import net.highwayfrogs.editor.gui.fxobject.FX3DDragController.LazyFX3DDragController;
import net.highwayfrogs.editor.utils.Scene3DUtils;

/**
 * Represents the basic implementation of editing a 2D spline point/a point snapped to the ground.
 * Created by Kneesnap on 2/23/2026.
 */
@Getter
public abstract class DraggableTrackedPositionEditor extends TrackedPositionEditor {
    private final FX3DDragController dragController;
    private final Box box;

    private static final float BOX_SIZE = 2.5F; // TODO: Consider size reduction.

    public DraggableTrackedPositionEditor(@NonNull SCPositionTracker positionTracker) {
        super(positionTracker);
        this.box = new Box(BOX_SIZE, BOX_SIZE, BOX_SIZE);
        this.dragController = new LazyFX3DDragController(positionTracker.getController(), this::onDragPositionChange, this::onDragPositionAccepted);
        this.dragController.setShape(this.box);
    }

    @Override
    protected void onAddToDisplayList(DisplayList displayList) {
        displayList.add(this.box);
        this.dragController.getController().getMainLight().getScope().add(this.box);
    }

    @Override
    protected void onRemoveFromDisplayList(DisplayList displayList) {
        displayList.remove(this.box);
        this.dragController.getController().getMainLight().getScope().remove(this.box);
    }

    @Override
    public void onPositionChange(float newX, float newY, float newZ) {
        super.onPositionChange(newX, newY, newZ);

        // If not currently being dragged, update position.
        if (!this.dragController.isMouseDragActive())
            Scene3DUtils.setNodePosition(this.box, newX, newY, newZ);
    }

    @Override
    public void updateDisplayPosition() {
        Scene3DUtils.setNodePosition(this.box, this.targetPosition.getFloatX(), this.targetPosition.getFloatY(), this.targetPosition.getFloatZ());
    }

    @Override
    public void setVisible(boolean visible) {
        this.box.setVisible(visible);
    }

    private Point3D onDragPositionChange(Point3D oldPosition, Point3D newPosition) {
        TrackedPositionEditor snapTarget = setTargetPosition((float) newPosition.getX(), (float) newPosition.getY(), (float) newPosition.getZ());
        if (snapTarget != null) { // Snap to the target.
            SVector snapTargetPos = snapTarget.targetPosition;
            return new Point3D(snapTargetPos.getFloatX(), snapTargetPos.getFloatY(), snapTargetPos.getFloatZ());
        } else {
            return newPosition;
        }
    }

    private void onDragPositionAccepted(Point3D oldPosition, Point3D newPosition) {
        trySnapToTarget();
    }
}
