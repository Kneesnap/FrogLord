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
import net.highwayfrogs.editor.utils.StringUtils;

import java.awt.*;
import java.text.DecimalFormat;

/**
 * Represents the 'kcColor3' struct
 * Created by Kneesnap on 8/1/2023.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class kcColor3 implements IInfoWriter, IBinarySerializable {
    private static final DecimalFormat DISPLAY_FORMAT = new DecimalFormat("0.###");
    private float red;
    private float green;
    private float blue;

    @Override
    public void load(DataReader reader) {
        this.red = reader.readFloat();
        this.green = reader.readFloat();
        this.blue = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeFloat(this.red);
        writer.writeFloat(this.green);
        writer.writeFloat(this.blue);
    }

    /**
     * Convert this color into a Color object.
     */
    public Color toColor() {
        float clampedRed = Math.max(0F, Math.min(1F, this.red));
        float clampedGreen = Math.max(0F, Math.min(1F, this.green));
        float clampedBlue = Math.max(0F, Math.min(1F, this.blue));
        return new Color(clampedRed, clampedGreen, clampedBlue);
    }

    /**
     * Gets this as a JavaFX color.
     */
    public javafx.scene.paint.Color toFxColor() {
        float clampedRed = Math.max(0F, Math.min(1F, this.red));
        float clampedGreen = Math.max(0F, Math.min(1F, this.green));
        float clampedBlue = Math.max(0F, Math.min(1F, this.blue));
        return javafx.scene.paint.Color.color(clampedRed, clampedGreen, clampedBlue);
    }

    /**
     * Load the values from an RGB color
     * @param rgbColor color to read from
     * @return this
     */
    public kcColor3 fromRGB(int rgbColor) {
        // TODO: We should allow a color object, with direct float access, maybe. Just thinking about what the new UI system could look like.
        this.red = ColorUtils.getRedInt(rgbColor) / 255F;
        this.green = ColorUtils.getGreenInt(rgbColor) / 255F;
        this.blue = ColorUtils.getBlueInt(rgbColor) / 255F;
        return this;
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        int rgbColor = toColor().getRGB() & 0xFFFFFF;
        builder.append("kcColor3[RGB=")
                .append(StringUtils.padStringLeft(Integer.toHexString(rgbColor).toUpperCase(), 6, '0'))
                .append(",red=").append(DISPLAY_FORMAT.format(this.red))
                .append(",green=").append(DISPLAY_FORMAT.format(this.green))
                .append(",blue=").append(DISPLAY_FORMAT.format(this.blue))
                .append(']');
    }
}