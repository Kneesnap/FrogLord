package net.highwayfrogs.editor.file.map.entity.data.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntitySquirt extends GameObject {
    private PSXMatrix matrix = new PSXMatrix();
    private short timeDelay;
    private short dropTime;
    private SVector target;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.timeDelay = reader.readShort();
        this.dropTime = reader.readShort();
        this.target = SVector.readWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeShort(this.timeDelay);
        writer.writeShort(this.dropTime);
        this.target.saveWithPadding(writer);
    }
}
