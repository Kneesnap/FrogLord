package net.highwayfrogs.editor.games.greatquest.entity;

/**
 * Represents the _kcAxisType enum.
 * Created by Kneesnap on 1/4/2021.
 */
public enum kcAxisType {
    X,
    Y,
    Z,
    NX,
    NY,
    NZ,
    ALL;

    /**
     * Gets the kcAxisType corresponding to the provided value.
     * @param value     The value to lookup.
     * @param allowNull If null is allowed.
     * @return axisType
     */
    public static kcAxisType getType(int value, boolean allowNull) {
        if (value < 0 || value >= values().length) {
            if (allowNull)
                return null;

            throw new RuntimeException("Couldn't determine the kcAxisType from value " + value + ".");
        }

        return values()[value];
    }
}