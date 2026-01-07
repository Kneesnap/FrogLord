package net.highwayfrogs.editor.games.sony.frogger.map.data.grid;

import javafx.scene.control.Tooltip;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked.FroggerUIGridManager;
import net.highwayfrogs.editor.gui.texture.basic.RawColorTextureSource;
import net.highwayfrogs.editor.utils.FXUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Implements the different settings which can be applied to a map grid square.
 * Created by Kneesnap on 4/6/2025.
 */
@Getter
public enum FroggerGridSquareReaction {
    // Ordering is based on flag test priority by the game.
    CLIFF_DEATH("Cliff death", FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_RED, FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.CLIFF_DEATH),
    SAFE("Safe", FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_LIME_GREEN, FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.SAFE),
    SAFE_EXTENDED_HOP_HEIGHT("Safe (w/extended hop height)", FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_GREEN, FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.SAFE, FroggerGridSquareFlag.EXTENDED_HOP_HEIGHT),
    SAFE_WITH_SOFT_GROUND("Safe (w/soft ground)", FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_GREEN, FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.SAFE, FroggerGridSquareFlag.SOFT_GROUND),
    DEADLY("Deadly", FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_RED, FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.DEADLY), // In FOR, this is FLOP, in others this is DROWN.
    POP_DEATH("Pop death", FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_RED, FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.POP_DEATH),
    WATER_DEATH("Water death", FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_RED, FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.WATER),
    SLIPPY("Slippy", FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_YELLOW, FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.SLIPPY), // The frog will slide in the direction they are facing. (Sewer Levels)
    SIMPLE_SLIPPY("Simple Slippy", FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_YELLOW, FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.SIMPLE_SLIPPY), // The frog will slide in the direction faced with control. (Airshow Antics)
    FREEFORM_SLIPPY("Skiing (freeform slippy)", FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_YELLOW, FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.FREEFORM_SLIPPY), // The frog will slide as seen in Frogger Goes Skiing.
    FREEFORM_SLIPPY_RAMP("Ski ramp", FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_ORANGE, FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.FREEFORM_SLIPPY, FroggerGridSquareFlag.DEADLY), // The frog will slide as seen in Frogger Goes Skiing.
    FREEFORM_SLIPPY_PIT("Ski pit", FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_RED, FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.FREEFORM_SLIPPY, FroggerGridSquareFlag.POP_DEATH), // The frog will slide as seen in Frogger Goes Skiing.
    BOUNCY("Bouncy", FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_YELLOW, FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.BOUNCY),
    FREE_FALL("Free fall", FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_YELLOW, FroggerGridSquareFlag.USABLE),
    NONE("None", FroggerUIGridManager.MATERIAL_HIGHLIGHT_LIGHT_GREY);

    private final String displayName;
    private final FroggerGridSquareFlag[] gridSquareFlags;
    private final RawColorTextureSource highlightTextureSource;
    private final int gridSquareFlagBitMask;

    // These are the flags tested
    private static final FroggerGridSquareFlag[] REACTION_FLAGS_CHECKED = {
            FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.CLIFF_DEATH, FroggerGridSquareFlag.SAFE,
            FroggerGridSquareFlag.DEADLY, FroggerGridSquareFlag.POP_DEATH, FroggerGridSquareFlag.WATER,
            FroggerGridSquareFlag.SLIPPY, FroggerGridSquareFlag.SIMPLE_SLIPPY, FroggerGridSquareFlag.BOUNCY,
            FroggerGridSquareFlag.FREEFORM_SLIPPY, FroggerGridSquareFlag.SOFT_GROUND, FroggerGridSquareFlag.EXTENDED_HOP_HEIGHT
    };
    static final int REACTION_BIT_MASK = FroggerGridSquareFlag.getFlagMask(REACTION_FLAGS_CHECKED);

    // The static block ensures no reactions are missing from this list.
    /**
     * The order to display reactions, as the ordinal order is the order flags are tested by the game.
     */
    public static final List<FroggerGridSquareReaction> DISPLAY_ORDER = Arrays.asList(NONE,
            SAFE, SAFE_EXTENDED_HOP_HEIGHT, SAFE_WITH_SOFT_GROUND,
            DEADLY, CLIFF_DEATH, POP_DEATH, WATER_DEATH,
            FREE_FALL, BOUNCY,
            SLIPPY, SIMPLE_SLIPPY,
            FREEFORM_SLIPPY, FREEFORM_SLIPPY_RAMP, FREEFORM_SLIPPY_PIT);

    // ----------------------------------
    // The following flags are seen in the retail game, but some flags are ignored/overridden by others.
    // We've hardcoded them for now as resolving to the closest reaction.
    // In the future we could make the code automatically determine the reaction to use based on the order flags are checked.

    // Seen in ISLAND.MAP, in-game it resolves to SAFE/USABLE.
    private static final int REACTION_SPECIAL_MASK_ISLAND = FroggerGridSquareFlag.getFlagMask(
            FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.SAFE, FroggerGridSquareFlag.SLIPPY);

    // This is seen in various levels and seems to be the same as not NONE.
    private static final int REACTION_SPECIAL_MASK_JUST_DEADLY = FroggerGridSquareFlag.getFlagMask(FroggerGridSquareFlag.DEADLY);

    // Seen in FOR2.MAP near the green frog, it is possible to jump from the ground into the water tile without dying. (Don't jump from the higher-up logs)
    private static final int REACTION_SPECIAL_SAFE_WATER = FroggerGridSquareFlag.getFlagMask(
            FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.SAFE, FroggerGridSquareFlag.WATER);

    // Seen in FOR2.MAP near the gold frog, there are some water tiles you can't hop into (and thus, we treat it as none.)
    private static final int REACTION_SPECIAL_BLOCK_WATER = FroggerGridSquareFlag.getFlagMask(FroggerGridSquareFlag.WATER);

    // Seen in FOR2.MAP, SAFE seems to override. Had to be tested in Build 30 to use flight to reach the squares.
    private static final int REACTION_SPECIAL_SAFE_DEADLY = FroggerGridSquareFlag.getFlagMask(
            FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.SAFE, FroggerGridSquareFlag.DEADLY);

    // Seen in CAV3.MAP, does nothing different from SIMPLE_SLIPPY
    private static final int REACTION_SPECIAL_SLIPPY_EXTENDED = FroggerGridSquareFlag.getFlagMask(
            FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.EXTENDED_HOP_HEIGHT, FroggerGridSquareFlag.SIMPLE_SLIPPY);

    // Seen in CAV3.MAP, seems the same as FREEFORM_SLIPPY
    private static final int REACTION_SPECIAL_FREEFORM_SLIPPY_SAFE = FroggerGridSquareFlag.getFlagMask(
            FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.SAFE, FroggerGridSquareFlag.FREEFORM_SLIPPY);

    // Seen in CAV3.MAP, seems the same as CLIFF_DEATH
    private static final int REACTION_SPECIAL_FREEFORM_SLIPPY_CLIFF = FroggerGridSquareFlag.getFlagMask(
            FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.FREEFORM_SLIPPY, FroggerGridSquareFlag.CLIFF_DEATH);

    // Seen in CAV3.MAP, the reaction is very strange.
    // Sometimes Frogger will get flung across the map, other times he'll get stuck and can't hop to nearby tiles.
    // I believe the closest match is NONE, and at the very least this combination is bugged/is probably unintended.
    private static final int REACTION_SPECIAL_FREEFORM_SLIPPY = FroggerGridSquareFlag.getFlagMask(
            FroggerGridSquareFlag.FREEFORM_SLIPPY);

    // Seen in SKY1.MAP near the red frog, seems to just be CLIFF_DEATH
    private static final int REACTION_SPECIAL_SAFE_CLIFF = FroggerGridSquareFlag.getFlagMask(
            FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.SAFE, FroggerGridSquareFlag.CLIFF_DEATH);

    // Seen in DES5.MAP, this is POP_DEATH
    private static final int REACTION_SPECIAL_DEADLY_POP_DEATH = FroggerGridSquareFlag.getFlagMask(
            FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.DEADLY,
            FroggerGridSquareFlag.EXTENDED_HOP_HEIGHT, FroggerGridSquareFlag.POP_DEATH);

    // Seen in DES5.MAP, this is NONE.
    private static final int REACTION_SPECIAL_CLIFF_POP_DEATH = FroggerGridSquareFlag.getFlagMask(
            FroggerGridSquareFlag.CLIFF_DEATH, FroggerGridSquareFlag.POP_DEATH);

    // DES5.MAP has these on the long climb up the left side of the map.
    // My hypothesis was that Frogger would fall through tiles with these flags, but no, these flags are safe to step on. Not entirely sure why.
    private static final int REACTION_SPECIAL_EXTENDED_HOT_HEIGHT = FroggerGridSquareFlag.getFlagMask(
            FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.EXTENDED_HOP_HEIGHT);

    // SKY2.MAP in many prototype builds such as PSX Build 11 has this.
    private static final int REACTION_SPECIAL_SIMPLE_SLIPPY = FroggerGridSquareFlag.getFlagMask(
            FroggerGridSquareFlag.SIMPLE_SLIPPY);

    FroggerGridSquareReaction(String displayName, RawColorTextureSource highlightTextureSource, FroggerGridSquareFlag... flags) {
        this.highlightTextureSource = highlightTextureSource;
        this.displayName = displayName;
        this.gridSquareFlags = flags;

        // Calculate & apply bit mask.
        int gridSquareFlagBitMask = 0;
        for (int i = 0; i < flags.length; i++)
            gridSquareFlagBitMask |= flags[i].getBitFlagMask();
        this.gridSquareFlagBitMask = gridSquareFlagBitMask;
    }

    /**
     * Gets the FX UI Tooltip used for the reaction.
     */
    public Tooltip getTooltip() {
        return FXUtils.createTooltip(getTooltipDescription());
    }

    /**
     * Gets the description used for the tooltip describing this reaction.
     */
    public String getTooltipDescription() {
        switch (this) {
            case SAFE_EXTENDED_HOP_HEIGHT:
                return "The tile is safe, and the frog can make higher (non-super) jumps to adjacent tiles.";
            case SAFE_WITH_SOFT_GROUND:
                return "The tile is safe, and fall damage is disabled.";
            case FREEFORM_SLIPPY_RAMP:
                return "The area is treated as a ramp when skiing.";
            case FREEFORM_SLIPPY_PIT:
                return "The area is treated as a death pit when skiing.";
            case FREE_FALL:
                return "The frog will fall until either they die or land on something.";
            case NONE:
                return "The frog will be unable to reach the square.";
            default:
                for (int i = 0; i < this.gridSquareFlags.length; i++) {
                    FroggerGridSquareFlag testFlag = this.gridSquareFlags[i];
                    if (testFlag != FroggerGridSquareFlag.USABLE)
                        return testFlag.getTooltipDescription();
                }

                return null;
        }
    }

    /**
     * Gets the reaction from a bit-flags value.
     * @param reactionFlags the reaction flags value to resolve
     * @return squareReaction, or null if the bits do not correspond to a known reaction
     */
    public static FroggerGridSquareReaction getReactionFromFlags(int reactionFlags) {
        int maskedFlags = (reactionFlags & REACTION_BIT_MASK);
        if (maskedFlags == REACTION_SPECIAL_MASK_ISLAND || maskedFlags == REACTION_SPECIAL_SAFE_WATER || maskedFlags == REACTION_SPECIAL_SAFE_DEADLY) {
            return SAFE;
        } else if (maskedFlags == REACTION_SPECIAL_MASK_JUST_DEADLY || maskedFlags == REACTION_SPECIAL_BLOCK_WATER || maskedFlags == REACTION_SPECIAL_FREEFORM_SLIPPY || maskedFlags == REACTION_SPECIAL_CLIFF_POP_DEATH) {
            return NONE;
        } else if (maskedFlags == REACTION_SPECIAL_SLIPPY_EXTENDED || maskedFlags == REACTION_SPECIAL_SIMPLE_SLIPPY) {
            return SIMPLE_SLIPPY;
        } else if (maskedFlags == REACTION_SPECIAL_FREEFORM_SLIPPY_SAFE) {
            return FREEFORM_SLIPPY;
        } else if (maskedFlags == REACTION_SPECIAL_SAFE_CLIFF || maskedFlags == REACTION_SPECIAL_FREEFORM_SLIPPY_CLIFF) {
            return CLIFF_DEATH;
        } else if (maskedFlags == REACTION_SPECIAL_EXTENDED_HOT_HEIGHT) {
            return SAFE_EXTENDED_HOP_HEIGHT;
        } else if (maskedFlags == REACTION_SPECIAL_DEADLY_POP_DEATH) {
            return POP_DEATH;
        }

        for (int i = 0; i < values().length; i++) {
            FroggerGridSquareReaction squareReaction = values()[i];
            if (squareReaction.getGridSquareFlagBitMask() == maskedFlags)
                return squareReaction;
        }

        return null;
    }

    static {
        for (FroggerGridSquareReaction reaction : values())
            if (!DISPLAY_ORDER.contains(reaction))
                throw new ExceptionInInitializerError("DISPLAY_ORDER is expected to contain all FroggerGridSquareReaction values, but missed " + reaction + ".");
    }
}
