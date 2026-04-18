package net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.ui;

import net.highwayfrogs.editor.games.sony.medievil.map.packet.spline.IMediEvilMapSpline;
import net.highwayfrogs.editor.games.sony.shared.pathing.ITrackedPosition;
import net.highwayfrogs.editor.games.sony.shared.spline.MRBezierCurve;
import net.highwayfrogs.editor.games.sony.shared.spline.MRBezierCurve.MRBezierCurvePointType;

/**
 * Represents a tracked position
 * Created by Kneesnap on 2/23/2026.
 */
interface IMediEvilPathSplineTrackedPosition extends ITrackedPosition {
    /**
     * Gets the path manager.
     */
    MediEvilPathManager getPathManager();

    /**
     * Gets the spline which the position is tracked for.
     */
    IMediEvilMapSpline getSpline();

    /**
     * Gets the Bézier curve which is edited.
     */
    MRBezierCurve getBezierCurve();

    /**
     * Identifies the position in the Bézier curve which this tracks.
     */
    MRBezierCurvePointType getPointType();
}
