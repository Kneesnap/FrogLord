package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest.BreakingBranchEntityData.BreakingBranchDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a breaking branch entity.
 * Created by Kneesnap on 12/17/2023.
 */
public class BreakingBranchEntityData extends MatrixEntityData<BreakingBranchDifficultyData> {
    public BreakingBranchEntityData(OldFroggerMapEntity entity) {
        super(entity, BreakingBranchDifficultyData::new);
    }

    @Getter
    public static class BreakingBranchDifficultyData extends OldFroggerDifficultyData {
        private int breakingDelay = 150;
        private int fallingSpeed = 2184;

        public BreakingBranchDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.breakingDelay = reader.readUnsignedShortAsInt();
            this.fallingSpeed = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.breakingDelay);
            writer.writeUnsignedShort(this.fallingSpeed);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Breaking Delay (secs)", this.breakingDelay, newValue -> this.breakingDelay = newValue, 30, 0, 3000);
            editor.addUnsignedFixedShort("Falling Speed (grid/sec)", this.fallingSpeed, newValue -> this.fallingSpeed = newValue, 2184);
        }
    }
}