package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest.FallingLeavesEntityData.FallingLeavesDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a falling leaf entity.
 * Created by Kneesnap on 12/17/2023.
 */
public class FallingLeavesEntityData extends PathEntityData<FallingLeavesDifficultyData> {
    public FallingLeavesEntityData(OldFroggerMapEntity entity) {
        super(entity, FallingLeavesDifficultyData::new);
    }

    @Getter
    public static class FallingLeavesDifficultyData extends OldFroggerDifficultyData {
        private int speed = 2184;
        private int swayDuration = 90;
        private int swayAngle = 90;

        public FallingLeavesDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.speed = reader.readUnsignedShortAsInt();
            this.swayDuration = reader.readUnsignedShortAsInt();
            this.swayAngle = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.speed);
            writer.writeUnsignedShort(this.swayDuration);
            writer.writeUnsignedShort(this.swayAngle);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184);
            editor.addUnsignedFixedShort("Sway Duration (secs)", this.swayDuration, newValue -> this.swayDuration = newValue, 30, 0, 3000);
            editor.addUnsignedFixedShort("Sway Angle (degrees)", this.swayAngle, newValue -> this.swayAngle = newValue, 1, 0, 360);
        }
    }
}