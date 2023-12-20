package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.desert;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.desert.HoleEntityData.HoleDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents data belonging to a hole entity.
 * Created by Kneesnap on 12/19/2023.
 */
@Getter
public class HoleEntityData extends MatrixEntityData<HoleDifficultyData> {
    private final int[] zones = new int[4];

    public HoleEntityData(OldFroggerMapEntity entity) {
        super(entity, HoleDifficultyData::new);
    }

    @Override
    protected void loadMainEntityData(DataReader reader) {
        super.loadMainEntityData(reader);
        for (int i = 0; i < this.zones.length; i++)
            this.zones[i] = reader.readUnsignedShortAsInt();
    }

    @Override
    protected void saveMainEntityData(DataWriter writer) {
        super.saveMainEntityData(writer);
        for (int i = 0; i < this.zones.length; i++)
            writer.writeUnsignedShort(this.zones[i]);
    }

    @Override
    public void setupMainEntityDataEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
        super.setupMainEntityDataEditor(manager, editor);
        for (int i = 0; i < this.zones.length; i++) {
            final int index = i;
            editor.addUnsignedFixedShort("Zone " + (i + 1), this.zones[i], newValue -> this.zones[index] = newValue, 1);
        }
    }

    @Getter
    public static class HoleDifficultyData extends OldFroggerDifficultyData {
        private int triggerDelay = 60;

        public HoleDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.triggerDelay = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.triggerDelay);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addUnsignedFixedShort("Trigger Delay (secs)", this.triggerDelay, newValue -> this.triggerDelay = newValue, 30);
        }
    }
}