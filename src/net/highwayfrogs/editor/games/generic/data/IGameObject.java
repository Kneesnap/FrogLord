package net.highwayfrogs.editor.games.generic.data;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.utils.NumberUtils;

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
     * NOTE: This does NOT call Utils.warnAboutInvalidBitFlags() in order to avoid the potential allocation of a new Logger object which isn't even used.
     * @param value The value to test
     * @param mask The bit mask to test against.
     * @return true iff there are no unsupported bits.
     */
    default boolean warnAboutInvalidBitFlags(long value, long mask) {
        return warnAboutInvalidBitFlags(value, mask, getClass().getSimpleName());
    }

    /**
     * Tests a value for bits outside the supplied mask, warning if found.
     * NOTE: This does NOT call Utils.warnAboutInvalidBitFlags() in order to avoid the potential allocation of a new Logger object which isn't even used.
     * @param value The value to test
     * @param mask The bit mask to test against.
     * @param target A display string representing the data type.
     * @return true iff there are no unsupported bits.
     */
    default boolean warnAboutInvalidBitFlags(long value, long mask, String target) {
        if ((value & ~mask) == 0)
            return true;

        if (target != null) {
            getLogger().warning(target + " had bit flag value " + NumberUtils.toHexString(value) + ", which contained unhandled bits. (Mask: " + NumberUtils.toHexString(mask) + ")");
        } else {
            getLogger().warning("Bit flag value " + NumberUtils.toHexString(value) + " had unexpected bits set! (Mask: " + NumberUtils.toHexString(mask) + ")");
        }
        return false;
    }

    /**
     * Requires the reader to be at a given index without creating an unnecessary Logger object.
     * @param reader the reader to warn if not at the index.
     * @param desiredIndex the index to require.
     * @param messagePrefix The message to print for a warning.
     */
    default void requireReaderIndex(DataReader reader, int desiredIndex, String messagePrefix) {
        if (reader.getIndex() != desiredIndex)
            reader.requireIndex(getLogger(), desiredIndex, messagePrefix);
    }
}