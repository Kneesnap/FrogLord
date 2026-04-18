package net.highwayfrogs.editor.games.sony.medievil.map.packet.spline;

import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.MediEvilMapPathChain;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain.ui.MediEvilPathManager;
import net.highwayfrogs.editor.games.sony.shared.spline.MRSplineMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

import java.util.List;

/**
 * Represents a spline in MediEvil.
 * Created by Kneesnap on 2/18/2026.
 */
public interface IMediEvilMapSpline {
    /**
     * Gets a list of subdivisions.
     */
    List<SVector> getSubDivisions();

    /**
     * Gets the path chain node which contains this spline.
     * @return pathChainNode
     */
    MediEvilMapPathChain getPathChain();

    /**
     * Gets the unique ID of the spline.
     */
    short getUniqueId();

    /**
     * Gets the local ID of the spline within the file.
     * @return id
     */
    int getId();

    /**
     * Sets up editor UI for this specific spline implementation.
     * @param manager the UI manager requesting the UI
     * @param editorGrid the editor grid to create the UI with
     */
    void setupEditor(MediEvilPathManager manager, GUIEditorGrid editorGrid);

    /**
     * Generates subdivisions and other spline data based on the provided curve
     * @param splineMatrix the spline matrix to load the curve from
     */
    void applySplineMatrix(MRSplineMatrix splineMatrix);

    /**
     * Applies the given spline matrix to the subDivisions in this spline, without any other changes to the spline.
     * @param splineMatrix the spline matrix to calculate subdivisions from
     */
    default void applySplineMatrixToSubDivisions(MRSplineMatrix splineMatrix) {
        List<SVector> subDivisions = getSubDivisions();
        for (int i = 0; i < subDivisions.size(); i++) {
            int t = (int) (MRSplineMatrix.SPLINE_PARAM_ONE * ((float) i / (subDivisions.size() - 1)));
            splineMatrix.evaluatePosition(subDivisions.get(i), t);
        }
    }
}
