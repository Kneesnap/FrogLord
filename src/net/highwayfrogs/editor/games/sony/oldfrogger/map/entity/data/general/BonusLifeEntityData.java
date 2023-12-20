package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general.BonusLifeEntityData.BonusLifeDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents bonus life entity data.
 * Created by Kneesnap on 12/16/2023.
 */
public class BonusLifeEntityData extends MatrixEntityData<BonusLifeDifficultyData> {
    public BonusLifeEntityData(OldFroggerMapEntity entity) {
        super(entity, BonusLifeDifficultyData::new);
    }

    @Getter
    public static class BonusLifeDifficultyData extends OldFroggerDifficultyData {
        private int livesGiven = 1;

        public BonusLifeDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.livesGiven = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.livesGiven);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedInt("Lives Given", this.livesGiven, newValue -> this.livesGiven = newValue, 1, 0, 10);
        }
    }
}