package net.highwayfrogs.editor.utils;

import net.highwayfrogs.editor.games.generic.GameInstance;

import java.util.logging.Logger;

/**
 * Represents functionality seen in GameObject.
 * Object inheritance is a little wonky in FrogLord Java since it's mimicking the style of the eventual ModToolFramework library, without fully recreating the MTF API.
 * Created by Kneesnap on 5/13/2024.
 */
public interface IGameObject {
    /**
     * Gets the game instance which the game object belongs to.
     */
    GameInstance getGameInstance();

    /**
     * Gets the logger used for logging data relating to the game object.
     */
    Logger getLogger();

    /**
     * Tests a value for bits outside the supplied mask, warning if found.
     * @param value The value to test
     * @param mask The bit mask to test against.
     * @return true iff there are no unsupported bits.
     */
    default boolean warnAboutInvalidBitFlags(long value, long mask) {
        return Utils.warnAboutInvalidBitFlags(getLogger(), value, mask, getClass().getSimpleName());
    }

    /**
     * Tests a value for bits outside the supplied mask, warning if found.
     * @param value The value to test
     * @param mask The bit mask to test against.
     * @param target A display string representing the data type.
     * @return true iff there are no unsupported bits.
     */
    default boolean warnAboutInvalidBitFlags(long value, long mask, String target) {
        return Utils.warnAboutInvalidBitFlags(getLogger(), value, mask, target);
    }
}