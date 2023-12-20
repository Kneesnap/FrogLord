package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp.StaticWasteBarrelEntityData.StaticWasteBarrelDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Chemical waste barrel entity data.
 * Created by Kneesnap on 12/16/2023.
 */
public class StaticWasteBarrelEntityData extends MatrixEntityData<StaticWasteBarrelDifficultyData> {
    public StaticWasteBarrelEntityData(OldFroggerMapEntity entity) {
        super(entity, StaticWasteBarrelDifficultyData::new);
    }

    @Getter
    public static class StaticWasteBarrelDifficultyData extends OldFroggerDifficultyData {
        private short floatTime = 10;
        private short sunkTime = 10;

        public StaticWasteBarrelDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.floatTime = reader.readShort();
            this.sunkTime = reader.readShort();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort(this.floatTime);
            writer.writeShort(this.sunkTime);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedShort("Float Time", this.floatTime, newValue -> this.floatTime = newValue, 30, true);
            editor.addFixedShort("Sunk Time", this.sunkTime, newValue -> this.sunkTime = newValue, 30, true);
        }
    }
}