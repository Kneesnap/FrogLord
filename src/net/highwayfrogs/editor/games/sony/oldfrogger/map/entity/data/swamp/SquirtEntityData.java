package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp.SquirtEntityData.SquirtDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents squirt entity data.
 * Created by Kneesnap on 12/16/2023.
 */
public class SquirtEntityData extends MatrixEntityData<SquirtDifficultyData> {
    public SquirtEntityData(OldFroggerMapEntity entity) {
        super(entity, SquirtDifficultyData::new);
    }

    @Getter
    public static class SquirtDifficultyData extends OldFroggerDifficultyData {
        private short timeDelay = 60; // Default: 60 (2 Seconds)
        private short timeToTarget = 30; // Default: 30 (1 Second)
        private final SVector target = new SVector();

        public SquirtDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.timeDelay = reader.readShort();
            this.timeToTarget = reader.readShort();
            this.target.loadWithPadding(reader);
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort(this.timeDelay);
            writer.writeShort(this.timeToTarget);
            this.target.saveWithPadding(writer);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedShort("Time Delay", this.timeDelay, newValue -> this.timeDelay = newValue, 30, false);
            editor.addFixedShort("Time To Target", this.timeToTarget, newValue -> this.timeToTarget = newValue, 30, false);
            editor.addFloatSVector("Target", this.target, manager.getController());
        }
    }
}