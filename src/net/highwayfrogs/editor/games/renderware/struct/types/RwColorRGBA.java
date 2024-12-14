package net.highwayfrogs.editor.games.renderware.struct.types;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.DataUtils;

/**
 * Represents RwRGBA defined in bacolor.h
 * Created by Kneesnap on 8/17/2024.
 */
@Getter
@Setter
public class RwColorRGBA extends RwStruct {
    private int colorABGR;

    public static final int SIZE_IN_BYTES = Constants.INTEGER_SIZE;

    public RwColorRGBA(GameInstance instance) {
        super(instance, RwStructType.COLOR_RGBA);
    }

    /**
     * Loads the color from the reader.
     * @param reader the reader to read the color from
     * @param version the version to read
     */
    public void load(DataReader reader, int version) {
        load(reader, version, SIZE_IN_BYTES);
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        this.colorABGR = reader.readInt();
    }

    @Override
    public void save(DataWriter writer, int version) {
        writer.writeInt(this.colorABGR);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Red", Integer.toHexString(getRed() & 0xFF).toUpperCase());
        propertyList.add("Green", Integer.toHexString(getGreen() & 0xFF).toUpperCase());
        propertyList.add("Blue", Integer.toHexString(getBlue() & 0xFF).toUpperCase());
        propertyList.add("Alpha", Integer.toHexString(getAlpha() & 0xFF).toUpperCase());
        return propertyList;
    }

    @Override
    public String toString() {
        int colorRGBA = DataUtils.readIntFromBytes(DataUtils.reverseByteArray(DataUtils.toByteArray(this.colorABGR)), 0);
        return "RwColorRGBA{" + Integer.toString(colorRGBA).toUpperCase() + "}";
    }

    @Override
    public int hashCode() {
        return this.colorABGR;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RwColorRGBA && ((RwColorRGBA) o).colorABGR == this.colorABGR;
    }

    /**
     * Returns the red color component as a byte value.
     */
    public byte getRed() {
        return ColorUtils.getBlue(this.colorABGR); // Get the blue component since Utils.getBlue() assumes ARGB format, but we're providing ABGR.
    }

    /**
     * Returns the green color component as a byte value.
     */
    public byte getGreen() {
        return ColorUtils.getGreen(this.colorABGR);
    }

    /**
     * Returns the blue color component as a byte value.
     */
    public byte getBlue() {
        return ColorUtils.getRed(this.colorABGR); // Get the red component since Utils.getRed() assumes ARGB format, but we're providing ABGR.
    }

    /**
     * Returns the alpha color component as a byte value.
     */
    public byte getAlpha() {
        return ColorUtils.getAlpha(this.colorABGR);
    }

    /**
     * Gets the color as an ARGB integer.
     */
    public int getColorARGB() {
        return ColorUtils.swapRedBlue(this.colorABGR);
    }

    /**
     * Sets the new color to match an ARGB value.
     * @param colorARGB the ARGB value to apply
     */
    public void setColorARGB(int colorARGB) {
        this.colorABGR = ColorUtils.swapRedBlue(colorARGB);
    }
}