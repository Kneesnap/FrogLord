package net.highwayfrogs.editor.games.generic;

/**
 * Contains static utilities for working with games.
 * Created by Kneesnap on 4/10/2024.
 */
public class GameUtils {
    /**
     * Test if a value looks like a valid pointer.
     * Built for Frogger 1997, but will likely be used in most games.
     * This function should generally give a rough guess based on heuristic knowledge about each platform.
     * However, it is recommended to build a specialized function for the specific game if necessary.
     * @param platform The platform value to test.
     * @param pointer  The pointer value to test.
     * @return looks like a pointer.
     */
    public static boolean isValidLookingPointer(GamePlatform platform, long pointer) {
        if (platform == GamePlatform.PLAYSTATION) {
            // Tests if the value is within address space of KSEG0, since that's Main RAM.
            // The first 64K (0x10000 bytes) are skipped because it's reserved for the BIOS.
            // There is 2 MB of RAM in this area, so the data must be within such a range.
            return (pointer >= 0x80010000L && pointer < 0x80200000L) || (pointer >= 0x10000L && pointer < 0x200000L);
        } else if (platform == GamePlatform.WINDOWS) {
            return (pointer & 0xFFF00000L) == 0x00400000;
        } else {
            throw new RuntimeException("Unsupported platform '" + platform + "'.");
        }
    }
}