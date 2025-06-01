package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.retro;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the 'ORG_CROC_HEAD' entity data definition in ent_org.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataRetroCrocodileHead extends FroggerEntityDataMatrix {
    private CrocodileHeadState type = CrocodileHeadState.WAITING;

    public FroggerEntityDataRetroCrocodileHead(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = CrocodileHeadState.values()[reader.readUnsignedShortAsInt()];
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.type.ordinal());
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addEnumSelector("Head State (Unused)", this.type, CrocodileHeadState.values(), false, newType -> this.type = newType)
                .setDisable(true);
    }

    public enum CrocodileHeadState {
        WAITING, APPEARING, APPEARED
    }
}