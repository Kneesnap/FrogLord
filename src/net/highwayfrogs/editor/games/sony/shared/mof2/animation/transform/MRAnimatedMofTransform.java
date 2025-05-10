package net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform;

import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;

/**
 * Represents a basic mof transform data holder.
 * Created by Kneesnap on 1/5/2019.
 */
public abstract class MRAnimatedMofTransform implements IBinarySerializable {
    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object object);

    /**
     * Get translation data.
     * @return transformData
     */
    public abstract short[] getTranslation();

    /**
     * Loads transform data from a PSXMatrix. createMatrix, but in reverse.
     * @param matrix The matrix to load data from.
     */
    public abstract void fromMatrix(PSXMatrix matrix);

    /**
     * Creates a PSXMatrix with the animation data.
     * @return matrix
     */
    public abstract PSXMatrix createMatrix();

    /**
     * Creates an interpolated result.
     * @return interpolatedResult
     */
    public PSXMatrix createInterpolatedResult() {
        return createMatrix();
    }

    /**
     * Generates a PSXMatrix with the part transform.
     * @return matrix
     */
    public PSXMatrix calculatePartTransform(boolean useInterpolation) {
        return useInterpolation ? createInterpolatedResult() : createMatrix();
    }
}
