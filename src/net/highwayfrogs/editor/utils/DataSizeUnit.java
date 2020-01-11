package net.highwayfrogs.editor.utils;

import lombok.Getter;

import java.text.DecimalFormat;

/**
 * Data size units.
 * Created by Kneesnap on 1/10/2020.
 */
@Getter
public enum DataSizeUnit {
    BYTE("B"),
    KILOBYTE("KB"),
    MEGABYTE("MB"),
    GIGABYTE("GB");
    // Anything past gigabyte has not been included, because it's more than what the 64bit long can include, which is what the JVM's Runtime.getRuntime() memory functions return.

    private final String unitDisplay;
    private final long increment;
    private static final DecimalFormat FORMAT = new DecimalFormat("#.#");

    DataSizeUnit(String display) {
        this.unitDisplay = display;
        this.increment = Utils.power(1024, ordinal());
    }

    /**
     * Format a size with units. Ie: 2048 -> "2KB".
     * @param size The size to convert
     * @return displaySize
     */
    public static String formatSize(long size) {
        for (int i = values().length - 1; i >= 0; i--) {
            DataSizeUnit unit = values()[i];
            if (size >= unit.getIncrement())
                return FORMAT.format(size / unit.getIncrement()) + unit.getUnitDisplay();
        }

        // Couldn't find a good unit, default to bytes.
        return size + "B";
    }
}
