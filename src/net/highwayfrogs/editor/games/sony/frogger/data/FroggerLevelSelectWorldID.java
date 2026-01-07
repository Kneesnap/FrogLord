package net.highwayfrogs.editor.games.sony.frogger.data;

/**
 * Recreation of the level select enum SEL_WORLD_ID_#### in SELECT.H
 * Created by Kneesnap on 2/1/2019.
 */
public enum FroggerLevelSelectWorldID {
    SUBURBIA,
    ORIGINAL,
    SEWER, // There exists a copy of this enum called 'SWAMP' in the game. Omitted here since there's no point to including it.
    SKY,
    FOREST,
    VOLCANO,
    DESERT,
    CAVES,
    JUNGLE_RIVER,
    RUINED_CITY
}
