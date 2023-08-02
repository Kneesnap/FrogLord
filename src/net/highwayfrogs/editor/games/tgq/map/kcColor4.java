package net.highwayfrogs.editor.games.tgq.map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.text.DecimalFormat;

/**
 * Represents the 'kcColor4' struct
 * Created by Kneesnap on 8/1/2023.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class kcColor4 extends GameObject {
    private static final DecimalFormat DISPLAY_FORMAT = new DecimalFormat("0.###");
    private float red;
    private float green;
    private float blue;
    private float alpha;

    @Override
    public void load(DataReader reader) {
        this.red = reader.readFloat();
        this.green = reader.readFloat();
        this.blue = reader.readFloat();
        this.alpha = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeFloat(this.red);
        writer.writeFloat(this.green);
        writer.writeFloat(this.blue);
        writer.writeFloat(this.alpha);
    }

    /**
     * Convert this color into a Color object.
     */
    public Color toColor() {
        // Ruins of Joy Town contains invalid values here, so we clamp it. We should investigate further
        float clampedRed = Math.max(0F, Math.min(1F, this.red));
        float clampedGreen = Math.max(0F, Math.min(1F, this.green));
        float clampedBlue = Math.max(0F, Math.min(1F, this.blue));
        float clampedAlpha = Math.max(0F, Math.min(1F, this.alpha));
        return new Color(clampedRed, clampedGreen, clampedBlue, clampedAlpha);
    }

    /**
     * Writes color data to the string builder.
     * @param builder The builder to write the color data to.
     */
    public void writeInfo(StringBuilder builder) {
        int rgbColor = toColor().getRGB();
        builder.append("kcColor4[ARGB=")
                .append(Utils.to0PrefixedHexString(rgbColor))
                .append(",red=").append(DISPLAY_FORMAT.format(this.red))
                .append(",green=").append(DISPLAY_FORMAT.format(this.green))
                .append(",blue=").append(DISPLAY_FORMAT.format(this.blue))
                .append(",alpha=").append(DISPLAY_FORMAT.format(this.alpha))
                .append(']');
    }
}
