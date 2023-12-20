package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.path;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.MRSplineHermite;
import net.highwayfrogs.editor.games.sony.shared.MRSplineMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a spline in old Frogger.
 * Created by Kneesnap on 12/12/2023.
 */
public class OldFroggerSpline extends SCSharedGameData {
    private final MRSplineHermite splineHermite;
    private final int[] smoothT = new int[4]; // Smooth T is the distance values to reach each point. (Translation)
    private final int[][] smoothC = new int[4][3]; // Smoothing coefficient data.

    private static final int SPLINE_FIX_INTERVAL = 0x200;
    private static final int SPLINE_WORLD_SHIFT = 3;
    private static final int SPLINE_PARAM_SHIFT = 11;
    private static final int SPLINE_T2_SHIFT = 3;
    public static final int SIZE_IN_BYTES = MRSplineMatrix.SIZE_IN_BYTES + (4 * Constants.INTEGER_SIZE) + (4 * 3 * Constants.INTEGER_SIZE);

    public OldFroggerSpline(SCGameInstance instance) {
        super(instance);
        this.splineHermite = new MRSplineHermite(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.splineHermite.load(reader);

        // Read smooth_t
        for (int i = 0; i < this.smoothT.length; i++)
            this.smoothT[i] = reader.readInt();

        // Read smooth_c:
        for (int i = 0; i < this.smoothC.length; i++)
            for (int j = 0; j < this.smoothC[i].length; j++)
                this.smoothC[i][j] = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        this.splineHermite.save(writer);

        // Write smooth_t:
        for (int i = 0; i < this.smoothT.length; i++)
            writer.writeInt(this.smoothT[i]);

        // Write smooth_c:
        for (int i = 0; i < this.smoothC.length; i++)
            for (int j = 0; j < this.smoothC[i].length; j++)
                writer.writeInt(this.smoothC[i][j]);
    }

    /**
     * Calculate a coefficient matrix from the spline hermite.
     */
    public MRSplineMatrix calculateMatrix() {
        return this.splineHermite.calculateMatrix(new MRSplineMatrix(getGameInstance()), true);
    }

    // What follows is horrible to read, but is accurate to the game.
    private int getSplineParamFromLength(int length) {
        length <<= 5;
        int d = length;

        int i;
        for (i = 3; i > 0; i--) {
            d = length - (this.smoothT[i - 1] >> SPLINE_WORLD_SHIFT);
            if (d >= 0)
                break;
        }

        if (i == 0)
            d = length;

        int e = d;
        d = 0;
        d += (this.smoothC[i][0] >> SPLINE_PARAM_SHIFT);
        d *= e;
        d >>= 13;

        d += (this.smoothC[i][1] >> SPLINE_PARAM_SHIFT);
        d *= e;
        d >>= 13;

        d += (this.smoothC[i][2] >> SPLINE_PARAM_SHIFT);
        d *= e;
        d >>= 13;
        d >>= 5;

        d += (i * SPLINE_FIX_INTERVAL);
        return (d << 1) >> 1;
    }

    // I hate this.
    public SVector calculateSplinePoint(int distance) {
        int t = getSplineParamFromLength(distance);
        int t2 = (t * t) >> SPLINE_T2_SHIFT;
        int t3 = (t2 * t) >> SPLINE_PARAM_SHIFT;

        SVector pos = new SVector();

        int[][] splineMatrix = calculateMatrix().getMatrix();
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

    public IVector calculateSplineTangent(int distance) {
        int t = getSplineParamFromLength(distance);
        int t2 = (3 * t * t) >> SPLINE_T2_SHIFT;

        int[][] splineMatrix = calculateMatrix().getMatrix();
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

    /**
     * Calculate the length of the spline.
     * This is currently not accurate enough.
     * TODO: Try different methods, like calculating the length of a bezier curve. What is the relationship of smoothC and smoothT to this?
     */
    public int calculateLength() {
        // We leave this up to user input, since I've yet to come up with an algorithm which is accurate enough to get this right,
        // and it seems like just leaving it as-is even during changes will create valid results.
        final int maxLength = 1000 * (1 << 4);
        int bestLength = 0;
        double bestDistance = Double.MAX_VALUE;

        SVector endPoint = this.splineHermite.getEndPoint();
        for (int testLength = 0; testLength < maxLength; testLength++) {
            SVector point = calculateSplinePoint(testLength);
            double distanceSq = point.distanceSquared(endPoint);
            if (distanceSq < bestDistance) {
                bestDistance = distanceSq;
                bestLength = testLength;
            } else if (bestDistance <= 10 && distanceSq > 10) {
                break;
            }
        }

        return bestLength;
    }

    /**
     * Calculates T smoothing percentages.
     * @return tPercentages
     */
    public float[] calculateSmoothing() {
        float length = Utils.fixedPointIntToFloat4Bit(calculateLength());
        float[] result = new float[4];
        for (int i = 0; i < result.length; i++)
            result[i] = Utils.fixedPointIntToFloatNBits(this.smoothT[i], 12) / length;
        return result;
    }

    /**
     * Loads smooth T values, or the percentage
     * @param smoothingT The amount of distance each segment takes up. [0,1]
     */
    public void loadSmoothT(float[] smoothingT) {
        float length = Utils.fixedPointIntToFloat4Bit(calculateLength());
        for (int i = 0; i < this.smoothT.length; i++)
            this.smoothT[i] = Utils.floatToFixedPointInt((length * smoothingT[i]), 12);
    }

    public void setupEditor(MeshViewController<?> controller, GUIEditorGrid editor) {
        editor.addFloatSVector("Start Point", this.splineHermite.getStartPoint(), controller);
        editor.addFloatSVector("End Point", this.splineHermite.getEndPoint(), controller);
        editor.addFloatVector("Start Tangent", this.splineHermite.getStartTangent(), null, controller);
        editor.addFloatVector("End Tangent", this.splineHermite.getEndTangent(), null, controller);
        makeTEditor(controller, editor);

        editor.addBoldLabel("Smooth C:"); //TODO: Make a real editor.
        for (int i = 0; i < this.smoothC.length; i++) {
            final int index1 = i;
            for (int j = 0; j < this.smoothC[i].length; j++) {
                final int index2 = j;
                editor.addIntegerField(i + "," + j, this.smoothC[i][j], newVal -> {
                    this.smoothC[index1][index2] = newVal;
                    // TODO: onUpdate(controller);
                }, null);
            }
        }
    }

    private void makeTEditor(MeshViewController<?> controller, GUIEditorGrid editor) {
        editor.addBoldLabel("Segment Length Percentages:");
        float[] smoothingT = calculateSmoothing();
        for (int i = 0; i < this.smoothT.length; i++) {
            final int index = i;
            editor.addFloatField("Segment #" + (i + 1) + ":", smoothingT[i], newValue -> {
                smoothingT[index] = newValue;
                loadSmoothT(smoothingT);
                // TODO: onUpdate(controller);
            }, newValue -> newValue >= 0F && newValue <= 1.1F);
        }
    }
}