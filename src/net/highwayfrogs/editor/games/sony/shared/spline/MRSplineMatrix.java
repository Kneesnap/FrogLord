package net.highwayfrogs.editor.games.sony.shared.spline;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;

/**
 * This data structure represents 'MR_SPLINE_MATRIX', a spline coefficient matrix.
 * Created by Kneesnap on 12/12/2023.
 */
@Getter
public class MRSplineMatrix extends SCSharedGameData {
    private final int[][] matrix = new int[4][3];

    public static final int SIZE_IN_BYTES = (4 * 3 * Constants.INTEGER_SIZE);
    public static final int SPLINE_WORLD_SHIFT = 3; // shift world coords down by this to fit into calculation size
    private static final int SPLINE_T2_SHIFT = 3; // shift applied to (t*t) before it is used
    public static final int SPLINE_PARAM_SHIFT = 11; // The number of bits used in a fixed point parameter 't'.
    public static final int SPLINE_PARAM_ONE = (1 << SPLINE_PARAM_SHIFT); // Represents 1.0 for parameter 't'. (1 << 11) = 2048.

    public MRSplineMatrix(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                this.matrix[i][j] = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                writer.writeInt(this.matrix[i][j]);
    }

    /**
     * Evaluates the world position of t, a distance along the spline curve.
     * @param t A value between 0 (0.0) and 2048 (1.0) representing how far along the spline curve to evaluate.
     * @return worldPosition
     */
    public SVector evaluatePosition(int t) {
        return evaluatePosition(new SVector(), t);
    }

    /**
     * Evaluates the world position of t, a distance along the spline curve.
     * @param result The vector to store the output position. If null is provided, a new vector will be created.
     * @param t A value between 0 (0.0) and 2048 (1.0) representing how far along the spline curve to evaluate.
     * @return worldPosition
     */
    public SVector evaluatePosition(SVector result, int t) {
        if (result == null)
            result = new SVector();

        int t2 = (t * t) >> SPLINE_T2_SHIFT;
        int t3 = (t2 * t) >> SPLINE_PARAM_SHIFT;

        result.setX((short) (((t3 * this.matrix[0][0]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t2 * this.matrix[1][0]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t * this.matrix[2][0]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT)) +
                ((this.matrix[3][0]) << SPLINE_WORLD_SHIFT)));

        result.setY((short) (((t3 * this.matrix[0][1]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t2 * this.matrix[1][1]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t * this.matrix[2][1]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT)) +
                ((this.matrix[3][1]) << SPLINE_WORLD_SHIFT)));

        result.setZ((short) (((t3 * this.matrix[0][2]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT))
                + ((t2 * this.matrix[1][2]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT))
                + ((t * this.matrix[2][2]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT))
                + ((this.matrix[3][2]) << SPLINE_WORLD_SHIFT)));

        return result;
    }

    /**
     * Evaluates the normalised tangent line at t, a distance along the spline curve.
     * Implementation of 'MRCalculateSplineTangentNormalised'
     * @param t A value between 0 (0.0) and 2048 (1.0) representing how far along the spline curve to evaluate.
     * @return tangentLine
     */
    public IVector calculateTangentLine(int t) {
        return calculateTangentLine(null, t);
    }

    /**
     * Evaluates the normalised tangent line at t, a distance along the spline curve.
     * Implementation of 'MRCalculateSplineTangentNormalised'
     * @param result The vector to store the output tangent line. If null is provided, a new vector will be created.
     * @param t A value between 0 (0.0) and 2048 (1.0) representing how far along the spline curve to evaluate.
     * @return tangentLine
     */
    public IVector calculateTangentLine(IVector result, int t) {
        if (result == null)
            result = new IVector();

        int t2 = (3 * t * t) >> SPLINE_T2_SHIFT;

        result.setX((((t2 * this.matrix[0][0]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT))
                + ((t * this.matrix[1][0]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT - 1))
                + (this.matrix[2][0] << SPLINE_WORLD_SHIFT)) >> 4);
        result.setY((((t2 * this.matrix[0][1]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT))
                + ((t * this.matrix[1][1]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT - 1))
                + (this.matrix[2][1] << SPLINE_WORLD_SHIFT)) >> 4);
        result.setZ((((t2 * this.matrix[0][2]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT))
                + ((t * this.matrix[1][2]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT - 1))
                + (this.matrix[2][2] << SPLINE_WORLD_SHIFT)) >> 4);

        return result.normalise();
    }

    /**
     * Convert this to a Bézier curve.
     * This function has been carefully written to maximize decimal accuracy during conversion.
     * @return bezierCurve
     */
    public MRBezierCurve toBezierCurve() {
        return toBezierCurve(null);
    }

    /**
     * Convert this to a Bézier curve.
     * This function has been carefully written to maximize decimal accuracy during conversion.
     * @param curve The curve instance to apply this matrix to. If null, a new instance will be created.
     * @return bezierCurve
     */
    public MRBezierCurve toBezierCurve(MRBezierCurve curve) {
        if (curve == null)
            curve = new MRBezierCurve(getGameInstance());

        int[][] m = this.matrix;
        SVector start = curve.getStart();
        start.setX((short) m[3][0]);
        start.setY((short) m[3][1]);
        start.setZ((short) m[3][2]);

        // Control Point 1
        SVector control1 = curve.getControl1();
        control1.setX((short) (m[2][0] + (3 * start.getX())));
        control1.setY((short) (m[2][1] + (3 * start.getY())));
        control1.setZ((short) (m[2][2] + (3 * start.getZ())));

        // Control Point 2
        SVector control2 = curve.getControl2();
        control2.setX((short) (m[1][0] + (2 * control1.getX()) - (3 * start.getX())));
        control2.setY((short) (m[1][1] + (2 * control1.getY()) - (3 * start.getY())));
        control2.setZ((short) (m[1][2] + (2 * control1.getZ()) - (3 * start.getZ())));

        // End
        SVector end = curve.getEnd();
        end.setX((short) (m[0][0] + start.getX() - control1.getX() + control2.getX()));
        end.setY((short) (m[0][1] + start.getY() - control1.getY() + control2.getY()));
        end.setZ((short) (m[0][2] + start.getZ() - control1.getZ() + control2.getZ()));

        // Shift all of them left.
        start.setX((short) (start.getX() << SPLINE_WORLD_SHIFT));
        start.setY((short) (start.getY() << SPLINE_WORLD_SHIFT));
        start.setZ((short) (start.getZ() << SPLINE_WORLD_SHIFT));
        control1.setX((short) ((control1.getX() << SPLINE_WORLD_SHIFT) / 3));
        control1.setY((short) ((control1.getY() << SPLINE_WORLD_SHIFT) / 3));
        control1.setZ((short) ((control1.getZ() << SPLINE_WORLD_SHIFT) / 3));
        control2.setX((short) ((control2.getX() << SPLINE_WORLD_SHIFT) / 3));
        control2.setY((short) ((control2.getY() << SPLINE_WORLD_SHIFT) / 3));
        control2.setZ((short) ((control2.getZ() << SPLINE_WORLD_SHIFT) / 3));
        end.setX((short) (end.getX() << SPLINE_WORLD_SHIFT));
        end.setY((short) (end.getY() << SPLINE_WORLD_SHIFT));
        end.setZ((short) (end.getZ() << SPLINE_WORLD_SHIFT));

        return curve;
    }

    /**
     * Converts this spline matrix to a spline hermite.
     * See Computer Graphics, Foley-Van Dam, p. 484, (11.19) Multiplies:	0
     * @return splineHermite
     */
    public MRSplineHermite toSplineHermite() {
        return toSplineHermite(null);
    }

    /**
     * Converts this spline matrix to a spline hermite.
     * @param hermite The spline hermite instance to write data to. If null, a new instance is created.
     * See Computer Graphics, Foley-Van Dam, p. 484, (11.19) Multiplies:	0
     * @return splineHermite
     */
    public MRSplineHermite toSplineHermite(MRSplineHermite hermite) {
        if (hermite == null)
            hermite = new MRSplineHermite(getGameInstance());

        int r1x = this.matrix[2][0];
        int r1y = this.matrix[2][1];
        int r1z = this.matrix[2][2];

        int p1x = this.matrix[3][0];
        int p1y = this.matrix[3][1];
        int p1z = this.matrix[3][2];

        int p4x = this.matrix[1][0] + this.matrix[0][0] + p1x + r1x;
        int p4y = this.matrix[1][1] + this.matrix[0][1] + p1y + r1y;
        int p4z = this.matrix[1][2] + this.matrix[0][2] + p1z + r1z;

        int r4x = this.matrix[0][0] - (2 * p1x) + (2 * p4x) - r1x;
        int r4y = this.matrix[0][1] - (2 * p1y) + (2 * p4y) - r1y;
        int r4z = this.matrix[0][2] - (2 * p1z) + (2 * p4z) - r1z;

        SVector startPoint = hermite.getStartPoint();
        startPoint.setX((short) (p1x << SPLINE_WORLD_SHIFT));
        startPoint.setY((short) (p1y << SPLINE_WORLD_SHIFT));
        startPoint.setZ((short) (p1z << SPLINE_WORLD_SHIFT));

        SVector endPoint = hermite.getEndPoint();
        endPoint.setX((short) (p4x << SPLINE_WORLD_SHIFT));
        endPoint.setY((short) (p4y << SPLINE_WORLD_SHIFT));
        endPoint.setZ((short) (p4z << SPLINE_WORLD_SHIFT));

        IVector startTangent = hermite.getStartTangent();
        startTangent.setX((r1x << SPLINE_WORLD_SHIFT));
        startTangent.setY((r1y << SPLINE_WORLD_SHIFT));
        startTangent.setZ((r1z << SPLINE_WORLD_SHIFT));

        IVector endTangent = hermite.getEndTangent();
        endTangent.setX((r4x << SPLINE_WORLD_SHIFT));
        endTangent.setY((r4y << SPLINE_WORLD_SHIFT));
        endTangent.setZ((r4z << SPLINE_WORLD_SHIFT));

        return hermite;
    }
}