package net.highwayfrogs.editor.file.map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the game's map enum.
 * Created by Kneesnap on 1/28/2019.
 */
@Getter
@AllArgsConstructor
public enum MAPLevel {
    CAVE1(MAPTheme.CAVE, "CAV1", true),
    CAVE2(MAPTheme.CAVE, "CAV2", false),
    CAVE3(MAPTheme.CAVE, "CAV3", true),
    CAVE4(MAPTheme.CAVE, "CAV4", true),
    CAVE5(MAPTheme.CAVE, "CAV5", false),
    CAVE_MULTIPLAYER(MAPTheme.CAVE, "CAVM", false),
    DESERT1(MAPTheme.DESERT, "DES1", true),
    DESERT2(MAPTheme.DESERT, "DES2", true),
    DESERT3(MAPTheme.DESERT, "DES3", true),
    DESERT4(MAPTheme.DESERT, "DES4", true),
    DESERT5(MAPTheme.DESERT, "DES5", true),
    DESERT_MULTIPLAYER(MAPTheme.DESERT, "DESM", false),
    FOREST1(MAPTheme.FOREST, "FOR1", true),
    FOREST2(MAPTheme.FOREST, "FOR2", true),
    FOREST3(MAPTheme.FOREST, "FOR3", false),
    FOREST4(MAPTheme.FOREST, "FOR4", false),
    FOREST5(MAPTheme.FOREST, "FOR5", false),
    FOREST_MULTIPLAYER(MAPTheme.FOREST, "FORM", true),
    JUNGLE1(MAPTheme.JUNGLE, "JUN1", true),
    JUNGLE2(MAPTheme.JUNGLE, "JUN2", true),
    JUNGLE3(MAPTheme.JUNGLE, "JUN3", false),
    JUNGLE4(MAPTheme.JUNGLE, "JUN4", false),
    JUNGLE5(MAPTheme.JUNGLE, "JUN5", false),
    JUNGLE_MULTIPLAYER(MAPTheme.JUNGLE, "JUNM", true),
    ORIGINAL1(MAPTheme.ORIGINAL, "ORG1", true),
    ORIGINAL2(MAPTheme.ORIGINAL, "ORG2", true),
    ORIGINAL3(MAPTheme.ORIGINAL, "ORG3", true),
    ORIGINAL4(MAPTheme.ORIGINAL, "ORG4", true),
    ORIGINAL5(MAPTheme.ORIGINAL, "ORG5", true),
    ORIGINAL_MULTIPLAYER(MAPTheme.ORIGINAL, "ORGM", true),
    RUINS1(MAPTheme.RUINED, "ARN1", false),
    RUINS2(MAPTheme.RUINED, "ARN2", false),
    RUINS3(MAPTheme.RUINED, "ARN3", false),
    RUINS4(MAPTheme.RUINED, "ARN4", false),
    RUINS5(MAPTheme.RUINED, "ARN5", false),
    RUINS_MULTIPLAYER(MAPTheme.RUINED, "ARNM", false),
    SWAMP1(MAPTheme.SWAMP, "SWP1", true),
    SWAMP2(MAPTheme.SWAMP, "SWP2", true),
    SWAMP3(MAPTheme.SWAMP, "SWP3", true),
    SWAMP4(MAPTheme.SWAMP, "SWP4", true),
    SWAMP5(MAPTheme.SWAMP, "SWP5", true),
    SWAMP_MULTIPLAYER(MAPTheme.SWAMP, "SWPM", false),
    SKY1(MAPTheme.SKY, "SKY1", true),
    SKY2(MAPTheme.SKY, "SKY2", true),
    SKY3(MAPTheme.SKY, "SKY3", true),
    SKY4(MAPTheme.SKY, "SKY4", true),
    SKY5(MAPTheme.SKY, "SKY5", false),
    SKY_MULTIPLAYER(MAPTheme.SKY, "SKYM", false),
    SUBURBIA1(MAPTheme.SUBURBIA, "SUB1", true),
    SUBURBIA2(MAPTheme.SUBURBIA, "SUB2", true),
    SUBURBIA3(MAPTheme.SUBURBIA, "SUB3", true),
    SUBURBIA4(MAPTheme.SUBURBIA, "SUB4", true),
    SUBURBIA5(MAPTheme.SUBURBIA, "SUB5", true),
    SUBURBIA_MULTIPLAYER(MAPTheme.SUBURBIA, "SUBM", true),
    VOLCANO1(MAPTheme.VOLCANO, "VOL1", true),
    VOLCANO2(MAPTheme.VOLCANO, "VOL2", true),
    VOLCANO3(MAPTheme.VOLCANO, "VOL3", true),
    VOLCANO4(MAPTheme.VOLCANO, "VOL4", false),
    VOLCANO5(MAPTheme.VOLCANO, "VOL5", false),
    VOLCANO_MULTIPLAYER(MAPTheme.VOLCANO, "VOLM", true),
    ISLAND(MAPTheme.SUBURBIA, "ISLAND", false),
    QB(MAPTheme.SUBURBIA, "QB", false);

    private final MAPTheme theme;
    private final String internalName;
    private final boolean exists; // Whether or not both a map file exists and the map is registered in Theme_library.

    /**
     * Get a MAPLevel by its internal name.
     * @param levelName The internal level name.
     * @return mapLevel
     */
    public static MAPLevel getByName(String levelName) {
        for (MAPLevel testLevel : values())
            if (testLevel.getInternalName().equalsIgnoreCase(levelName))
                return testLevel;
        return null;
    }
}
