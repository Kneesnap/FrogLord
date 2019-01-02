package net.highwayfrogs.editor.file.map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Registry of different map themes.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@AllArgsConstructor
public enum MAPTheme {
    GENERAL("GEN", 0x100, 128),
    CAVE("CAV", 0x1000, 0),
    DESERT("DES", 0x100, 2),
    FOREST("FOR", 0x100, 0),
    JUNGLE("JUN", 0x100, 8),
    ORIGINAL("ORG", 0x100, 0),
    RUINED("ARN", 0x100, 0),
    SWAMP("SWP", 0x100, 0),
    SKY("SKY", 0x1800 << 1, 0), // SKY_LAND_HEIGHT = 0x1800
    SUBURBIA("SUB", 0x100, 0),
    VOLCANO("VOL", 0x100, 8); // Also sometimes called Industrial or IND.

    private String internalName;
    private int deathHeight; // Frog drowns under this height.
    private int formOffset;

    /**
     * Gets the theme from a file name.
     * @param name The name of the file to get.
     * @return theme
     */
    public static MAPTheme getTheme(String name) {
        MAPTheme partialMatch = null;

        for (MAPTheme theme : values()) {
            if (name.contains(theme.name()))
                return theme;

            if (name.contains(theme.getInternalName()))
                partialMatch = theme;
        }

        return partialMatch;
    }
}
