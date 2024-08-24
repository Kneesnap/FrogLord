package net.highwayfrogs.editor.games.renderware;

import net.highwayfrogs.editor.gui.ImageResource;

/**
 * Represents a RenderWare stream chunk definition.
 * Created by Kneesnap on 8/11/2024.
 */
public interface IRwStreamChunkType {
    /**
     * Gets the type ID uniquely identifying the stream chunk.
     */
    int getTypeId();

    /**
     * Gets the display name of the chunk.
     * Usually taken directly from the gta wikis.
     */
    String getDisplayName();

    /**
     * Represents the icon used to represent the stream chunk.
     */
    ImageResource getIcon();

    /**
     * Gets the display importance for the particular type.
     * Can be used in different scenarios where we'd like to select something to display, such as an icon, but have multiple files.
     */
    RwStreamChunkTypeDisplayImportance getDisplayImportance();

    enum RwStreamChunkTypeDisplayImportance {
        LOWEST, LOW, NORMAL, HIGH, HIGHEST
    }
}