package net.highwayfrogs.editor.games.konami.greatquest.generic;

/**
 * Represents the 'kcBlend' enum.
 * Created by Kneesnap on 8/22/2023.
 */
public enum kcBlend {
    ZERO, // 1
    ONE, // 2
    SRC_COLOR, // 3
    INV_SRC_COLOR, // 4
    SRC_ALPHA, // 5
    INV_SRC_ALPHA, // 6
    DEST_ALPHA, // 7
    INV_DEST_ALPHA, // 8
    DEST_COLOR, // 9
    INV_DEST_COLOR, // 10
    SRC_ALPHA_SAT, // 11
    BOTH_SRC_ALPHA, // 12
    BOTH_INV_SRC_ALPHA; // 13

    /**
     * Gets the value represented by the enum val.
     */
    public int getValue() {
        return ordinal() + 1;
    }

    /**
     * Gets the kcBlend corresponding to the provided value.
     * @param value     The value to lookup.
     * @param allowNull If null is allowed.
     * @return blendType
     */
    public static kcBlend getMode(int value, boolean allowNull) {
        if (value < 1 || value > values().length) {
            if (allowNull)
                return null;

            throw new RuntimeException("Couldn't determine the kcBlend from value " + value + ".");
        }

        return values()[value - 1];
    }
}