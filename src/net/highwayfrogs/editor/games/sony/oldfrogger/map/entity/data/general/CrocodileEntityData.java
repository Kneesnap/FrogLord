package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general.CrocodileEntityData.CrocodileDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents crocodile entity data.
 * Created by Kneesnap on 12/16/2023.
 */
public class CrocodileEntityData extends PathEntityData<CrocodileDifficultyData> {
    public CrocodileEntityData(OldFroggerMapEntity entity) {
        super(entity, CrocodileDifficultyData::new);
    }

    @Getter
    public static class CrocodileDifficultyData extends OldFroggerDifficultyData {
        private int speed = 2184;
        private short openMouthDelay = 1;
        private short closeMouthDelay = 1;

        public CrocodileDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.speed = reader.readUnsignedShortAsInt();
            reader.alignRequireEmpty(4);
            this.openMouthDelay = reader.readShort();
            this.closeMouthDelay = reader.readShort();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.speed);
            writer.writeUnsignedShort(0); // Padding
            writer.writeShort(this.openMouthDelay);
            writer.writeShort(this.closeMouthDelay);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184);
            editor.addFixedShort("Open Mouth Delay", this.openMouthDelay, newValue -> this.openMouthDelay = newValue, 30, 0, 1000);
            editor.addFixedShort("Close Mouth Delay", this.closeMouthDelay, newValue -> this.closeMouthDelay = newValue, 30, 0, 1000);
        }
    }
}