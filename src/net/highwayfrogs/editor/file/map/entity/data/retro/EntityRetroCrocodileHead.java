package net.highwayfrogs.editor.file.map.entity.data.retro;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

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

    public enum CrocodileHeadState {
        WAITING, APPEARING, APPEARED
    }
}