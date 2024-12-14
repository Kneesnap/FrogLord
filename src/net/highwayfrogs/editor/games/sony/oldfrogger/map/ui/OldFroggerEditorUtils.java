package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerReactionType;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
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

            // The entity data here can differ between different scenarios. For now this editor will do, I don't see it being hyper necessary to deal with this in the future.
            editor.addUnsignedFixedShort("Reaction Data " + (i + 1), reactionData[i], newValue -> reactionData[index] = newValue, 1);
        }
    }

    /**
     * Adds the difficulty editor.
     * @param editor The editor to add the UI for.
     * @param difficulty The integer difficulty value.
     * @param setter Applies the new value.
     * @param allowEditingLevels If difficulty levels should be toggled.
     */
    public static void addDifficultyEditor(GUIEditorGrid editor, int difficulty, Consumer<Integer> setter, boolean allowEditingLevels, Runnable updateEditor) {
        final Label label = editor.addLabel("Difficulty Value", NumberUtils.toHexString(difficulty));
        AtomicInteger currentDifficulty = new AtomicInteger(difficulty);

        // Add handler to update the label.
        Consumer<Integer> ourSetter = newValue -> {
            currentDifficulty.set(newValue);
            if (label != null)
                label.setText(NumberUtils.toHexString(newValue));

            if (setter != null)
                setter.accept(newValue);
        };

        BiConsumer<Integer, Boolean> setFlagState = (difficultyLevel, newState) -> {
            ourSetter.accept(setDifficultyLevelEnabled(currentDifficulty.get(), difficultyLevel, newState));

            if (updateEditor != null && difficultyLevel == OldFroggerGameInstance.DIFFICULTY_FLAG_STATIC_MODEL)
                updateEditor.run();
        };

        // Add checks.
        addFlag(editor, "Unknown Reserved Flag 1", difficulty, OldFroggerGameInstance.DIFFICULTY_FLAG_UNKNOWN_1, false, setFlagState);
        addFlag(editor, "Unknown Reserved Flag 2", difficulty, OldFroggerGameInstance.DIFFICULTY_FLAG_UNKNOWN_2, false, setFlagState);
        addFlag(editor, "Unknown Reserved Flag 3", difficulty, OldFroggerGameInstance.DIFFICULTY_FLAG_UNKNOWN_3, false, setFlagState);
        addFlag(editor, "Checkpoint", difficulty, OldFroggerGameInstance.DIFFICULTY_FLAG_CHECKPOINT, true, setFlagState);
        addFlag(editor, "Wait for Trigger", difficulty, OldFroggerGameInstance.DIFFICULTY_FLAG_INACTIVE_DEFAULT, true, setFlagState);
        addFlag(editor, "Static Model (No Data/Updates)", difficulty, OldFroggerGameInstance.DIFFICULTY_FLAG_STATIC_MODEL, true, setFlagState);

        // Allow editing difficulty levels.
        if (allowEditingLevels && !isDifficultyLevelEnabled(difficulty, OldFroggerGameInstance.DIFFICULTY_FLAG_STATIC_MODEL)) {
            for (int i = 0; i < OldFroggerGameInstance.DIFFICULTY_LEVELS; i++) {
                final int difficultyLevel = i;
                boolean isEnabled = isDifficultyLevelEnabled(difficulty, i);

                editor.addCheckBox("Difficulty Level " + (i + 1) + " Enabled", isEnabled,
                        newState -> setFlagState.accept(difficultyLevel, newState));
            }
        }
    }

    private static void addFlag(GUIEditorGrid editor, String label, int difficulty, int flag, boolean allowIfUnset, BiConsumer<Integer, Boolean> setFlagState) {
        boolean isEnabled = isDifficultyLevelEnabled(difficulty, flag);
        if (allowIfUnset || isEnabled) {
            CheckBox box = editor.addCheckBox(label, isEnabled, newState -> setFlagState.accept(flag, newState));

            // If the static model flag is set, and this isn't the static model flag, disable the checkbox.
            if (isDifficultyLevelEnabled(difficulty, OldFroggerGameInstance.DIFFICULTY_FLAG_STATIC_MODEL) && flag != OldFroggerGameInstance.DIFFICULTY_FLAG_STATIC_MODEL)
                box.setDisable(true);
        }
    }

    /**
     * Test if a difficulty level is enabled.
     * @param level The level to test. (0 indexed)
     * @return If the difficulty level is enabled.
     */
    public static boolean isDifficultyLevelEnabled(int difficulty, int level) {
        if (level < 0 || level > OldFroggerGameInstance.DIFFICULTY_FLAG_STATIC_MODEL)
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
        if (level < 0 || level > OldFroggerGameInstance.DIFFICULTY_FLAG_STATIC_MODEL)
            throw new IllegalArgumentException("Invalid difficulty level: " + level);

        int mask = (1 << level);
        if (newState) {
            return difficulty | mask;
        } else {
            return difficulty & ~mask;
        }
    }
}
