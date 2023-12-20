package net.highwayfrogs.editor.file.mof.animation.transform;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;

/**
 * Represents a basic mof transform data holder.
 * The child classes should implement hashCode() and equals().
 * Created by Kneesnap on 1/5/2019.
 */
public abstract class TransformObject extends GameObject {

    /**
     * Get transform data.
     * @return transformData
     */
    public abstract short[] getTransform();

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
