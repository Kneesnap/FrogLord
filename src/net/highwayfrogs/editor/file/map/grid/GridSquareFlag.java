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
    CHECKPOINT(Constants.BIT_FLAG_6, Color.YELLOW), // Checkpoint here? Skip this one.
    SLIPPY_CONTROLS(Constants.BIT_FLAG_7, Color.PINK), // Slippy but frogger can control.
    SOFT_GROUND(Constants.BIT_FLAG_8, Color.YELLOW), // Frog won't die from fall damage.
    EXTENDED_HOP_HEIGHT(Constants.BIT_FLAG_9, Color.ORANGE), // Unused. Believe this was supposed to extend the height the frog can super jump at. But, it's not used.
    SIMPLE_SLIPPY(Constants.BIT_FLAG_10, Color.PINK), // Not sure how this differs from the first slippy flag.
    CLIFF_DEATH(Constants.BIT_FLAG_11, Color.RED), // Kill the frog with a cliff death.
    POP_DEATH(Constants.BIT_FLAG_12, Color.RED), // Frog does a polygon-pop death.
    // Flag 13 seems completely unused, and it doesn't appear set in any known location.
    FALL(Constants.BIT_FLAG_14, Color.YELLOW), // This is how you can fall off the bird flock in "Time Flies". It was added in Build 56.
    WALL_BOUNCE_VERTICAL(Constants.BIT_FLAG_14, Color.MAGENTA), // This enables a movement check for walls like webs, fences, etc.
    WALL_BOUNCE_HORIZONTAL(Constants.BIT_FLAG_15, Color.MAGENTA), // This enables a movement check for walls like webs, fences, etc.
    ENTITY_NORTH(Constants.BIT_FLAG_16, null), // Seems to have been used by mappy, but not the game. Not entirely sure what this is, but "Time Flies" seems to have a lot of it.
    ENTITY_EAST(Constants.BIT_FLAG_17, null), // Seems to have been used by mappy, but not the game.
    ENTITY_SOUTH(Constants.BIT_FLAG_18, null), // Seems to have been used by mappy, but not the game.
    ENTITY_WEST(Constants.BIT_FLAG_19, null); // Seems to have been used by mappy, but not the game.

    private final int flag;
    private final Color uiColor;

    /**
     * Test if this flag is active for form data.
     */
    public boolean isFormData() {
        return this != WALL_BOUNCE_VERTICAL && this != WALL_BOUNCE_HORIZONTAL && this != ENTITY_NORTH
                && this != ENTITY_EAST && this != ENTITY_SOUTH && this != ENTITY_WEST;
    }

    /**
     * Test if this flag is active for land data.
     */
    public boolean isLandData() {
        return this != FALL;
    }
}
