package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.original;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.original.FlyEntityData.FlyDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents fly entity data.
 * Created by Kneesnap on 12/16/2023.
 */
@Getter
public class FlyEntityData extends MatrixEntityData<FlyDifficultyData> {
    private int triggerPoint = 1;

    public FlyEntityData(OldFroggerMapEntity entity) {
        super(entity, FlyDifficultyData::new);
    }

    @Override
    public void loadMainEntityData(DataReader reader) {
        super.loadMainEntityData(reader);
        this.triggerPoint = reader.readUnsignedShortAsInt();
    }

    @Override
    public void saveMainEntityData(DataWriter writer) {
        super.saveMainEntityData(writer);
        writer.writeUnsignedShort(this.triggerPoint);
    }

    @Override
    public void setupMainEntityDataEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
        super.setupMainEntityDataEditor(manager, editor);
        editor.addUnsignedFixedShort("Trigger Point #", this.triggerPoint, newValue -> this.triggerPoint = newValue, 1, 1, 5);
    }

    @Getter
    public static class FlyDifficultyData extends OldFroggerDifficultyData {
        private int value = 200;
        private int blank = 200;
        private int showDelay = 10;
        private int showTime = 10;

        public FlyDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.value = reader.readUnsignedShortAsInt();
            this.blank = reader.readUnsignedShortAsInt();
            this.showDelay = reader.readUnsignedShortAsInt();
            this.showTime = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.value);
            writer.writeUnsignedShort(this.blank);
            writer.writeUnsignedShort(this.showDelay);
            writer.writeUnsignedShort(this.showTime);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedInt("Value", this.value, newValue -> this.value = newValue, 1, 0, 1000);
            editor.addFixedInt("Blank", this.blank, newValue -> this.blank = newValue, 1, 0, 1000);
            editor.addFixedInt("Show Delay", this.showDelay, newValue -> this.showDelay = newValue, 30, 0, 1000);
            editor.addFixedInt("Show Time", this.showTime, newValue -> this.showTime = newValue, 30, 0, 1000);
        }
    }
}