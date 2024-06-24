package net.highwayfrogs.editor.games.sony.frogger.map.data.form;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquareFlag;

import java.awt.*;

/**
 * Implements the different settings which can be applied to a form grid square.
 * Created by Kneesnap on 6/14/2024.
 */
@Getter
public enum FroggerMapFormSquareReaction {
    SAFE(Color.GREEN, "Safe", FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.SAFE),
    DEADLY(Color.RED, "Deadly", FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.DEADLY), // Uses the death type in the form book.
    CHECKPOINT(Color.YELLOW, "Checkpoint", FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.CHECKPOINT),
    FALL(Color.PINK, "Fall", FroggerGridSquareFlag.FALL),
    SAFE_WITH_SOFT_GROUND(Color.GREEN, "Safe (w/Soft Ground)", FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.SAFE, FroggerGridSquareFlag.SOFT_GROUND),
    INTERACT_GOLD_FROG(Color.YELLOW, "Interact (Gold Frog)", FroggerGridSquareFlag.USABLE), // Interacts with the entity. Examples: 'ORG_BABY_FROG', 'GEN_GOLD_FROG'
    WATER(Color.CYAN, "Water (Unused)", FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.WATER), // Unused in the vanilla game.
    NONE(Color.BLACK, "None"), // Also seen on 'GEN_GOLD_FROG' in places like DES4.MAP
    BOUNCY(Color.PINK, "Bouncy", FroggerGridSquareFlag.USABLE, FroggerGridSquareFlag.BOUNCY);

    private final Color previewColor;
    private final String displayName;
    private final FroggerGridSquareFlag[] gridSquareFlags;
    private final short gridSquareFlagBitMask;

    FroggerMapFormSquareReaction(Color color, String displayName, FroggerGridSquareFlag... flags) {
        this.previewColor = color;
        this.displayName = displayName;
        this.gridSquareFlags = flags;

        // Calculate & apply bit mask.
        short gridSquareFlagBitMask = 0;
        for (int i = 0; i < flags.length; i++)
            gridSquareFlagBitMask |= (short) flags[i].getBitFlagMask();
        this.gridSquareFlagBitMask = gridSquareFlagBitMask;
    }

    /**
     * Gets the reaction from a bit-flags value.
     * @param reactionFlags the reaction flags value to resolve
     * @return squareReaction, or null if the bits do not correspond to a known reaction
     */
    public static FroggerMapFormSquareReaction getReactionFromFlags(short reactionFlags) {
        for (int i = 0; i < values().length; i++) {
            FroggerMapFormSquareReaction squareReaction = values()[i];
            if (squareReaction.getGridSquareFlagBitMask() == reactionFlags)
                return squareReaction;
        }

        return null;
    }
}