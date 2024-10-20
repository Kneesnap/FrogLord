package net.highwayfrogs.editor.system.math;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;

import java.util.Arrays;

/**
 * Represents a 4x4 matrix composed of floating point values.
 * This has been backported from ModToolFramework.
 * Methods operate in-place (update the class instance) and can be assumed to not allocate heap memory unless otherwise specified.
 * Created by Kneesnap on 10/3/2024.
 */
@Getter
public class Matrix4x4f {
    private final float[][] internalMatrix = new float[MATRIX_WIDTH][MATRIX_HEIGHT];
    
    public static final int MATRIX_WIDTH = 4;
    public static final int MATRIX_HEIGHT = 4;
    public static final int BYTE_SIZE = MATRIX_WIDTH * MATRIX_HEIGHT * Constants.FLOAT_SIZE;

    public Matrix4x4f() {
        // Default matrix is all zero.
    }

    /**
     * Calculates the determinant of this matrix.
     */
    public double getDeterminant() {
        return internalMatrix[0][0] * internalMatrix[1][1] * internalMatrix[2][2] * internalMatrix[3][3] - internalMatrix[0][0] * internalMatrix[1][1] * internalMatrix[2][3] * internalMatrix[3][2] + internalMatrix[0][0] * internalMatrix[1][2] * internalMatrix[2][3] * internalMatrix[3][1] - internalMatrix[0][0] * internalMatrix[1][2] * internalMatrix[2][1] * internalMatrix[3][3]
                + internalMatrix[0][0] * internalMatrix[1][3] * internalMatrix[2][1] * internalMatrix[3][2] - internalMatrix[0][0] * internalMatrix[1][3] * internalMatrix[2][2] * internalMatrix[3][1] - internalMatrix[0][1] * internalMatrix[1][2] * internalMatrix[2][3] * internalMatrix[3][0] + internalMatrix[0][1] * internalMatrix[1][2] * internalMatrix[2][0] * internalMatrix[3][3]
                - internalMatrix[0][1] * internalMatrix[1][3] * internalMatrix[2][0] * internalMatrix[3][2] + internalMatrix[0][1] * internalMatrix[1][3] * internalMatrix[2][2] * internalMatrix[3][0] - internalMatrix[0][1] * internalMatrix[1][0] * internalMatrix[2][2] * internalMatrix[3][3] + internalMatrix[0][1] * internalMatrix[1][0] * internalMatrix[2][3] * internalMatrix[3][2]
                + internalMatrix[0][2] * internalMatrix[1][3] * internalMatrix[2][0] * internalMatrix[3][1] - internalMatrix[0][2] * internalMatrix[1][3] * internalMatrix[2][1] * internalMatrix[3][0] + internalMatrix[0][2] * internalMatrix[1][0] * internalMatrix[2][1] * internalMatrix[3][3] - internalMatrix[0][2] * internalMatrix[1][0] * internalMatrix[2][3] * internalMatrix[3][1]
                + internalMatrix[0][2] * internalMatrix[1][1] * internalMatrix[2][3] * internalMatrix[3][0] - internalMatrix[0][2] * internalMatrix[1][1] * internalMatrix[2][0] * internalMatrix[3][3] - internalMatrix[0][3] * internalMatrix[1][0] * internalMatrix[2][1] * internalMatrix[3][2] + internalMatrix[0][3] * internalMatrix[1][0] * internalMatrix[2][2] * internalMatrix[3][1]
                - internalMatrix[0][3] * internalMatrix[1][1] * internalMatrix[2][2] * internalMatrix[3][0] + internalMatrix[0][3] * internalMatrix[1][1] * internalMatrix[2][0] * internalMatrix[3][2] - internalMatrix[0][3] * internalMatrix[1][2] * internalMatrix[2][0] * internalMatrix[3][1] + internalMatrix[0][3] * internalMatrix[1][2] * internalMatrix[2][1] * internalMatrix[3][0];
    }

    /**
     * Becomes an identity matrix.
     */
    public Matrix4x4f setIdentity() {
        for (int i = 0; i < this.internalMatrix.length; i++) {
            Arrays.fill(this.internalMatrix[i], 0);
            this.internalMatrix[i][i] = 1F;
        }

        return this;
    }

    /**
     * Returns a new matrix with the same contents as this one.
     */
    public Matrix4x4f clone() {
        return new Matrix4x4f().set(this.internalMatrix);
    }

    /**
     * Set the contents of the matrix to match the provided floating point arrays.
     * @param row0 The floating point values to treat as the first row
     * @param row1 The floating point values to treat as the second row
     * @param row2 The floating point values to treat as the third row
     * @param row3 The floating point values to treat as the fourth (translation) row
     */
    public Matrix4x4f set(float[] row0, float[] row1, float[] row2, float[] row3) {
        if (row0 == null)
            throw new NullPointerException("row0");
        if (row1 == null)
            throw new NullPointerException("row1");
        if (row2 == null)
            throw new NullPointerException("row2");
        if (row3 == null)
            throw new NullPointerException("row3");
        if (row0.length != MATRIX_WIDTH)
            throw new IllegalArgumentException("row0 had " + row0.length + " elements, instead of " + MATRIX_WIDTH + "!");
        if (row1.length != MATRIX_WIDTH)
            throw new IllegalArgumentException("row1 had " + row1.length + " elements, instead of " + MATRIX_WIDTH + "!");
        if (row2.length != MATRIX_WIDTH)
            throw new IllegalArgumentException("row2 had " + row2.length + " elements, instead of " + MATRIX_WIDTH + "!");
        if (row3.length != MATRIX_WIDTH)
            throw new IllegalArgumentException("row3 had " + row3.length + " elements, instead of " + MATRIX_WIDTH + "!");
        
        System.arraycopy(row0, 0, this.internalMatrix[0], 0, MATRIX_WIDTH);
        System.arraycopy(row1, 0, this.internalMatrix[1], 0, MATRIX_WIDTH);
        System.arraycopy(row2, 0, this.internalMatrix[2], 0, MATRIX_WIDTH);
        System.arraycopy(row3, 0, this.internalMatrix[3], 0, MATRIX_WIDTH);
        return this;
    }

    /**
     * Set the contents of the matrix to match the provided floating point array.
     * @param rawMatrixData The floating point values to apply
     */
    public Matrix4x4f set(float[][] rawMatrixData) {
        if (rawMatrixData == null)
            throw new NullPointerException("rawMatrixData");
        if (rawMatrixData.length != MATRIX_HEIGHT)
            throw new IllegalArgumentException("The provided matrix data had " + rawMatrixData.length + " rows, instead of " + MATRIX_HEIGHT + "!");

        for (int i = 0; i < rawMatrixData.length; i++)
            if (rawMatrixData[i].length != MATRIX_WIDTH)
                throw new IllegalArgumentException("row" + i + " had " + rawMatrixData[i].length + " elements, instead of " + MATRIX_WIDTH + "!");

        for (int i = 0; i < this.internalMatrix.length; i++)
            System.arraycopy(rawMatrixData[i], 0, this.internalMatrix[i], 0, MATRIX_WIDTH);
        return this;
    }

    /**
     * Sets the contents of this matrix to match that of another matrix.
     * @param other the matrix to apply the contents from
     * @return this
     */
    public Matrix4x4f set(Matrix4x4f other) {
        if (other == null)
            throw new NullPointerException("other");
        
        return this.set(other.internalMatrix);
    }

    /**
     * Gets the translation X coordinate
     * @return translationX
     */
    public float getTranslationX() {
        return this.internalMatrix[3][0];
    }

    /**
     * Gets the translation Y coordinate
     * @return translationY
     */
    public float getTranslationY() {
        return this.internalMatrix[3][1];
    }

    /**
     * Gets the translation Z coordinate
     * @return translationZ
     */
    public float getTranslationZ() {
        return this.internalMatrix[3][2];
    }

    /**
     * Gets the matrix translation component as a vector.
     * @return The resulting translation vector
     */
    public Vector3f getTranslation() {
        return getTranslation(null);
    }

    /**
     * Gets the matrix translation component as a vector.
     * @param result The vector to store the translation within. If null is specified, a new one will be allocated.
     * @return The resulting translation vector
     */
    public Vector3f getTranslation(Vector3f result) {
        if (result == null)
            result = new Vector3f();
        
        return result.setXYZ(this.internalMatrix[3][0], this.internalMatrix[3][1], this.internalMatrix[3][2]);
    }

    /**
     * Sets the translation for this matrix.
     * @param x The new x translation coordinate
     * @param y The new y translation coordinate
     * @param z The new z translation coordinate
     * @return this
     */
    public Matrix4x4f setTranslation(float x, float y, float z) {
        this.internalMatrix[3][0] = x;
        this.internalMatrix[3][1] = y;
        this.internalMatrix[3][2] = z;
        return this;
    }

    /**
     * Sets the translation for this matrix.
     * @param translation the translation to apply
     * @return this
     */
    public Matrix4x4f setTranslation(Vector3f translation) {
        if (translation == null)
            throw new NullPointerException("translation");
        return setTranslation(translation.getX(), translation.getY(), translation.getZ());
    }
    
    /**
     * Converts this matrix into its inverse.
     */
    public Matrix4x4f invert() {
        float m41 = this.internalMatrix[3][0], m42 = this.internalMatrix[3][1], m43 = this.internalMatrix[3][2], m44 = this.internalMatrix[3][3];
        float m11 = this.internalMatrix[0][0], m12 = this.internalMatrix[0][1], m13 = this.internalMatrix[0][2], m14 = this.internalMatrix[0][3];
        float m21 = this.internalMatrix[1][0], m22 = this.internalMatrix[1][1], m23 = this.internalMatrix[1][2], m24 = this.internalMatrix[1][3];
        float m31 = this.internalMatrix[2][0], m32 = this.internalMatrix[2][1], m33 = this.internalMatrix[2][2], m34 = this.internalMatrix[2][3];

        // Do an affine inversion.
        if (m41 == 0 && m42 == 0 && m43 == 0 && m44 == 1.0f) {
            float d = m11 * m22 * m33 + m21 * m32 * m13 + m31 * m12 * m23 -
                    m11 * m32 * m23 - m31 * m22 * m13 - m21 * m12 * m33;

            if (d == 0.0f)
                throw new IllegalStateException("Matrix is singular and cannot be inverted.");

            float d1 = 1 / d;

            // sub 3x3 inv
            this.internalMatrix[0][0] = d1 * (m22 * m33 - m23 * m32);
            this.internalMatrix[0][1] = d1 * (m13 * m32 - m12 * m33);
            this.internalMatrix[0][2] = d1 * (m12 * m23 - m13 * m22);
            this.internalMatrix[1][0] = d1 * (m23 * m31 - m21 * m33);
            this.internalMatrix[1][1] = d1 * (m11 * m33 - m13 * m31);
            this.internalMatrix[1][2] = d1 * (m13 * m21 - m11 * m23);
            this.internalMatrix[2][0] = d1 * (m21 * m32 - m22 * m31);
            this.internalMatrix[2][1] = d1 * (m12 * m31 - m11 * m32);
            this.internalMatrix[2][2] = d1 * (m11 * m22 - m12 * m21);

            // - sub 3x3 inv * b
            this.internalMatrix[0][3] = -this.internalMatrix[0][0] * m14 - this.internalMatrix[0][1] * m24 - this.internalMatrix[0][2] * m34;
            this.internalMatrix[1][3] = -this.internalMatrix[1][0] * m14 - this.internalMatrix[1][1] * m24 - this.internalMatrix[1][2] * m34;
            this.internalMatrix[2][3] = -this.internalMatrix[2][0] * m14 - this.internalMatrix[2][1] * m24 - this.internalMatrix[2][2] * m34;

            // last row remains 0 0 0 1
            this.internalMatrix[3][0] = this.internalMatrix[3][1] = this.internalMatrix[3][2] = 0.0f;
            this.internalMatrix[3][3] = 1.0f;
            return this;
        }

        float d = (float) getDeterminant();
        if (d == 0.0f)
            throw new IllegalStateException("Matrix is singular and cannot be inverted.");

        float d1 = 1 / d;
        this.internalMatrix[0][0] = d1 * (m22 * m33 * m44 + m23 * m34 * m42 + m24 * m32 * m43 - m22 * m34 * m43 - m23 * m32 * m44 - m24 * m33 * m42);
        this.internalMatrix[0][1] = d1 * (m12 * m34 * m43 + m13 * m32 * m44 + m14 * m33 * m42 - m12 * m33 * m44 - m13 * m34 * m42 - m14 * m32 * m43);
        this.internalMatrix[0][2] = d1 * (m12 * m23 * m44 + m13 * m24 * m42 + m14 * m22 * m43 - m12 * m24 * m43 - m13 * m22 * m44 - m14 * m23 * m42);
        this.internalMatrix[0][3] = d1 * (m12 * m24 * m33 + m13 * m22 * m34 + m14 * m23 * m32 - m12 * m23 * m34 - m13 * m24 * m32 - m14 * m22 * m33);
        this.internalMatrix[1][0] = d1 * (m21 * m34 * m43 + m23 * m31 * m44 + m24 * m33 * m41 - m21 * m33 * m44 - m23 * m34 * m41 - m24 * m31 * m43);
        this.internalMatrix[1][1] = d1 * (m11 * m33 * m44 + m13 * m34 * m41 + m14 * m31 * m43 - m11 * m34 * m43 - m13 * m31 * m44 - m14 * m33 * m41);
        this.internalMatrix[1][2] = d1 * (m11 * m24 * m43 + m13 * m21 * m44 + m14 * m23 * m41 - m11 * m23 * m44 - m13 * m24 * m41 - m14 * m21 * m43);
        this.internalMatrix[1][3] = d1 * (m11 * m23 * m34 + m13 * m24 * m31 + m14 * m21 * m33 - m11 * m24 * m33 - m13 * m21 * m34 - m14 * m23 * m31);
        this.internalMatrix[2][0] = d1 * (m21 * m32 * m44 + m22 * m34 * m41 + m24 * m31 * m42 - m21 * m34 * m42 - m22 * m31 * m44 - m24 * m32 * m41);
        this.internalMatrix[2][1] = d1 * (m11 * m34 * m42 + m12 * m31 * m44 + m14 * m32 * m41 - m11 * m32 * m44 - m12 * m34 * m41 - m14 * m31 * m42);
        this.internalMatrix[2][2] = d1 * (m11 * m22 * m44 + m12 * m24 * m41 + m14 * m21 * m42 - m11 * m24 * m42 - m12 * m21 * m44 - m14 * m22 * m41);
        this.internalMatrix[2][3] = d1 * (m11 * m24 * m32 + m12 * m21 * m34 + m14 * m22 * m31 - m11 * m22 * m34 - m12 * m24 * m31 - m14 * m21 * m32);
        this.internalMatrix[3][0] = d1 * (m21 * m33 * m42 + m22 * m31 * m43 + m23 * m32 * m41 - m21 * m32 * m43 - m22 * m33 * m41 - m23 * m31 * m42);
        this.internalMatrix[3][1] = d1 * (m11 * m32 * m43 + m12 * m33 * m41 + m13 * m31 * m42 - m11 * m33 * m42 - m12 * m31 * m43 - m13 * m32 * m41);
        this.internalMatrix[3][2] = d1 * (m11 * m23 * m42 + m12 * m21 * m43 + m13 * m22 * m41 - m11 * m22 * m43 - m12 * m23 * m41 - m13 * m21 * m42);
        this.internalMatrix[3][3] = d1 * (m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32 - m11 * m23 * m32 - m12 * m21 * m33 - m13 * m22 * m31);
        return this;
    }

    /**
     * Converts this matrix into its transpose.
     */
    public Matrix4x4f transpose() {
        float oldM01 = this.internalMatrix[0][1];
        float oldM02 = this.internalMatrix[0][2];
        float oldM03 = this.internalMatrix[0][3];
        float oldM12 = this.internalMatrix[1][2];
        float oldM13 = this.internalMatrix[1][3];
        float oldM23 = this.internalMatrix[2][3];

        this.internalMatrix[0][1] = this.internalMatrix[1][0];
        this.internalMatrix[0][2] = this.internalMatrix[2][0];
        this.internalMatrix[0][3] = this.internalMatrix[3][0];
        this.internalMatrix[1][0] = oldM01;
        this.internalMatrix[1][2] = this.internalMatrix[2][1];
        this.internalMatrix[1][3] = this.internalMatrix[3][1];
        this.internalMatrix[2][0] = oldM02;
        this.internalMatrix[2][1] = oldM12;
        this.internalMatrix[2][3] = this.internalMatrix[3][2];
        this.internalMatrix[3][0] = oldM03;
        this.internalMatrix[3][1] = oldM13;
        this.internalMatrix[3][2] = oldM23;
        return this;
    }

    /**
     * Returns the rotation component of this instance expressed as a quaternion. Quite slow.
     * Function taken from OpenTK.
     */
    public Vector4f extractRotation() {
        Vector3f row0 = new Vector3f(this.internalMatrix[0][0], this.internalMatrix[0][1], this.internalMatrix[0][2]).normalise();
        Vector3f row1 = new Vector3f(this.internalMatrix[1][0], this.internalMatrix[1][1], this.internalMatrix[1][2]).normalise();
        Vector3f row2 = new Vector3f(this.internalMatrix[2][0], this.internalMatrix[2][1], this.internalMatrix[2][2]).normalise();

        // code below adapted from Blender
        Vector4f q = new Vector4f();
        double trace = 0.25 * (row0.getX() + row1.getY() + row2.getZ() + 1.0);

        if (trace > 0) {
            double sq = Math.sqrt(trace);

            q.setW((float) sq);
            sq = 1.0 / (4.0 * sq);
            q.setX((float) ((row1.getZ() - row2.getY()) * sq));
            q.setY((float) ((row2.getX() - row0.getZ()) * sq));
            q.setZ((float) ((row0.getY() - row1.getX()) * sq));
        } else if (row0.getX() > row1.getY() && row0.getX() > row2.getZ()) {
            double sq = 2.0 * Math.sqrt(1.0 + row0.getX() - row1.getY() - row2.getZ());

            q.setX((float) (0.25 * sq));
            sq = 1.0 / sq;
            q.setW((float) ((row2.getY() - row1.getZ()) * sq));
            q.setY((float) ((row1.getX() + row0.getY()) * sq));
            q.setZ((float) ((row2.getX() + row0.getZ()) * sq));
        } else if (row1.getY() > row2.getZ()) {
            double sq = 2.0 * Math.sqrt(1.0 + row1.getY() - row0.getX() - row2.getZ());

            q.setY((float) (0.25 * sq));
            sq = 1.0 / sq;
            q.setW((float) ((row2.getX() - row0.getZ()) * sq));
            q.setX((float) ((row1.getX() + row0.getY()) * sq));
            q.setZ((float) ((row2.getY() + row1.getZ()) * sq));
        } else {
            double sq = 2.0 * Math.sqrt(1.0 + row2.getZ() - row0.getX() - row1.getY());

            q.setZ((float) (0.25 * sq));
            sq = 1.0 / sq;
            q.setW((float) ((row1.getX() - row0.getY()) * sq));
            q.setX((float) ((row2.getX() + row0.getZ()) * sq));
            q.setY((float) ((row2.getY() + row1.getZ()) * sq));
        }

        return q.normalise();
    }

    /**
     * Gets the x-axis rotation angle (pitch) in radians from this matrix.
     * The rules for approaching 1 are from here. <a href="https://web.archive.org/web/20190807191003/https://www.gregslabaugh.net/publications/euler.pdf"/>
     * TODO: This might not actually be valid, this might need to go with a different matrix type.
     * @return pitchInRadians
     */
    public double getRotationAngleX() {
        float r31 = this.internalMatrix[2][0];

        float r12 = this.internalMatrix[0][1];
        float r13 = this.internalMatrix[0][2];
        if (r31 >= 1) { // Gymbal lock at pitch = -90
            return Math.atan2(-r12, -r13);
        } else if (r31 <= -1) { // Lock at pitch = 90
            return Math.atan2(r12, r13);
        } else {
            float r32 = this.internalMatrix[2][1];
            float r33 = this.internalMatrix[2][2];
            return Math.atan2(r32, r33);
        }
    }

    /**
     * Gets the y-axis rotation angle (yaw) in radians from this matrix.
     * @return yawInRadians
     */
    public double getRotationAngleY() {
        float r31 = this.internalMatrix[2][0];

        if (r31 >= 1) {
            return -Math.PI / 2;
        } else if (r31 <= -1) {
            return Math.PI / 2;
        } else {
            return Math.asin(-r31);
        }
    }

    /**
     * Gets the z rotation angle (roll) from this matrix.
     * @return yawRadians
     */
    public double getRotationAngleZ() {
        float r31 = this.internalMatrix[2][0];

        if (r31 >= 1 || r31 <= -1) { // Gymbal lock at pitch = -90 or 90
            return 0F;
        } else {
            float r11 = this.internalMatrix[0][0];
            float r21 = this.internalMatrix[1][0];
            return Math.atan2(r21, r11);
        }
    }

    /**
     * Gets the euler rotation angles from the matrix and returns them as a vector.
     * @return The resulting euler angles
     */
    public Vector3f getEulerRotationAngles() {
        return getEulerRotationAngles(new Vector3f());
    }

    /**
     * Gets the euler rotation angles from the matrix and returns them as a vector.
     * @param result The vector to save the rotation angles within
     * @return The resulting euler angles
     */
    public Vector3f getEulerRotationAngles(Vector3f result) {
        if (result == null)
            result = new Vector3f();

        return result.setXYZ((float) getRotationAngleX(), (float) getRotationAngleY(), (float) getRotationAngleZ());
    }

    /**
     * Multiply the matrix by a floating-point scalar
     * @param scalar the scalar to multiply the matrix by
     * @return this
     */
    public Matrix4x4f multiply(float scalar) {
        for (int i = 0; i < this.internalMatrix.length; i++)
            for (int j = 0; j < this.internalMatrix[i].length; j++)
                this.internalMatrix[i][j] *= scalar;
        return this;
    }

    /**
     * Multiplies this matrix by another, with new values stored in place.
     * This will perform the operation 'this * other', NOT 'other * this', which is an important distinction for matrices.
     * @param other the matrix to multiply against
     * @return this
     */
    public Matrix4x4f multiply(Matrix4x4f other) {
        return multiply(other, this);
    }

    /**
     * Multiplies this matrix by another, with new values stored in place.
     * This will perform the operation 'this * other', NOT 'other * this', which is an important distinction for matrices.
     * This function has been observed to be functionally equivalent to kcMatrixMul() from the Great Quest, even though it was based on OpenTK.
     * @param other the matrix to multiply against.
     * @return this
     */
    public Matrix4x4f multiply(Matrix4x4f other, Matrix4x4f outputMtx) {
        float lM11 = this.internalMatrix[0][0],
                lM12 = this.internalMatrix[0][1],
                lM13 = this.internalMatrix[0][2],
                lM14 = this.internalMatrix[0][3],
                lM21 = this.internalMatrix[1][0],
                lM22 = this.internalMatrix[1][1],
                lM23 = this.internalMatrix[1][2],
                lM24 = this.internalMatrix[1][3],
                lM31 = this.internalMatrix[2][0],
                lM32 = this.internalMatrix[2][1],
                lM33 = this.internalMatrix[2][2],
                lM34 = this.internalMatrix[2][3],
                lM41 = this.internalMatrix[3][0],
                lM42 = this.internalMatrix[3][1],
                lM43 = this.internalMatrix[3][2],
                lM44 = this.internalMatrix[3][3],
                rM11 = other.internalMatrix[0][0],
                rM12 = other.internalMatrix[0][1],
                rM13 = other.internalMatrix[0][2],
                rM14 = other.internalMatrix[0][3],
                rM21 = other.internalMatrix[1][0],
                rM22 = other.internalMatrix[1][1],
                rM23 = other.internalMatrix[1][2],
                rM24 = other.internalMatrix[1][3],
                rM31 = other.internalMatrix[2][0],
                rM32 = other.internalMatrix[2][1],
                rM33 = other.internalMatrix[2][2],
                rM34 = other.internalMatrix[2][3],
                rM41 = other.internalMatrix[3][0],
                rM42 = other.internalMatrix[3][1],
                rM43 = other.internalMatrix[3][2],
                rM44 = other.internalMatrix[3][3];

        outputMtx.internalMatrix[0][0] = (((lM11 * rM11) + (lM12 * rM21)) + (lM13 * rM31)) + (lM14 * rM41);
        outputMtx.internalMatrix[0][1] = (((lM11 * rM12) + (lM12 * rM22)) + (lM13 * rM32)) + (lM14 * rM42);
        outputMtx.internalMatrix[0][2] = (((lM11 * rM13) + (lM12 * rM23)) + (lM13 * rM33)) + (lM14 * rM43);
        outputMtx.internalMatrix[0][3] = (((lM11 * rM14) + (lM12 * rM24)) + (lM13 * rM34)) + (lM14 * rM44);
        outputMtx.internalMatrix[1][0] = (((lM21 * rM11) + (lM22 * rM21)) + (lM23 * rM31)) + (lM24 * rM41);
        outputMtx.internalMatrix[1][1] = (((lM21 * rM12) + (lM22 * rM22)) + (lM23 * rM32)) + (lM24 * rM42);
        outputMtx.internalMatrix[1][2] = (((lM21 * rM13) + (lM22 * rM23)) + (lM23 * rM33)) + (lM24 * rM43);
        outputMtx.internalMatrix[1][3] = (((lM21 * rM14) + (lM22 * rM24)) + (lM23 * rM34)) + (lM24 * rM44);
        outputMtx.internalMatrix[2][0] = (((lM31 * rM11) + (lM32 * rM21)) + (lM33 * rM31)) + (lM34 * rM41);
        outputMtx.internalMatrix[2][1] = (((lM31 * rM12) + (lM32 * rM22)) + (lM33 * rM32)) + (lM34 * rM42);
        outputMtx.internalMatrix[2][2] = (((lM31 * rM13) + (lM32 * rM23)) + (lM33 * rM33)) + (lM34 * rM43);
        outputMtx.internalMatrix[2][3] = (((lM31 * rM14) + (lM32 * rM24)) + (lM33 * rM34)) + (lM34 * rM44);
        outputMtx.internalMatrix[3][0] = (((lM41 * rM11) + (lM42 * rM21)) + (lM43 * rM31)) + (lM44 * rM41);
        outputMtx.internalMatrix[3][1] = (((lM41 * rM12) + (lM42 * rM22)) + (lM43 * rM32)) + (lM44 * rM42);
        outputMtx.internalMatrix[3][2] = (((lM41 * rM13) + (lM42 * rM23)) + (lM43 * rM33)) + (lM44 * rM43);
        outputMtx.internalMatrix[3][3] = (((lM41 * rM14) + (lM42 * rM24)) + (lM43 * rM34)) + (lM44 * rM44);
        return outputMtx;
    }

    /**
     * Multiplies a vector by this matrix.
     * Probably applies a transform to the vector treated as a position.
     * @param position The position to multiply against.
     */
    public Vector3f multiply(Vector3f position) {
        return multiply(position, new Vector3f());
    }

    /**
     * Multiplies a vector by this matrix.
     * Probably applies a transform to the vector treated as a position.
     * @param position The position to multiply against.
     */
    public Vector3f multiply(Vector3f position, Vector3f output) {
        if (output == null)
            output = new Vector3f();

        float posX = position.getX();
        float posY = position.getY();
        float posZ = position.getZ();
        output.setX((((this.internalMatrix[0][0] * posX) + (this.internalMatrix[0][1] * posY) + (this.internalMatrix[0][2] * posZ))) + this.internalMatrix[3][0]);
        output.setY((((this.internalMatrix[1][0] * posX) + (this.internalMatrix[1][1] * posY) + (this.internalMatrix[1][2] * posZ))) + this.internalMatrix[3][1]);
        output.setZ((((this.internalMatrix[2][0] * posX) + (this.internalMatrix[2][1] * posY) + (this.internalMatrix[2][2] * posZ))) + this.internalMatrix[3][2]);
        return output;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Matrix4x4f))
            return false;

        Matrix4x4f other = (Matrix4x4f) obj;
        return (this == obj)
                || (Arrays.equals(this.internalMatrix[0], other.internalMatrix[0])
                && Arrays.equals(this.internalMatrix[1], other.internalMatrix[1])
                && Arrays.equals(this.internalMatrix[2], other.internalMatrix[2])
                && Arrays.equals(this.internalMatrix[3], other.internalMatrix[3]));
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (int i = 0; i < this.internalMatrix.length; i++)
            for (int j = 0; j < this.internalMatrix[i].length; j++)
                hash ^= Float.hashCode(this.internalMatrix[i][j]);
        return hash;
    }

    @Override
    public String toString() {
        return "Matrix4x4[" + Arrays.toString(this.internalMatrix[0]) + "," + Arrays.toString(this.internalMatrix[1])
                + "," + Arrays.toString(this.internalMatrix[2]) + "," + Arrays.toString(this.internalMatrix[3]) + "]";
    }

    /**
     * Initialize a rotation matrix from the specified axis/angle rotation.
     * @param axis The axis to rotate about
     * @param angle Angle in radians to rotate counter-clockwise (looking in the direction of the given axis)
     * @param result The matrix to apply the axis angle to
     * @return The resulting matrix
     */
    public static Matrix4x4f initializeFromAxisAngle(Vector3f axis, float angle, Matrix4x4f result) {
        float cos = (float) Math.cos(-angle);
        float sin = (float) Math.sin(-angle);
        float t = 1.0f - cos;

        // Don't allocate any memory, so we put it in 3 axis values which go on the stack.
        float oldAxisX = axis.getX();
        float oldAxisY = axis.getY();
        float oldAxisZ = axis.getZ();
        axis.normalise();

        if (result == null)
            result = new Matrix4x4f();

        result.internalMatrix[0][0] = t * axis.getX() * axis.getX() + cos;
        result.internalMatrix[0][1] = t * axis.getX() * axis.getY() - sin * axis.getZ();
        result.internalMatrix[0][2] = t * axis.getX() * axis.getZ() + sin * axis.getY();
        result.internalMatrix[0][3] = 0.0f;

        result.internalMatrix[1][0] = t * axis.getX() * axis.getY() + sin * axis.getZ();
        result.internalMatrix[1][1] = t * axis.getY() * axis.getY() + cos;
        result.internalMatrix[1][2] = t * axis.getY() * axis.getZ() - sin * axis.getX();
        result.internalMatrix[1][3] = 0f;

        result.internalMatrix[2][0] = t * axis.getX() * axis.getZ() - sin * axis.getY();
        result.internalMatrix[2][1] = t * axis.getY() * axis.getZ() + sin * axis.getX();
        result.internalMatrix[2][2] = t * axis.getZ() * axis.getZ() + cos;
        result.internalMatrix[2][3] = 0f;

        result.internalMatrix[3][0] = 0;
        result.internalMatrix[3][1] = 0;
        result.internalMatrix[3][2] = 0;
        result.internalMatrix[3][3] = 1;

        // Restore axis values, don't leave with any changes to it.
        axis.setXYZ(oldAxisX, oldAxisY, oldAxisZ);
        return result;
    }

    /**
     * Build a new rotation matrix from the specified axis/angle rotation.
     * @param axis The axis to rotate about
     * @param angle Angle in radians to rotate counter-clockwise (looking in the direction of the given axis)
     * @return The resulting matrix
     */
    public static Matrix4x4f createFromAxisAngle(Vector3f axis, float angle) {
        return initializeFromAxisAngle(axis, angle, new Matrix4x4f());
    }

    /**
     * Converts the quaternion's rotation into a rotation matrix, using the OpenTK convention.
     * @param quaternion The quaternion to rotate.
     * @param result The matrix to store the resulting matrix data within
     * @return The resulting matrix
     */
    public static Matrix4x4f initialiseFromQuaternion(Vector4f quaternion, Matrix4x4f result) {
        // Adapted from OpenTK, which adapted it from https://en.wikipedia.org/wiki/Quaternions_and_spatial_rotation#Quaternion-derived_rotation_matrix
        // with the caveat that opentk uses row-major matrices so the matrix we create is transposed
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

        result.internalMatrix[0][0] = 1f - (s2 * (sqy + sqz));
        result.internalMatrix[1][1] = 1f - (s2 * (sqx + sqz));
        result.internalMatrix[2][2] = 1f - (s2 * (sqx + sqy));

        result.internalMatrix[0][1] = s2 * (xy + zw);
        result.internalMatrix[1][0] = s2 * (xy - zw);

        result.internalMatrix[2][0] = s2 * (xz + yw);
        result.internalMatrix[0][2] = s2 * (xz - yw);

        result.internalMatrix[2][1] = s2 * (yz - xw);
        result.internalMatrix[1][2] = s2 * (yz + xw);

        result.internalMatrix[0][3] = 0;
        result.internalMatrix[1][3] = 0;
        result.internalMatrix[2][3] = 0;

        result.internalMatrix[3][0] = 0;
        result.internalMatrix[3][1] = 0;
        result.internalMatrix[3][2] = 0;
        result.internalMatrix[3][3] = 1;
        return result;
    }

    /**
     * Builds a rotation matrix from a quaternion.
     * @param quaternion The quaternion to rotate.
     * @return The resulting matrix
     */
    public static Matrix4x4f createFromQuaternion(Vector4f quaternion) {
        return initialiseFromQuaternion(quaternion, new Matrix4x4f());
    }

    /**
     * Initializes a rotation matrix for a rotation around the x-axis.
     * @param angle The counter-clockwise angle in radians.
     * @param result The matrix to write the changes to
     * @return The resulting matrix
     */
    public static Matrix4x4f initializeRotationX(float angle, Matrix4x4f result) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        result.setIdentity();
        result.internalMatrix[1][1] = cos;
        result.internalMatrix[1][2] = sin;
        result.internalMatrix[2][1] = -sin;
        result.internalMatrix[2][2] = cos;
        return result;
    }

    /**
     * Builds a new rotation matrix for a rotation around the x-axis.
     * @param angle The counter-clockwise angle in radians.
     * @return The resulting matrix
     */
    public static Matrix4x4f createRotationX(float angle) {
        return initializeRotationX(angle, new Matrix4x4f());
    }

    /**
     * Initializes a rotation matrix for a rotation around the y-axis.
     * @param angle The counter-clockwise angle in radians.
     * @param result The matrix to write the changes to
     * @return The resulting matrix
     */
    public static Matrix4x4f initializeRotationY(float angle, Matrix4x4f result) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        result = result.setIdentity();
        result.internalMatrix[0][0] = cos;
        result.internalMatrix[0][2] = -sin;
        result.internalMatrix[2][0] = sin;
        result.internalMatrix[2][2] = cos;
        return result;
    }

    /**
     * Builds a new rotation matrix for a rotation around the y-axis.
     * @param angle The counter-clockwise angle in radians.
     * @return The resulting matrix
     */
    public static Matrix4x4f createRotationY(float angle) {
        return initializeRotationY(angle, new Matrix4x4f());
    }

    /**
     * Initializes a rotation matrix for a rotation around the z-axis.
     * @param angle The counter-clockwise angle in radians.
     * @param result The matrix to write the changes to
     * @return The resulting matrix
     */
    public static Matrix4x4f initializeRotationZ(float angle, Matrix4x4f result) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        result = result.setIdentity();
        result.internalMatrix[0][0] = cos;
        result.internalMatrix[0][1] = sin;
        result.internalMatrix[1][0] = -sin;
        result.internalMatrix[1][1] = cos;
        return result;
    }

    /**
     * Builds a rotation matrix for a rotation around the z-axis.
     * @param angle The counter-clockwise angle in radians.
     * @return The resulting matrix
     */
    public static Matrix4x4f createRotationZ(float angle) {
        return initializeRotationZ(angle, new Matrix4x4f());
    }

    /**
     * Initializes a translation matrix.
     * @param x The x coordinate translation
     * @param y The y coordinate translation
     * @param z The z coordinate translation
     * @param result The matrix to write the changes to
     * @return The resulting matrix
     */
    public static Matrix4x4f initializeTranslation(float x, float y, float z, Matrix4x4f result) {
        result = result.setIdentity();
        result.internalMatrix[3][0] = x;
        result.internalMatrix[3][1] = y;
        result.internalMatrix[3][2] = z;
        result.internalMatrix[3][3] = 1;
        return result;
    }

    /**
     * Builds a new translation matrix.
     * @param vector The translation vector
     * @param result The matrix to write the changes to
     * @return The resulting matrix
     */
    public static Matrix4x4f createTranslation(Vector3f vector, Matrix4x4f result) {
        return initializeTranslation(vector.getX(), vector.getY(), vector.getZ(), result);
    }

    /**
     * Builds a new translation matrix.
     * @param x The x coordinate translation
     * @param y The y coordinate translation
     * @param z The z coordinate translation
     * @return The resulting matrix
     */
    public static Matrix4x4f createTranslation(float x, float y, float z) {
        return initializeTranslation(x, y, z, new Matrix4x4f());
    }

    /**
     * Builds a new translation matrix.
     * @param vector The translation vector
     * @return The resulting matrix
     */
    public static Matrix4x4f createTranslation(Vector3f vector) {
        return initializeTranslation(vector.getX(), vector.getY(), vector.getZ(), new Matrix4x4f());
    }
    
    /**
     * Initialises an orthographic projection matrix.
     * @param width The width of the projection volume
     * @param height The height of the projection volume
     * @param zNear The near edge of the projection volume
     * @param zFar The far edge of the projection volume
     * @param result The resulting Matrix4 instance.
     * @return The resulting Matrix4 instance.
     */
    public static Matrix4x4f initialiseOrthographic(float width, float height, float zNear, float zFar, Matrix4x4f result) {
        return initialiseOrthographicOffCenter(-width / 2, width / 2, -height / 2, height / 2, zNear, zFar, result);
    }

    /**
     * Creates an orthographic projection matrix.
     * @param width The width of the projection volume
     * @param height The height of the projection volume
     * @param zNear The near edge of the projection volume
     * @param zFar The far edge of the projection volume
     * @return The resulting Matrix4 instance.
     */
    public static Matrix4x4f createOrthographic(float width, float height, float zNear, float zFar) {
        return initialiseOrthographicOffCenter(-width / 2, width / 2, -height / 2, height / 2, zNear, zFar, null);
    }

    /**
     * Initialises an orthographic projection matrix.
     * @param left The left edge of the projection volume
     * @param right The right edge of the projection volume
     * @param bottom The bottom edge of the projection volume
     * @param top The top edge of the projection volume
     * @param zNear The near edge of the projection volume
     * @param zFar The far edge of the projection volume
     * @param result The resulting Matrix4 instance.
     * @return The resulting Matrix4 instance.
     */
    public static Matrix4x4f initialiseOrthographicOffCenter(float left, float right, float bottom, float top, float zNear, float zFar, Matrix4x4f result) {
        float invRL = 1 / (right - left);
        float invTB = 1 / (top - bottom);
        float invFN = 1 / (zFar - zNear);
        
        if (result == null)
            result = new Matrix4x4f();

        result.internalMatrix[0][0] = 2 * invRL; // M11
        result.internalMatrix[1][1] = 2 * invTB; // M22
        result.internalMatrix[2][2] = -2 * invFN; // M33

        result.internalMatrix[3][0] = -(right + left) * invRL; // M41
        result.internalMatrix[3][1] = -(top + bottom) * invTB; // M42
        result.internalMatrix[3][2] = -(zFar + zNear) * invFN; // M43
        result.internalMatrix[3][3] = 1; // M44
        
        return result;
    }
    
    /**
     * Creates an orthographic projection matrix.
     * @param left The left edge of the projection volume
     * @param right The right edge of the projection volume
     * @param bottom The bottom edge of the projection volume
     * @param top The top edge of the projection volume
     * @param zNear The near edge of the projection volume
     * @param zFar The far edge of the projection volume
     * @return The resulting Matrix4 instance.
     */
    public static Matrix4x4f createOrthographicOffCenter(float left, float right, float bottom, float top, float zNear, float zFar) {
        return initialiseOrthographicOffCenter(left, right, bottom, top, zNear, zFar, new Matrix4x4f());
    }

    /**
     * Initialises a perspective projection matrix.
     * @param fovY Angle of the field of view in the y direction (in radians)
     * @param aspect Aspect ratio of the view (width / height)
     * @param zNear Distance to the near clip plane
     * @param zFar Distance to the far clip plane
     * @param result A projection matrix that transforms camera space to raster space
     * @return A projection matrix that transforms camera space to raster space
     */
    public static Matrix4x4f initialisePerspectiveFieldOfView(float fovY, float aspect, float zNear, float zFar, Matrix4x4f result) {
        if (fovY <= 0 || fovY > Math.PI)
            throw new IllegalArgumentException("fovY is outside of the valid range!");
        if (aspect <= 0)
            throw new IllegalArgumentException("the aspect ratio cannot be less than or equal to zero!");
        if (zNear <= 0)
            throw new IllegalArgumentException("zNear cannot be less than or equal to zero!");
        if (zFar <= 0)
            throw new IllegalArgumentException("zFar cannot be less than or equal to zero!");
        
        float yMax = zNear * (float) Math.tan(0.5f * fovY);
        float yMin = -yMax;
        float xMin = yMin * aspect;
        float xMax = yMax * aspect;
        
        return initialisePerspectiveOffCenter(xMin, xMax, yMin, yMax, zNear, zFar, result);
    }

    /**
     * Creates a new perspective projection matrix.
     * @param fovY Angle of the field of view in the y direction (in radians)
     * @param aspect Aspect ratio of the view (width / height)
     * @param zNear Distance to the near clip plane
     * @param zFar Distance to the far clip plane
     * @return A projection matrix that transforms camera space to raster space
     */
    public static Matrix4x4f createPerspectiveFieldOfView(float fovY, float aspect, float zNear, float zFar) {
        return initialisePerspectiveFieldOfView(fovY, aspect, zNear, zFar, null);
    }

    /**
     * Initialises a perspective projection matrix.
     * @param left Left edge of the view frustum
     * @param right Right edge of the view frustum
     * @param bottom Bottom edge of the view frustum
     * @param top Top edge of the view frustum
     * @param zNear Distance to the near clip plane
     * @param zFar Distance to the far clip plane
     * @param result A projection matrix that transforms camera space to raster space
     */
    public static Matrix4x4f initialisePerspectiveOffCenter(float left, float right, float bottom, float top, float zNear, float zFar, Matrix4x4f result) {
        if (zNear <= 0)
            throw new IllegalArgumentException("zNear cannot be less than or equal to zero!");
        if (zFar <= 0)
            throw new IllegalArgumentException("zFar cannot be less than or equal to zero!");
        if (zNear >= zFar)
            throw new IllegalArgumentException("zNear cannot match or exceed zFar!");

        float x = (2.0f * zNear) / (right - left);
        float y = (2.0f * zNear) / (top - bottom);
        float a = (right + left) / (right - left);
        float b = (top + bottom) / (top - bottom);
        float c = -(zFar + zNear) / (zFar - zNear);
        float d = -(2.0f * zFar * zNear) / (zFar - zNear);

        if (result == null)
            result = new Matrix4x4f();
        
        result.internalMatrix[0][0] = x;
        result.internalMatrix[0][1] = 0;
        result.internalMatrix[0][2] = 0;
        result.internalMatrix[0][3] = 0;
        result.internalMatrix[1][0] = 0;
        result.internalMatrix[1][1] = y;
        result.internalMatrix[1][2] = 0;
        result.internalMatrix[1][3] = 0;
        result.internalMatrix[2][0] = a;
        result.internalMatrix[2][1] = b;
        result.internalMatrix[2][2] = c;
        result.internalMatrix[2][3] = -1;
        result.internalMatrix[3][0] = 0;
        result.internalMatrix[3][1] = 0;
        result.internalMatrix[3][2] = d;
        result.internalMatrix[3][3] = 0;
        
        return result;
    }

    /**
     * Creates a new perspective projection matrix.
     * @param left Left edge of the view frustum
     * @param right Right edge of the view frustum
     * @param bottom Bottom edge of the view frustum
     * @param top Top edge of the view frustum
     * @param zNear Distance to the near clip plane
     * @param zFar Distance to the far clip plane
     */
    public static Matrix4x4f createPerspectiveOffCenter(float left, float right, float bottom, float top, float zNear, float zFar) {
        return initialisePerspectiveOffCenter(left, right, bottom, top, zNear, zFar, null);
    }
    
    /**
     * Setup a scale matrix.
     * Based on kcMatrixScale() from Frogger: The Great Quest.
     * @param result The matrix to initialise as a scale matrix.
     * @param scaleX Single scale factor for x-axis
     * @param scaleY Single scale factor for y-axis
     * @param scaleZ Single scale factor for z-axis
     * @return The resulting scale matrix
     */
    public static Matrix4x4f initialiseScaleMatrix(Matrix4x4f result, float scaleX, float scaleY, float scaleZ) {
        if (result == null)
            result = new Matrix4x4f();

        result = result.setIdentity();
        result.internalMatrix[0][0] = scaleX;
        result.internalMatrix[1][1] = scaleY;
        result.internalMatrix[2][2] = scaleZ;
        // [3][3] is already 1 because it was set to be an identity matrix.
        return result;
    }

    /**
     * Build a new scale matrix.
     * @param scaleX Single scale factor for x-axis
     * @param scaleY Single scale factor for y-axis
     * @param scaleZ Single scale factor for z-axis
     * @return The resulting scale matrix
     */
    public static Matrix4x4f createScaleMatrix(float scaleX, float scaleY, float scaleZ) {
        return initialiseScaleMatrix(new Matrix4x4f(), scaleX, scaleY, scaleZ);
    }

    /**
     * Build a scaling matrix.
     * @param result The matrix to initialise as a scale matrix.
     * @param scale Single scale factor for x,y and z axes
     * @return The resulting scale matrix
     */
    public static Matrix4x4f initialiseScaleMatrix(Matrix4x4f result, float scale) {
        return initialiseScaleMatrix(result, scale, scale, scale);
    }

    /**
     * Build a new scale matrix.
     * @param scale Single scale factor for x,y and z axes
     * @return The resulting scale matrix
     */
    public static Matrix4x4f createScaleMatrix(float scale) {
        return initialiseScaleMatrix(new Matrix4x4f(), scale, scale, scale);
    }

    /**
     * Initialise a scaling matrix.
     * @param result The matrix to initialise as a scale matrix.
     * @param scale Single scale factor for x,y and z axes
     * @return The resulting scale matrix
     */
    public static Matrix4x4f initialiseScaleMatrix(Matrix4x4f result, Vector3f scale) {
        return initialiseScaleMatrix(result, scale.getX(), scale.getY(), scale.getZ());
    }

    /**
     * Build a new scale matrix.
     * @param scale Vector containing single scale factors for each axis.
     * @return The resulting scale matrix
     */
    public static Matrix4x4f createScaleMatrix(Vector3f scale) {
        return initialiseScaleMatrix(new Matrix4x4f(), scale.getX(), scale.getY(), scale.getZ());
    }

    /**
     * Initialise a world space to camera space matrix.
     * @param eye Eye (camera) position in world space
     * @param target Target position in world space
     * @param up Up vector in world space (should not be parallel to the camera direction, that is target - eye)
     * @param result The matrix to initialise as a lookAt matrix.
     * @return A Matrix4 that transforms world space to camera space
     */
    public static Matrix4x4f initialiseLookAtMatrix(Vector3f eye, Vector3f target, Vector3f up, Matrix4x4f result) {
        Vector3f z = target.clone().subtract(eye).normalise(); // (eye - target) = right-handed matrix.
        Vector3f x = up.crossProduct(z).normalise();
        Vector3f y = z.crossProduct(x).normalise();

        if (result == null)
            result = new Matrix4x4f();

        result.internalMatrix[0][0] = x.getX();
        result.internalMatrix[0][1] = y.getX();
        result.internalMatrix[0][2] = z.getX();
        result.internalMatrix[0][3] = 0;
        result.internalMatrix[1][0] = x.getY();
        result.internalMatrix[1][1] = y.getY();
        result.internalMatrix[1][2] = z.getY();
        result.internalMatrix[1][3] = 0;
        result.internalMatrix[2][0] = x.getZ();
        result.internalMatrix[2][1] = y.getZ();
        result.internalMatrix[2][2] = z.getZ();
        result.internalMatrix[2][3] = 0;
        result.internalMatrix[3][0] = 0;
        result.internalMatrix[3][1] = 0;
        result.internalMatrix[3][2] = 0;
        result.internalMatrix[3][3] = 1;

        // Apply the translation.
        eye = eye.negate();
        result = result.multiply(createTranslation(eye));
        eye.negate();

        return result;
    }

    /**
     * Initialise a world space to camera space matrix.
     * @param eye Eye (camera) position in world space
     * @param target Target position in world space
     * @param up Up vector in world space (should not be parallel to the camera direction, that is target - eye)
     * @return A Matrix4 that transforms world space to camera space
     */
    public static Matrix4x4f createLookAtMatrix(Vector3f eye, Vector3f target, Vector3f up) {
        return initialiseLookAtMatrix(eye, target, up, new Matrix4x4f());
    }

    /**
     * Build a world space to camera space matrix
     * @param eyeX Eye (camera) position in world space
     * @param eyeY Eye (camera) position in world space
     * @param eyeZ Eye (camera) position in world space
     * @param targetX Target position in world space
     * @param targetY Target position in world space
     * @param targetZ Target position in world space
     * @param upX Up vector in world space (should not be parallel to the camera direction, that is target - eye)
     * @param upY Up vector in world space (should not be parallel to the camera direction, that is target - eye)
     * @param upZ Up vector in world space (should not be parallel to the camera direction, that is target - eye)
     * @return A Matrix4 that transforms world space to camera space
     */
    public static Matrix4x4f createLookAtMatrix(float eyeX, float eyeY, float eyeZ, float targetX, float targetY, float targetZ, float upX, float upY, float upZ) {
        return createLookAtMatrix(new Vector3f(eyeX, eyeY, eyeZ), new Vector3f(targetX, targetY, targetZ), new Vector3f(upX, upY, upZ));
    }

    /**
     * Linearly interpolates between the corresponding values of two matrices.
     * Copied from .NET's System.Numerics.Matrix4x4.Lerp.
     * @param matrix1 The first source matrix.
     * @param matrix2 The second source matrix.
     * @param amount The relative weight of the second source matrix. Between 0 and 1
     * @param outputMatrix The matrix to store the interpolated result within
     * @return the resulting interpolated matrix
     */
    public static Matrix4x4f lerp(Matrix4x4f matrix1, Matrix4x4f matrix2, float amount, Matrix4x4f outputMatrix) {
        if (outputMatrix == null)
            outputMatrix = new Matrix4x4f();

        // First row
        outputMatrix.internalMatrix[0][0] = matrix1.internalMatrix[0][0] + (matrix2.internalMatrix[0][0] - matrix1.internalMatrix[0][0]) * amount;
        outputMatrix.internalMatrix[0][1] = matrix1.internalMatrix[0][1] + (matrix2.internalMatrix[0][1] - matrix1.internalMatrix[0][1]) * amount;
        outputMatrix.internalMatrix[0][2] = matrix1.internalMatrix[0][2] + (matrix2.internalMatrix[0][2] - matrix1.internalMatrix[0][2]) * amount;
        outputMatrix.internalMatrix[0][3] = matrix1.internalMatrix[0][3] + (matrix2.internalMatrix[0][3] - matrix1.internalMatrix[0][3]) * amount;

        // Second row
        outputMatrix.internalMatrix[1][0] = matrix1.internalMatrix[1][0] + (matrix2.internalMatrix[1][0] - matrix1.internalMatrix[1][0]) * amount;
        outputMatrix.internalMatrix[1][1] = matrix1.internalMatrix[1][1] + (matrix2.internalMatrix[1][1] - matrix1.internalMatrix[1][1]) * amount;
        outputMatrix.internalMatrix[1][2] = matrix1.internalMatrix[1][2] + (matrix2.internalMatrix[1][2] - matrix1.internalMatrix[1][2]) * amount;
        outputMatrix.internalMatrix[1][3] = matrix1.internalMatrix[1][3] + (matrix2.internalMatrix[1][3] - matrix1.internalMatrix[1][3]) * amount;

        // Third row
        outputMatrix.internalMatrix[2][0] = matrix1.internalMatrix[2][0] + (matrix2.internalMatrix[2][0] - matrix1.internalMatrix[2][0]) * amount;
        outputMatrix.internalMatrix[2][1] = matrix1.internalMatrix[2][1] + (matrix2.internalMatrix[2][1] - matrix1.internalMatrix[2][1]) * amount;
        outputMatrix.internalMatrix[2][2] = matrix1.internalMatrix[2][2] + (matrix2.internalMatrix[2][2] - matrix1.internalMatrix[2][2]) * amount;
        outputMatrix.internalMatrix[2][3] = matrix1.internalMatrix[2][3] + (matrix2.internalMatrix[2][3] - matrix1.internalMatrix[2][3]) * amount;

        // Fourth row
        outputMatrix.internalMatrix[3][0] = matrix1.internalMatrix[3][0] + (matrix2.internalMatrix[3][0] - matrix1.internalMatrix[3][0]) * amount;
        outputMatrix.internalMatrix[3][1] = matrix1.internalMatrix[3][1] + (matrix2.internalMatrix[3][1] - matrix1.internalMatrix[3][1]) * amount;
        outputMatrix.internalMatrix[3][2] = matrix1.internalMatrix[3][2] + (matrix2.internalMatrix[3][2] - matrix1.internalMatrix[3][2]) * amount;
        outputMatrix.internalMatrix[3][3] = matrix1.internalMatrix[3][3] + (matrix2.internalMatrix[3][3] - matrix1.internalMatrix[3][3]) * amount;
        return outputMatrix;
    }

    /**
     * Linearly interpolates between the corresponding values of two matrices.
     * Copied from .NET's System.Numerics.Matrix4x4.Lerp.
     * @param matrix1 The first source matrix.
     * @param matrix2 The second source matrix.
     * @param amount The relative weight of the second source matrix. Between 0 and 1
     * @return the resulting interpolated matrix
     */
    public static Matrix4x4f lerp(Matrix4x4f matrix1, Matrix4x4f matrix2, float amount) {
        return lerp(matrix1, matrix2, amount, new Matrix4x4f());
    }
}