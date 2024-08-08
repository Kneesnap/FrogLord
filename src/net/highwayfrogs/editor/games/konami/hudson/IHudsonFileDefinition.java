package net.highwayfrogs.editor.games.konami.hudson;

import net.highwayfrogs.editor.utils.IGameObject;

/**
 * Represents the file definition of a Frogger game developed by Hudson.
 * Created by Kneesnap on 8/4/2024.
 */
public interface IHudsonFileDefinition extends IGameObject {

    /**
     * Gets the game instance for the file.
     */
    HudsonGameInstance getGameInstance();

    /**
     * Gets the individual file name.
     */
    String getFileName();

    /**
     * Gets the full file name/path.
     */
    String getFullFileName();
}