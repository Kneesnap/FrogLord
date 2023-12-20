package net.highwayfrogs.editor.games.sony.shared;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.IBinarySerializable;
import net.highwayfrogs.editor.utils.Utils;

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
        this.u = Utils.floatToByte(u);
        this.v = Utils.floatToByte(v);
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
     * Get U as a float ranging from 0 to 1.
     * @return floatU
     */
    public float getFloatU() {
        return Utils.unsignedByteToFloat(this.u);
    }

    /**
     * Get V as a float ranging from 0 to 1.
     * @return floatV
     */
    public float getFloatV() {
        return Utils.unsignedByteToFloat(this.v);
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
     * Creates a new ByteUV object which has the same properties as this one.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public net.highwayfrogs.editor.file.standard.psx.ByteUV clone() {
        return new net.highwayfrogs.editor.file.standard.psx.ByteUV(this.u, this.v);
    }

    /**
     * Get this as an OBJ vt command.
     * @return objTextureString
     */
    public String toObjTextureString() {
        return "vt " + getFloatU() + " " + getFloatV();
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

            this.u = floatToByte(u);
            this.v = floatToByte(v);
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