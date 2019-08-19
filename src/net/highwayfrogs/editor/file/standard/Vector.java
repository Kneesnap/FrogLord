package net.highwayfrogs.editor.file.standard;

/**
 * Represents a general vector.
 * Created by Kneesnap on 2/21/2019.
 */
public interface Vector {
    default float getFloatX() {
        return getFloatX(defaultBits());
    }

    default float getFloatY() {
        return getFloatY(defaultBits());
    }

    default float getFloatZ() {
        return getFloatZ(defaultBits());
    }

    public float getFloatX(int bits);

    public float getFloatY(int bits);

    public float getFloatZ(int bits);

    default void setFloatX(float xVal) {
        setFloatX(xVal, defaultBits());
    }

    default void setFloatY(float yVal) {
        setFloatY(yVal, defaultBits());
    }

    default void setFloatZ(float zVal) {
        setFloatZ(zVal, defaultBits());
    }

    public void setFloatX(float xVal, int bits);

    public void setFloatY(float yVal, int bits);

    public void setFloatZ(float zVal, int bits);

    default int defaultBits() {
        return 4;
    }

    default float getExportFloatX() {
        return -getFloatX();
    }

    default float getExportFloatY() {
        return -getFloatY();
    }

    default float getExportFloatZ() {
        return getFloatZ();
    }

    /**
     * Get this vector as a Wavefront-OBJ vertex command.
     * @return vertexCommandString
     */
    default String toOBJString() {
        return "v " + getExportFloatX() + " " + getExportFloatY() + " " + getExportFloatZ();
    }

    /**
     * Get a decimal coordinate string for this float vector.
     * @return coordinateString
     */
    default String toFloatString() {
        return getFloatX() + ", " + getFloatY() + ", " + getFloatZ();
    }

    default String toString0() {
        return getClass().getSimpleName() + "<" + toFloatString() + ">";
    }
}
