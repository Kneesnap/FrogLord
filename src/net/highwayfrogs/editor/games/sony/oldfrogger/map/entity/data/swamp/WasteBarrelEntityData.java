package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp.WasteBarrelEntityData.WasteBarrelDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Waste barrel entity data.
 * Created by Kneesnap on 12/16/2023.
 */
public class WasteBarrelEntityData extends PathEntityData<WasteBarrelDifficultyData> {
    public WasteBarrelEntityData(OldFroggerMapEntity entity) {
        super(entity, WasteBarrelDifficultyData::new);
    }

    @Getter
    public static class WasteBarrelDifficultyData extends OldFroggerDifficultyData {
        private int splineSpeed = 2184;
        private short floatTime = 10;
        private short sunkTime = 10;
        private int spinAcceleration = 2184;
        private int maxSpin = 2184;

        public WasteBarrelDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.splineSpeed = reader.readUnsignedShortAsInt();
            this.floatTime = reader.readShort();
            this.sunkTime = reader.readShort();
            this.spinAcceleration = reader.readInt();
            this.maxSpin = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.splineSpeed);
            writer.writeShort(this.floatTime);
            writer.writeShort(this.sunkTime);
            writer.writeInt(this.spinAcceleration);
            writer.writeInt(this.maxSpin);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Spline Speed", this.splineSpeed, newValue -> this.splineSpeed = newValue, 2184);
            editor.addFixedShort("Float Time", this.floatTime, newValue -> this.floatTime = newValue, 30, true);
            editor.addFixedShort("Sunk Time", this.sunkTime, newValue -> this.sunkTime = newValue, 30, true);
            editor.addFixedInt("Spin Acceleration", this.spinAcceleration, newValue -> this.spinAcceleration = newValue, 2184, -1, 4473856);
            editor.addFixedInt("Max Spin", this.maxSpin, newValue -> this.maxSpin = newValue, 2184, 0, 8947712);
        }

        @Override
        public int getByteAlignment() {
            return 2;
        }
    }
}