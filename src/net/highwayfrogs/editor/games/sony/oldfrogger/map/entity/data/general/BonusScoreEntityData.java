package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general.BonusScoreEntityData.BonusScoreDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents bonus score entity data.
 * Created by Kneesnap on 12/16/2023.
 */
public class BonusScoreEntityData extends MatrixEntityData<BonusScoreDifficultyData> {
    public BonusScoreEntityData(OldFroggerMapEntity entity) {
        super(entity, BonusScoreDifficultyData::new);
    }

    @Getter
    public static class BonusScoreDifficultyData extends OldFroggerDifficultyData {
        private int scoreGiven = 1000;

        public BonusScoreDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.scoreGiven = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.scoreGiven);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedInt("Score Given", this.scoreGiven, newValue -> this.scoreGiven = newValue, 1, 0, 65535);
        }
    }
}