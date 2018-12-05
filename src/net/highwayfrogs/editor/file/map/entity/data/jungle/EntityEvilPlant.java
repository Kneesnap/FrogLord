package net.highwayfrogs.editor.file.map.entity.data.jungle;

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
public class EntityEvilPlant extends GameObject implements MatrixEntity {
    private PSXMatrix matrix = new PSXMatrix();
    private short snapTime;
    private short snapDelay;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.snapTime = reader.readShort();
        this.snapDelay = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeShort(this.snapTime);
        writer.writeShort(this.snapDelay);
    }
}
