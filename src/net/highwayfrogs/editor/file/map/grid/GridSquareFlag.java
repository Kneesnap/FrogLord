package net.highwayfrogs.editor.file.map.grid;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;

import java.awt.*;

/**
 * Represents flags found on grid squares.
 * Created by Kneesnap on 1/24/2019.
 */
@Getter
@AllArgsConstructor
public enum GridSquareFlag {
    CAN_HOP(Constants.BIT_FLAG_0, Color.GREEN), // 0x01, Frog can jump here.
    COLLISION(Constants.BIT_FLAG_1, Color.BLUE), // 0x02, Standard land.
    DEADLY(Constants.BIT_FLAG_2, Color.RED), // 0x04, Frogger dies.
    WATER(Constants.BIT_FLAG_3, Color.CYAN), // 0x08, Frogger drowns here.
    SLIPPY(Constants.BIT_FLAG_4, Color.PINK), // 0x10, Frogger slides around.
    BOUNCY(Constants.BIT_FLAG_5, Color.MAGENTA), // 0x20, Frogger bounces.
    CHECKPOINT(Constants.BIT_FLAG_6, Color.YELLOW), // 0x40, Checkpoint here? Skip this one.
    SLIPPY_CONTROLS(Constants.BIT_FLAG_7, Color.PINK), // 0x80, Slippy but frogger can control.
    SOFT_GROUND(Constants.BIT_FLAG_8, Color.YELLOW), // 0x100, Frog won't die from fall damage.
    EXTENDED_HOP_HEIGHT(Constants.BIT_FLAG_9, Color.ORANGE), // 0x200, Unused. Believe this was supposed to extend the height the frog can super jump at. But, it's not used.
    SIMPLE_SLIPPY(Constants.BIT_FLAG_10, Color.PINK), // 0x400, Not sure how this differs from the first slippy flag.
    CLIFF_DEATH(Constants.BIT_FLAG_11, Color.RED), // 0x800, Kill the frog with a cliff death.
    POP_DEATH(Constants.BIT_FLAG_12, Color.RED), // 0x1000, Frog does a polygon-pop death.
    // Flag 13 seems completely unused, and it doesn't appear set in any known location. TODO: Actually hold on, this might be set in form data padding? unclear.
    FALL(Constants.BIT_FLAG_14, Color.YELLOW), // 0x4000 (Form Specific), This is how you can fall off the bird flock in "Time Flies". It was added in Build 56.
    WALL_BOUNCE_VERTICAL(Constants.BIT_FLAG_14, Color.MAGENTA), // 0x4000 (Grid Specific), This enables a movement check for walls like webs, fences, etc.
    WALL_BOUNCE_HORIZONTAL(Constants.BIT_FLAG_15, Color.MAGENTA), // 0x8000 (Grid Specific), This enables a movement check for walls like webs, fences, etc.
    ENTITY_NORTH(Constants.BIT_FLAG_16, null), // 0x10000 (Grid Specific), Seems to have been used by mappy, but not the game. Not entirely sure what this is, but "Time Flies" seems to have a lot of it.
    ENTITY_EAST(Constants.BIT_FLAG_17, null), // 0x20000 (Grid Specific), Seems to have been used by mappy, but not the game.
    ENTITY_SOUTH(Constants.BIT_FLAG_18, null), // 0x40000 (Grid Specific), Seems to have been used by mappy, but not the game.
    ENTITY_WEST(Constants.BIT_FLAG_19, null); // 0x80000 (Grid Specific), Seems to have been used by mappy, but not the game.

    private final int flag;
    private final Color uiColor;
    public static final int FORM_VALIDATION_BIT_MASK = 0b101111111111111;
    public static final int GRID_VALIDATION_BIT_MASK = 0b11111101111111111111;

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