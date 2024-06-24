package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.general;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerFlyScoreType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.IFroggerFlySpriteData;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Includes the identifier which identifies a fly as 'GEN_BONUS_FLY' from ent_gen.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataBonusFly extends FroggerEntityDataMatrix implements IFroggerFlySpriteData {
    private int flyTypeId = FroggerFlyScoreType.values()[0].ordinal();

    public FroggerEntityDataBonusFly(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.flyTypeId = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.flyTypeId);
    }

    @Override
    public FroggerFlyScoreType getFlyType() {
        return (this.flyTypeId >= 0 && this.flyTypeId < FroggerFlyScoreType.values().length)
                ? FroggerFlyScoreType.values()[this.flyTypeId] : null;
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        super.setupEditor(editor, manager);

        FroggerFlyScoreType flyType = getFlyType();
        if (flyType != null) {
            editor.addEnumSelector("Fly Score Type", flyType, FroggerFlyScoreType.values(), false, newType -> {
                this.flyTypeId = newType.ordinal();
                if (manager != null)
                    manager.updateEntityMesh(getParentEntity());
            });
        } else {
            editor.addSignedIntegerField("Fly Score Type ID", this.flyTypeId, newTypeId -> {
                this.flyTypeId = newTypeId;
                if (manager != null)
                    manager.updateEntityMesh(getParentEntity());
            });
        }
    }
}