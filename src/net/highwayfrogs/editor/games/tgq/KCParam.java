package net.highwayfrogs.editor.games.tgq;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents 4 bytes with ambiguous interpretation. Used in Frogger TGQ.
 * Created by Kneesnap on 1/4/2021.
 */
@Getter
public class KCParam {
    private byte[] bytes;

    public KCParam(byte[] value) {
        Utils.verify(value != null && value.length == 4, "Invalid input array! (" + (value != null ? value.length : "null") + ")");
        this.bytes = value;
    }

    public KCParam(int value) {
        setValue(value);
    }

    public KCParam(float value) {
        setValue(value);
    }

    public KCParam(boolean value) {
        setValue(value);
    }

    /**
     * Gets the value as an integer.
     */
    public int getAsInteger() {
        return Utils.readIntFromBytes(this.bytes, 0);
    }

    /**
     * Gets the value as a float.
     */
    public float getAsFloat() {
        return Utils.readFloatFromBytes(this.bytes);
    }

    /**
     * Gets the value as a boolean.
     */
    public boolean getAsBoolean() {
        return getAsInteger() != 0;
    }

    /**
     * Sets the integer value.
     */
    public void setValue(int value) {
        this.bytes = Utils.toByteArray(value);
    }

    /**
     * Sets the float value.
     */
    public void setValue(float value) {
        this.bytes = Utils.writeFloatToBytes(value);
    }

    /**
     * Sets the boolean value.
     */
    public void setValue(boolean value) {
        setValue(value ? 1 : 0);
    }

    /**
     * Reads a KCParam from a DataReader.
     * @param reader The reader to read from.
     * @return loadedParam
     */
    public static KCParam readParam(DataReader reader) {
        return new KCParam(reader.readBytes(4));
    }
}
