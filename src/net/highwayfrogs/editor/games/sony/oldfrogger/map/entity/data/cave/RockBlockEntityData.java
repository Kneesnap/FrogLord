package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.cave.RockBlockEntityData.RockBlockDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data for the rock block entity.
 * Created by Kneesnap on 12/17/2023.
 */
public class RockBlockEntityData extends MatrixEntityData<RockBlockDifficultyData> {
    public RockBlockEntityData(OldFroggerMapEntity entity) {
        super(entity, RockBlockDifficultyData::new);
    }

    @Getter
    public static class RockBlockDifficultyData extends OldFroggerDifficultyData {
        private boolean active;

        public RockBlockDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            int activeValue = reader.readInt();
            if (activeValue == 1) {
                this.active = true;
            } else if (activeValue == 0) {
                this.active = false;
            } else {
                throw new RuntimeException("Unexpected rock block active state " + activeValue + ", when only 0 and 1 are known to be valid.");
            }
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.active ? 1 : 0);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addCheckBox("Active", this.active, newValue -> this.active = newValue);
        }
    }
}