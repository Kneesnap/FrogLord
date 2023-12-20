package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest.HiveEntityData.HiveDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a beehive entity.
 * Created by Kneesnap on 12/17/2023.
 */
public class HiveEntityData extends MatrixEntityData<HiveDifficultyData> {
    public HiveEntityData(OldFroggerMapEntity entity) {
        super(entity, HiveDifficultyData::new);
    }

    @Getter
    public static class HiveDifficultyData extends OldFroggerDifficultyData {
        private int criticalDistance = 256;
        private int frogDelay = 60;
        private int swarmSpeed = 2184;
        private int interestTime = 150;

        public HiveDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.criticalDistance = reader.readUnsignedShortAsInt();
            this.frogDelay = reader.readUnsignedShortAsInt();
            this.swarmSpeed = reader.readUnsignedShortAsInt();
            this.interestTime = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.criticalDistance);
            writer.writeUnsignedShort(this.frogDelay);
            writer.writeUnsignedShort(this.swarmSpeed);
            writer.writeUnsignedShort(this.interestTime);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Critical Distance", this.criticalDistance, newValue -> this.criticalDistance = newValue, 256, 0, Short.MAX_VALUE);
            editor.addUnsignedFixedShort("Frog Delay", this.frogDelay, newValue -> this.frogDelay = newValue, 30, 0, 300);
            editor.addUnsignedFixedShort("Swarm Speed", this.swarmSpeed, newValue -> this.swarmSpeed = newValue, 2184, 0, Short.MAX_VALUE);
            editor.addUnsignedFixedShort("Interest Time", this.interestTime, newValue -> this.interestTime = newValue, 30, 0, 3000);
        }
    }
}