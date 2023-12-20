package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general.HedgehogEntityData.HedgehogDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents entity data for the hedgehog entity.
 * Created by Kneesnap on 12/17/2023.
 */
public class HedgehogEntityData extends PathEntityData<HedgehogDifficultyData> {
    public HedgehogEntityData(OldFroggerMapEntity entity) {
        super(entity, HedgehogDifficultyData::new);
    }

    @Getter
    public static class HedgehogDifficultyData extends OldFroggerDifficultyData {
        private int splineDelay = 1;
        private int walkDelay = 1;
        private int walkSpeed = 2184;
        private int rollSpeed = 2184;
        private int rollAcceleration = 2184;

        public HedgehogDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.splineDelay = reader.readUnsignedShortAsInt();
            this.walkDelay = reader.readUnsignedShortAsInt();
            this.walkSpeed = reader.readUnsignedShortAsInt();
            this.rollSpeed = reader.readUnsignedShortAsInt();
            this.rollAcceleration = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.splineDelay);
            writer.writeUnsignedShort(this.walkDelay);
            writer.writeUnsignedShort(this.walkSpeed);
            writer.writeUnsignedShort(this.rollSpeed);
            writer.writeUnsignedShort(this.rollAcceleration);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Spline Delay", this.splineDelay, newValue -> this.splineDelay = newValue, 30, 0, 100);
            editor.addUnsignedFixedShort("Walk Delay", this.walkDelay, newValue -> this.walkDelay = newValue, 30, 0, 100);
            editor.addUnsignedFixedShort("Walk Speed", this.walkSpeed, newValue -> this.walkSpeed = newValue, 2184);
            editor.addUnsignedFixedShort("Roll Speed", this.rollSpeed, newValue -> this.rollSpeed = newValue, 2184);
            editor.addUnsignedFixedShort("Roll Acceleration", this.rollAcceleration, newValue -> this.rollAcceleration = newValue, 2184);
        }
    }
}