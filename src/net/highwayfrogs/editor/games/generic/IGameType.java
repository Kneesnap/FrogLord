package net.highwayfrogs.editor.games.generic;

import net.highwayfrogs.editor.utils.Utils;

import java.io.InputStream;
import java.net.URL;

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
     * Gets an identifier which can be used to identify the game uniquely.
     * Should be alphanumeric, since it may be used for a file name.
     */
    String getIdentifier();

    /**
     * Creates a new instance of the game.
     */
    GameInstance createGameInstance();

    /**
     * Gets an InputStream to files included for this specific game.
     * @param localPath the local path of the file to load
     * @return embeddedResourceStream
     */
    default InputStream getEmbeddedResourceStream(String localPath) {
        return Utils.getResourceStream("games/" + getIdentifier() + "/" + localPath);
    }

    /**
     * Gets a URL to files included for this specific game.
     * @param localPath the local path of the file to load
     * @return embeddedResourceURL
     */
    default URL getEmbeddedResourceURL(String localPath) {
        return Utils.getResourceURL("games/" + getIdentifier() + "/" + localPath);
    }
}