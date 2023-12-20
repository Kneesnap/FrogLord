package net.highwayfrogs.editor.games.sony.shared;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
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
    private static final int SPLINE_WORLD_SHIFT = 3;

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
     * Loads curve data from a bezier curve.
     * @param curve The curve to load data from.
     */
    public void loadFromCurve(MRBezierCurve curve) {
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

        this.matrix[0][0] = -uX + vX - UX + VX;
        this.matrix[0][1] = -uY + vY - UY + VY;
        this.matrix[0][2] = -uZ + vZ - UZ + VZ;

        this.matrix[3][0] = uX;
        this.matrix[3][1] = uY;
        this.matrix[3][2] = uZ;

        uX *= 3;
        uY *= 3;
        uZ *= 3;
        this.matrix[1][0] = uX - (2 * vX) + UX;
        this.matrix[1][1] = uY - (2 * vY) + UY;
        this.matrix[1][2] = uZ - (2 * vZ) + UZ;

        this.matrix[2][0] = -uX + vX;
        this.matrix[2][1] = -uY + vY;
        this.matrix[2][2] = -uZ + vZ;
    }

    /**
     * Generate this as a bezier curve.
     * @return bezierCurve
     */
    public MRBezierCurve convertToBezierCurve() {
        MRBezierCurve bezier = new MRBezierCurve(null);

        SVector start = bezier.getStart();
        start.setX((short) this.matrix[3][0]);
        start.setY((short) this.matrix[3][1]);
        start.setZ((short) this.matrix[3][2]);

        // Control Point 1
        SVector control1 = bezier.getControl1();
        control1.setX((short) (((start.getX() * 3) + this.matrix[2][0]) / 3));
        control1.setY((short) (((start.getY() * 3) + this.matrix[2][1]) / 3));
        control1.setZ((short) (((start.getZ() * 3) + this.matrix[2][2]) / 3));

        // Control Point 2
        SVector control2 = bezier.getControl2();
        control2.setX((short) ((this.matrix[1][0] + (6 * control1.getX()) - (3 * start.getX())) / 3));
        control2.setY((short) ((this.matrix[1][1] + (6 * control1.getY()) - (3 * start.getY())) / 3));
        control2.setZ((short) ((this.matrix[1][2] + (6 * control1.getZ()) - (3 * start.getZ())) / 3));

        // End
        SVector end = bezier.getEnd();
        end.setX((short) (this.matrix[0][0] + start.getX() - (3 * control1.getX()) + (3 * control2.getX())));
        end.setY((short) (this.matrix[0][1] + start.getY() - (3 * control1.getY()) + (3 * control2.getY())));
        end.setZ((short) (this.matrix[0][2] + start.getZ() - (3 * control1.getZ()) + (3 * control2.getZ())));

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
}