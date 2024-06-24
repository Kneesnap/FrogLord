package net.highwayfrogs.editor.games.sony.shared.misc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;

/**
 * Represents types of MR_LIGHT.
 * Frogger was built with OpenInventor, but also Alias Wavefront (what became Maya).
 * Maya had these lighting options: https://www.expertrating.com/courseware/mayacourse/MAYA-Cameras-Lighting-2.asp
 * However, OpenInventor is most likely what was used to manage lighting.
 * Created by Kneesnap on 1/20/2019.
 */
@Getter
@AllArgsConstructor
public enum MRLightType {
    AMBIENT(Constants.BIT_FLAG_0), // Bumps up ambient color. viewport back color is increased by color value.
    PARALLEL(Constants.BIT_FLAG_1), // Rebuilds light and color matrices.
    POINT(Constants.BIT_FLAG_2), // Rebuilds color matrix.
    SPOT(Constants.BIT_FLAG_3); // This does not seem to be supported by the code, it seems to be baked when the map is exported. It might be spot, it might be area, not sure.

    private final int bitFlagMask;

    /**
     * Gets the MRLightType by its number.
     * @param type The number to get.
     * @return lightType
     */
    public static MRLightType getType(int type) {
        for (int i = 0; i < values().length; i++) {
            MRLightType lightType = values()[i];
            if (lightType.getBitFlagMask() == type)
                return lightType;
        }

        throw new RuntimeException("Unknown MRLightType: " + type);
    }
}