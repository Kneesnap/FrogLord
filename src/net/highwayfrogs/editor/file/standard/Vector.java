package net.highwayfrogs.editor.file.standard;

/**
 * Represents a general vector.
 * Created by Kneesnap on 2/21/2019.
 */
public interface Vector {
    public float getFloatX();

    public float getFloatY();

    public float getFloatZ();

    /**
     * Get this vector as a Wavefront-OBJ vertex command.
     * @return vertexCommandString
     */
    default String toOBJString() {
        return "v " + -getFloatX() + " " + -getFloatY() + " " + getFloatZ();
    }

    /**
     * Get a decimal coordinate string for this float vector.
     * @return coordinateString
     */
    default String toFloatString() {
        return getFloatX() + ", " + getFloatY() + ", " + getFloatZ();
    }

    public String toRegularString();

    default String toString0() {
        return getClass().getSimpleName() + "<" + toFloatString() + ">";
    }
}
