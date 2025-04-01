package net.highwayfrogs.editor.file.map.entity.data.general;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerEntityTriggerType;

/**
 * Represents a trigger entity.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class TriggerEntity extends MatrixData {
    private FroggerEntityTriggerType type = FroggerEntityTriggerType.BEGIN;
    private short[] uniqueIds = new short[ENTITY_TYPE_TRIGGER_MAX_IDS];

    public static final int ENTITY_TYPE_TRIGGER_MAX_IDS = 10;

    public TriggerEntity(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = FroggerEntityTriggerType.values()[reader.readInt()];
        for (int i = 0; i < uniqueIds.length; i++)
            this.uniqueIds[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.type.ordinal());
        for (short id : uniqueIds)
            writer.writeShort(id);
    }
}