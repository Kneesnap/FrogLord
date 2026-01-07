package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.swamp.RatEntityData.RatDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents rat entity data.
 * Created by Kneesnap on 12/16/2023.
 */
public class RatEntityData extends PathEntityData<RatDifficultyData> {
    public RatEntityData(OldFroggerMapEntity entity) {
        super(entity, RatDifficultyData::new);
    }

    /**
     * Represents rat data.
     */
    @Getter
    public static class RatDifficultyData extends OldFroggerDifficultyData {
        private final SVector target = new SVector();
        private int ratSpeed = 2184;
        private int timeToTarget = 30;
        private int ratToFrogDistance = 512;

        public RatDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.target.loadWithPadding(reader);
            this.ratSpeed = reader.readUnsignedShortAsInt();
            this.timeToTarget = reader.readUnsignedShortAsInt();
            this.ratToFrogDistance = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            this.target.saveWithPadding(writer);
            writer.writeUnsignedShort(this.ratSpeed);
            writer.writeUnsignedShort(this.timeToTarget);
            writer.writeUnsignedShort(this.ratToFrogDistance);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFloatSVector("Target", this.target, manager.getController());
            editor.addUnsignedFixedShort("Speed", this.ratSpeed, newValue -> this.ratSpeed = newValue, 2184);
            editor.addUnsignedFixedShort("Time to Target", this.timeToTarget, newValue -> this.timeToTarget = newValue, 30, 0, 1000);
            editor.addUnsignedFixedShort("Rat to Frog Distance", this.ratToFrogDistance, newValue -> this.ratToFrogDistance = newValue, 256);
        }
    }
}