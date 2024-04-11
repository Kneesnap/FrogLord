package net.highwayfrogs.editor.games.generic;

/**
 * Represents a unique game.
 * Provides a way to work with game types which are not currently loaded.
 * Created by Kneesnap on 4/10/2024.
 */
public interface IGameType {
    /**
     * Gets the display name of the game.
     */
    String getDisplayName();

    /**
     * Creates a new instance of the game.
     */
    GameInstance createGameInstance();
}