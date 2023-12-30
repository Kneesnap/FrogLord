package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.desert;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.desert.EarthquakeEntityData.EarthquakeDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to the earthquake entity.
 * Created by Kneesnap on 12/19/2023.
 */
public class EarthquakeEntityData extends MatrixEntityData<EarthquakeDifficultyData> {
    public EarthquakeEntityData(OldFroggerMapEntity entity) {
        super(entity, EarthquakeDifficultyData::new);
    }

    @Getter
    public static class EarthquakeDifficultyData extends OldFroggerDifficultyData {
        private int unitFlag = 1; // Seems to always be one. Some kind of bit flag. I don't really know.
        private int rampUp = 100;
        private int maxExtent = 100;
        private int extentSpeed = 32;
        private int contantRun = 200;
        private int rampDown = 100;
        private final int[] entities = new int[10];

        public EarthquakeDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.unitFlag = reader.readUnsignedShortAsInt();
            this.rampUp = reader.readUnsignedShortAsInt();
            this.maxExtent = reader.readUnsignedShortAsInt();
            this.extentSpeed = reader.readUnsignedShortAsInt();
            this.contantRun = reader.readUnsignedShortAsInt();
            this.rampDown = reader.readUnsignedShortAsInt();
            for (int i = 0; i < this.entities.length; i++)
                this.entities[i] = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.unitFlag);
            writer.writeUnsignedShort(this.rampUp);
            writer.writeUnsignedShort(this.maxExtent);
            writer.writeUnsignedShort(this.extentSpeed);
            writer.writeUnsignedShort(this.contantRun);
            writer.writeUnsignedShort(this.rampDown);
            for (int i = 0; i < this.entities.length; i++)
                writer.writeUnsignedShort(this.entities[i]);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("UnitFlag", this.unitFlag, newValue -> this.rampUp = newValue, 1);
            editor.addUnsignedFixedShort("Ramp Up", this.rampUp, newValue -> this.rampUp = newValue, 100);
            editor.addUnsignedFixedShort("Max Extent", this.maxExtent, newValue -> this.maxExtent = newValue, 100);
            editor.addUnsignedFixedShort("Extent Speed", this.extentSpeed, newValue -> this.extentSpeed = newValue, 32, 0, 4095);
            editor.addUnsignedFixedShort("Contant Run", this.contantRun, newValue -> this.contantRun = newValue, 200);
            editor.addUnsignedFixedShort("Ramp Down", this.rampDown, newValue -> this.rampDown = newValue, 100);
            for (int i = 0; i < this.entities.length; i++) {
                final int index = i;
                editor.addUnsignedFixedShort("Entity " + (i + 1), this.entities[i], newValue -> this.entities[index] = newValue, 1);
            }
        }
    }
}