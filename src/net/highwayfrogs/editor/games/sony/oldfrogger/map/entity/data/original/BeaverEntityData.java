package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.original;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.original.BeaverEntityData.BeaverDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents entity data for the beaver entity.
 * Created by Kneesnap on 12/17/2023.
 */
public class BeaverEntityData extends PathEntityData<BeaverDifficultyData> {
    public BeaverEntityData(OldFroggerMapEntity entity) {
        super(entity, BeaverDifficultyData::new);
    }

    @Getter
    public static class BeaverDifficultyData extends OldFroggerDifficultyData {
        private int speed = 2184;
        private int delayBeforeFollowing = 9;

        public BeaverDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.speed = reader.readUnsignedShortAsInt();
            this.delayBeforeFollowing = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.speed);
            writer.writeUnsignedShort(this.delayBeforeFollowing);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184);
            editor.addUnsignedFixedShort("Delay Before Follow", this.delayBeforeFollowing, newValue -> this.delayBeforeFollowing = newValue, 30, 0, 1000);
        }
    }
}