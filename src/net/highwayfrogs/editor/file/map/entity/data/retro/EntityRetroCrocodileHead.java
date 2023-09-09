package net.highwayfrogs.editor.file.map.entity.data.retro;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityRetroCrocodileHead extends MatrixData {
    private CrocodileHeadState type = CrocodileHeadState.WAITING;

    public EntityRetroCrocodileHead(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = CrocodileHeadState.values()[reader.readUnsignedShortAsInt()];
        reader.skipShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.type.ordinal());
        writer.writeUnsignedShort(0);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addEnumSelector("Head State", getType(), CrocodileHeadState.values(), false, this::setType);
    }

    public enum CrocodileHeadState {
        WAITING, APPEARING, APPEARED
    }
}