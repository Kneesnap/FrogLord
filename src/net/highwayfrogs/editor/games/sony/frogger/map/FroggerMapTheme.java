package net.highwayfrogs.editor.games.sony.frogger.map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Registry of different map themes.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@AllArgsConstructor
public enum FroggerMapTheme {
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

    private final String internalName;
    private final int formOffset;

    /**
     * Gets the theme from a file name.
     * @param name The name of the file to get.
     * @return theme
     */
    public static FroggerMapTheme getTheme(String name) {
        FroggerMapTheme partialMatch = null;

        for (FroggerMapTheme theme : values()) {
            if (name.contains(theme.name()))
                return theme;

            if (name.contains(theme.getInternalName()))
                partialMatch = theme;
        }

        return partialMatch;
    }
}