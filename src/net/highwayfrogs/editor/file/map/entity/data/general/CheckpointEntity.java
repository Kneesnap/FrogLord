package net.highwayfrogs.editor.file.map.entity.data.general;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.entity.data.MatrixEntity;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents "GEN_CHECKPOINT".
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class CheckpointEntity extends GameObject implements MatrixEntity {
    private PSXMatrix matrix = new PSXMatrix();
    private int id;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.id = reader.readUnsignedShortAsInt();
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeUnsignedShort(this.id);
        writer.writeUnsignedShort(0);
    }
}
