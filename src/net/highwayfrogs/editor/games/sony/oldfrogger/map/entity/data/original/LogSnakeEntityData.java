package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.original;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.original.LogSnakeEntityData.LogSnakeDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents snake data.
 * Created by Kneesnap on 12/16/2023.
 */
public class LogSnakeEntityData extends MatrixEntityData<LogSnakeDifficultyData> {
    public LogSnakeEntityData(OldFroggerMapEntity entity) {
        super(entity, LogSnakeDifficultyData::new);
    }

    @Getter
    public static class LogSnakeDifficultyData extends OldFroggerDifficultyData {
        private int logUniqueId;
        private int speed = 2184;

        public LogSnakeDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.logUniqueId = reader.readUnsignedShortAsInt();
            this.speed = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.logUniqueId);
            writer.writeUnsignedShort(this.speed);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Log Unique ID", this.logUniqueId, newValue -> this.logUniqueId = newValue, 1, 0, 1000);
            editor.addUnsignedFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184);
        }
    }
}