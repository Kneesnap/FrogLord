package net.highwayfrogs.editor.games.konami.greatquest.proxy;

/**
 * Represents the ProxyReact enum.
 * Created by Kneesnap on 8/24/2023.
 */
public enum ProxyReact {
    NOTIFY, // This is sometimes called 'PENETRATE', as it lets the user through.
    HALT, // 'HALT' presumably presents the user from moving.
    SLIDE; // Causes the player to slide off the surface.

    /**
     * Gets the ProxyReact corresponding to the provided value.
     * @param value     The value to lookup.
     * @param allowNull If null is allowed.
     * @return proxyReact
     */
    public static ProxyReact getReaction(int value, boolean allowNull) {
        if (value < 0 || value >= values().length) {
            if (allowNull)
                return null;

            throw new RuntimeException("Couldn't determine the proxy reaction type from value " + value + ".");
        }

        return values()[value];
    }
}