package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.cave.LightBugEntityData.LightBugDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents glow worm entity data.
 * Created by Kneesnap on 12/17/2023.
 */
public class LightBugEntityData extends PathEntityData<LightBugDifficultyData> {
    public LightBugEntityData(OldFroggerMapEntity entity) {
        super(entity, LightBugDifficultyData::new);
    }

    @Getter
    public static class LightBugDifficultyData extends OldFroggerDifficultyData {
        private int speed = 2184; // 2184.5 is 65535 / 30
        private int lightValue = 768;

        public LightBugDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.speed = reader.readUnsignedShortAsInt();
            this.lightValue = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.speed);
            writer.writeUnsignedShort(this.lightValue);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184);
            editor.addUnsignedFixedShort("Light Value", this.lightValue, newValue -> this.lightValue = newValue, 256);
        }
    }
}