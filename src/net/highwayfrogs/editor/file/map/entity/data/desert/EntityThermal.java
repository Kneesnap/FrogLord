package net.highwayfrogs.editor.file.map.entity.data.desert;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityThermal extends GameObject {
    private int rotateTime;

    @Override
    public void load(DataReader reader) {
        this.rotateTime = reader.readUnsignedShortAsInt();
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.rotateTime);
        writer.writeUnsignedShort(0);
    }
}
