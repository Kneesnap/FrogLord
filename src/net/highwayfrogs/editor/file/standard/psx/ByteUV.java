package net.highwayfrogs.editor.file.standard.psx;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Holds texture UV information.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ByteUV extends GameObject {
    private byte u;
    private byte v;

    public static final int BYTE_SIZE = 2 * Constants.BYTE_SIZE;
    private static final String SPLIT_CHAR = ",";

    public ByteUV(float u, float v) {
        this.u = Utils.floatToByte(u);
        this.v = Utils.floatToByte(v);
    }

    @Override
    public void load(DataReader reader) {
        this.u = reader.readByte();
        this.v = reader.readByte();
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

    @Override
    public void save(DataWriter writer) {
        writer.writeByte(this.u);
        writer.writeByte(this.v);
    }

    /**
     * Creates a new ByteUV object which has the same properties as this one.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public ByteUV clone() {
        return new ByteUV(this.u, this.v);
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
        editor.addTextField(label, getFloatU() + SPLIT_CHAR + getFloatV(), newStr -> {
            if (!newStr.contains(SPLIT_CHAR))
                return false;

            String[] split = newStr.split(SPLIT_CHAR);
            if (split.length != 2)
                return false;

            float u = Float.parseFloat(split[0]);
            float v = Float.parseFloat(split[1]);

            if ((u < 0.0) || (u > 1.0) || (v < 0.0) || (v > 1.0))
                return false;

            this.u = Utils.floatToByte(u);
            this.v = Utils.floatToByte(v);
            if (onUpdate != null)
                onUpdate.run();
            return true;
        });
    }
}
