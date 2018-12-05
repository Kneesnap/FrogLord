package net.highwayfrogs.editor.file.map.entity.data.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.entity.data.MatrixEntity;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityCrusher extends GameObject implements MatrixEntity {
    private PSXMatrix matrix = new PSXMatrix();
    private short speed;
    private short distance;
    private short direction;
    private short delay;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.speed = reader.readShort();
        this.distance = reader.readShort();
        this.direction = reader.readShort();
        this.delay = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeShort(this.speed);
        writer.writeShort(this.distance);
        writer.writeShort(this.direction);
        writer.writeShort(this.delay);
    }
}
