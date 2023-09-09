package net.highwayfrogs.editor.file.map.entity.data.general;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.FlyScoreType;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.map.manager.EntityManager;

/**
 * Includes the identifier which identifies a fly.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class BonusFlyEntity extends MatrixData {
    private int flyTypeId;

    public BonusFlyEntity(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.flyTypeId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.flyTypeId);
    }

    public FlyScoreType getFlyType() {
        return (this.flyTypeId >= 0 && this.flyTypeId < FlyScoreType.values().length)
                ? FlyScoreType.values()[this.flyTypeId] : null;
    }

    @Override
    public void addData(EntityManager manager, GUIEditorGrid editor) {
        super.addData(manager, editor);

        FlyScoreType flyType = getFlyType();
        if (flyType != null) {
            editor.addEnumSelector("Fly Type", flyType, FlyScoreType.values(), false, newType -> {
                this.flyTypeId = newType.ordinal();
                manager.updateEntity(getParentEntity());
            });
        } else {
            editor.addIntegerField("Fly Type ID", this.flyTypeId, newTypeId -> {
                this.flyTypeId = newTypeId;
                manager.updateEntity(getParentEntity());
            }, null);
        }
    }
}