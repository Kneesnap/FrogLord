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
}