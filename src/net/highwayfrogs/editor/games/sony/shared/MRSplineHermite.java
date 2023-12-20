package net.highwayfrogs.editor.games.sony.shared;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;

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
     * Calculates the coefficient matrix from the 4x3 hermite boundary conditions.
     * See Computer Graphics, Foley-Van Dam, p. 484, (11.19) Multiplies:	0
     */
    public MRSplineMatrix calculateMatrix(boolean multiplier) {
        return calculateMatrix(new MRSplineMatrix(getGameInstance()), multiplier);
    }

    /**
     * Calculates the coefficient matrix from the 4x3 hermite boundary conditions.
     * See Computer Graphics, Foley-Van Dam, p. 484, (11.19) Multiplies:	0
     */
    public MRSplineMatrix calculateMatrix(MRSplineMatrix matrix, boolean multiplier) {
        int ux, uy, uz;
        int vx, vy, vz;
        int ax, ay, az;
        int bx, by, bz;

        ux = this.startPoint.getX() >> MR_SPLINE_WORLD_SHIFT;
        uy = this.startPoint.getY() >> MR_SPLINE_WORLD_SHIFT;
        uz = this.startPoint.getZ() >> MR_SPLINE_WORLD_SHIFT;

        vx = this.endPoint.getX() >> MR_SPLINE_WORLD_SHIFT;
        vy = this.endPoint.getY() >> MR_SPLINE_WORLD_SHIFT;
        vz = this.endPoint.getZ() >> MR_SPLINE_WORLD_SHIFT;

        ax = (this.startTangent.getX() * (multiplier ? 3 : 1)) >> MR_SPLINE_WORLD_SHIFT;
        ay = (this.startTangent.getY() * (multiplier ? 3 : 1)) >> MR_SPLINE_WORLD_SHIFT;
        az = (this.startTangent.getZ() * (multiplier ? 3 : 1)) >> MR_SPLINE_WORLD_SHIFT;

        bx = (this.endTangent.getY() * (multiplier ? -3 : 1)) >> MR_SPLINE_WORLD_SHIFT;
        by = (this.endTangent.getY() * (multiplier ? -3 : 1)) >> MR_SPLINE_WORLD_SHIFT;
        bz = (this.endTangent.getZ() * (multiplier ? -3 : 1)) >> MR_SPLINE_WORLD_SHIFT;

        matrix.getMatrix()[0][0] = (2 * ux) - (2 * vx) + (ax) + (bx);
        matrix.getMatrix()[0][1] = (2 * uy) - (2 * vy) + (ay) + (by);
        matrix.getMatrix()[0][2] = (2 * uz) - (2 * vz) + (az) + (bz);

        // New version using no multiplies
        matrix.getMatrix()[1][0] = (-matrix.getMatrix()[0][0]) - ux + vx - ax;
        matrix.getMatrix()[1][1] = (-matrix.getMatrix()[0][1]) - uy + vy - ay;
        matrix.getMatrix()[1][2] = (-matrix.getMatrix()[0][2]) - uz + vz - az;

        matrix.getMatrix()[2][0] = (ax);
        matrix.getMatrix()[2][1] = (ay);
        matrix.getMatrix()[2][2] = (az);

        matrix.getMatrix()[3][0] = (ux);
        matrix.getMatrix()[3][1] = (uy);
        matrix.getMatrix()[3][2] = (uz);

        return matrix;
    }

    /**
     * Calculates the spline hermite matrix from the spline coefficient matrix.
     * See Computer Graphics, Foley-Van Dam, p. 484, (11.19) Multiplies:	0
     */
    public void loadFromSplineMatrix(MRSplineMatrix splineMatrix, boolean multiplier) {
        int ux, uy, uz;
        int vx, vy, vz;
        int ax, ay, az;
        int bx, by, bz;

        ax = splineMatrix.getMatrix()[2][0];
        ay = splineMatrix.getMatrix()[2][1];
        az = splineMatrix.getMatrix()[2][2];

        ux = splineMatrix.getMatrix()[3][0];
        uy = splineMatrix.getMatrix()[3][1];
        uz = splineMatrix.getMatrix()[3][2];

        vx = splineMatrix.getMatrix()[1][0] + splineMatrix.getMatrix()[0][0] + ux + ax;
        vy = splineMatrix.getMatrix()[1][1] + splineMatrix.getMatrix()[0][1] + uy + ay;
        vz = splineMatrix.getMatrix()[1][2] + splineMatrix.getMatrix()[0][2] + uz + az;

        bx = splineMatrix.getMatrix()[0][0] - (2 * ux) + (2 * vx) - ax;
        by = splineMatrix.getMatrix()[0][1] - (2 * uy) + (2 * vy) - ay;
        bz = splineMatrix.getMatrix()[0][2] - (2 * uz) + (2 * vz) - az;

        this.startPoint.setX((short) (ux << MR_SPLINE_WORLD_SHIFT));
        this.startPoint.setY((short) (uy << MR_SPLINE_WORLD_SHIFT));
        this.startPoint.setZ((short) (uz << MR_SPLINE_WORLD_SHIFT));

        this.endPoint.setX((short) (vx << MR_SPLINE_WORLD_SHIFT));
        this.endPoint.setY((short) (vy << MR_SPLINE_WORLD_SHIFT));
        this.endPoint.setZ((short) (vz << MR_SPLINE_WORLD_SHIFT));

        this.startTangent.setX((splineMatrix.getMatrix()[2][0] << MR_SPLINE_WORLD_SHIFT) / (multiplier ? 3 : 1));
        this.startTangent.setY((splineMatrix.getMatrix()[2][1] << MR_SPLINE_WORLD_SHIFT) / (multiplier ? 3 : 1));
        this.startTangent.setZ((splineMatrix.getMatrix()[2][2] << MR_SPLINE_WORLD_SHIFT) / (multiplier ? 3 : 1));

        this.endTangent.setX((bx << MR_SPLINE_WORLD_SHIFT) / (multiplier ? 3 : 1));
        this.endTangent.setY((by << MR_SPLINE_WORLD_SHIFT) / (multiplier ? 3 : 1));
        this.endTangent.setZ((bz << MR_SPLINE_WORLD_SHIFT) / (multiplier ? 3 : 1));
    }
}