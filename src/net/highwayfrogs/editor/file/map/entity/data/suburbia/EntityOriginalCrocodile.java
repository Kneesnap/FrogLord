package net.highwayfrogs.editor.file.map.entity.data.suburbia;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityOriginalCrocodile extends GameObject {
    private int mouthOpenDelay;

    @Override
    public void load(DataReader reader) {
        this.mouthOpenDelay = reader.readUnsignedShortAsInt();
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.mouthOpenDelay);
        writer.writeUnsignedShort(0);
    }
}
