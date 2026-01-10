package net.highwayfrogs.editor.games.sony.medievil.map.misc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the
 * Created by Kneesnap on 1/9/2026.
 */
@Getter
@RequiredArgsConstructor
public enum MediEvilMapInteractionType {
    NONE("None"),
    WATER("Water"),
    MUD("Mud"),
    DEADLY_MUD("Deadly Mud"),
    NOT_GROUND("Not Ground"),
    CORN("Corn"),
    SPECIAL1("Special 1 (Unused)"), // Scarecrow fields. (Unused in final?)
    SPECIAL2("Special 2 (Unused)");

    private final String displayName;
}
