package net.highwayfrogs.editor.games.sony.shared.landscaper;

/**
 * A landscape atom is part of the landscape.
 * Created by Kneesnap on 7/17/2024.
 */
public interface ILandscapeComponent {
    /**
     * Gets the landscape which this is part of
     * @return landscape
     */
    Landscape getLandscape();

    /**
     * Returns true iff the component is currently registered to the landscape.
     */
    boolean isRegistered();
}
