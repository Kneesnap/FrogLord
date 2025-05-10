package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.suburbia;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.RotateDirection;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.suburbia.LillypadEntityData.LillypadDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents lillypad entity data.
 * Created by Kneesnap on 12/16/2023.
 */
public class LillypadEntityData extends PathEntityData<LillypadDifficultyData> {
    public LillypadEntityData(OldFroggerMapEntity entity) {
        super(entity, LillypadDifficultyData::new);
    }

    @Getter
    public static class LillypadDifficultyData extends OldFroggerDifficultyData {
        private int speed = 2184;
        private int rotation = 10;
        private RotateDirection direction = RotateDirection.CLOCKWISE;

        public LillypadDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.speed = reader.readUnsignedShortAsInt();
            this.rotation = reader.readUnsignedShortAsInt();

            int directionValue = reader.readUnsignedShortAsInt();
            this.direction = directionValue != 0 ? RotateDirection.values()[directionValue - 1] : null;
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.speed);
            writer.writeUnsignedShort(this.rotation);
            writer.writeUnsignedShort(this.direction != null ? this.direction.ordinal() + 1 : 0);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184);
            editor.addUnsignedFixedShort("Rotation", this.rotation, newValue -> this.rotation = newValue, 1, 0, 100);
        }

        @Override
        public int getByteAlignment() {
            return 2;
        }
    }
}