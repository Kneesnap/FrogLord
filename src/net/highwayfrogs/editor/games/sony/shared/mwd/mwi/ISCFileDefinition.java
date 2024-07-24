package net.highwayfrogs.editor.games.sony.shared.mwd.mwi;

import net.highwayfrogs.editor.games.sony.SCGameFile;

import java.util.logging.Logger;

/**
 * Represents a file definition.
 * Created by Kneesnap on 7/18/2024.
 */
public interface ISCFileDefinition {
    /**
     * Get the display name of this file definition.
     */
    String getDisplayName();

    /**
     * Gets the game file associated with the file definition.
     */
    SCGameFile<?> getGameFile();

    /**
     * Gets the full file path to the file.
     */
    String getFullFilePath();

    /**
     * Returns true iff there is a full file path.
     */
    default boolean hasFullFilePath() {
        return getFullFilePath() != null;
    }

    /**
     * Gets the index entry in the MWI corresponding to this file definition, if there is one.
     * This will return null if the file has no corresponding MWI resource entry.
     */
    MWIResourceEntry getIndexEntry();

    /**
     * Gets a cached Logger object usable for all logging for the file.
     */
    Logger getLogger();

    /**
     * Gets the Logger string identifying all logging for the file.
     */
    String getLoggerString();

    /**
     * Returns true iff the file is compressed using PowerPacker.
     */
    boolean isCompressed();
}