package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.forest.SquirrelEntityData.SquirrelDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a squirrel entity.
 * Created by Kneesnap on 12/17/2023.
 */
public class SquirrelEntityData extends PathEntityData<SquirrelDifficultyData> {
    public SquirrelEntityData(OldFroggerMapEntity entity) {
        super(entity, SquirrelDifficultyData::new);
    }

    @Getter
    public static class SquirrelDifficultyData extends OldFroggerDifficultyData {
        private int speed = 2184;
        private int turnDuration = 90;

        public SquirrelDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.speed = reader.readUnsignedShortAsInt();
            this.turnDuration = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.speed);
            writer.writeUnsignedShort(this.turnDuration);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Speed", this.speed, newValue -> this.speed = newValue, 2184);
            editor.addUnsignedFixedShort("Turn Duration (secs)", this.turnDuration, newValue -> this.turnDuration = newValue, 30, 0, 3000);
        }
    }
}