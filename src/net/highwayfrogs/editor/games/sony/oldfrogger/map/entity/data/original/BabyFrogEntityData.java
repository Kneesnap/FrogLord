package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.original;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.original.BabyFrogEntityData.BabyFrogDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents baby frog entity data.
 * Created by Kneesnap on 12/16/2023.
 */
public class BabyFrogEntityData extends MatrixEntityData<BabyFrogDifficultyData> {
    public BabyFrogEntityData(OldFroggerMapEntity entity) {
        super(entity, BabyFrogDifficultyData::new);
    }

    @Getter
    public static class BabyFrogDifficultyData extends OldFroggerDifficultyData {
        private int logUniqueId;
        private int pointsValue = 200;

        public BabyFrogDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.logUniqueId = reader.readUnsignedShortAsInt();
            this.pointsValue = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.logUniqueId);
            writer.writeUnsignedShort(this.pointsValue);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Log Unique ID", this.logUniqueId, newValue -> this.logUniqueId = newValue, 1, 0, 1000);
            editor.addFixedInt("Points Value", this.pointsValue, newValue -> this.pointsValue = newValue, 1, 0, 1000);
        }
    }
}