package net.highwayfrogs.editor.file.map.grid;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;

import java.awt.*;

/**
 * Created by Kneesnap on 1/24/2019.
 */
@Getter
@AllArgsConstructor
public enum GridSquareFlag {
    CAN_HOP(Constants.BIT_FLAG_0, Color.GREEN), // Frog can jump here.
    COLLISION(Constants.BIT_FLAG_1, Color.BLUE), // Standard land.
    DEADLY(Constants.BIT_FLAG_2, Color.RED), // Frogger dies.
    WATER(Constants.BIT_FLAG_3, Color.CYAN), // Frogger drowns here.
    SLIPPY(Constants.BIT_FLAG_4, Color.PINK), // Frogger slides around.
    BOUNCY(Constants.BIT_FLAG_5, Color.MAGENTA), // Frogger bounces.
    CHECKPOINT(Constants.BIT_FLAG_6, Color.GREEN), // Checkpoint here? Skip this one.
    SLIPPY_CONTROLS(Constants.BIT_FLAG_7, Color.PINK), // Slippy but frogger can control.
    SOFT_GROUND(Constants.BIT_FLAG_8, Color.YELLOW), // Frog won't die from fall damage.
    EXTENDED_HOP_HEIGHT(Constants.BIT_FLAG_9, Color.ORANGE), // Unused. Believe this was supposed to extend the height the frog can super jump at. But, it's not used.
    SIMPLE_SLIPPY(Constants.BIT_FLAG_10, Color.PINK), // Not sure how this differs from the first slippy flag.
    CLIFF_DEATH(Constants.BIT_FLAG_11, Color.RED), // Kill the frog with a cliff death.
    POP_DEATH(Constants.BIT_FLAG_12, Color.RED); // Frog does a polygon-pop death.

    private final int flag;
    private final Color uiColor;
}
