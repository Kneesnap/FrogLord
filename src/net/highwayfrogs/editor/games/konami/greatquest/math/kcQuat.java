package net.highwayfrogs.editor.games.konami.greatquest.math;

import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter;
import net.highwayfrogs.editor.system.math.Quaternion;
import net.highwayfrogs.editor.system.math.Vector4f;

/**
 * Represents the 'kcQuat' struct.
 * Created by Kneesnap on 10/2/2024.
 */
public class kcQuat extends kcVector4 implements IInfoWriter {
    public kcQuat() {
        super();
    }

    public kcQuat(float x, float y, float z, float w) {
        super(x, y, z, w);
    }

    public kcQuat(Vector4f vec) {
        super(vec);
    }

    /**
     * Implementation of kcQuatMul() from Frogger: The Great Quest.
     * @param q1 The first quaternion to multiply
     * @param q2 The second quaternion to multiply
     * @param result The resulting quaternion storage object
     * @return resulting quaternion
     */
    public static Vector4f kcQuatMul(Vector4f q1, Vector4f q2, Vector4f result) {
        return Quaternion.multiply(q1, q2, result);
    }

    /**
     * Perform Spherical linear interpolation between two quaternions based on the kcQuatSlerp function
     * @param q1 The first quaternion
     * @param q2 The second quaternion
     * @param blend The blend factor between 0 and 1
     * @return A smooth blend between the given quaternions
     */
    public static Vector4f kcQuatSlerp(Vector4f q1, Vector4f q2, float blend, Vector4f result) {
        if (result == null)
            result = new Vector4f();

        // Dot product!
        boolean negateQ2 = false;
        double halfAngle = Vector4f.dotProduct(q1, q2);
        if(halfAngle < 0f) {
            halfAngle = -halfAngle;
            negateQ2 = true;
            q2.negate();
        }

        if (halfAngle < 0.999999f && halfAngle > .000001f) { // The > .000001f has been added by me. It should avoid creating NaN results. This however isn't in the real game. For PS2, it makes sense since the PS2 can't represent NaN, but the PC version also doesn't do this. I wonder if the divide by zero behavior on PC is something unexpected.
            // do proper slerp for big angles
            float cosHalfAngle = (float) Math.acos(halfAngle);
            float sinHalfAngle = (float) Math.sin(halfAngle);
            float oneOverSinHalfAngle = 1.0f / sinHalfAngle; // The PS2
            float blendA = (float) Math.sin(cosHalfAngle - cosHalfAngle * blend) * oneOverSinHalfAngle;
            float blendB = (float) Math.sin(cosHalfAngle * blend) * oneOverSinHalfAngle;

            result.setX(blendA * q1.getX() + blendB * q2.getX());
            result.setY(blendA * q1.getY() + blendB * q2.getY());
            result.setZ(blendA * q1.getZ() + blendB * q2.getZ());
            result.setW(blendA * q1.getW() + blendB * q2.getW());
        } else {
            Vector4f.lerp(q1, q2, blend, result);
        }

        if (negateQ2)
            q2.negate(); // Undo change to quaternion.

        return result;
    }
}
