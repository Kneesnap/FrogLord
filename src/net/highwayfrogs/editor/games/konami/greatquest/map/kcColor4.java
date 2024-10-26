package net.highwayfrogs.editor.games.konami.greatquest.map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.NumberUtils;

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
public class kcColor4 implements IInfoWriter, IBinarySerializable {
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
     * Load color data from an ARGB integer
     * @param argbColor the argb integer value to load from
     * @return this
     */
    public kcColor4 fromARGB(int argbColor) {
        this.red = ColorUtils.getRedInt(argbColor) / 255F;
        this.green = ColorUtils.getGreenInt(argbColor) / 255F;
        this.blue = ColorUtils.getBlueInt(argbColor) / 255F;
        this.alpha = ColorUtils.getAlphaInt(argbColor) / 255F;
        return this;
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        int rgbColor = toColor().getRGB();
        builder.append("kcColor4[ARGB=")
                .append(NumberUtils.to0PrefixedHexString(rgbColor))
                .append(",red=").append(DISPLAY_FORMAT.format(this.red))
                .append(",green=").append(DISPLAY_FORMAT.format(this.green))
                .append(",blue=").append(DISPLAY_FORMAT.format(this.blue))
                .append(",alpha=").append(DISPLAY_FORMAT.format(this.alpha))
                .append(']');
    }
}