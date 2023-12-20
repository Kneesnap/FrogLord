package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.desert;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.desert.TumbleweedEntityData.TumbleweedDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a tumbleweed entity.
 * Created by Kneesnap on 12/19/2023.
 */
public class TumbleweedEntityData extends PathEntityData<TumbleweedDifficultyData> {
    public TumbleweedEntityData(OldFroggerMapEntity entity) {
        super(entity, TumbleweedDifficultyData::new);
    }

    @Getter
    public static class TumbleweedDifficultyData extends OldFroggerDifficultyData {
        private int splineSpeed = 2184;
        private int windStartDelay = 30;
        private int windAccelerationTime = 300;
        private int windAcceleration = 2184;

        public TumbleweedDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.splineSpeed = reader.readUnsignedShortAsInt();
            this.windStartDelay = reader.readUnsignedShortAsInt();
            this.windAccelerationTime = reader.readUnsignedShortAsInt();
            this.windAcceleration = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.splineSpeed);
            writer.writeUnsignedShort(this.windStartDelay);
            writer.writeUnsignedShort(this.windAccelerationTime);
            writer.writeUnsignedShort(this.windAcceleration);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Spline Speed", this.splineSpeed, newValue -> this.splineSpeed = newValue, 2184);
            editor.addUnsignedFixedShort("Wind Start Delay (secs)", this.windStartDelay, newValue -> this.windStartDelay = newValue, 30);
            editor.addUnsignedFixedShort("Wind Acceleration Time (secs)", this.windAccelerationTime, newValue -> this.windAccelerationTime = newValue, 30);
            editor.addUnsignedFixedShort("Wind Acceleration (gs)", this.windAcceleration, newValue -> this.windAcceleration = newValue, 2184);
        }
    }
}