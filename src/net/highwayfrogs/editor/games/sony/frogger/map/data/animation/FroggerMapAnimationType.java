package net.highwayfrogs.editor.games.sony.frogger.map.data.animation;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the available Frogger map animation types.
 * Created by Kneesnap on 6/15/2024.
 */
@Getter
@AllArgsConstructor
public enum FroggerMapAnimationType {
    UV(1),
    TEXTURE(2),
    BOTH(3);

    private final int flagBitMask;

    /**
     * Returns whether this animation type has a texture flipbook animation.
     */
    public boolean hasTextureAnimation() {
        return this == BOTH || this == TEXTURE;
    }

    /**
     * Returns whether this animation type has a UV animation.
     */
    public boolean hasUVAnimation() {
        return this == BOTH || this == UV;
    }

    /**
     * Get a FroggerMapAnimationType by its flag bit value.
     * @param typeValue The flag type value to lookup.
     * @return animationType
     */
    public static FroggerMapAnimationType getType(int typeValue) {
        for (int i = 0; i < values().length; i++) {
            FroggerMapAnimationType type = values()[i];
            if (type.getFlagBitMask() == typeValue)
                return type;
        }

        throw new RuntimeException("Unknown MAPAnimationType value: " + typeValue);
    }
}