package net.highwayfrogs.editor.games.sony.medievil.map.packet.pathchain;

/**
 * Represents the different possible path group types.
 * Created by Kneesnap on 2/18/2026.
 */
public enum MediEvilMapPathGroupType {
    ENTITY, // Default
    NO_ENTITY, // Unused.
    LOCKED, // Usable for auto-routing.
    LIMIT; // Unused.

    public static final MediEvilMapPathGroupType[] VALID_TYPES = {ENTITY, LOCKED};
}
