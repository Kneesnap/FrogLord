package net.highwayfrogs.editor.file.mof.animation.transform;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;

/**
 * Represents a basic mof transform data holder.
 * Created by Kneesnap on 1/5/2019.
 */
public abstract class TransformObject extends GameObject {

    /**
     * Get transform data.
     * @return transformData
     */
    public abstract short[] getTransform();

    /**
     * Creates a PSXMatrix with the animation data.
     * @return matrix
     */
    public abstract PSXMatrix createMatrix();
}
