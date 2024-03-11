package net.highwayfrogs.editor.games.sony.shared.spline;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;

import java.util.function.Function;

/**
 * This data structure represents 'MR_BSPLINE', found in Frogger.
 * Created by Kneesnap on 9/22/2023.
 */
@Getter
public class MRBezierCurve extends SCSharedGameData {
    private final SVector start;
    private final SVector control1; // For start tangent.
    private final SVector control2; // For end tangent.
    private final SVector end;

    public MRBezierCurve(SCGameInstance instance) {
        this(instance, new SVector(), new SVector(), new SVector(), new SVector());
    }

    public MRBezierCurve(SCGameInstance instance, SVector start, SVector control1, SVector control2, SVector end) {
        super(instance);
        this.start = start;
        this.control1 = control1;
        this.control2 = control2;
        this.end = end;
    }

    @Override
    public void load(DataReader reader) {
        this.start.loadWithPadding(reader);
        this.control1.loadWithPadding(reader);
        this.control2.loadWithPadding(reader);
        this.end.loadWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.start.saveWithPadding(writer);
        this.control1.saveWithPadding(writer);
        this.control2.saveWithPadding(writer);
        this.end.saveWithPadding(writer);
    }

    /**
     * Calculate the length of this bezier curve. Doesn't seem very accurate.
     * Nabbed from http://steve.hollasch.net/cgindex/curves/cbezarclen.html
     * @return splineLength
     */
    public double calculateLength() {
        SVector k1 = new SVector(this.end).subtract(this.start).add(this.control1.clone().subtract(this.control2).multiply(3));
        SVector k2 = new SVector(this.start).add(this.control2).multiply(3).subtract(this.control1.clone().multiply(6)); // 3 * (p0 + p2) - 6 * p1;
        SVector k3 = new SVector(this.control1).subtract(this.start).multiply(3);

        double q1 = 9.0 * ((k1.getFloatX() * k1.getFloatX()) + (k1.getFloatY() * k1.getFloatY()));
        double q2 = 12.0 * (k1.getFloatX() * k2.getFloatX() + k1.getFloatY() * k2.getFloatY());
        double q3 = 3.0 * (k1.getFloatX() * k3.getFloatX() + k1.getFloatY() * k3.getFloatY()) + 4.0 * ((k2.getFloatX() * k2.getFloatX()) + (k2.getFloatY() * k2.getFloatY()));
        double q4 = 4.0 * (k2.getFloatX() * k3.getFloatX() + k2.getFloatY() * k3.getFloatY());
        double q5 = (k3.getFloatX() * k3.getFloatX()) + (k3.getFloatY() * k3.getFloatY());

        return simpson(t -> Math.sqrt(q5 + t * (q4 + t * (q3 + t * (q2 + t * q1)))), 0, 1, 4096, .001);
    }

    private double simpson(Function<Double, Double> balf, double a, double b, int nLimit, double tolerance) {
        int n = 1;
        double multiplier = (b - a) / 6.0;
        double endsum = balf.apply(a) + balf.apply(b);
        double interval = (b - a) / 2.0;
        double asum = 0;
        double bsum = balf.apply(a + interval);
        double est1 = multiplier * (endsum + 2 * asum + 4 * bsum);
        double est0 = 2 * est1;

        while (n < nLimit && (Math.abs(est1) > 0 && Math.abs((est1 - est0) / est1) > tolerance)) {
            n *= 2;
            multiplier /= 2;
            interval /= 2;
            asum += bsum;
            bsum = 0;
            est0 = est1;
            double interval_div_2n = interval / (2.0 * n);

            for (int i = 1; i < 2 * n; i += 2) {
                double t = a + i * interval_div_2n;
                bsum += balf.apply(t);
            }

            est1 = multiplier * (endsum + 2 * asum + 4 * bsum);
        }

        return est1;
    }

    /**
     * Converts this curve to a spline matrix.
     * @return splineMatrix
     */
    public MRSplineMatrix toSplineMatrix() {
        return toSplineMatrix(null);
    }

    /**
     * Converts this curve to a spline matrix.
     * Implementation of 'MRCalculateSplineBezierMatrix'.
     * @param matrix The spline matrix to write information to. If null is provided, a new one will be created.
     * @return splineMatrix
     */
    public MRSplineMatrix toSplineMatrix(MRSplineMatrix matrix) {
        if (matrix == null)
            matrix = new MRSplineMatrix(getGameInstance());

        short uX = (short) (this.start.getX() >> MRSplineMatrix.SPLINE_WORLD_SHIFT);
        short uY = (short) (this.start.getY() >> MRSplineMatrix.SPLINE_WORLD_SHIFT);
        short uZ = (short) (this.start.getZ() >> MRSplineMatrix.SPLINE_WORLD_SHIFT);
        short vX = (short) ((this.control1.getX() >> MRSplineMatrix.SPLINE_WORLD_SHIFT) * 3);
        short vY = (short) ((this.control1.getY() >> MRSplineMatrix.SPLINE_WORLD_SHIFT) * 3);
        short vZ = (short) ((this.control1.getZ() >> MRSplineMatrix.SPLINE_WORLD_SHIFT) * 3);
        short UX = (short) ((this.control2.getX() >> MRSplineMatrix.SPLINE_WORLD_SHIFT) * 3);
        short UY = (short) ((this.control2.getY() >> MRSplineMatrix.SPLINE_WORLD_SHIFT) * 3);
        short UZ = (short) ((this.control2.getZ() >> MRSplineMatrix.SPLINE_WORLD_SHIFT) * 3);
        short VX = (short) (this.end.getX() >> MRSplineMatrix.SPLINE_WORLD_SHIFT);
        short VY = (short) (this.end.getY() >> MRSplineMatrix.SPLINE_WORLD_SHIFT);
        short VZ = (short) (this.end.getZ() >> MRSplineMatrix.SPLINE_WORLD_SHIFT);

        int[][] splineMatrix = matrix.getMatrix();
        splineMatrix[0][0] = -uX + vX - UX + VX;
        splineMatrix[0][1] = -uY + vY - UY + VY;
        splineMatrix[0][2] = -uZ + vZ - UZ + VZ;

        splineMatrix[3][0] = uX;
        splineMatrix[3][1] = uY;
        splineMatrix[3][2] = uZ;

        uX *= 3;
        uY *= 3;
        uZ *= 3;
        splineMatrix[1][0] = uX - (2 * vX) + UX;
        splineMatrix[1][1] = uY - (2 * vY) + UY;
        splineMatrix[1][2] = uZ - (2 * vZ) + UZ;

        splineMatrix[2][0] = -uX + vX;
        splineMatrix[2][1] = -uY + vY;
        splineMatrix[2][2] = -uZ + vZ;

        return matrix;
    }
}