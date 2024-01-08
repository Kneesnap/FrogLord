package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerReactionType;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

import java.util.function.Consumer;

/**
 * Contains static utilities.
 * Created by Kneesnap on 12/22/2023.
 */
public class OldFroggerEditorUtils {
    /**
     * Sets up an editor for reaction data.
     * @param editor             The editor UI to add reaction data to.
     * @param reactionType       The current reaction type.
     * @param reactionData       The reaction data to add.
     * @param reactionTypeSetter A callback to set the new reaction type.
     */
    public static void setupReactionEditor(GUIEditorGrid editor, OldFroggerReactionType reactionType, int[] reactionData, Consumer<OldFroggerReactionType> reactionTypeSetter) {
        editor.addEnumSelector("Reaction Type", reactionType, OldFroggerReactionType.values(), false, reactionTypeSetter);
        for (int i = 0; i < reactionData.length; i++) {
            final int index = i;
            // TODO: This differs by type. Perhaps I can go through to figure out each reaction type has for values, and setup a nice editor for each.
            editor.addUnsignedFixedShort("Reaction Data " + (i + 1), reactionData[i], newValue -> reactionData[index] = newValue, 1);
        }
    }

    /**
     * Adds the difficulty editor.
     * @param editor     The editor to add the UI for.
     * @param difficulty The integer difficulty value.
     * @param setter     Applies the new value.
     */
    public static void addDifficultyEditor(GUIEditorGrid editor, int difficulty, Consumer<Integer> setter) {
        // TODO: Let's make a better difficulty editor.
        editor.addUnsignedFixedShort("Difficulty", difficulty, setter, 1);
    }

    /**
     * Test if a difficulty level is enabled.
     * @param level The level to test. (0 indexed)
     * @return If the difficulty level is enabled.
     */
    public static boolean isDifficultyLevelEnabled(int difficulty, int level) {
        if (level < 0 || level >= OldFroggerGameInstance.DIFFICULTY_LEVELS)
            return false;

        int mask = (1 << level);
        return (difficulty & mask) == mask;
    }

    /**
     * Set if a difficulty level is enabled.
     * @param level    The difficulty level to set. (0 indexed)
     * @param newState The new state of this difficulty level.
     */
    public static int setDifficultyLevelEnabled(int difficulty, int level, boolean newState) {
        if (level < 0 || level >= OldFroggerGameInstance.DIFFICULTY_LEVELS)
            throw new IllegalArgumentException("Invalid difficulty level: " + level);

        int mask = (1 << level);
        if (newState) {
            return difficulty | mask;
        } else {
            return difficulty & ~mask;
        }
    }
}
