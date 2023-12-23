package net.highwayfrogs.editor.file.map.light;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;

/**
 * Represents types of MR_LIGHT.
 * Based on the lighting options Maya had https://www.expertrating.com/courseware/mayacourse/MAYA-Cameras-Lighting-2.asp
 * Frogger was built with OpenInventor, but also Alias Wavefront (what became Maya).
 * After this class was created, it was determined that OpenInventor is actually what we should base our lights on, not Maya.
 * Created by Kneesnap on 1/20/2019.
 */
@Getter
@AllArgsConstructor
public enum APILightType {
    AMBIENT(Constants.BIT_FLAG_0), // Bumps up ambient color. viewport back color is increased by color value.
    PARALLEL(Constants.BIT_FLAG_1), // Rebuilds light and color matrices.
    POINT(Constants.BIT_FLAG_2), // Rebuilds color matrix.
    SPOT(Constants.BIT_FLAG_3); // This does not seem to be supported by the code, it seems to be baked when the map is exported. It might be spot, it might be area, not sure.

    private final int flag;

    /**
     * Gets the APILightType by its number.
     * @param type The number to get.
     * @return lightType
     */
    public static APILightType getType(int type) {
        for (APILightType testType : values())
            if (testType.getFlag() == type)
                return testType;
        throw new RuntimeException("Unknown API LightType: " + type);
    }
}
