package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp.SinkingBoxEntityData.SinkingBoxDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a sinking box entity.
 * Created by Kneesnap on 12/17/2023.
 */
public class SinkingBoxEntityData extends PathEntityData<SinkingBoxDifficultyData> {
    public SinkingBoxEntityData(OldFroggerMapEntity entity) {
        super(entity, SinkingBoxDifficultyData::new);
    }

    @Getter
    public static class SinkingBoxDifficultyData extends OldFroggerDifficultyData {
        private short sinkRate = 2184;
        private int splineSpeed = 2184;

        public SinkingBoxDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.sinkRate = reader.readShort();
            this.splineSpeed = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeShort(this.sinkRate);
            writer.writeUnsignedShort(this.splineSpeed);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedShort("Sink Rate (gs)", this.sinkRate, newValue -> this.sinkRate = newValue, 2184, Short.MIN_VALUE, Short.MAX_VALUE);
            editor.addUnsignedFixedShort("Spline Speed", this.splineSpeed, newValue -> this.splineSpeed = newValue, 2184);
        }
    }
}