package net.highwayfrogs.editor.games.sony.frogger.map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the game's map enum. Recreation of LEVEL_##### enum in LIBRARY.H
 * Created by Kneesnap on 1/28/2019.
 */
@Getter
@AllArgsConstructor
public enum FroggerMapLevelID {
    CAVE1(FroggerMapTheme.CAVE, "CAV1"), // 0
    CAVE2(FroggerMapTheme.CAVE, "CAV2"),
    CAVE3(FroggerMapTheme.CAVE, "CAV3"),
    CAVE4(FroggerMapTheme.CAVE, "CAV4"),
    CAVE5(FroggerMapTheme.CAVE, "CAV5"),
    CAVE_MULTIPLAYER(FroggerMapTheme.CAVE, "CAVM"),
    DESERT1(FroggerMapTheme.DESERT, "DES1"), // 6
    DESERT2(FroggerMapTheme.DESERT, "DES2"),
    DESERT3(FroggerMapTheme.DESERT, "DES3"),
    DESERT4(FroggerMapTheme.DESERT, "DES4"),
    DESERT5(FroggerMapTheme.DESERT, "DES5"),
    DESERT_MULTIPLAYER(FroggerMapTheme.DESERT, "DESM"),
    FOREST1(FroggerMapTheme.FOREST, "FOR1"), // 12
    FOREST2(FroggerMapTheme.FOREST, "FOR2"),
    FOREST3(FroggerMapTheme.FOREST, "FOR3"),
    FOREST4(FroggerMapTheme.FOREST, "FOR4"),
    FOREST5(FroggerMapTheme.FOREST, "FOR5"),
    FOREST_MULTIPLAYER(FroggerMapTheme.FOREST, "FORM"),
    JUNGLE1(FroggerMapTheme.JUNGLE, "JUN1"), // 18
    JUNGLE2(FroggerMapTheme.JUNGLE, "JUN2"),
    JUNGLE3(FroggerMapTheme.JUNGLE, "JUN3"),
    JUNGLE4(FroggerMapTheme.JUNGLE, "JUN4"),
    JUNGLE5(FroggerMapTheme.JUNGLE, "JUN5"),
    JUNGLE_MULTIPLAYER(FroggerMapTheme.JUNGLE, "JUNM"),
    ORIGINAL1(FroggerMapTheme.ORIGINAL, "ORG1"), // 24
    ORIGINAL2(FroggerMapTheme.ORIGINAL, "ORG2"),
    ORIGINAL3(FroggerMapTheme.ORIGINAL, "ORG3"),
    ORIGINAL4(FroggerMapTheme.ORIGINAL, "ORG4"),
    ORIGINAL5(FroggerMapTheme.ORIGINAL, "ORG5"),
    ORIGINAL_MULTIPLAYER(FroggerMapTheme.ORIGINAL, "ORGM"),
    RUINS1(FroggerMapTheme.RUINS, "ARN1"), // 30
    RUINS2(FroggerMapTheme.RUINS, "ARN2"),
    RUINS3(FroggerMapTheme.RUINS, "ARN3"),
    RUINS4(FroggerMapTheme.RUINS, "ARN4"),
    RUINS5(FroggerMapTheme.RUINS, "ARN5"),
    RUINS_MULTIPLAYER(FroggerMapTheme.RUINS, "ARNM"),
    SWAMP1(FroggerMapTheme.SWAMP, "SWP1"), // 36
    SWAMP2(FroggerMapTheme.SWAMP, "SWP2"),
    SWAMP3(FroggerMapTheme.SWAMP, "SWP3"),
    SWAMP4(FroggerMapTheme.SWAMP, "SWP4"),
    SWAMP5(FroggerMapTheme.SWAMP, "SWP5"),
    SWAMP_MULTIPLAYER(FroggerMapTheme.SWAMP, "SWPM"),
    SKY1(FroggerMapTheme.SKY, "SKY1"), // 42
    SKY2(FroggerMapTheme.SKY, "SKY2"),
    SKY3(FroggerMapTheme.SKY, "SKY3"),
    SKY4(FroggerMapTheme.SKY, "SKY4"),
    SKY5(FroggerMapTheme.SKY, "SKY5"),
    SKY_MULTIPLAYER(FroggerMapTheme.SKY, "SKYM"),
    SUBURBIA1(FroggerMapTheme.SUBURBIA, "SUB1"), // 48
    SUBURBIA2(FroggerMapTheme.SUBURBIA, "SUB2"),
    SUBURBIA3(FroggerMapTheme.SUBURBIA, "SUB3"),
    SUBURBIA4(FroggerMapTheme.SUBURBIA, "SUB4"),
    SUBURBIA5(FroggerMapTheme.SUBURBIA, "SUB5"),
    SUBURBIA_MULTIPLAYER(FroggerMapTheme.SUBURBIA, "SUBM"),
    VOLCANO1(FroggerMapTheme.VOLCANO, "VOL1"), // 54
    VOLCANO2(FroggerMapTheme.VOLCANO, "VOL2"),
    VOLCANO3(FroggerMapTheme.VOLCANO, "VOL3"),
    VOLCANO4(FroggerMapTheme.VOLCANO, "VOL4"),
    VOLCANO5(FroggerMapTheme.VOLCANO, "VOL5"),
    VOLCANO_MULTIPLAYER(FroggerMapTheme.VOLCANO, "VOLM"),
    ISLAND(FroggerMapTheme.SUBURBIA, "ISLAND"), // 60
    QB(FroggerMapTheme.SUBURBIA, "QB"); // 61

    private final FroggerMapTheme theme;
    private final String internalName;

    /**
     * Get a FroggerMapLevelID by its internal name.
     * @param levelName The internal level name.
     * @return mapLevel
     */
    public static FroggerMapLevelID getByName(String levelName) {
        String levelNameTest = levelName.toUpperCase();
        for (FroggerMapLevelID testLevel : values())
            if (levelNameTest.startsWith(testLevel.getInternalName()))
                return testLevel;
        return null;
    }
}