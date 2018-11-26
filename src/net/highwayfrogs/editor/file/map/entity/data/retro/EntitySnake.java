package net.highwayfrogs.editor.file.map.entity.data.retro;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntitySnake extends GameObject {
    private PSXMatrix matrix = new PSXMatrix();
    private short logId;
    private int speed;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.logId = reader.readShort();
        this.speed = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeShort(this.logId);
        writer.writeUnsignedShort(this.speed);
    }
}
