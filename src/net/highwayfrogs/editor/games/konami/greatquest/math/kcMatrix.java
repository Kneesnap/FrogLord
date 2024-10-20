package net.highwayfrogs.editor.games.konami.greatquest.math;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.system.math.Matrix4x4f;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.system.math.Vector4f;

import java.util.Arrays;

/**
 * Represents a 4x4 matrix.
 * Created by Kneesnap on 4/19/2024.
 */
@Getter
public class kcMatrix extends GameData<GreatQuestInstance> {
    private final Matrix4x4f wrappedMatrix = new Matrix4x4f();

    public static final int MATRIX_WIDTH = Matrix4x4f.MATRIX_WIDTH;
    public static final int MATRIX_HEIGHT = Matrix4x4f.MATRIX_HEIGHT;
    public static final int BYTE_SIZE = Matrix4x4f.BYTE_SIZE;

    public kcMatrix(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        for (int y = 0; y < MATRIX_HEIGHT; y++)
            for (int x = 0; x < MATRIX_WIDTH; x++)
                this.wrappedMatrix.getInternalMatrix()[y][x] = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        for (int y = 0; y < MATRIX_HEIGHT; y++)
            for (int x = 0; x < MATRIX_WIDTH; x++)
                writer.writeFloat(this.wrappedMatrix.getInternalMatrix()[y][x]);
    }

    @Override
    public String toString() {
        float[][] rawMatrixData = this.wrappedMatrix.getInternalMatrix();
        return "kcMatrix[" + Arrays.toString(rawMatrixData[0]) + "," + Arrays.toString(rawMatrixData[1]) + "," + Arrays.toString(rawMatrixData[2]) + "," + Arrays.toString(rawMatrixData[3]) + "]";
    }

    /**
     * Converts the quaternion's rotation into a rotation matrix using the methods seen in Frogger: The Great Quest.
     * Equivalent to kcQuatToMatrix(), differing from Matrix4x4f.initialiseFromQuaternion with subtle differences.
     * @param quaternion The quaternion to rotate.
     * @param result The matrix to store the resulting matrix data within
     * @return The resulting matrix
     */
    public static Matrix4x4f kcQuatToMatrix(Vector4f quaternion, Matrix4x4f result) {
        float sqx = quaternion.getX() * quaternion.getX();
        float sqy = quaternion.getY() * quaternion.getY();
        float sqz = quaternion.getZ() * quaternion.getZ();
        float sqw = quaternion.getW() * quaternion.getW();
        float dot = (sqx + sqy + sqz + sqw); // This is effectively calculating the dot product, though we avoid the function call because we want to directly re-use some of these values.)

        float xy = quaternion.getX() * quaternion.getY();
        float xz = quaternion.getX() * quaternion.getZ();
        float xw = quaternion.getX() * quaternion.getW();

        float yz = quaternion.getY() * quaternion.getZ();
        float yw = quaternion.getY() * quaternion.getW();

        float zw = quaternion.getZ() * quaternion.getW();

        // NOTE: kcQuatToMatrix will set s2 to zero if the dot product is <= 0. However, as the dot product is calculated by squaring numbers, it can never be negative, meaning it will only do that when it's zero.
        // I've not seen this elsewhere, but it seems like a decent thing to do so, I've added it.
        float s2 = (dot > 0) ? (2f / dot) : 0;

        float[][] resultMatrixRaw = result.getInternalMatrix();
        resultMatrixRaw[0][0] = 1f - (s2 * (sqy + sqz));
        resultMatrixRaw[1][1] = 1f - (s2 * (sqx + sqz));
        resultMatrixRaw[2][2] = 1f - (s2 * (sqx + sqy));

        resultMatrixRaw[0][1] = s2 * (xy - zw);
        resultMatrixRaw[1][0] = s2 * (xy + zw);

        resultMatrixRaw[2][0] = s2 * (xz - yw);
        resultMatrixRaw[0][2] = s2 * (xz + yw);

        resultMatrixRaw[2][1] = s2 * (yz + xw);
        resultMatrixRaw[1][2] = s2 * (yz - xw);

        resultMatrixRaw[0][3] = 0;
        resultMatrixRaw[1][3] = 0;
        resultMatrixRaw[2][3] = 0;

        resultMatrixRaw[3][0] = 0;
        resultMatrixRaw[3][1] = 0;
        resultMatrixRaw[3][2] = 0;
        resultMatrixRaw[3][3] = 1;
        return result;
    }

    /**
     * Builds a rotation matrix from a quaternion.
     * @param quaternion The quaternion to rotate.
     * @return The resulting matrix
     */
    public static Matrix4x4f createFromQuaternion(Vector4f quaternion) {
        return kcQuatToMatrix(quaternion, new Matrix4x4f());
    }

    /**
     * Inverts a matrix using the implementation seen in Frogger: The Great Quest.
     * Equivalent to kcMatrixInvert(), implemented differently than Matrix4x4f.invert().
     * @param input The matrix to invert.
     * @param result The matrix to store the inverted matrix data within
     * @return The resulting/inverted matrix
     */
    public static Matrix4x4f kcMatrixInvert(Matrix4x4f input, Matrix4x4f result) {
        float[][] inputMatrix = input.getInternalMatrix();
        
        float m11 = inputMatrix[0][0], m12 = inputMatrix[0][1], m13 = inputMatrix[0][2],
                m21 = inputMatrix[1][0], m22 = inputMatrix[1][1], m23 = inputMatrix[1][2],
                m31 = inputMatrix[2][0], m32 = inputMatrix[2][1], m33 = inputMatrix[2][2],
                m41 = inputMatrix[3][0], m42 = inputMatrix[3][1], m43 = inputMatrix[3][2], m44 = inputMatrix[3][3];
        
        float[][] outputMatrix = result.getInternalMatrix();
        outputMatrix[0][0] = m11;
        outputMatrix[0][1] = m21;
        outputMatrix[0][2] = m31;
        outputMatrix[0][3] = 0;
        outputMatrix[1][0] = m12;
        outputMatrix[1][1] = m22;
        outputMatrix[1][2] = m32;
        outputMatrix[1][3] = 0;
        outputMatrix[2][0] = m13;
        outputMatrix[2][1] = m23;
        outputMatrix[2][2] = m33;
        outputMatrix[2][3] = 0;
        outputMatrix[3][0] = -((m11 * m41) + (m12 * m42) + (m13 * m43));
        outputMatrix[3][1] = -((m21 * m41) + (m22 * m42) + (m23 * m43));
        outputMatrix[3][2] = -((m31 * m41) + (m32 * m42) + (m33 * m43));
        outputMatrix[3][3] = m44;
        return result;
    }

    /**
     * Multiplies a vector by this matrix. (Applies the rotation)
     * This inverts the column/row usages of the matrix entries as seen in the real kcMatrixMulVector().
     * @param matrix the rotation matrix to apply
     * @param position The position to multiply against.
     * @param output The vector to store the output within
     */
    public static Vector3f kcMatrixMulVector(Matrix4x4f matrix, Vector3f position, Vector3f output) {
        if (output == null)
            output = new Vector3f();

        float posX = position.getX();
        float posY = position.getY();
        float posZ = position.getZ();
        float[][] internalMatrix = matrix.getInternalMatrix();
        output.setX((((internalMatrix[0][0] * posX) + (internalMatrix[1][0] * posY) + (internalMatrix[2][0] * posZ))) + internalMatrix[3][0]);
        output.setY((((internalMatrix[0][1] * posX) + (internalMatrix[1][1] * posY) + (internalMatrix[2][1] * posZ))) + internalMatrix[3][1]);
        output.setZ((((internalMatrix[0][2] * posX) + (internalMatrix[1][2] * posY) + (internalMatrix[2][2] * posZ))) + internalMatrix[3][2]);
        return output;
    }
}