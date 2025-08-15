package net.highwayfrogs.editor.games.sony.shared;

import java.util.List;

/**
 * Represents a file which uses textures.
 * Created by Kneesnap on 8/14/2025.
 */
public interface ISCTextureUser {
    /**
     * Gets a list of all texture IDs used by this object.
     */
    List<Short> getUsedTextureIds();
}
