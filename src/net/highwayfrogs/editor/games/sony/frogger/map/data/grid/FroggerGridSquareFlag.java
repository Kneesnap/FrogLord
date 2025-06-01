package net.highwayfrogs.editor.games.sony.frogger.map.data.grid;

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
public enum FroggerGridSquareFlag {
    // Flags with FroggerGridSquareFlagTarget.BOTH have been manually verified to support both.
    // Often time this label is applied when no form uses the flag in the vanilla game, but we still want to support it.
    USABLE(Constants.BIT_FLAG_0, FroggerGridSquareFlagTarget.BOTH, Color.GREEN, "Jumping to this tile is permitted (Even if there's no collision)"), // 0x01
    SAFE(Constants.BIT_FLAG_1, FroggerGridSquareFlagTarget.BOTH, Color.BLUE, "Standard land which is safe to stand on."), // 0x02
    DEADLY(Constants.BIT_FLAG_2, FroggerGridSquareFlagTarget.BOTH, Color.RED, "Kills the player using the generic flop or drown death."), // 0x04
    WATER(Constants.BIT_FLAG_3, FroggerGridSquareFlagTarget.BOTH, Color.CYAN, "Drowns the player."), // 0x08.
    SLIPPY(Constants.BIT_FLAG_4, FroggerGridSquareFlagTarget.GRID_ONLY, Color.PINK, "The frog will slide in the direction they are facing. (Sewer Levels)"), // 0x10, Frogger slides around. (Doesn't do anything on form)
    BOUNCY(Constants.BIT_FLAG_5, FroggerGridSquareFlagTarget.BOTH, Color.MAGENTA, "Bounces the player. (Unused)"), // 0x20
    CHECKPOINT(Constants.BIT_FLAG_6, FroggerGridSquareFlagTarget.FORM_ONLY, Color.YELLOW, "The player will collect the entity as a checkpoint."), // 0x40
    FREEFORM_SLIPPY(Constants.BIT_FLAG_7, FroggerGridSquareFlagTarget.GRID_ONLY, Color.PINK, "The frog will slide as seen in Frogger Goes Skiing."), // 0x80
    SOFT_GROUND(Constants.BIT_FLAG_8, FroggerGridSquareFlagTarget.BOTH, Color.YELLOW, "The player will not die from fall damage on this square."), // 0x100
    EXTENDED_HOP_HEIGHT(Constants.BIT_FLAG_9, FroggerGridSquareFlagTarget.GRID_ONLY, Color.ORANGE, "Allows the player to super-jump higher than usual while standing a square with this flag."), // 0x200 (This has only been observed in the grid)
    SIMPLE_SLIPPY(Constants.BIT_FLAG_10, FroggerGridSquareFlagTarget.GRID_ONLY, Color.PINK, "The frog will slide in the direction faced with control. (Airshow Antics)"), // 0x400
    CLIFF_DEATH(Constants.BIT_FLAG_11, FroggerGridSquareFlagTarget.BOTH, Color.RED, "Kills the player with the cliff roll animation."), // 0x800 This does work if used on a form, but forms use their own way of specifying deaths so this goes unused for forms.
    POP_DEATH(Constants.BIT_FLAG_12, FroggerGridSquareFlagTarget.BOTH, Color.RED, "Kills the player with the pop animation. (Cacti)\nWhile in freeform slippy mode, this causes pit ramp deaths."), // 0x1000 This does work if used on a form, but forms use their own way of specifying deaths so this goes unused for forms.
    // Flag 13 seems completely unused, and it doesn't appear set in any known location.
    FALL(Constants.BIT_FLAG_14, FroggerGridSquareFlagTarget.FORM_ONLY, Color.YELLOW), // 0x4000, This is how you can fall off the bird flock in "Time Flies". It was added in Build 56.
    WALL_BOUNCE_VERTICAL(Constants.BIT_FLAG_14, FroggerGridSquareFlagTarget.GRID_ONLY, Color.MAGENTA, "This enables a movement prevention check for entity walls like webs, fences, etc."), // 0x4000
    WALL_BOUNCE_HORIZONTAL(Constants.BIT_FLAG_15, FroggerGridSquareFlagTarget.GRID_ONLY, Color.MAGENTA, "This enables a movement prevention check for entity walls like webs, fences, etc."), // 0x8000
    ENTITY_NORTH(Constants.BIT_FLAG_16, FroggerGridSquareFlagTarget.GRID_ONLY, null, "Indicates an entity path intersects with the tile to the north. (Unused by the game)"), // 0x10000, "Time Flies" seems to have a lot of these.
    ENTITY_EAST(Constants.BIT_FLAG_17, FroggerGridSquareFlagTarget.GRID_ONLY, null, "Indicates an entity path intersects with the tile to the east. (Unused by the game)"), // 0x20000
    ENTITY_SOUTH(Constants.BIT_FLAG_18, FroggerGridSquareFlagTarget.GRID_ONLY, null, "Indicates an entity path intersects with the tile to the south. (Unused by the game)"), // 0x40000
    ENTITY_WEST(Constants.BIT_FLAG_19, FroggerGridSquareFlagTarget.GRID_ONLY, null, "Indicates an entity path intersects with the tile to the west. (Unused by the game)"); // 0x80000

    private final int bitFlagMask;
    private final FroggerGridSquareFlagTarget target;
    private final Color uiColor;
    private final String tooltipDescription;
    public static final int FORM_VALIDATION_BIT_MASK = 0b0101111111111111;
    public static final int GRID_VALIDATION_BIT_MASK = 0b11111101111111111111;

    FroggerGridSquareFlag(int bitFlagMask, FroggerGridSquareFlagTarget target, Color uiColor) {
        this(bitFlagMask, target, uiColor, null);
    }

    /**
     * Returns true iff the flag goes unused in the retail game.
     * The flags may still carry meaning. Bouncy for example has unused behavior which works if applied.
     * The directional entity flags appear to be fully unused.
     */
    public boolean isUnused() {
        return this == BOUNCY || this == ENTITY_NORTH || this == ENTITY_SOUTH || this == ENTITY_EAST || this == ENTITY_WEST;
    }

    /**
     * Test if this flag is active for form data.
     */
    public boolean isFormData() {
        return (this.target == FroggerGridSquareFlagTarget.BOTH || this.target == FroggerGridSquareFlagTarget.FORM_ONLY);
    }

    /**
     * Test if this flag is active for land grid data.
     */
    public boolean isLandGridData() {
        return (this.target == FroggerGridSquareFlagTarget.BOTH || this.target == FroggerGridSquareFlagTarget.GRID_ONLY);
    }

    /**
     * Test if this flag is part of a simple reaction shorthand.
     */
    public boolean isPartOfSimpleReaction() {
        return isLandGridData() && (this.bitFlagMask & FroggerGridSquareReaction.REACTION_BIT_MASK) == this.bitFlagMask;
    }

    /**
     * Calculate a bitMask containing
     * @param flags the flags to create the bit mask from
     * @return flagBitMask
     */
    public static int getFlagMask(FroggerGridSquareFlag... flags) {
        if (flags == null)
            throw new NullPointerException("flags");

        int bitMask = 0;
        for (int i = 0; i < flags.length; i++)
            bitMask |= flags[i].getBitFlagMask();

        return bitMask;
    }

    public enum FroggerGridSquareFlagTarget {
        GRID_ONLY,
        FORM_ONLY,
        BOTH
    }
}