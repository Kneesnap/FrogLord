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
    GENERAL("GEN"),
    CAVE("CAV"),
    DESERT("DES"),
    FOREST("FOR"),
    JUNGLE("JUN"),
    ORIGINAL("ORG"),
    RUINS("ARN"),
    SWAMP("SWP"),
    SKY("SKY"),
    SUBURBIA("SUB"),
    VOLCANO("VOL"); // Also sometimes called Industrial or IND.

    private final String internalName;

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