package net.highwayfrogs.editor.games.sony.shared;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.DataUtils;

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
        this.u = floatToByte(u);
        this.v = floatToByte(v);
    }

    /**
     * Get U as a float ranging from 0 to 1.
     * @return floatU
     */
    public float getFloatU() {
        return DataUtils.unsignedByteToFloat(this.u);
    }

    /**
     * Set U as a float ranging from 0 to 1.
     * @param value the float value to apply
     */
    public void setFloatU(float value) {
        this.u = floatToByte(value);
    }

    /**
     * Get V as a float ranging from 0 to 1.
     * @return floatV
     */
    public float getFloatV() {
        return DataUtils.unsignedByteToFloat(this.v);
    }

    /**
     * Set V as a float ranging from 0 to 1.
     * @param value the float value to apply
     */
    public void setFloatV(float value) {
        this.v = floatToByte(value);
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

    /**
     * Converts a floating point value between 0 and 1 to a value between 0 and 255, stored in a signed byte.
     * @param floatValue The value to convert to a byte.
     * @return byteValue
     */
    private static byte floatToByte(float floatValue) {
        short small = (short) Math.round(floatValue * 0xFF);
        return small >= 128 ? ((byte) (small - 256)) : (byte) small;
    }
}