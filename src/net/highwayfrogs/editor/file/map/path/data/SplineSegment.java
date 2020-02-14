package net.highwayfrogs.editor.file.map.path.data;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.path.*;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Arrays;

/**
 * Holds Spline segment data.
 * Not entirely sure how we'll edit much of this data.
 * This could be using polynomial curves to form a shape. https://en.wikipedia.org/wiki/Spline_(mathematics)
 * Created by Kneesnap on 9/16/2018.
 */
public class SplineSegment extends PathSegment {
    private int[][] splineMatrix = new int[4][3];
    private int[] smoothT = new int[4];
    private int[][] smoothC = new int[4][3];

    private static final int SPLINE_FIX_INTERVAL = 0x200;

    private static final int SPLINE_WORLD_SHIFT = 3;
    private static final int SPLINE_PARAM_SHIFT = 11;
    private static final int SPLINE_T2_SHIFT = 3;

    public SplineSegment() {
        super(PathType.SPLINE, false);
    }

    @Override
    protected void loadData(DataReader reader) {
        // Read MR_SPLINE_MATRIX:
        for (int i = 0; i < splineMatrix.length; i++)
            for (int j = 0; j < splineMatrix[i].length; j++)
                this.splineMatrix[i][j] = reader.readInt();

        // Read ps_smooth_t
        for (int i = 0; i < smoothT.length; i++)
            this.smoothT[i] = reader.readInt();

        // Read ps_smooth_c:
        for (int i = 0; i < smoothC.length; i++)
            for (int j = 0; j < smoothC[i].length; j++)
                this.smoothC[i][j] = reader.readInt();
    }

    @Override
    protected void saveData(DataWriter writer) {
        for (int[] arr : this.splineMatrix)
            for (int val : arr)
                writer.writeInt(val);

        for (int val : this.smoothT)
            writer.writeInt(val);

        for (int[] arr : this.smoothC)
            for (int val : arr)
                writer.writeInt(val);
    }

    @Override
    protected PathResult calculatePosition(PathInfo info) {
        return new PathResult(calculateSplinePoint(info.getSegmentDistance()), calculateSplineTangent(info.getSegmentDistance()), false);
    }

    // What follows is insanely nasty, but it is what the game engine does, so we have no choice...
    private int getSplineParamFromLength(int length) {
        length <<= 5;
        int d = length;

        int i;
        for (i = 3; i > 0; i--) {
            d = length - (smoothT[i - 1] >> SPLINE_WORLD_SHIFT);
            if (d >= 0)
                break;
        }

        if (i == 0)
            d = length;

        int e = d;
        d = 0;
        d += (smoothC[i][0] >> SPLINE_PARAM_SHIFT);
        d *= e;
        d >>= 13;

        d += (smoothC[i][1] >> SPLINE_PARAM_SHIFT);
        d *= e;
        d >>= 13;

        d += (smoothC[i][2] >> SPLINE_PARAM_SHIFT);
        d *= e;
        d >>= 13;
        d >>= 5;

        d += (i * SPLINE_FIX_INTERVAL);
        return (d << 1) >> 1;
    }

    // I hate this.
    private SVector calculateSplinePoint(int distance) {
        int t = getSplineParamFromLength(distance);
        int t2 = (t * t) >> SPLINE_T2_SHIFT;
        int t3 = (t2 * t) >> SPLINE_PARAM_SHIFT;

        SVector pos = new SVector();

        pos.setX((short) (((t3 * splineMatrix[0][0]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t2 * splineMatrix[1][0]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t * splineMatrix[2][0]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT)) +
                ((splineMatrix[3][0]) << SPLINE_WORLD_SHIFT)));

        pos.setY((short) (((t3 * splineMatrix[0][1]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t2 * splineMatrix[1][1]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t * splineMatrix[2][1]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT)) +
                ((splineMatrix[3][1]) << SPLINE_WORLD_SHIFT)));

        pos.setZ((short) (((t3 * splineMatrix[0][2]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t2 * splineMatrix[1][2]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t * splineMatrix[2][2]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT)) +
                ((splineMatrix[3][2]) << SPLINE_WORLD_SHIFT)));

        return pos;
    }

    private IVector calculateSplineTangent(int distance) {
        int t = getSplineParamFromLength(distance);
        int t2 = (3 * t * t) >> SPLINE_T2_SHIFT;

        int x = ((t2 * splineMatrix[0][0]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t * splineMatrix[1][0]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT - 1)) +
                (splineMatrix[2][0] << SPLINE_WORLD_SHIFT);
        int y = ((t2 * splineMatrix[0][1]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t * splineMatrix[1][1]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT - 1)) +
                (splineMatrix[2][1] << SPLINE_WORLD_SHIFT);
        int z = ((t2 * splineMatrix[0][2]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t * splineMatrix[1][2]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT - 1)) +
                (splineMatrix[2][2] << SPLINE_WORLD_SHIFT);

        x >>= 4;
        y >>= 4;
        z >>= 4;

        return new IVector(x, y, z).normalise();
    }

    @Override
    public void recalculateLength() {
        //TODO
    }

    @Override
    public SVector getStartPosition() {
        return calculateSplinePoint(0);
    }

    @Override
    public void setupEditor(Path path, MapUIController controller, GUIEditorGrid editor) {
        super.setupEditor(path, controller, editor);

        BezierCurve curve = convertToBezierCurve();
        editor.addFloatVector("Start", curve.getStart(), () -> loadFromCurve(curve, controller), controller);
        editor.addFloatVector("Control 1", curve.getControl1(), () -> loadFromCurve(curve, controller), controller);
        editor.addFloatVector("Control 2", curve.getControl2(), () -> loadFromCurve(curve, controller), controller);
        editor.addFloatVector("End", curve.getEnd(), () -> loadFromCurve(curve, controller), controller);

        editor.addLabel("Smooth T:", Arrays.toString(this.smoothT), 25.0); //TODO: Allow edits.
        editor.addLabel("Smooth C:", Utils.matrixToString(this.smoothC), 25.0);
    }

    /**
     * Loads curve data from a bezier curve.
     * @param curve The curve to load data from.
     */
    public void loadFromCurve(BezierCurve curve, MapUIController controller) {
        short uX = (short) (curve.getStart().getX() >> SPLINE_WORLD_SHIFT);
        short uY = (short) (curve.getStart().getY() >> SPLINE_WORLD_SHIFT);
        short uZ = (short) (curve.getStart().getZ() >> SPLINE_WORLD_SHIFT);
        short vX = (short) ((curve.getControl1().getX() >> SPLINE_WORLD_SHIFT) * 3);
        short vY = (short) ((curve.getControl1().getY() >> SPLINE_WORLD_SHIFT) * 3);
        short vZ = (short) ((curve.getControl1().getZ() >> SPLINE_WORLD_SHIFT) * 3);
        short UX = (short) ((curve.getControl2().getX() >> SPLINE_WORLD_SHIFT) * 3);
        short UY = (short) ((curve.getControl2().getY() >> SPLINE_WORLD_SHIFT) * 3);
        short UZ = (short) ((curve.getControl2().getZ() >> SPLINE_WORLD_SHIFT) * 3);
        short VX = (short) (curve.getEnd().getX() >> SPLINE_WORLD_SHIFT);
        short VY = (short) (curve.getEnd().getY() >> SPLINE_WORLD_SHIFT);
        short VZ = (short) (curve.getEnd().getZ() >> SPLINE_WORLD_SHIFT);

        this.splineMatrix[0][0] = -uX + vX - UX + VX;
        this.splineMatrix[0][1] = -uY + vY - UY + VY;
        this.splineMatrix[0][2] = -uZ + vZ - UZ + VZ;

        this.splineMatrix[3][0] = uX;
        this.splineMatrix[3][1] = uY;
        this.splineMatrix[3][2] = uZ;

        uX *= 3;
        uY *= 3;
        uZ *= 3;
        this.splineMatrix[1][0] = uX - (2 * vX) + UX;
        this.splineMatrix[1][1] = uY - (2 * vY) + UY;
        this.splineMatrix[1][2] = uZ - (2 * vZ) + UZ;

        this.splineMatrix[2][0] = -uX + vX;
        this.splineMatrix[2][1] = -uY + vY;
        this.splineMatrix[2][2] = -uZ + vZ;
        onUpdate(controller);
    }

    /**
     * Generate this as a bezier curve.
     * @return bezierCurve
     */
    public BezierCurve convertToBezierCurve() {
        BezierCurve bezier = new BezierCurve();

        int[][] m = this.splineMatrix;
        SVector start = bezier.getStart();
        start.setX((short) m[3][0]);
        start.setY((short) m[3][1]);
        start.setZ((short) m[3][2]);

        // Control Point 1
        SVector control1 = bezier.getControl1();
        control1.setX((short) (((start.getX() * 3) + m[2][0]) / 3));
        control1.setY((short) (((start.getY() * 3) + m[2][1]) / 3));
        control1.setZ((short) (((start.getZ() * 3) + m[2][2]) / 3));

        // Control Point 2
        SVector control2 = bezier.getControl2();
        control2.setX((short) ((m[1][0] + (6 * control1.getX()) - (3 * start.getX())) / 3));
        control2.setY((short) ((m[1][1] + (6 * control1.getY()) - (3 * start.getY())) / 3));
        control2.setZ((short) ((m[1][2] + (6 * control1.getZ()) - (3 * start.getZ())) / 3));

        // End
        SVector end = bezier.getEnd();
        end.setX((short) (m[0][0] + start.getX() - (3 * control1.getX()) + (3 * control2.getX())));
        end.setY((short) (m[0][1] + start.getY() - (3 * control1.getY()) + (3 * control2.getY())));
        end.setZ((short) (m[0][2] + start.getZ() - (3 * control1.getZ()) + (3 * control2.getZ())));

        // Shift all of them left.
        start.setX((short) (start.getX() << SPLINE_WORLD_SHIFT));
        start.setY((short) (start.getY() << SPLINE_WORLD_SHIFT));
        start.setZ((short) (start.getZ() << SPLINE_WORLD_SHIFT));
        control1.setX((short) (control1.getX() << SPLINE_WORLD_SHIFT));
        control1.setY((short) (control1.getY() << SPLINE_WORLD_SHIFT));
        control1.setZ((short) (control1.getZ() << SPLINE_WORLD_SHIFT));
        control2.setX((short) (control2.getX() << SPLINE_WORLD_SHIFT));
        control2.setY((short) (control2.getY() << SPLINE_WORLD_SHIFT));
        control2.setZ((short) (control2.getZ() << SPLINE_WORLD_SHIFT));
        end.setX((short) (end.getX() << SPLINE_WORLD_SHIFT));
        end.setY((short) (end.getY() << SPLINE_WORLD_SHIFT));
        end.setZ((short) (end.getZ() << SPLINE_WORLD_SHIFT));

        return bezier;
    }

    @Getter
    public static class BezierCurve {
        private SVector start = new SVector();
        private SVector control1 = new SVector();
        private SVector control2 = new SVector();
        private SVector end = new SVector();
    }
}
