package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest.SwayingBranchEntityData.SwayingBranchDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a swinging branch entity.
 * Created by Kneesnap on 12/17/2023.
 */
public class SwayingBranchEntityData extends MatrixEntityData<SwayingBranchDifficultyData> {
    public SwayingBranchEntityData(OldFroggerMapEntity entity) {
        super(entity, SwayingBranchDifficultyData::new);
    }

    @Getter
    public static class SwayingBranchDifficultyData extends OldFroggerDifficultyData {
        private short animAngle = 90;
        private int swayDuration = 90;

        public SwayingBranchDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.animAngle = reader.readShort();
            this.swayDuration = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort(this.animAngle);
            writer.writeUnsignedShort(this.swayDuration);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedShort("Anim Angle (degrees)", this.animAngle, newValue -> this.animAngle = newValue, 1, 0, 360);
            editor.addUnsignedFixedShort("Sway Duration (secs)", this.swayDuration, newValue -> this.swayDuration = newValue, 30, 0, 3000);
        }
    }
}