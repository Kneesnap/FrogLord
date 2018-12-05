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
public class EntityWeb extends GameObject implements MatrixEntity {
    private PSXMatrix matrix = new PSXMatrix();
    private short spiderId;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.spiderId = reader.readShort();
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeShort(this.spiderId);
        writer.writeUnsignedShort(0);
    }
}
