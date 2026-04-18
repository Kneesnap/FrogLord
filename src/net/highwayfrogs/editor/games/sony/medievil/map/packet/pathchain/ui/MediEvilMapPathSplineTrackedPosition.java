package net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.ui;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.ui.MediEvilPathManager.MediEvilPathPreview;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.ui.MediEvilPathManager.MediEvilSplinePreview;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.spline.IMediEvilMapSpline;
import net.highwayfrogs.editor.games.sony.shared.pathing.SCPositionTracker;
import net.highwayfrogs.editor.games.sony.shared.pathing.TrackedPositionEditor;
import net.highwayfrogs.editor.games.sony.shared.spline.MRBezierCurve;
import net.highwayfrogs.editor.games.sony.shared.spline.MRBezierCurve.MRBezierCurvePointType;
import net.highwayfrogs.editor.games.sony.shared.spline.MRSplineMatrix;

/**
 * Implements path position tracking for a point on a MediEvil 3D spline, so an editor can exist.
 * Created by Kneesnap on 2/23/2026.
 */
@Getter
@RequiredArgsConstructor
class MediEvilMapPathSplineTrackedPosition implements IMediEvilPathSplineTrackedPosition {
    @NonNull private final MediEvilPathManager pathManager;
    @NonNull private final IMediEvilMapSpline spline;
    @NonNull private final MRBezierCurve bezierCurve;
    @NonNull private final MRBezierCurvePointType pointType;

    @Override
    public SVector getPositionVector() {
        return this.bezierCurve.getPosition(this.pointType);
    }

    @Override
    public void onPositionUpdate() {
        MediEvilPathPreview preview = this.pathManager.getDelegatesByValue().get(this.spline.getPathChain());
        if (preview == null)
            return;

        MediEvilSplinePreview splinePreview = preview.getSplinePreview(this.spline);
        if (splinePreview == null)
            return;

        MRSplineMatrix splineMatrix = splinePreview.getSplineMatrix();
        this.bezierCurve.toSplineMatrix(splineMatrix);
        this.spline.applySplineMatrix(splineMatrix);
    }

    @Override
    public void updatePreview() {
        MediEvilPathPreview preview = this.pathManager.getDelegatesByValue().get(this.spline.getPathChain());
        if (preview != null)
            preview.updatePreview();
    }

    @Override
    public TrackedPositionEditor createEditor(SCPositionTracker positionTracker) {
        return new MediEvilPathEditor(positionTracker);
    }

    @Override
    public boolean canSnapTo(TrackedPositionEditor positionEditor) {
        return true; // TODO: Implement.
    }

    @Override
    public void snapTo(TrackedPositionEditor positionEditor) {
        // TODO: Implement.
    }
}
