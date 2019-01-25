package net.highwayfrogs.editor.file.map.grid;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;

/**
 * Created by Kneesnap on 1/24/2019.
 */
@Getter
@AllArgsConstructor
public enum GridSquareFlag {
    USABLE(Constants.BIT_FLAG_0), // Frog can jump here.
    SAFE(Constants.BIT_FLAG_1), // Standard land.
    DEADLY(Constants.BIT_FLAG_2), // Frogger dies.
    WATER(Constants.BIT_FLAG_3), // Frogger drowns here.
    SLIPPY(Constants.BIT_FLAG_4), // Frogger slides around.
    BOUNCY(Constants.BIT_FLAG_5), // Frogger bounces.
    CHECKPOINT(Constants.BIT_FLAG_6), // Checkpoint here? Skip this one.
    SLIPPY_CONTROL(Constants.BIT_FLAG_7), // Slippy but frogger can control.
    SOFT_GROUND(Constants.BIT_FLAG_8), // Frog won't die from fall damage.
    EXTEND_HOP_HEIGHT(Constants.BIT_FLAG_9), // Unused. Believe this was supposed to extend the height the frog can super jump at. But, it's not used.
    SIMPLE_SLIPPY(Constants.BIT_FLAG_10), // Not sure how this differs from the first slippy flag.
    CLIFF_DEATH(Constants.BIT_FLAG_11), // Kill the frog with a cliff death.
    POP_DEATH(Constants.BIT_FLAG_12); // Frog does a polygon-pop death.

    private final int flag;
}
