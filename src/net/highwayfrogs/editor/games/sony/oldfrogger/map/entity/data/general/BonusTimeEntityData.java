package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general.BonusTimeEntityData.BonusTimeDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents bonus time entity data.
 * Created by Kneesnap on 12/16/2023.
 */
public class BonusTimeEntityData extends MatrixEntityData<BonusTimeDifficultyData> {
    public BonusTimeEntityData(OldFroggerMapEntity entity) {
        super(entity, BonusTimeDifficultyData::new);
    }

    @Getter
    public static class BonusTimeDifficultyData extends OldFroggerDifficultyData {
        private int timeGiven = 30;

        public BonusTimeDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.timeGiven = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.timeGiven);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedInt("Time Given", this.timeGiven, newValue -> this.timeGiven = newValue, 30, 0, 65535);
        }
    }
}