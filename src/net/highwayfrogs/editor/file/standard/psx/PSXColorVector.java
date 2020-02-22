package net.highwayfrogs.editor.file.standard.psx;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;

/**
 * Implements the PSX "CVECTOR" struct. Used for storing color data.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PSXColorVector extends GameObject {
    private byte red;
    private byte green;
    private byte blue;
    private byte cd; // Might be alpha? Frogger seems to have 0xFF for all cases here, which would make sense.

    public static final int BYTE_LENGTH = 4 * Constants.BYTE_SIZE;

    @Override
    public void load(DataReader reader) {
        this.red = reader.readByte();
        this.green = reader.readByte();
        this.blue = reader.readByte();
        this.cd = reader.readByte();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte(this.red);
        writer.writeByte(this.green);
        writer.writeByte(this.blue);
        writer.writeByte(this.cd);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PSXColorVector))
            return false;

        PSXColorVector other = (PSXColorVector) obj;
        return getRed() == other.getRed() && getGreen() == other.getGreen() && getBlue() == other.getBlue() && getCd() == other.getCd();
    }

    @Override
    public int hashCode() {
        return ((0xFF & this.red) << 24) | ((0xFF & this.green) << 16) |
                ((0xFF & this.blue) << 8) | (0xFF & this.cd);
    }

    /**
     * Get this color as a Java color.
     * @return javaColor
     */
    public Color toColor() {
        return toColor((byte) 0xFF);
    }

    /**
     * Get this color as a Java color.
     * @return javaColor
     */
    public Color toColor(byte alpha) {
        return new Color(Utils.byteToUnsignedShort(getRed()), Utils.byteToUnsignedShort(getGreen()), Utils.byteToUnsignedShort(getBlue()), (alpha & 0xFF));
    }

    /**
     * Turn this color into an RGB integer.
     * @return rgbValue
     */
    public int toRGB() {
        return Utils.toRGB(getRed(), getGreen(), getBlue());
    }

    /**
     * Read color data from an integer value.
     * @param rgbValue The value to read from.
     */
    public void fromRGB(int rgbValue) {
        this.red = Utils.unsignedShortToByte((short) ((rgbValue >> 16) & 0xFF));
        this.green = Utils.unsignedShortToByte((short) ((rgbValue >> 8) & 0xFF));
        this.blue = Utils.unsignedShortToByte((short) (rgbValue & 0xFF));
    }

    /**
     * Create a PSXColorVector from an RGB value.
     * @param rgbValue The rgb color value.
     * @return colorVector
     */
    public static PSXColorVector makeColorFromRGB(int rgbValue) {
        PSXColorVector vec = new PSXColorVector();
        vec.fromRGB(rgbValue);
        return vec;
    }

    /**
     * Creates a copy of this object.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public PSXColorVector clone() {
        return new PSXColorVector(this.red, this.green, this.blue, this.cd);
    }
}
