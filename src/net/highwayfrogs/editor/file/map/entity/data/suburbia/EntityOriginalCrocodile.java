package net.highwayfrogs.editor.file.map.entity.data.suburbia;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.EntityData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityOriginalCrocodile extends EntityData {
    private int mouthOpenDelay;

    public EntityOriginalCrocodile(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.mouthOpenDelay = reader.readUnsignedShortAsInt();
        reader.skipShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.mouthOpenDelay);
        writer.writeUnsignedShort(0);
    }
}