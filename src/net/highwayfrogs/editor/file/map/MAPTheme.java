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
    GENERAL("GEN", 128),
    CAVE("CAV", 0),
    DESERT("DES", 2),
    FOREST("FOR", 0),
    JUNGLE("JUN", 8),
    ORIGINAL("ORG", 0),
    RUINS("ARN", 0),
    SWAMP("SWP", 0),
    SKY("SKY", 0),
    SUBURBIA("SUB", 0),
    VOLCANO("VOL", 8); // Also sometimes called Industrial or IND.

    private String internalName;
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
