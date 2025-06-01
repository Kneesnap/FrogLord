package net.highwayfrogs.editor.games.sony.shared;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a texture UV pair stored as bytes in a Sony Cambridge game.
 * Created by Kneesnap on 12/9/2023.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SCByteTextureUV implements IBinarySerializable {
    private byte u;
    private byte v;

    public static final int BYTE_SIZE = 2 * Constants.BYTE_SIZE;

    public SCByteTextureUV(float u, float v) {
        setFloatU(u);
        setFloatV(v);
    }

    @Override
    public void load(DataReader reader) {
        this.u = reader.readByte();
        this.v = reader.readByte();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte(this.u);
        writer.writeByte(this.v);
    }

    @Override
    public int hashCode() {
        return ((this.u & 0xFF) << 8) | (this.v & 0xFF);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SCByteTextureUV))
            return false;

        SCByteTextureUV other = (SCByteTextureUV) object;
        return this.u == other.u && this.v == other.v;
    }

    /**
     * Set UV values as floats ranging from 0 to 1.
     * @param u the float value to apply
     * @param v the float value to apply
     */
    public void setFloatUV(float u, float v) {
        this.u = DataUtils.floatToByte(u);
        this.v = DataUtils.floatToByte(v);
    }

    /**
     * Set UV values as snapped floats ranging from 0 to 1.
     * @param u the float value to apply
     * @param v the float value to apply
     */
    public void setSnappedFloatUV(GameImage image, float u, float v) {
        setSnappedFloatU(image, u);
        setSnappedFloatV(image, v);
    }

    /**
     * Get U as a float ranging from 0 to 1.
     * @return floatU
     */
    public float getFloatU() {
        return DataUtils.unsignedByteToFloat(this.u);
    }

    /**
     * Get U as a float ranging from 0 to 1.
     * This value is snapped to the nearest pixel, just as seen in both MR API MOF setup code and Frogger map rendering code.
     * In other words, this should yield a more accurate UV value than getFloatV() in most situations.
     * @return floatU
     */
    public float getSnappedFloatU(GameImage image) {
        if (image == null)
            return getFloatU();

        return getSnappedFloat(this.u, image.getIngameWidth());
    }

    /**
     * Set U as a float ranging from 0 to 1.
     * @param value the float value to apply
     */
    public void setFloatU(float value) {
        this.u = DataUtils.floatToByte(value);
    }

    /**
     * Set U as a float ranging from 0 to 1.
     * This value is snapped to the nearest pixel, just as seen in both MR API MOF setup code and Frogger map rendering code.
     * In other words, this should never change the underlying 'u' value when setSnappedFloatU(image, getSnappedFloatU(image)) is called.
     * @param image the image to use to snap the value
     * @param newU the float value to apply
     */
    public void setSnappedFloatU(GameImage image, float newU) {
        if (image != null) {
            this.u = calculateByteForSnappedFloat(newU, image.getIngameWidth());
        } else {
            setFloatU(newU);
        }
    }

    /**
     * Get V as a float ranging from 0 to 1.
     * @return floatV
     */
    public float getFloatV() {
        return DataUtils.unsignedByteToFloat(this.v);
    }

    /**
     * Get V as a float ranging from 0 to 1.
     * This value is snapped to the nearest pixel, just as seen in both MR API MOF setup code and Frogger map rendering code.
     * In other words, this should yield a more accurate UV value than getFloatV() in most situations.
     * @return floatV
     */
    public float getSnappedFloatV(GameImage image) {
        if (image == null)
            return getFloatV();

        return getSnappedFloat(this.v, image.getIngameHeight());
    }

    /**
     * Set V as a float ranging from 0 to 1.
     * @param value the float value to apply
     */
    public void setFloatV(float value) {
        this.v = DataUtils.floatToByte(value);
    }

    /**
     * Set V as a float ranging from 0 to 1.
     * This value is snapped to the nearest pixel, just as seen in both MR API MOF setup code and Frogger map rendering code.
     * In other words, this should never change the underlying 'v' value when setSnappedFloatV(image, getSnappedFloatV(image)) is called.
     * @param image the image to use to snap the value
     * @param newV the float value to apply
     */
    public void setSnappedFloatV(GameImage image, float newV) {
        if (image != null) {
            this.v = calculateByteForSnappedFloat(newV, image.getIngameHeight());
        } else {
            setFloatV(newV);
        }
    }

    /**
     * Apply uv coordinates to a vector.
     * @param instance The vector to save float values to.
     * @return floatVector
     */
    public Vector2f toVector(Vector2f instance) {
        instance.setX(getFloatU());
        instance.setY(getFloatV());
        return instance;
    }

    /**
     * Apply uv coordinates to a vector.
     * @param instance The vector to save float values to.
     * @return floatVector
     */
    public Vector2f toSnappedVector(GameImage image, Vector2f instance) {
        if (image == null)
            return toVector(instance);

        instance.setX(getSnappedFloatU(image));
        instance.setY(getSnappedFloatV(image));
        return instance;
    }

    /**
     * Adds uv coordinate offsets to this vector
     * @param uOffset the u offset to apply
     * @param vOffset the v offset to apply
     */
    public void add(float uOffset, float vOffset) {
        setFloatU(Math.max(0F, Math.min(1F, getFloatU() + uOffset)));
        setFloatV(Math.max(0F, Math.min(1F, getFloatV() + vOffset)));
    }

    /**
     * Copy the data of another UV to this one.
     * @param other the uv to assume
     */
    public void copyFrom(SCByteTextureUV other) {
        if (other == null)
            throw new NullPointerException("other");

        this.u = other.u;
        this.v = other.v;
    }

    /**
     * Creates a new ByteUV object which has the same properties as this one.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public SCByteTextureUV clone() {
        return new SCByteTextureUV(this.u, this.v);
    }

    /**
     * Get this as an OBJ vt command.
     * @return objTextureString
     */
    public String toObjTextureString() {
        return "vt " + getFloatU() + " " + getFloatV();
    }

    @Override
    public String toString() {
        return String.format("SCByteTextureUV<u=%02X,v=%02X>", this.u, this.v);
    }

    /**
     * Setup an editor.
     * @param editor The editor to setup under.
     */
    public void setupEditor(String label, GUIEditorGrid editor, Runnable onUpdate) {
        editor.addTextField(label, getFloatU() + ", " + getFloatV(), newStr -> {
            String[] split = newStr.split(",\\s*");
            if (split.length != 2)
                return false;

            float u;
            float v;

            // Parse numbers.
            try {
                u = Float.parseFloat(split[0]);
                v = Float.parseFloat(split[1]);
            } catch (NumberFormatException nfe) {
                return false;
            }

            // Ensure the values we got are within an acceptable range.
            if (!Float.isFinite(u) || !Float.isFinite(v) || (u < 0F) || (u > 1F) || (v < 0F) || (v > 1F))
                return false;

            setFloatU(u);
            setFloatV(v);
            if (onUpdate != null)
                onUpdate.run();
            return true;
        });
    }

    private static float getSnappedFloat(byte value, int imageLength) {
        int newValue = ((value & 0xFF) * imageLength) / 255; // MR_STAT.C/MRStaticResolveMOFTextures
        return (float) newValue / imageLength;
    }

    private static byte calculateByteForSnappedFloat(float value, int imageLength) {
        if (value < 0)
            value = 0;
        if (value > 1)
            value = 1;

        // We round at the part that we do, since the actual game has a whole number at that exact moment.
        // This allows us to ensure that when we do setSnappedFloatV(image, getSnappedFloatV(image)), it will apply the correct value for certain.
        byte testValue = (byte) Math.max(0, Math.min(0xFF, (0xFF * Math.round(value * imageLength)) / imageLength));
        float testFloat = getSnappedFloat(testValue, imageLength);

        // Increase the value until we have the best match.
        while (0xFF > (testValue & 0xFF)) {
            byte nextValue = (byte) ((testValue & 0xFF) + 1);
            float nextFloat = getSnappedFloat(nextValue, imageLength);
            if (Math.abs(nextFloat - value) > Math.abs(testFloat - value))
                break; // If increasing the value would make the uv float value become less accurate, stop!

            testValue = nextValue;
            testFloat = nextFloat;
        }

        return testValue;
    }
}