package net.highwayfrogs.editor.games.sony.shared.math;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.psx.math.PSXMatrix;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.utils.MathUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the PT Quaternion struct. Seems to match MR_QUAT.
 * Created by Kneesnap on 5/21/2024.
 */
@Getter
@Setter
public class PTQuaternion extends SCSharedGameData {
    private short c = 4096; // 4096 -> 1.0F, 'real'. (1.3.12 format)
    private short x; // Angle.
    private short y;
    private short z;

    public PTQuaternion(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.c = reader.readShort();
        this.x = reader.readShort();
        this.y = reader.readShort();
        this.z = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.c);
        writer.writeShort(this.x);
        writer.writeShort(this.y);
        writer.writeShort(this.z);
    }

    @Override
    public int hashCode() {
        return ((this.c & 0xFF) << 24) | ((this.x & 0xFF) << 16) | ((this.y & 0xFF) << 8) | (this.z & 0xFF);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PTQuaternion))
            return false;

        PTQuaternion other = (PTQuaternion) obj;
        return this.c == other.c && this.x == other.x && this.y == other.y && this.z == other.z;
    }

    /**
     * Loads this data from a PSX matrix.
     * @param matrix the matrix to load data from.
     */
    public void fromMatrix(PSXMatrix matrix) {
        int trace = matrix.getMatrix()[0][0] + matrix.getMatrix()[1][1] + matrix.getMatrix()[2][2];
        if (trace > 0) {
            int s = MathUtils.fixedSqrt((trace + 0x1000) << 12);
            this.c = (short) (s >> 1);
            s = ((0x800 << 12) / s);
            this.x = (short) (((matrix.getMatrix()[1][2] - matrix.getMatrix()[2][1]) * s) >> 12);
            this.y = (short) (((matrix.getMatrix()[2][0] - matrix.getMatrix()[0][2]) * s) >> 12);
            this.z = (short) (((matrix.getMatrix()[0][1] - matrix.getMatrix()[1][0]) * s) >> 12);
        } else {
            int i = 0;
            if (matrix.getMatrix()[1][1] > matrix.getMatrix()[0][0])
                i = 1;
            if (matrix.getMatrix()[2][2] > matrix.getMatrix()[i][i])
                i = 2;

            int j = (i + 1) % 3; // 1, 2, 0
            int k = (j + 1) % 3; // 1, 2, 0
            int s = MathUtils.fixedSqrt(((matrix.getMatrix()[i][i] - (matrix.getMatrix()[j][j] + matrix.getMatrix()[k][k])) + 0x1000) << 12);
            setComponent(i + 1, (short) (s >> 1));
            s = (0x800 << 12) / s;
            this.c = (short) (((matrix.getMatrix()[j][k] - matrix.getMatrix()[k][j]) * s) >> 12);
            setComponent(j + 1, (short) (((matrix.getMatrix()[i][j] + matrix.getMatrix()[j][i]) * s) >> 12));
            setComponent(k + 1, (short) (((matrix.getMatrix()[i][k] + matrix.getMatrix()[k][i]) * s) >> 12));
        }
    }

    private void setComponent(int component, short value) {
        switch (component) {
            case 0:
                this.c = value;
                break;
            case 1:
                this.x = value;
                break;
            case 2:
                this.y = value;
                break;
            case 3:
                this.z = value;
                break;
            default:
                throw new RuntimeException("Invalid array component " + component);
        }
    }



    /**
     * Creates a matrix represented by this quaternion.
     */
    public PSXMatrix createMatrix() {
        PSXMatrix matrix = new PSXMatrix();
        applyToMatrix(matrix, this.c, this.x, this.y, this.z);
        return matrix;
    }

    private static void applyToMatrix(PSXMatrix matrix, short c, short x, short y, short z) {
        int xs = x << 1;
        int ys = y << 1;
        int zs = z << 1;
        int wx = c * xs;
        int wy = c * ys;
        int wz = c * zs;
        int xx = x * xs;
        int xy = x * ys;
        int xz = x * zs;
        int yy = y * ys;
        int yz = y * zs;
        int zz = z * zs;

        // Oddly, every set is bit-shifted right 0 places. Not sure what that does, maybe it does something special in C.
        matrix.getMatrix()[0][0] = (short) (0x1000 - ((yy + zz) >> 12));
        matrix.getMatrix()[0][1] = (short) ((xy + wz) >> 12);
        matrix.getMatrix()[0][2] = (short) ((xz - wy) >> 12);
        matrix.getMatrix()[1][0] = (short) ((xy - wz) >> 12);
        matrix.getMatrix()[1][1] = (short) (0x1000 - ((xx + zz) >> 12));
        matrix.getMatrix()[1][2] = (short) ((yz + wx) >> 12);
        matrix.getMatrix()[2][0] = (short) ((xz + wy) >> 12);
        matrix.getMatrix()[2][1] = (short) ((yz - wx) >> 12);
        matrix.getMatrix()[2][2] = (short) (0x1000 - ((xx + yy) >> 12));
    }

    @Override
    public String toString() {
        return "PTQuaternion<c=" + this.c + ",x=" + this.x + ",y=" + this.y + ",z=" + this.z + ">";
    }
}