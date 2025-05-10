package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerFlyScoreType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.IFroggerFlySpriteData;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Holds onto butterfly data as 'ENTSTR_BUTTERFLY' in ENTLIB.H
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class FroggerEntityScriptDataButterfly extends FroggerEntityScriptData implements IFroggerFlySpriteData {
    private int flyTypeId = FroggerFlyScoreType.values()[0].ordinal();

    public FroggerEntityScriptDataButterfly(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        this.flyTypeId = reader.readUnsignedShortAsInt(); // JUN1.MAP has corrupted data here in certain mwds. This just makes it not crash from it.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.flyTypeId);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
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

    @Override
    public FroggerFlyScoreType getFlyType() {
        return this.flyTypeId >= 0 && this.flyTypeId < FroggerFlyScoreType.values().length
                ? FroggerFlyScoreType.values()[this.flyTypeId] : null;
    }
}