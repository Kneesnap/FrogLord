package net.highwayfrogs.editor.games.renderware;

import net.highwayfrogs.editor.gui.ImageResource;

/**
 * Represents a RenderWare stream section definition.
 * Created by Kneesnap on 8/11/2024.
 */
public interface IRwStreamSectionType {
    /**
     * Gets the type ID uniquely identifying the stream section.
     */
    int getTypeId();

    /**
     * Gets the display name of the section.
     * Usually taken directly from the gta wikis.
     */
    String getDisplayName();

    /**
     * Represents the icon used to represent the stream section.
     */
    ImageResource getIcon();
}