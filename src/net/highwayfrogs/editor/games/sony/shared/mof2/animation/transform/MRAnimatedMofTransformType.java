package net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;

import java.util.function.Supplier;

/**
 * Represents animated MOF transform data types.
 * Created by Kneesnap on 1/5/2019.
 */
@RequiredArgsConstructor
public enum MRAnimatedMofTransformType {
    NORMAL((byte) 0x30, MRAnimatedMofTransformMatrix::new), // '0'
    BYTE((byte) 0x31, MRAnimatedMofTransformMatrixByte::new), // '1'
    QUAT_BYTE((byte) 0x32, MRAnimatedMofTransformQuatByte::new), // '2'
    QUAT((byte) 0x33, MRAnimatedMofTransformQuat::new), // '3'
    QUAT_SCALE_BYTE((byte) 0x34, MRAnimatedMofTransformScaleByte::new), // '4'
    QUAT_SCALE((byte) 0x35, MRAnimatedMofTransformQuatScale::new); // '5'

    @Getter private final byte opcode;
    private final Supplier<MRAnimatedMofTransform> maker;

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