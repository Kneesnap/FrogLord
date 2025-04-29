package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky.HawkEntityData.HawkDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents data belonging to a hawk entity.
 * Created by Kneesnap on 12/18/2023.
 */
public class HawkEntityData extends MatrixEntityData<HawkDifficultyData> {
    public HawkEntityData(OldFroggerMapEntity entity) {
        super(entity, HawkDifficultyData::new);
    }

    @Getter
    public static class HawkDifficultyData extends OldFroggerDifficultyData {
        private short swoopDelay = 150;
        private short swoopTime = 30;

        public HawkDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.swoopDelay = reader.readShort();
            this.swoopTime = reader.readShort();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort(this.swoopDelay);
            writer.writeShort(this.swoopTime);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedShort("Critical Distance", this.swoopDelay, newValue -> this.swoopDelay = newValue, 30, 0, 1000);
            editor.addFixedShort("Frog Delay", this.swoopTime, newValue -> this.swoopTime = newValue, 30, 0, 300);
        }
    }
}