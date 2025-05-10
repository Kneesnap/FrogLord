package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.cave.BreakingFloorEntityData.BreakingFloorDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents entity data for the cave breaking floor entity.
 * Created by Kneesnap on 12/18/2023.
 */
@Getter
public class BreakingFloorEntityData extends MatrixEntityData<BreakingFloorDifficultyData> {
    public BreakingFloorEntityData(OldFroggerMapEntity entity) {
        super(entity, BreakingFloorDifficultyData::new);
    }

    @Getter
    public static class BreakingFloorDifficultyData extends OldFroggerDifficultyData {
        private int time = 300;

        public BreakingFloorDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.time = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.time);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Time", this.time, newValue -> this.time = newValue, 30);
        }
    }
}