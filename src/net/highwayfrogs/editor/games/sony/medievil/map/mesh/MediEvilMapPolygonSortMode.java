package net.highwayfrogs.editor.games.sony.medievil.map.mesh;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the polygon sorting modes.
 * Created by Kneesnap on 1/8/2026.
 */
@Getter
@RequiredArgsConstructor
public enum MediEvilMapPolygonSortMode {
    SORT_BY_AVERAGE_Z_ALLOW_OVERRIDE("Average"),
    SORT_BY_NEAR_Z("Nearest"),
    SORT_BY_FAR_Z("Furthest"),
    SORT_BY_AVERAGE_Z_FORCE_OVERRIDE("Average (Override)");

    private final String displayName;
}