package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.original;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.original.CrocHeadEntityData.CrocHeadDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents crocodile head entity data.
 * Created by Kneesnap on 12/16/2023.
 */
@Getter
public class CrocHeadEntityData extends MatrixEntityData<CrocHeadDifficultyData> {
    private int triggerPoint = 1;

    public CrocHeadEntityData(OldFroggerMapEntity entity) {
        super(entity, CrocHeadDifficultyData::new);
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
    public static class CrocHeadDifficultyData extends OldFroggerDifficultyData {
        private int showDelay = 10;
        private int showTime = 10;

        public CrocHeadDifficultyData(OldFroggerMapEntity entity) {
            super(entity);
        }

        @Override
        public void load(DataReader reader) {
            this.showDelay = reader.readUnsignedShortAsInt();
            this.showTime = reader.readUnsignedShortAsInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeUnsignedShort(this.showDelay);
            writer.writeUnsignedShort(this.showTime);
        }

        @Override
        public void setupEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
            editor.addFixedInt("Show Delay", this.showDelay, newValue -> this.showDelay = newValue, 30, 0, 1000);
            editor.addFixedInt("Show Time", this.showTime, newValue -> this.showTime = newValue, 30, 0, 1000);
        }
    }
}