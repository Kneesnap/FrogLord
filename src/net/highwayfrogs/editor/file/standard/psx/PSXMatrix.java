package net.highwayfrogs.editor.file.standard.psx;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.animation.transform.MR_MAT34B;
import net.highwayfrogs.editor.file.mof.animation.transform.MR_QUATB_TRANS;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a MR_MAT struct, which is based on the PSX "MATRIX" struct in libgte.h.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PSXMatrix extends GameObject {
    private short[][] matrix = new short[DIMENSION][DIMENSION]; // 3x3 Rotation Matrix.
    private int[] transform = new int[DIMENSION]; // Transform vector.
    private short padding; // This is not in the struct. It is present in all tested versions, so it may be added automatically by the compiler.

    private static final int DIMENSION = 3;
    public static final int BYTE_SIZE = (DIMENSION * DIMENSION * Constants.SHORT_SIZE) + (DIMENSION * Constants.INTEGER_SIZE);

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                this.matrix[i][j] = reader.readShort();

        for (int i = 0; i < this.transform.length; i++)
            this.transform[i] = reader.readInt();

        this.padding = reader.readShort();
        if (this.padding != 0 && this.padding != -1)
            throw new RuntimeException("Expected a padding value!");
    }

    @Override
    public void save(DataWriter writer) {
        for (short[] aMatrix : this.matrix)
            for (short aShort : aMatrix)
                writer.writeShort(aShort);

        for (int aTransfer : this.transform)
            writer.writeInt(aTransfer);

        writer.writeShort(this.padding);
    }

    /**
     * Make a new PSXMatrix from a QUATB_TRANSLATION.
     * @param quatB The QuatB to make the matrix from.
     * @return psxMatrix
     */
    public static PSXMatrix makeMatrixFromQuatB(MR_QUATB_TRANS quatB) {
        int xs = quatB.getX() << 1;
        int ys = quatB.getY() << 1;
        int zs = quatB.getZ() << 1;
        int wx = quatB.getC() * xs;
        int wy = quatB.getC() * ys;
        int wz = quatB.getC() * zs;
        int xx = quatB.getX() * xs;
        int xy = quatB.getX() * ys;
        int xz = quatB.getX() * zs;
        int yy = quatB.getY() * ys;
        int yz = quatB.getY() * zs;
        int zz = quatB.getZ() * zs;

        PSXMatrix matrix = new PSXMatrix();

        // Oddly, every set is bit-shifted right 0 places. Not sure what that does, maybe it does something special in C.
        matrix.matrix[0][0] = (short) (0x1000 - (yy + zz));
        matrix.matrix[0][1] = (short) (xy + wz);
        matrix.matrix[0][2] = (short) (xz - wy);
        matrix.matrix[1][0] = (short) (xy - wz);
        matrix.matrix[1][1] = (short) (0x1000 - (xx + zz));
        matrix.matrix[1][2] = (short) (yz + wx);
        matrix.matrix[2][0] = (short) (xz + wy);
        matrix.matrix[2][1] = (short) (yz - wx);
        matrix.matrix[2][2] = (short) (0x1000 - (xx + yy));

        // Set transform.
        matrix.transform[0] = quatB.getTransform()[0];
        matrix.transform[1] = quatB.getTransform()[1];
        matrix.transform[2] = quatB.getTransform()[2];

        return matrix;
    }

    /**
     * Makes a PSXMatrix from a MR_MAT34B.
     * @param mat The matrix to make a new matrix from.
     * @return matrix
     */
    public static PSXMatrix makeMatrixFromMat34B(MR_MAT34B mat) {
        PSXMatrix matrix = new PSXMatrix();
        matrix.matrix[0][0] = (short) (mat.getMatrix()[0][0] << 5);
        matrix.matrix[0][1] = (short) (mat.getMatrix()[0][1] << 5);
        matrix.matrix[0][2] = (short) (mat.getMatrix()[0][2] << 5);
        matrix.matrix[1][0] = (short) (mat.getMatrix()[1][0] << 5);
        matrix.matrix[1][1] = (short) (mat.getMatrix()[1][1] << 5);
        matrix.matrix[1][2] = (short) (mat.getMatrix()[1][2] << 5);
        matrix.matrix[2][0] = (short) (mat.getMatrix()[2][0] << 5);
        matrix.matrix[2][1] = (short) (mat.getMatrix()[2][1] << 5);
        matrix.matrix[2][2] = (short) (mat.getMatrix()[2][2] << 5);

        // Copy translation
        matrix.transform[0] = mat.getTransform()[0];
        matrix.transform[1] = mat.getTransform()[1];
        matrix.transform[2] = mat.getTransform()[2];
        return matrix;
    }

    /**
     * Equivalent to ApplyRotMatrix.
     * @param rotationMatrix The matrix to get rotation data from.
     * @param input          The coordinate input.
     * @param outputMatrix   Where to write the output.
     * @return rotMatrix
     */
    public static IVector MRApplyRotMatrix(PSXMatrix rotationMatrix, SVector input, IVector outputMatrix) {
        //TODO
        return outputMatrix;
    }


    /**
     * Equivalent to MulMatrix0. (http://psxdev.tlrmcknz.com/psyq/ref/libref46/0441.html?sidebar=outlines)
     * @param m0 An input matrix.
     * @param m1 Another input matrix.
     * @param m2 Output matrix.
     */
    public static void MRMulMatrixABC(PSXMatrix m0, PSXMatrix m1, PSXMatrix m2) {
        //TODO
        throw new UnsupportedOperationException("MulMatrix0 is not implemented yet.");
    }

    /**
     * Equivalent to ApplyMatrix. matrix.svec = vec (http://psxdev.tlrmcknz.com/psyq/ref/libref46/0392.html?sidebar=outlines)
     * @param matrix The matrix to multiply.
     * @param vector The short vector to multiply the matrix by.
     * @param output The output to write to.
     */
    public static void MRApplyMatrix(PSXMatrix matrix, SVector vector, IVector output) {
        //TODO
        throw new UnsupportedOperationException("ApplyMatrix is not implemented yet.");
    }

    /**
     * Equivalent to ApplyMatrix. matrix.svec = vec (http://psxdev.tlrmcknz.com/psyq/ref/libref46/0392.html?sidebar=outlines)
     * @param matrix The matrix to multiply.
     * @param vector The short vector to multiply the matrix by.
     * @param output The output to write to.
     */
    public static void MRApplyMatrix(PSXMatrix matrix, SVector vector, int[] output) {
        IVector outputVector = new IVector();
        MRApplyMatrix(matrix, vector, outputVector);
        output[0] = outputVector.getX();
        output[1] = outputVector.getY();
        output[2] = outputVector.getZ();
    }
}
