package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.sky.SmallBirdEntityData.SmallBirdDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a small bird entity.
 * Created by Kneesnap on 12/18/2023.
 */
public class SmallBirdEntityData extends PathEntityData<SmallBirdDifficultyData> {
    public SmallBirdEntityData(OldFroggerMapEntity entity) {
        super(entity, SmallBirdDifficultyData::new);
    }

    @Getter
    public static class SmallBirdDifficultyData extends OldFroggerDifficultyData {
        private short fallRate = 2184;
        private short speed = 2184;
        private int splineDelay = 30;

        public SmallBirdDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.fallRate = reader.readShort();
            this.speed = reader.readShort();
            this.splineDelay = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort(this.fallRate);
            writer.writeShort(this.speed);
            writer.writeUnsignedShort(this.splineDelay);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedShort("Fall Rate", this.fallRate, newValue -> this.fallRate = newValue, 2184, 0, Short.MAX_VALUE);
            editor.addFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184, 0, Short.MAX_VALUE);
            editor.addUnsignedFixedShort("Spline Delay", this.splineDelay, newValue -> this.splineDelay = newValue, 30, 0, 1000);
        }
    }
}