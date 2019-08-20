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
    private short u;
    private short v;

    public static final int BYTE_SIZE = 2 * Constants.BYTE_SIZE;
    private static final String SPLIT_CHAR = ",";

    @Override
    public void load(DataReader reader) {
        this.u = reader.readUnsignedByteAsShort();
        this.v = reader.readUnsignedByteAsShort();
    }

    /**
     * Get U as a float ranging from 0 to 1.
     * @return floatU
     */
    public float getFloatU() {
        return Utils.unsignedByteToFloat(Utils.unsignedShortToByte(this.u));
    }

    /**
     * Get V as a float ranging from 0 to 1.
     * @return floatV
     */
    public float getFloatV() {
        return Utils.unsignedByteToFloat(Utils.unsignedShortToByte(this.v));
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte(this.u);
        writer.writeUnsignedByte(this.v);
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
    public void setupEditor(String label, GUIEditorGrid editor) {
        editor.addTextField(label, getU() + SPLIT_CHAR + getV(), newStr -> {
            if (!newStr.contains(SPLIT_CHAR))
                return false;

            String[] split = newStr.split(SPLIT_CHAR);
            if (split.length != 2 || !Utils.isUnsignedByte(split[0]) || !Utils.isUnsignedByte(split[1]))
                return false;

            this.u = Short.parseShort(split[0]);
            this.v = Short.parseShort(split[1]);
            return true;
        });
    }
}
