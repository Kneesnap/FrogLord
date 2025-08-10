package net.highwayfrogs.editor.games.sony.beastwars.map.data.object;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the dropped item on death.
 * Created by Kneesnap on 8/2/2025.
 */
@Getter
@RequiredArgsConstructor
public enum BeastWarsMapDeathDropType {
    NONE("None"), // 0
    ENERGON_RESISTANCE("Energon Resistance (50%)"), // 1, The percentage is a rough guess. Seems to be percentage since it seems to be regardless of character.
    BIG_HEALTH("Big Health (50%)"), // 2, The percentage is a rough guess.
    POWER("Power"), // 3
    LOCKON("Lock-On"), // 4
    SENTRY_GUN("Laser Gun (Volcano)"), // 5, VOL_LASGUNALLBASE.XMR
    BIG_HEALTH_HEALS_ZERO("Debug (Big Health, 0%)"), // 6, Prototype PSX September 19
    BIG_HEALTH_CANNOT_COLLECT("Debug (Big Health, 0%)"), // 7, Prototype PSX September 19
    UNKNOWN_8("Unknown (Never Seen)"), // 8
    UNKNOWN_9("Unknown (Never Seen)"), // 9
    EXTREMELY_SMALL_ENERGON_RESISTANCE("Small Energon Resistance (50%)"), // 10, The percentage is a rough guess.
    ALTERNATE_NOTHING("Nothing?"), // 11, TODO: This is seen by the game, and feels weird to me.
    SMALL_HEALTH("Small Health (??%)"), // 12, TODO: The percentage is a rough guess.
    SMALL_ENERGON_RESISTANCE("Small Energon Resistance (10%)"), // 13, The percentage is a rough guess.
    RESCUE_MISSION_ICON("Rescue Mission Icon"); // 14

    private final String displayName;
}
