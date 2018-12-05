package net.highwayfrogs.editor.file.map.entity.data.cave;

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
public class EntityFrogLight extends GameObject implements MatrixEntity {
    private PSXMatrix matrix = new PSXMatrix();
    private int minRadius;
    private int maxRadius;
    private int dieSpeed;
    private int count;
    private int setup;

    @Override
    public void load(DataReader reader) {
        matrix.load(reader);
        this.minRadius = reader.readInt();
        this.maxRadius = reader.readInt();
        this.dieSpeed = reader.readInt();
        this.count = reader.readInt();
        this.setup = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeInt(this.minRadius);
        writer.writeInt(this.maxRadius);
        writer.writeInt(this.dieSpeed);
        writer.writeInt(this.count);
        writer.writeInt(this.setup);
    }
}
