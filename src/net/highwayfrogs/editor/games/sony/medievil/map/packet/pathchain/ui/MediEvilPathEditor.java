package net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.ui;

import lombok.NonNull;
import net.highwayfrogs.editor.games.sony.shared.pathing.DraggableGizmoTrackedPositionEditor;
import net.highwayfrogs.editor.games.sony.shared.pathing.ITrackedPosition;
import net.highwayfrogs.editor.games.sony.shared.pathing.SCPositionTracker;
import net.highwayfrogs.editor.games.sony.shared.pathing.TrackedPositionEditor;
import net.highwayfrogs.editor.games.sony.shared.spline.MRBezierCurve.MRBezierCurvePointType;

/**
 * Represents the 3D editor UI for an individual MediEvil path position.
 * Created by Kneesnap on 2/24/2026.
 */
class MediEvilPathEditor extends DraggableGizmoTrackedPositionEditor {
    public MediEvilPathEditor(@NonNull SCPositionTracker positionTracker) {
        super(positionTracker, true);
    }

    @Override
    protected void onPositionChange(float newX, float newY, float newZ) {
        super.onPositionChange(newX, newY, newZ);

        // Attempt to find a single path group node to choose to load the UI with.
        MediEvilMapPathSplineTrackedPosition startPosition = null;
        MediEvilMapPathSplineTrackedPosition endPosition = null;
        int startPoints = 0;
        int endPoints = 0;
        for (int i = 0; i < this.immutableLinkedPositions.size(); i++) {
            ITrackedPosition position = this.immutableLinkedPositions.get(i);
            if (!(position instanceof MediEvilMapPathSplineTrackedPosition))
                continue;

            MediEvilMapPathSplineTrackedPosition trackedPosition = (MediEvilMapPathSplineTrackedPosition) position;
            if (trackedPosition.getPointType() == MRBezierCurvePointType.START_POINT) {
                startPoints++;
                startPosition = trackedPosition;
            } else if (trackedPosition.getPointType() == MRBezierCurvePointType.END_POINT) {
                endPoints++;
                endPosition = trackedPosition;
            }
        }

        if (startPoints == 1 || (startPoints > 0 && endPoints != 1)) {
            startPosition.getPathManager().setSelectedSpline(startPosition.getSpline());
        } else if (endPoints > 0) {
            endPosition.getPathManager().setSelectedSpline(endPosition.getSpline());
        }
    }

    @Override
    public boolean canSnapTo(TrackedPositionEditor other) {
        return false; // TODO: Implement.
    }

    @Override
    protected void onSnapTo(TrackedPositionEditor other) {
        // TODO: Implement.
    }
}
