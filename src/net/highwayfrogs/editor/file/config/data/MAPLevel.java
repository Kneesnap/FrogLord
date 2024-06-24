package net.highwayfrogs.editor.file.config.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;

/**
 * Represents the game's map enum.
 * Created by Kneesnap on 1/28/2019.
 */
@Getter
@AllArgsConstructor
public enum MAPLevel {
    CAVE1(FroggerMapTheme.CAVE, "CAV1", true), // 0
    CAVE2(FroggerMapTheme.CAVE, "CAV2", false),
    CAVE3(FroggerMapTheme.CAVE, "CAV3", true),
    CAVE4(FroggerMapTheme.CAVE, "CAV4", true),
    CAVE5(FroggerMapTheme.CAVE, "CAV5", false),
    CAVE_MULTIPLAYER(FroggerMapTheme.CAVE, "CAVM", false),
    DESERT1(FroggerMapTheme.DESERT, "DES1", true), // 6
    DESERT2(FroggerMapTheme.DESERT, "DES2", true),
    DESERT3(FroggerMapTheme.DESERT, "DES3", true),
    DESERT4(FroggerMapTheme.DESERT, "DES4", true),
    DESERT5(FroggerMapTheme.DESERT, "DES5", true),
    DESERT_MULTIPLAYER(FroggerMapTheme.DESERT, "DESM", false),
    FOREST1(FroggerMapTheme.FOREST, "FOR1", true), // 12
    FOREST2(FroggerMapTheme.FOREST, "FOR2", true),
    FOREST3(FroggerMapTheme.FOREST, "FOR3", false),
    FOREST4(FroggerMapTheme.FOREST, "FOR4", false),
    FOREST5(FroggerMapTheme.FOREST, "FOR5", false),
    FOREST_MULTIPLAYER(FroggerMapTheme.FOREST, "FORM", true),
    JUNGLE1(FroggerMapTheme.JUNGLE, "JUN1", true), // 18
    JUNGLE2(FroggerMapTheme.JUNGLE, "JUN2", true),
    JUNGLE3(FroggerMapTheme.JUNGLE, "JUN3", false),
    JUNGLE4(FroggerMapTheme.JUNGLE, "JUN4", false),
    JUNGLE5(FroggerMapTheme.JUNGLE, "JUN5", false),
    JUNGLE_MULTIPLAYER(FroggerMapTheme.JUNGLE, "JUNM", true),
    ORIGINAL1(FroggerMapTheme.ORIGINAL, "ORG1", true), // 24
    ORIGINAL2(FroggerMapTheme.ORIGINAL, "ORG2", true),
    ORIGINAL3(FroggerMapTheme.ORIGINAL, "ORG3", true),
    ORIGINAL4(FroggerMapTheme.ORIGINAL, "ORG4", true),
    ORIGINAL5(FroggerMapTheme.ORIGINAL, "ORG5", true),
    ORIGINAL_MULTIPLAYER(FroggerMapTheme.ORIGINAL, "ORGM", true),
    RUINS1(FroggerMapTheme.RUINS, "ARN1", false), // 30
    RUINS2(FroggerMapTheme.RUINS, "ARN2", false),
    RUINS3(FroggerMapTheme.RUINS, "ARN3", false),
    RUINS4(FroggerMapTheme.RUINS, "ARN4", false),
    RUINS5(FroggerMapTheme.RUINS, "ARN5", false),
    RUINS_MULTIPLAYER(FroggerMapTheme.RUINS, "ARNM", false),
    SWAMP1(FroggerMapTheme.SWAMP, "SWP1", true), // 36
    SWAMP2(FroggerMapTheme.SWAMP, "SWP2", true),
    SWAMP3(FroggerMapTheme.SWAMP, "SWP3", true),
    SWAMP4(FroggerMapTheme.SWAMP, "SWP4", true),
    SWAMP5(FroggerMapTheme.SWAMP, "SWP5", true),
    SWAMP_MULTIPLAYER(FroggerMapTheme.SWAMP, "SWPM", false),
    SKY1(FroggerMapTheme.SKY, "SKY1", true), // 42
    SKY2(FroggerMapTheme.SKY, "SKY2", true),
    SKY3(FroggerMapTheme.SKY, "SKY3", true),
    SKY4(FroggerMapTheme.SKY, "SKY4", true),
    SKY5(FroggerMapTheme.SKY, "SKY5", false),
    SKY_MULTIPLAYER(FroggerMapTheme.SKY, "SKYM", false),
    SUBURBIA1(FroggerMapTheme.SUBURBIA, "SUB1", true), // 48
    SUBURBIA2(FroggerMapTheme.SUBURBIA, "SUB2", true),
    SUBURBIA3(FroggerMapTheme.SUBURBIA, "SUB3", true),
    SUBURBIA4(FroggerMapTheme.SUBURBIA, "SUB4", true),
    SUBURBIA5(FroggerMapTheme.SUBURBIA, "SUB5", true),
    SUBURBIA_MULTIPLAYER(FroggerMapTheme.SUBURBIA, "SUBM", true),
    VOLCANO1(FroggerMapTheme.VOLCANO, "VOL1", true), // 54
    VOLCANO2(FroggerMapTheme.VOLCANO, "VOL2", true),
    VOLCANO3(FroggerMapTheme.VOLCANO, "VOL3", true),
    VOLCANO4(FroggerMapTheme.VOLCANO, "VOL4", false),
    VOLCANO5(FroggerMapTheme.VOLCANO, "VOL5", false),
    VOLCANO_MULTIPLAYER(FroggerMapTheme.VOLCANO, "VOLM", true),
    ISLAND(FroggerMapTheme.SUBURBIA, "ISLAND", false), // 60
    QB(FroggerMapTheme.SUBURBIA, "QB", false); // 61

    private final FroggerMapTheme theme;
    private final String internalName;
    private final boolean exists; // Whether this map exists on the level stack.

    /**
     * Get a MAPLevel by its internal name.
     * @param levelName The internal level name.
     * @return mapLevel
     */
    public static MAPLevel getByName(String levelName) {
        for (MAPLevel testLevel : values())
            if (levelName.toUpperCase().startsWith(testLevel.getInternalName()))
                return testLevel;
        return null;
    }
}