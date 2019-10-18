package net.highwayfrogs.editor.file.map.entity.data.general;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.FlyScoreType;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.map.manager.EntityManager;

/**
 * Data for a entity.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class BonusFlyEntity extends MatrixData {
    private FlyScoreType type = FlyScoreType.SCORE_10;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = FlyScoreType.values()[reader.readInt()];
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.type.ordinal());
    }

    @Override
    public void addData(EntityManager manager, GUIEditorGrid editor) {
        super.addData(manager, editor);
        editor.addEnumSelector("Fly Type", getType(), FlyScoreType.values(), false, newType -> {
            this.type = newType;
            manager.updateEntity(getParentEntity());
        });
    }
}
