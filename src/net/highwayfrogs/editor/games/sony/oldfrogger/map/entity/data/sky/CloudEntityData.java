package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky.CloudEntityData.CloudDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents data belonging to a cloud entity.
 * Created by Kneesnap on 12/18/2023.
 */
public class CloudEntityData extends PathEntityData<CloudDifficultyData> {
    public CloudEntityData(OldFroggerMapEntity entity) {
        super(entity, CloudDifficultyData::new);
    }

    @Getter
    public static class CloudDifficultyData extends OldFroggerDifficultyData {
        private short killTime = 1;

        public CloudDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.killTime = reader.readShort();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort(this.killTime);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedShort("Kill Time (secs)", this.killTime, newValue -> this.killTime = newValue, 30, 0, 100);
        }
    }
}