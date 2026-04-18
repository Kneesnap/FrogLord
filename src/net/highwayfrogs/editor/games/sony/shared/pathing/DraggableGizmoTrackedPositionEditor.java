package net.highwayfrogs.editor.games.sony.shared.pathing;

import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Scale;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.fxobject.FX3DDragController;
import net.highwayfrogs.editor.gui.fxobject.FX3DDragController.LazyDragTranslationGizmo;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo;
import net.highwayfrogs.editor.utils.Scene3DUtils;

/**
 * Represents the basic implementation of editing a 3D spline point.
 * TODO: Control snap to terrain.
 * Created by Kneesnap on 2/23/2026.
 */
@Getter
public abstract class DraggableGizmoTrackedPositionEditor extends TrackedPositionEditor {
    private final FX3DDragController dragController;
    private final TranslationGizmo translationGizmo;
    private final MeshView meshView;

    private static final Scale TRANSLATION_GIZMO_SCALE = new Scale(.6F, .25F, .6F);

    public DraggableGizmoTrackedPositionEditor(@NonNull SCPositionTracker positionTracker, boolean enableYAxis) {
        super(positionTracker);
        this.translationGizmo = new TranslationGizmo(false, enableYAxis, false);
        this.meshView = new MeshView();

        // Setup the drag controller.
        this.dragController = new LazyDragTranslationGizmo(positionTracker.getController(), this.translationGizmo, this::onDragPositionChange, this::onDragPositionAccepted);
        this.dragController.setShape(this.meshView);
    }

    @Override
    protected void onAddToDisplayList(DisplayList displayList) {
        displayList.add(this.meshView);
        this.dragController.getController().getMainLight().getScope().add(this.meshView);
    }

    @Override
    protected void onRemoveFromDisplayList(DisplayList displayList) {
        displayList.remove(this.meshView);
        this.dragController.getController().getMainLight().getScope().remove(this.meshView);
    }

    @Override
    protected void onPositionChange(float newX, float newY, float newZ) {
        super.onPositionChange(newX, newY, newZ);

        // If not currently being dragged, update position.
        if (!this.dragController.isMouseDragActive() && !this.translationGizmo.isDraggingActive())
            Scene3DUtils.setNodePosition(this.meshView, newX, newY, newZ);
    }

    @Override
    public void updateDisplayPosition() {
        Scene3DUtils.setNodePosition(this.meshView, this.targetPosition.getFloatX(), this.targetPosition.getFloatY(), this.targetPosition.getFloatZ());
        Scene3DUtils.setNodeScale(this.meshView, TRANSLATION_GIZMO_SCALE.getX(), TRANSLATION_GIZMO_SCALE.getY(), TRANSLATION_GIZMO_SCALE.getZ());
    }

    @Override
    public void setVisible(boolean visible) {
        this.meshView.setVisible(visible);
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
