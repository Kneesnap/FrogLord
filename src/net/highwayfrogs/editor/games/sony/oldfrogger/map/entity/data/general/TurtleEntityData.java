package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general.TurtleEntityData.TurtleDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents turtle entity data.
 * Created by Kneesnap on 12/16/2023.
 */
public class TurtleEntityData extends PathEntityData<TurtleDifficultyData> {
    public TurtleEntityData(OldFroggerMapEntity entity) {
        super(entity, TurtleDifficultyData::new);
    }

    @Getter
    public static class TurtleDifficultyData extends OldFroggerDifficultyData {
        private short diveDelay = -1;
        private short riseDelay = -1;
        private int diveSpeed = 2184;
        private int riseSpeed = 2184;
        private int speed = 2184;
        private int splineDelay;

        public TurtleDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.diveDelay = reader.readShort();
            this.riseDelay = reader.readShort();
            this.diveSpeed = reader.readUnsignedShortAsInt();
            this.riseSpeed = reader.readUnsignedShortAsInt();
            this.speed = reader.readUnsignedShortAsInt();
            this.splineDelay = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort(this.diveDelay);
            writer.writeShort(this.riseDelay);
            writer.writeUnsignedShort(this.diveSpeed);
            writer.writeUnsignedShort(this.riseSpeed);
            writer.writeUnsignedShort(this.speed);
            writer.writeUnsignedShort(this.splineDelay);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedShort("Dive Delay", this.diveDelay, newValue -> this.diveDelay = newValue, 30, true);
            editor.addFixedShort("Rise Delay", this.riseDelay, newValue -> this.riseDelay = newValue, 30, true);
            editor.addUnsignedFixedShort("Dive Speed", this.diveSpeed, newValue -> this.diveSpeed = newValue, 2184);
            editor.addUnsignedFixedShort("Rise Speed", this.riseSpeed, newValue -> this.riseSpeed = newValue, 2184);
            editor.addUnsignedFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184);
            editor.addUnsignedFixedShort("Spline Delay", this.splineDelay, newValue -> this.splineDelay = newValue, 30, 0, 100);
        }
    }
}