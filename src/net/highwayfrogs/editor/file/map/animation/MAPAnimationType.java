package net.highwayfrogs.editor.file.map.animation;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a map animation type.
 * Created by Kneesnap on 1/21/2019.
 */
@Getter
@AllArgsConstructor
public enum MAPAnimationType {
    UV(1),
    TEXTURE(2),
    BOTH(3);

    private final int flag;

    /**
     * Get a MAPAnimationType by its flag.
     * @param type The flag to get the type by.
     * @return type
     */
    public static MAPAnimationType getType(int type) {
        for (MAPAnimationType testType : values())
            if (type == testType.getFlag())
                return testType;
        throw new RuntimeException("Unknown MAPAnimationType: " + type);
    }
}
