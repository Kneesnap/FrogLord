package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.suburbia;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.suburbia.SwanEntityData.SwanDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents swan entity data.
 * Created by Kneesnap on 12/16/2023.
 */
public class SwanEntityData extends PathEntityData<SwanDifficultyData> {
    public SwanEntityData(OldFroggerMapEntity entity) {
        super(entity, SwanDifficultyData::new);
    }

    @Getter
    public static class SwanDifficultyData extends OldFroggerDifficultyData {
        private int speed = 2184;
        private int splineDelay = 1;
        private short swimmingTime = 1;
        private short thinkingTime = 1;
        private short flappingTime = 1;

        public SwanDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.speed = reader.readUnsignedShortAsInt();
            this.splineDelay = reader.readUnsignedShortAsInt();
            this.swimmingTime = reader.readShort();
            this.thinkingTime = reader.readShort();
            this.flappingTime = reader.readShort();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.speed);
            writer.writeUnsignedShort(this.splineDelay);
            writer.writeShort(this.swimmingTime);
            writer.writeShort(this.thinkingTime);
            writer.writeShort(this.flappingTime);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184);
            editor.addUnsignedFixedShort("Spline Delay", this.splineDelay, newValue -> this.splineDelay = newValue, 30, 0, 100);
            editor.addFixedShort("Swimming Time", this.swimmingTime, newValue -> this.swimmingTime = newValue, 30, -1, 100);
            editor.addFixedShort("Thinking Time", this.thinkingTime, newValue -> this.thinkingTime = newValue, 30, 0, 100);
            editor.addFixedShort("Flapping Time", this.flappingTime, newValue -> this.flappingTime = newValue, 30, 0, 100);
        }
    }
}