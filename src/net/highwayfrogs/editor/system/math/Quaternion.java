package net.highwayfrogs.editor.system.math;

/**
 * Implements Quaternion math operations which can be used upon Vector4f objects.
 * While we generally want to use a Quaternion object when working with quaternions, some games organize their data in a way which makes this difficult.
 * So the functionality in this class has also been exposed to allow it to be used statically on Vector4fs.
 * Created by Kneesnap on 10/2/2024.
 */
public class Quaternion {
    private Quaternion() {
        throw new UnsupportedOperationException("Cannot instantiate Quaternion object.");
    }

    /**
     * Make the provided quaternion into an identity quaternion
     * @param quaternion the quaterion to make identity.
     * @return the resulting quaternion
     */
    public static Vector4f setIdentity(Vector4f quaternion) {
        if (quaternion == null)
            quaternion = new Vector4f();

        return quaternion.setXYZW(0, 0, 0, 1F);
    }

    /**
     * Make a new identity quaternion.
     */
    public static Vector4f newIdentity() {
        return new Vector4f(0, 0, 0, 1F);
    }

    /**
     * Converts this instance to an axis-angle representation.
     * @return A vector4 whose xyz component is the axis, and the w component is the resulting angle.
     */
    public static Vector4f toAxisAngle(Vector4f quaternion, Vector4f result) {
        if (result == null)
            result = new Vector4f();

        Vector4f clone = quaternion.clone();
        if (Math.abs(clone.getW()) > 1.0f)
            clone.normalise();

        result.setW(2.0f * (float) Math.acos(clone.getW())); // angle
        float den = (float)Math.sqrt(1.0 - clone.getW() * clone.getW());
        if (den > 0.0001f) {
            result.setX(clone.getX() / den);
            result.setY(clone.getY() / den);
            result.setZ(clone.getZ() / den);
        } else {
            // This occurs when the angle is zero.
            // Not a problem: just set an arbitrary normalized axis.
            result.setX(1F);
            result.setY(0F);
            result.setZ(0F);
        }

        return result;
    }

    /**
     * Converts this instance to an axis-angle representation.
     * @param quaternion The quaternion to get the axis-angle representation from
     * @return A vector4 whose xyz component is the axis, and the w component is the resulting angle.
     */
    public static Vector4f toAxisAngle(Vector4f quaternion) {
        return toAxisAngle(quaternion, new Vector4f());
    }

    /**
     * Convert the current quaternion to Euler angle representation.
     * Copied from OpenTK.
     * @param quaternion The quaternion to get the Euler angles from
     * @param result the output storage for the Euler angles
     * @return The Euler angles in radians. (Pitch, Yaw, Roll)
     */
    public static Vector3f toEulerAngles(Vector4f quaternion, Vector3f result) {
        if (result == null)
            result = new Vector3f();

        /*
         reference
         http://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
         http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToEuler/
        */

        // Threshold for the singularities found at the north/south poles.
        final float singularityThreshold = 0.4999995f;

        float quatX = quaternion.getX();
        float quatY = quaternion.getY();
        float quatZ = quaternion.getZ();
        float quatW = quaternion.getW();

        float sqw = quatW * quatW;
        float sqx = quatX * quatX;
        float sqy = quatY * quatY;
        float sqz = quatZ * quatZ;
        float unit = sqx + sqy + sqz + sqw; // if normalised is one, otherwise is correction factor
        float singularityTest = (quatX * quatZ) + (quatW * quatY);

        if (singularityTest > singularityThreshold * unit) {
            result.setZ((float) (2 * Math.atan2(quatX, quatW)));
            result.setY((float) (Math.PI / 2));
            result.setX(0);
        } else if (singularityTest < -singularityThreshold * unit) {
            result.setZ((float) (-2 * Math.atan2(quatX, quatW)));
            result.setY((float) (-Math.PI / 2));
            result.setX(0F);
        } else {
            result.setZ((float) Math.atan2(2 * ((quatW * quatZ) - (quatX * quatY)), sqw + sqx - sqy - sqz));
            result.setY((float) Math.asin(2 * singularityTest / unit));
            result.setX((float) Math.atan2(2 * ((quatW * quatX) - (quatY * quatZ)), sqw - sqx - sqy + sqz));
        }

        return result;
    }

    /**
     * Convert the current quaternion to Euler angle representation.
     * Copied from OpenTK.
     * @param quaternion The quaternion to get the Euler angles from
     * @return The Euler angles in radians. (Pitch, Yaw, Roll)
     */
    public static Vector3f toEulerAngles(Vector4f quaternion) {
        return toEulerAngles(quaternion, new Vector3f());
    }

    /**
     * Build a quaternion from an axis and an angle.
     * Confirmed Behavior Matches: [Great Quest: kcAxisAngToQuat]
     * @param axis the axis to rotate about
     * @param angle the rotation angle in radians
     * @return newQuaternion
     */
    public static Vector4f fromAxisAngle(Vector3f axis, float angle) {
        Vector4f result = new Vector4f();
        if (axis.calculateLengthSquared() == 0.0f)
            return result;

        angle *= 0.5f;
        float sin = (float) Math.sin(angle);

        axis.normalise();
        result.setX(axis.getX() * sin);
        result.setY(axis.getY() * sin);
        result.setZ(axis.getZ() * sin);
        result.setW((float) Math.cos(angle));
        result.normalise();
        return result;
    }

    /**
     * Perform Spherical linear interpolation between two quaternions
     * Behavior is ported from MTF.
     * @param q1 The first quaternion
     * @param q2 The second quaternion
     * @param blend The blend factor between 0 and 1
     * @return A smooth blend between the given quaternions
     */
    public static Vector4f sLerp(Vector4f q1, Vector4f q2, float blend) {
        return sLerp(q1, q2, blend, null);
    }

    /**
     * Perform Spherical linear interpolation between two quaternions
     * Behavior is ported from MTF.
     * @param q1 The first quaternion
     * @param q2 The second quaternion
     * @param blend The blend factor between 0 and 1
     * @return A smooth blend between the given quaternions
     */
    public static Vector4f sLerp(Vector4f q1, Vector4f q2, float blend, Vector4f result) {
        // if either input is zero, return the other.
        if (q1.calculateLengthSquared() == 0.0f) {
            return q2.calculateLengthSquared() == 0.0f ? new Vector4f() : q2;
        } else if (q2.calculateLengthSquared() == 0.0f) {
            return q1;
        }

        // Dot product!
        double cosHalfAngle = Vector4f.dotProduct(q1, q2);
        if (cosHalfAngle >= 1.0f || cosHalfAngle <= -1.0f) {
            // angle = 0.0f, so just return one input.
            return q1;
        }

        boolean shouldNegate = (cosHalfAngle < 0.0f);
        if (shouldNegate)
            cosHalfAngle = -cosHalfAngle;

        float blendA;
        float blendB;
        if (cosHalfAngle < 0.99f) {
            // do proper slerp for big angles
            float halfAngle = (float) Math.acos(cosHalfAngle);
            float sinHalfAngle = (float) Math.sin(halfAngle);
            float oneOverSinHalfAngle = 1.0f / sinHalfAngle;
            blendA = (float) Math.sin(halfAngle * (1.0f - blend)) * oneOverSinHalfAngle;
            blendB = (float) Math.sin(halfAngle * blend) * oneOverSinHalfAngle;
        } else {
            // do lerp if angle is very small.
            blendA = 1.0f - blend;
            blendB = blend;
        }

        if (shouldNegate)
            blendB = -blendB;

        if (result == null)
            result = new Vector4f();

        result.setX(blendA * q1.x + blendB * q2.x);
        result.setY(blendA * q1.y + blendB * q2.y);
        result.setZ(blendA * q1.z + blendB * q2.z);
        result.setW(blendA * q1.w + blendB * q2.w);

        if (Float.isNaN(result.getW()) || result.calculateLengthSquared() <= 0.0f) {
            result.setXYZW(0, 0, 0, 1); // Become the identity matrix.
            return result;
        }

        result.normalise();
        return result;
    }

    /**
     * Inverts the provided quaternion in-place.
     * @param quaternion the quaternion to invert
     * @return The resulting inverted quaternion
     */
    public static Vector4f invert(Vector4f quaternion) {
        return invert(quaternion, quaternion);
    }

    /**
     * Inverts the provided quaternion.
     * @param quaternion the quaternion to invert
     * @param result The vector to store the inverted quaternion within
     * @return The resulting inverted quaternion
     */
    public static Vector4f invert(Vector4f quaternion, Vector4f result) {
        if (result == null)
            result = new Vector4f();

        double lengthSq = quaternion.calculateLengthSquared();
        if (lengthSq != 0.0) {
            float i = (float) (1.0f / lengthSq);
            result.setX(quaternion.getX() * -i);
            result.setY(quaternion.getY() * -i);
            result.setZ(quaternion.getZ() * -i);
            result.setW(quaternion.getW() * i);
        } else {
            result = result.setXYZW(quaternion);
        }

        return result;
    }

    /**
     * Multiplies two quaternions together, storing their results in the result quaternion.
     * This implementation was created by reverse engineering 'kcQuatMul' from Frogger: The Great Quest.
     * @param q1 the first quaternion
     * @param q2 the second quaternion
     * @param result the quaternion to store the results within
     * @return the quaternion product
     */
    public static Vector4f multiply(Vector4f q1, Vector4f q2, Vector4f result) {
        if (result == null)
            result = new Vector4f();

        float x1 = q1.getX(), x2 = q2.getX();
        float y1 = q1.getY(), y2 = q2.getY();
        float z1 = q1.getZ(), z2 = q2.getZ();
        float w1 = q1.getW(), w2 = q2.getW();

        result.setX((y2 * z1 + x2 * w1 + w2 * x1) - z2 * y1);
        result.setY((z2 * x1 + y2 * w1 + w2 * y1) - x2 * z1);
        result.setZ((x2 * y1 + z2 * w1 + w2 * z1) - y2 * x1);
        result.setW(((w2 * w1 - x2 * x1) - y2 * y1) - z2 * z1);
        return result;
    }

    /**
     * Multiplies two quaternions together
     * @param q1 the first quaternion
     * @param q2 the second quaternion
     * @return the quaternion product
     */
    public static Vector4f multiply(Vector4f q1, Vector4f q2) {
        return multiply(q1, q2, new Vector4f());
    }

    /**
     * Initializes a quaternion from given Euler angles in radians.
     * The rotations will get applied in following order:
     * 1. around X axis, 2. around Y axis, 3. around Z axis.
     * @param rotationX Counterclockwise rotation around X axis in radian.
     * @param rotationY Counterclockwise rotation around Y axis in radian.
     * @param rotationZ Counterclockwise rotation around Z axis in radian.
     */
    public static Vector4f setRotationAngles(Vector4f result, float rotationX, float rotationY, float rotationZ) {
        rotationX *= 0.5f;
        rotationY *= 0.5f;
        rotationZ *= 0.5f;

        double c1 = Math.cos(rotationX);
        double c2 = Math.cos(rotationY);
        double c3 = Math.cos(rotationZ);
        double s1 = Math.sin(rotationX);
        double s2 = Math.sin(rotationY);
        double s3 = Math.sin(rotationZ);

        if (result == null)
            result = new Vector4f();

        result.setW((float) ((c1 * c2 * c3) - (s1 * s2 * s3)));
        result.setX((float) ((s1 * c2 * c3) + (c1 * s2 * s3)));
        result.setY((float) ((c1 * s2 * c3) - (s1 * c2 * s3)));
        result.setZ((float) ((c1 * c2 * s3) + (s1 * s2 * c3)));
        return result;
    }

    /**
     * Creates a quaternion from given Euler angles in radians.
     * The rotations will get applied in following order:
     * 1. around X axis, 2. around Y axis, 3. around Z axis.
     * @param rotationX Counterclockwise rotation around X axis in radian.
     * @param rotationY Counterclockwise rotation around Y axis in radian.
     * @param rotationZ Counterclockwise rotation around Z axis in radian.
     */
    public static Vector4f createFromRotationAngles(float rotationX, float rotationY, float rotationZ) {
        return setRotationAngles(new Vector4f(), rotationX, rotationY, rotationZ);
    }
}
