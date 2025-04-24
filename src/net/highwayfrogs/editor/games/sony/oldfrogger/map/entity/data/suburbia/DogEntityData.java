package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.suburbia;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.suburbia.DogEntityData.DogDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents dog entity data.
 * Created by Kneesnap on 12/16/2023.
 */
public class DogEntityData extends PathEntityData<DogDifficultyData> {
    public DogEntityData(OldFroggerMapEntity entity) {
        super(entity, DogDifficultyData::new);
    }

    @Getter
    public static class DogDifficultyData extends OldFroggerDifficultyData {
        private int speed = 2184;
        private int splineDelay = 1;
        private int headDelay = 1;
        private int walkSpeed = 2184;
        private int runSpeed = 4369;

        public DogDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.speed = reader.readUnsignedShortAsInt();
            this.splineDelay = reader.readUnsignedShortAsInt();
            this.headDelay = reader.readUnsignedShortAsInt();
            this.walkSpeed = reader.readUnsignedShortAsInt();
            this.runSpeed = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.speed);
            writer.writeUnsignedShort(this.splineDelay);
            writer.writeUnsignedShort(this.headDelay);
            writer.writeUnsignedShort(this.walkSpeed);
            writer.writeUnsignedShort(this.runSpeed);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184);
            editor.addUnsignedFixedShort("Spline Delay", this.splineDelay, newValue -> this.splineDelay = newValue, 30, 0, 100);
            editor.addUnsignedFixedShort("Head Delay", this.headDelay, newValue -> this.headDelay = newValue, 30, 0, 100);
            editor.addUnsignedFixedShort("Walk Speed", this.walkSpeed, newValue -> this.walkSpeed = newValue, 2184);
            editor.addUnsignedFixedShort("Run Speed", this.runSpeed, newValue -> this.runSpeed = newValue, 2184);
        }
    }
}