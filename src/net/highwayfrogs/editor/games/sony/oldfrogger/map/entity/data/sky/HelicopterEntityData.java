package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky.HelicopterEntityData.HelicopterDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents the data belonging to a helicopter entity.
 * Created by Kneesnap on 12/18/2023.
 */
public class HelicopterEntityData extends MatrixEntityData<HelicopterDifficultyData> {
    public HelicopterEntityData(OldFroggerMapEntity entity) {
        super(entity, HelicopterDifficultyData::new);
    }

    @Getter
    public static class HelicopterDifficultyData extends OldFroggerDifficultyData {
        private short followSpeed = 2184;
        private short riseDelay = 1;
        private short riseSpeed = 2184;

        public HelicopterDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.followSpeed = reader.readShort();
            this.riseDelay = reader.readShort();
            this.riseSpeed = reader.readShort();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort(this.followSpeed);
            writer.writeShort(this.riseDelay);
            writer.writeShort(this.riseSpeed);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedShort("Follow Speed", this.followSpeed, newValue -> this.followSpeed = newValue, 2184, 0, Short.MAX_VALUE);
            editor.addFixedShort("Rise Delay", this.riseDelay, newValue -> this.riseDelay = newValue, 30, 0, 100);
            editor.addFixedShort("Rise Speed", this.riseSpeed, newValue -> this.riseSpeed = newValue, 2184, 0, Short.MAX_VALUE);
        }
    }
}