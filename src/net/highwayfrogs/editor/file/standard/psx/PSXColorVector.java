package net.highwayfrogs.editor.file.standard.psx;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

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

    /**
     * Get this color as a Java color.
     * @return javaColor
     */
    public Color toColor() {
        return new Color(getRed(), getBlue(), getGreen());
    }
}
