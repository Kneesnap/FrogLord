package net.highwayfrogs.editor.file.standard.psx;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Arrays;

/**
 * Represents a MR_MAT struct, which is based on the PSX "MATRIX" struct in libgte.h.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@Setter
@AllArgsConstructor
public class PSXMatrix extends GameObject {
    private short[][] matrix = new short[DIMENSION][DIMENSION]; // 3x3 Rotation Matrix.
    private int[] transform = new int[DIMENSION]; // Transform vector.
    private short padding;

    private static final int DIMENSION = 3;
    public static final int BYTE_SIZE = (DIMENSION * DIMENSION * Constants.SHORT_SIZE) + (DIMENSION * Constants.INTEGER_SIZE) + Constants.SHORT_SIZE;
    public static final PSXMatrix IDENTITY = newIdentityMatrix();

    public PSXMatrix() {
        // Initialise to an identity matrix.
        this.matrix[0][0] = 4096;
        this.matrix[1][1] = 4096;
        this.matrix[2][2] = 4096;
    }

    /**
     * Become an identity matrix.
     */
    public void setIdentity() {
        for (int i = 0; i < this.matrix.length; i++)
            Arrays.fill(this.matrix[i], (short) 0);
        Arrays.fill(this.transform, 0);
        this.matrix[0][0] = (short) 4096;
        this.matrix[1][1] = (short) 4096;
        this.matrix[2][2] = (short) 4096;
    }

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                this.matrix[i][j] = reader.readShort();

        this.padding = reader.readShort(); // Used to align to 4-bytes.
        /*if (this.padding != 0 && this.padding != -1) // It's -1 in some of the maps in Build 01. MediEvil also appears to use the padding field to store data too.
            throw new RuntimeException("Matrix padding was not zero! (" + padding + ")");*/ // TODO: Re-enable this later once PSXMatrix is renamed to MRMatrix and has access to the game instance so we can check if it's MediEvil.

        for (int i = 0; i < this.transform.length; i++)
            this.transform[i] = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        for (short[] aMatrix : this.matrix)
            for (short aShort : aMatrix)
                writer.writeShort(aShort);

        writer.writeShort(this.padding);
        for (int aTransfer : this.transform)
            writer.writeInt(aTransfer);
    }

    /**
     * Get the transform as an SVector.
     * @return transformVector
     */
    public SVector toVector() {
        return new SVector((short) getTransform()[0], (short) getTransform()[1], (short) getTransform()[2]);
    }

    /**
     * Copy matrix data from another matrix.
     * @param otherMatrix the matrix to copy data from
     */
    public void copyFrom(PSXMatrix otherMatrix) {
        if (otherMatrix == null)
            throw new NullPointerException("otherMatrix");

        System.arraycopy(otherMatrix.matrix[0], 0, this.matrix[0], 0, DIMENSION);
        System.arraycopy(otherMatrix.matrix[1], 0, this.matrix[1], 0, DIMENSION);
        System.arraycopy(otherMatrix.matrix[2], 0, this.matrix[2], 0, DIMENSION);
        System.arraycopy(otherMatrix.transform, 0, this.transform, 0, DIMENSION);
        this.padding = otherMatrix.padding;
    }

    /**
     * Gets the x-axis angle (pitch) from this matrix.
     * The rules for approaching 1 are from here: <a href="https://www.gregslabaugh.net/publications/euler.pdf"/>
     * @return pitchInRadians
     */
    public double getPitchAngle() {
        double r32 = Utils.fixedPointShortToFloat12Bit(getMatrix()[2][1]);
        double r33 = Utils.fixedPointShortToFloat12Bit(getMatrix()[2][2]);
        return Math.atan2(r32, r33);
    }

    /**
     * Gets the y-axis angle (yaw) from this matrix.
     * @return yawInRadians
     */
    public double getYawAngle() {
        float r31 = Utils.fixedPointShortToFloat12Bit(getMatrix()[2][0]);
        return Math.asin(-r31);
    }

    /**
     * Gets the z-axis angle (roll) from this matrix.
     * @return rollInRadians
     */
    public double getRollAngle() {
        double r11 = Utils.fixedPointShortToFloat12Bit(getMatrix()[0][0]);
        double r21 = Utils.fixedPointShortToFloat12Bit(getMatrix()[1][0]);
        return Math.atan2(r21, r11);
    }

    /**
     * Update the matrix.
     * Useful: http://danceswithcode.net/engineeringnotes/rotations_in_3d/rotations_in_3d_part2.html
     * @param yaw   The new pitch.
     * @param pitch The new yaw.
     * @param roll  The new roll.
     */
    public void updateMatrix(double pitch, double yaw, double roll) {
        double sx = Math.sin(pitch);
        double cx = Math.cos(pitch);
        double sy = Math.sin(yaw);
        double cy = Math.cos(yaw);
        double sz = Math.sin(roll);
        double cz = Math.cos(roll);

        if (sy >= 1 || sy <= -1)
            return; // Avoid gymbal lock. (Or at least I think that's what we call the reset to pitch/roll by this, and the weird behavior which can happen when Y is in this range.)

        // Update rotation matrix.
        this.matrix[0][0] = Utils.floatToFixedPointShort12Bit((float) (cy * cz)); // r11
        this.matrix[0][1] = Utils.floatToFixedPointShort12Bit((float) (sx * sy * cz - cx * sz)); // r12
        this.matrix[0][2] = Utils.floatToFixedPointShort12Bit((float) (cx * sy * cz + sx * sz)); // r13
        this.matrix[1][0] = Utils.floatToFixedPointShort12Bit((float) (cy * sz)); // r21
        this.matrix[1][1] = Utils.floatToFixedPointShort12Bit((float) (sx * sy * sz + cx * cz)); // r22
        this.matrix[1][2] = Utils.floatToFixedPointShort12Bit((float) (cx * sy * sz - sx * cz)); // r23
        this.matrix[2][0] = Utils.floatToFixedPointShort12Bit((float) -sy); // r31
        this.matrix[2][1] = Utils.floatToFixedPointShort12Bit((float) (sx * cy)); // r32
        this.matrix[2][2] = Utils.floatToFixedPointShort12Bit((float) (cx * cy)); // r33
    }

    /**
     * Equivalent to ApplyRotMatrix.
     * @param rotationMatrix The matrix to get rotation data from.
     * @param input          The coordinate input.
     * @param output         Where to write the output.
     * @return rotMatrix
     */
    public static IVector MRApplyRotMatrix(PSXMatrix rotationMatrix, SVector input, IVector output) {
        output.setX(((rotationMatrix.matrix[0][0] * input.getX()) + (rotationMatrix.matrix[0][1] * input.getY()) + (rotationMatrix.matrix[0][2] * input.getZ())) >> 12);
        output.setY(((rotationMatrix.matrix[1][0] * input.getX()) + (rotationMatrix.matrix[1][1] * input.getY()) + (rotationMatrix.matrix[1][2] * input.getZ())) >> 12);
        output.setZ(((rotationMatrix.matrix[2][0] * input.getX()) + (rotationMatrix.matrix[2][1] * input.getY()) + (rotationMatrix.matrix[2][2] * input.getZ())) >> 12);
        return output;
    }

    /**
     * Equivalent to MulMatrix0. (http://psxdev.tlrmcknz.com/psyq/ref/libref46/0441.html?sidebar=outlines)
     * @param a An input matrix.
     * @param b Another input matrix.
     * @param c Output matrix.
     */
    public static void MRMulMatrixABC(PSXMatrix a, PSXMatrix b, PSXMatrix c) {
        PSXMatrix tmpMtx = new PSXMatrix();
        tmpMtx.setTransform(c.getTransform());

        // Clear matrix (IMPORTANT!)
        for (int i = 0; i < DIMENSION; i++)
            for (int j = 0; j < DIMENSION; j++)
                tmpMtx.matrix[i][j] = 0;

        // Perform multiplication
        for (int i = 0; i < DIMENSION; i++)
            for (int j = 0; j < DIMENSION; j++)
                for (int k = 0; k < DIMENSION; k++)
                    tmpMtx.matrix[i][j] += (short) ((b.matrix[k][j] * a.matrix[i][k]) >> 12);

        // Copy values across to output matrix
        c.setTransform(tmpMtx.getTransform());
        c.setMatrix(tmpMtx.getMatrix());
    }

    /**
     * Equivalent to MR_SCALE_MATRIX.
     */
    public static void MRScaleMatrix(PSXMatrix matrix, int scaleX, int scaleY, int scaleZ) {
        PSXMatrix scalingMatrix = new PSXMatrix();
        scalingMatrix.getMatrix()[0][0] = (short) scaleX;
        scalingMatrix.getMatrix()[1][1] = (short) scaleY;
        scalingMatrix.getMatrix()[2][2] = (short) scaleZ;
        MRMulMatrixABB(scalingMatrix, matrix);
    }

    /**
     * Equivalent to MulMatrix2
     * @param input  The matrix to multiply by.
     * @param output The output matrix.
     */
    public static void MRMulMatrixABB(PSXMatrix input, PSXMatrix output) {
        // Equivalent to MRMulMatrixABC(a, b, b);
        int i11 = input.getMatrix()[0][0];
        int i12 = input.getMatrix()[0][1];
        int i13 = input.getMatrix()[0][2];
        int i21 = input.getMatrix()[1][0];
        int i22 = input.getMatrix()[1][1];
        int i23 = input.getMatrix()[1][2];
        int i31 = input.getMatrix()[2][0];
        int i32 = input.getMatrix()[2][1];
        int i33 = input.getMatrix()[2][2];
        int o11 = output.getMatrix()[0][0];
        int o12 = output.getMatrix()[0][1];
        int o13 = output.getMatrix()[0][2];
        int o21 = output.getMatrix()[1][0];
        int o22 = output.getMatrix()[1][1];
        int o23 = output.getMatrix()[1][2];
        int o31 = output.getMatrix()[2][0];
        int o32 = output.getMatrix()[2][1];
        int o33 = output.getMatrix()[2][2];

        output.getMatrix()[0][0] = (short) (((i12 * o21) + (i13 * o31) + (o11 * i11)) >> 12);
        output.getMatrix()[0][1] = (short) (((i12 * o22) + (i13 * o32) + (o12 * i11)) >> 12);
        output.getMatrix()[0][2] = (short) (((i13 * o33) + (i12 * o23) + (o13 * i11)) >> 12);
        output.getMatrix()[1][0] = (short) (((i23 * o31) + (o21 * i22) + (i21 * o11)) >> 12);
        output.getMatrix()[1][1] = (short) (((o13 * i31) + (o23 * i32) + (o33 * i33)) >> 12);
        output.getMatrix()[1][2] = (short) (((o12 * i21) + (o22 * i22) + (i23 * o32)) >> 12);
        output.getMatrix()[2][0] = (short) (((o13 * i21) + (i22 * o23) + (i23 * o33)) >> 12);
        output.getMatrix()[2][1] = (short) (((o21 * i32) + (o31 * i33) + (i31 * o11)) >> 12);
        output.getMatrix()[2][2] = (short) (((o22 * i32) + (o12 * i31) + (o32 * i33)) >> 12);
    }

    /**
     * Equivalent to ApplyMatrix. matrix -> vec (http://psxdev.tlrmcknz.com/psyq/ref/libref46/0392.html?sidebar=outlines)
     * @param matrix The matrix to multiply.
     * @param vector The short vector to multiply the matrix by.
     * @param output The output to write to.
     */
    public static IVector MRApplyMatrix(PSXMatrix matrix, SVector vector, IVector output) {
        output.setX((((matrix.matrix[0][0] * vector.getX()) + (matrix.matrix[0][1] * vector.getY()) + (matrix.matrix[0][2] * vector.getZ())) >> 12) + matrix.transform[0]);
        output.setY((((matrix.matrix[1][0] * vector.getX()) + (matrix.matrix[1][1] * vector.getY()) + (matrix.matrix[1][2] * vector.getZ())) >> 12) + matrix.transform[1]);
        output.setZ((((matrix.matrix[2][0] * vector.getX()) + (matrix.matrix[2][1] * vector.getY()) + (matrix.matrix[2][2] * vector.getZ())) >> 12) + matrix.transform[2]);
        return output;
    }

    public static PSXMatrix WriteAxesAsMatrix(PSXMatrix matrix, IVector vecX, IVector vecY, IVector vecZ) {
        matrix.getMatrix()[0][0] = (short) vecX.getX();
        matrix.getMatrix()[1][0] = (short) vecX.getY();
        matrix.getMatrix()[2][0] = (short) vecX.getZ();
        matrix.getMatrix()[0][1] = (short) vecY.getX();
        matrix.getMatrix()[1][1] = (short) vecY.getY();
        matrix.getMatrix()[2][1] = (short) vecY.getZ();
        matrix.getMatrix()[0][2] = (short) vecZ.getX();
        matrix.getMatrix()[1][2] = (short) vecZ.getY();
        matrix.getMatrix()[2][2] = (short) vecZ.getZ();
        return matrix;
    }

    @Override
    public String toString() {
        return "PsxMatrix Pos[" + getTransform()[0] + ", " + getTransform()[1] + ", " + getTransform()[2] + "] Pitch: " + getPitchAngle() + ", Yaw: " + getYawAngle() + ", Roll: " + getRollAngle();
    }

    /**
     * Returns a new identity matrix, assuming 4.12 fixed point representation.
     * @return identityMatrix
     */
    public static PSXMatrix newIdentityMatrix() {
        PSXMatrix matrix = new PSXMatrix();
        matrix.getMatrix()[0][0] = (short) 0x1000;
        matrix.getMatrix()[1][1] = (short) 0x1000;
        matrix.getMatrix()[2][2] = (short) 0x1000;
        return matrix;
    }
}