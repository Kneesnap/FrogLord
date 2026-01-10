package net.highwayfrogs.editor.games.sony.medievil.map.misc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the friction level to use when
 * Created by Kneesnap on 1/9/2026.
 */
@Getter
@RequiredArgsConstructor
public enum MediEvilMapFrictionLevel {
    STANDARD("3x (Standard)"), // 3 << 8 (DEFAULT) 24:8 fixed point number
    LEVEL_2("2x"), // 2 << 8
    LEVEL_3("1x"), // 1 << 8
    LEVEL_4("6x"); // 6 << 8

    private final String displayName;
}
