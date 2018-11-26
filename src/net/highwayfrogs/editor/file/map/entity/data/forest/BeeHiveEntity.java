package net.highwayfrogs.editor.file.map.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class BeeHiveEntity extends GameObject {
    private PSXMatrix matrix = new PSXMatrix();
    private int releaseDistance;
    private int swarmSpeed;

    @Override
    public void load(DataReader reader) {
        matrix.load(reader);
        this.releaseDistance = reader.readInt();
        this.swarmSpeed = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        matrix.save(writer);
        writer.writeInt(this.releaseDistance);
        writer.writeInt(this.swarmSpeed);
    }
}
