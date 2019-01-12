package net.highwayfrogs.editor.file.map.path.data;

import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.path.PathSegment;
import net.highwayfrogs.editor.file.map.path.PathType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Holds Spline segment data.
 * Not entirely sure how we'll edit much of this data.
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
        super(PathType.SPLINE);
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
    protected SVector calculatePosition(PathInfo info) {
        return calculateSplinePoint(info.getSegmentDistance());
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

        SVector splinePoint = new SVector();

        splinePoint.setX((short) (((t3 * splineMatrix[0][0]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t2 * splineMatrix[1][0]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t * splineMatrix[2][0]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT)) +
                ((splineMatrix[3][0]) << SPLINE_WORLD_SHIFT)));

        splinePoint.setY((short) (((t3 * splineMatrix[0][1]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t2 * splineMatrix[1][1]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t * splineMatrix[2][1]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT)) +
                ((splineMatrix[3][1]) << SPLINE_WORLD_SHIFT)));

        splinePoint.setZ((short) (((t3 * splineMatrix[0][2]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t2 * splineMatrix[1][2]) >> (SPLINE_PARAM_SHIFT * 2 - SPLINE_WORLD_SHIFT - SPLINE_T2_SHIFT)) +
                ((t * splineMatrix[2][2]) >> (SPLINE_PARAM_SHIFT - SPLINE_WORLD_SHIFT)) +
                ((splineMatrix[3][2]) << SPLINE_WORLD_SHIFT)));

        return splinePoint;
    }
}
