package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general;

import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.general.TriggerPointEntityData.TriggerPointDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents trigger point entity data in old Frogger.
 * Created by Kneesnap on 12/11/2023.
 */
public class TriggerPointEntityData extends MatrixEntityData<TriggerPointDifficultyData> {
    public TriggerPointEntityData(OldFroggerMapEntity entity) {
        super(entity, TriggerPointDifficultyData::new);
    }

    /**
     * Represents trigger point difficulty data.
     */
    public static class TriggerPointDifficultyData extends OldFroggerDifficultyData {
        public TriggerPointDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            reader.skipBytesRequireEmpty(4);
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeNull(4);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            // Do nothing.
        }
    }
}