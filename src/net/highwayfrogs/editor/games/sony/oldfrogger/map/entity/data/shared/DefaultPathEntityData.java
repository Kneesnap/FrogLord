package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.DefaultPathEntityData.OldFroggerDefaultPathData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements default path data.
 * Created by Kneesnap on 12/15/2023.
 */
public class DefaultPathEntityData extends PathEntityData<OldFroggerDefaultPathData> {
    public DefaultPathEntityData(OldFroggerMapEntity entity) {
        super(entity, OldFroggerDefaultPathData::new);
    }

    /**
     * Represents old default path data.
     */
    @Getter
    public static class OldFroggerDefaultPathData extends OldFroggerDifficultyData {
        private int speed = 2184; // 2184.5 is 65535 / 30
        private int splineDelay;

        public OldFroggerDefaultPathData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.speed = reader.readUnsignedShortAsInt();
            this.splineDelay = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.speed);
            writer.writeUnsignedShort(this.splineDelay);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Speed", this.speed, newSpeed -> this.speed = newSpeed, 2184);
            editor.addUnsignedFixedShort("Spline Delay", this.splineDelay, newSplineDelay -> this.splineDelay = newSplineDelay, 30, 0, 100);
        }
    }
}