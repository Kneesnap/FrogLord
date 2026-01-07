package net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.psx.PSXMatrix;
import net.highwayfrogs.editor.utils.lambda.Function3;

import java.util.function.Supplier;

/**
 * Represents animated MOF transform data types.
 * Created by Kneesnap on 1/5/2019.
 */
@RequiredArgsConstructor
public enum MRAnimatedMofTransformType {
    NORMAL((byte) 0x30, MRAnimatedMofTransformMatrix::new, null), // '0'
    BYTE((byte) 0x31, MRAnimatedMofTransformMatrixByte::new, null), // '1'
    QUAT_BYTE((byte) 0x32, MRAnimatedMofTransformQuatByte::new, MRAnimatedMofTransformQuatByte::interpolate), // '2'
    QUAT((byte) 0x33, MRAnimatedMofTransformQuat::new, MRAnimatedMofTransformQuat::interpolate), // '3'
    QUAT_SCALE_BYTE((byte) 0x34, MRAnimatedMofTransformQuatScaleByte::new, MRAnimatedMofTransformQuatScaleByte::interpolate), // '4'
    QUAT_SCALE((byte) 0x35, MRAnimatedMofTransformQuatScale::new, MRAnimatedMofTransformQuatScale::interpolate); // '5'

    @Getter private final byte opcode;
    private final Supplier<MRAnimatedMofTransform> maker;
    private final Function3<MRAnimatedMofTransform, MRAnimatedMofTransform, Integer, PSXMatrix> interpolator;

    /**
     * Make a transform of this type.
     * @return mofTransform
     */
    public MRAnimatedMofTransform makeTransform() {
        if (this.maker == null) // The null ones are not used by the retail game files.
            throw new RuntimeException("FrogLord does not have a transform parser for " + name() + ".");
        return this.maker.get();
    }

    /**
     * Make a transform of this type, loading data from a rotation matrix.
     * @return mofTransform
     */
    public MRAnimatedMofTransform makeTransform(PSXMatrix rotMatrix) {
        MRAnimatedMofTransform newObject = makeTransform();
        newObject.fromMatrix(rotMatrix);
        return newObject;
    }

    /**
     * Interpolates between thee two transforms
     * @param previousTransform the previous transform
     * @param nextTransform the next transform
     * @param t the time factor
     * @return interpolatedMatrix
     */
    public PSXMatrix interpolate(MRAnimatedMofTransform previousTransform, MRAnimatedMofTransform nextTransform, int t) {
        if (this.interpolator == null)
            throw new UnsupportedOperationException("The transform type " + this  + " does not support interpolation!");

        return this.interpolator.apply(previousTransform, nextTransform, t);
    }

    /**
     * Return the TransformType based on a byte opcode.
     * @param opcode The byte value to get.
     * @return type
     */
    public static MRAnimatedMofTransformType getTypeFromOpcode(byte opcode) {
        for (MRAnimatedMofTransformType type : values())
            if (type.opcode == opcode)
                return type;

        throw new RuntimeException("Unknown MRAnimatedMofTransformType for byte-opcode: " + opcode);
    }
}