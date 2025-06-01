package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.util.Arrays;
import java.util.function.Function;

/**
 * This is a wrapper around entity data which can be different depending on the difficulty configuration.
 * Created by Kneesnap on 12/15/2023.
 */
public class OldFroggerDifficultyWrapper<TDifficultyData extends OldFroggerDifficultyData> extends SCGameData<OldFroggerGameInstance> {
    @Getter private final OldFroggerMapEntity entity;
    private final Function<OldFroggerMapEntity, TDifficultyData> dataMaker;
    private final Object[] difficultyArray = new Object[OldFroggerGameInstance.DIFFICULTY_LEVELS];

    public OldFroggerDifficultyWrapper(OldFroggerMapEntity entity, Function<OldFroggerMapEntity, TDifficultyData> dataMaker) {
        super(entity.getGameInstance());
        this.entity = entity;
        this.dataMaker = dataMaker;
    }

    @Override
    public void load(DataReader reader) {
        if (this.dataMaker == null)
            return; // If no data is created for this type, skip it.

        Arrays.fill(this.difficultyArray, null);
        for (int i = 0; i < OldFroggerGameInstance.DIFFICULTY_LEVELS; i++) {
            if (!this.entity.isDifficultyLevelEnabled(i))
                continue; // Difficulty level is disabled.

            TDifficultyData newData = this.dataMaker.apply(this.entity);
            newData.load(reader);
            reader.alignRequireEmpty(newData.getByteAlignment());
            this.difficultyArray[i] = newData;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void save(DataWriter writer) {
        for (int i = 0; i < OldFroggerGameInstance.DIFFICULTY_LEVELS; i++) {
            if (this.entity.isDifficultyLevelEnabled(i) && this.difficultyArray[i] != null) {
                TDifficultyData data = (TDifficultyData) this.difficultyArray[i];
                data.save(writer);
                writer.align(data.getByteAlignment());
            }
        }
    }

    @Override
    public ILogger getLogger() {
        return this.entity != null ? this.entity.getLogger() : super.getLogger();
    }

    /**
     * Gets difficulty data for the given level.
     * @param difficultyLevel The zero-indexed difficulty level.
     * @return difficultyData, if there is any.
     */
    @SuppressWarnings("unchecked")
    public TDifficultyData getDifficultyData(int difficultyLevel) {
        return difficultyLevel >= 0 && difficultyLevel < this.difficultyArray.length
                ? (TDifficultyData) this.difficultyArray[difficultyLevel] : null;
    }

    /**
     * Sets up the editor gui for difficulty data.
     * @param manager The manager to setup the data under.
     * @param editor  The ui context to setup the UI for.
     */
    @SuppressWarnings("unchecked")
    public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
        for (int i = 0; i < OldFroggerGameInstance.DIFFICULTY_LEVELS; i++) {
            final int difficultyLevel = i;
            editor.addSeparator();
            boolean isEnabled = this.entity.isDifficultyLevelEnabled(i);
            editor.addBoldLabel("Difficulty Level " + (i + 1) + " Data:");

            editor.addCheckBox("Difficulty Level " + (i + 1) + " Enabled", isEnabled, newState -> {
                this.entity.setDifficultyLevelEnabled(difficultyLevel, newState);

                // If we're enabling the difficulty level, and no previous data exists, setup new data.
                if (newState && this.difficultyArray[difficultyLevel] == null && this.dataMaker != null)
                    this.difficultyArray[difficultyLevel] = this.dataMaker.apply(this.entity);

                manager.updateEditor();
            }).setDisable(this.dataMaker == null);

            // Display UI for difficulty.
            if (isEnabled && this.difficultyArray[i] != null)
                ((TDifficultyData) this.difficultyArray[i]).setupEditor(manager, editor);
        }
    }

    @Getter
    public static abstract class OldFroggerDifficultyData extends SCGameData<OldFroggerGameInstance> {
        private final OldFroggerMapEntity entity;

        public OldFroggerDifficultyData(OldFroggerMapEntity entity) {
            super(entity.getGameInstance());
            this.entity = entity;
        }

        @Override
        public ILogger getLogger() {
            return this.entity != null ? this.entity.getLogger() : super.getLogger();
        }

        /**
         * Sets up the UI editor for the entity data.
         * @param manager The manager to create the ui for.
         * @param editor The editor context to setup ui under.
         */
        public abstract void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor);

        /**
         * Get the number of bytes this data is aligned to.
         */
        public int getByteAlignment() {
            return 4;
        }
    }

    /**
     * Represents no / null empty data.
     */
    public static class OldFroggerNullDifficultyData extends OldFroggerDifficultyData {

        // Don't allow this constructor to be called.
        private OldFroggerNullDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            // Do nothing.
        }

        @Override
        public void load(DataReader reader) {
            // Do nothing.
        }

        @Override
        public void save(DataWriter writer) {
            // Do nothing.
        }
    }
}