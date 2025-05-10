package net.highwayfrogs.editor.games.sony.shared.spline;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo.IPositionChangeListener;

import java.util.UUID;

/**
 * Represents the MR_SPLINE_HERMITE struct.
 * Created by Kneesnap on 12/14/2023.
 */
@Getter
public class MRSplineHermite extends SCSharedGameData {
    private final SVector startPoint = new SVector(); // sh_p1
    private final SVector endPoint = new SVector(); // sh_p4
    private final IVector startTangent = new IVector(); // sh_r1
    private final IVector endTangent = new IVector(); // sh_r4

    public static final int SIZE_IN_BYTES = (2 * SVector.PADDED_BYTE_SIZE) + (2 * IVector.PADDED_BYTE_SIZE);
    private static final int MR_SPLINE_WORLD_SHIFT = 3;
    private static final UUID EDITOR_ID = UUID.randomUUID();

    public MRSplineHermite(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.startPoint.loadWithPadding(reader);
        this.endPoint.loadWithPadding(reader);
        this.startTangent.loadWithPadding(reader);
        this.endTangent.loadWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.startPoint.saveWithPadding(writer);
        this.endPoint.saveWithPadding(writer);
        this.startTangent.saveWithPadding(writer);
        this.endTangent.saveWithPadding(writer);
    }

    /**
     * Creates an editor for this object.
     * @param controller The controller to create the editor under.
     * @param grid the grid to add UI elements to
     * @param changeListener Listener for any change to the curve.
     */
    public void setupEditor(MeshViewController<?> controller, GUIEditorGrid grid, Runnable changeListener) {
        IPositionChangeListener changeHook = null;
        if (changeListener != null)
            changeHook = (meshView, oldX, oldY, oldZ, newX, newY, newZ, flags) -> changeListener.run();

        grid.addPositionEditor(controller, EDITOR_ID, "Start Point", this.startPoint, changeHook);
        grid.addPositionEditor(controller, EDITOR_ID, "End Point", this.endPoint, changeHook);
        grid.addPositionOffsetEditor(controller, EDITOR_ID, "Start Tangent", this.startTangent, this.startPoint, changeHook);
        grid.addPositionOffsetEditor(controller, EDITOR_ID, "End Tangent", this.endTangent, this.endPoint, changeHook);
    }

    /**
     * Calculates the coefficient matrix from the 4x3 hermite boundary conditions.
     * Implementation of 'MRCalculateSplineHermiteMatrix'
     * See Computer Graphics, Foley-Van Dam, p. 484, (11.19) Multiplies:	0
     */
    public MRSplineMatrix toSplineMatrix() {
        return toSplineMatrix(null);
    }

    /**
     * Calculates the coefficient matrix from the 4x3 hermite boundary conditions.
     * Implementation of 'MRCalculateSplineHermiteMatrix'
     * See Computer Graphics, Foley-Van Dam, p. 484, (11.19) Multiplies:	0
     */
    public MRSplineMatrix toSplineMatrix(MRSplineMatrix matrix) {
        if (matrix == null)
            matrix = new MRSplineMatrix(getGameInstance());

        int p1x, p1y, p1z;
        int p4x, p4y, p4z;
        int r1x, r1y, r1z;
        int r4x, r4y, r4z;

        p1x = this.startPoint.getX() >> MR_SPLINE_WORLD_SHIFT;
        p1y = this.startPoint.getY() >> MR_SPLINE_WORLD_SHIFT;
        p1z = this.startPoint.getZ() >> MR_SPLINE_WORLD_SHIFT;

        p4x = this.endPoint.getX() >> MR_SPLINE_WORLD_SHIFT;
        p4y = this.endPoint.getY() >> MR_SPLINE_WORLD_SHIFT;
        p4z = this.endPoint.getZ() >> MR_SPLINE_WORLD_SHIFT;

        r1x = this.startTangent.getX() >> MR_SPLINE_WORLD_SHIFT;
        r1y = this.startTangent.getY() >> MR_SPLINE_WORLD_SHIFT;
        r1z = this.startTangent.getZ() >> MR_SPLINE_WORLD_SHIFT;

        r4x = this.endTangent.getY() >> MR_SPLINE_WORLD_SHIFT;
        r4y = this.endTangent.getY() >> MR_SPLINE_WORLD_SHIFT;
        r4z = this.endTangent.getZ() >> MR_SPLINE_WORLD_SHIFT;

        int[][] splineMatrix = matrix.getMatrix();
        splineMatrix[0][0] = (2 * p1x) - (2 * p4x) + r1x + r4x;
        splineMatrix[0][1] = (2 * p1y) - (2 * p4y) + r1y + r4y;
        splineMatrix[0][2] = (2 * p1z) - (2 * p4z) + r1z + r4z;

        // New version using no multiplies
        splineMatrix[1][0] = -splineMatrix[0][0] - p1x + p4x - r1x;
        splineMatrix[1][1] = -splineMatrix[0][1] - p1y + p4y - r1y;
        splineMatrix[1][2] = -splineMatrix[0][2] - p1z + p4z - r1z;

        splineMatrix[2][0] = r1x;
        splineMatrix[2][1] = r1y;
        splineMatrix[2][2] = r1z;

        splineMatrix[3][0] = p1x;
        splineMatrix[3][1] = p1y;
        splineMatrix[3][2] = p1z;

        return matrix;
    }
}