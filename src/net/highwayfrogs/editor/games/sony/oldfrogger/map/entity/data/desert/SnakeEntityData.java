package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.desert;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.desert.SnakeEntityData.SnakeDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.PathEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a snake entity.
 * Created by Kneesnap on 12/19/2023.
 */
public class SnakeEntityData extends PathEntityData<SnakeDifficultyData> {
    public SnakeEntityData(OldFroggerMapEntity entity) {
        super(entity, SnakeDifficultyData::new);
    }

    @Getter
    public static class SnakeDifficultyData extends OldFroggerDifficultyData {
        private int splineSpeed = 2184;
        private int distance = 256;

        public SnakeDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.splineSpeed = reader.readUnsignedShortAsInt();
            this.distance = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.splineSpeed);
            writer.writeUnsignedShort(this.distance);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Spline Speed", this.splineSpeed, newValue -> this.splineSpeed = newValue, 2184);
            editor.addUnsignedFixedShort("Distance (gs)", this.distance, newValue -> this.distance = newValue, 256);
        }
    }
}