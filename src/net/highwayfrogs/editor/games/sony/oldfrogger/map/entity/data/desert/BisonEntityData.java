package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.desert;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.desert.BisonEntityData.BisonDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a bison entity.
 * Created by Kneesnap on 12/19/2023.
 */
public class BisonEntityData extends PathEntityData<BisonDifficultyData> {
    public BisonEntityData(OldFroggerMapEntity entity) {
        super(entity, BisonDifficultyData::new);
    }

    @Getter
    public static class BisonDifficultyData extends OldFroggerDifficultyData {
        private int speed = 2184;
        private int cloudGap = 60;
        private int cloudDuration = 90;

        public BisonDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.speed = reader.readUnsignedShortAsInt();
            this.cloudGap = reader.readUnsignedShortAsInt();
            this.cloudDuration = reader.readUnsignedShortAsInt();

        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.speed);
            writer.writeUnsignedShort(this.cloudGap);
            writer.writeUnsignedShort(this.cloudDuration);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184);
            editor.addUnsignedFixedShort("Cloud Gap (secs)", this.cloudGap, newValue -> this.cloudGap = newValue, 30);
            editor.addUnsignedFixedShort("Cloud Duration (secs)", this.cloudDuration, newValue -> this.cloudDuration = newValue, 30);
        }
    }
}