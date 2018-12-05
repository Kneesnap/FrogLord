package net.highwayfrogs.editor.file.map.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.entity.data.MatrixEntity;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityFatFireFly extends GameObject implements MatrixEntity {
    private PSXMatrix matrix = new PSXMatrix();
    private int type;
    private SVector target;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.type = reader.readUnsignedShortAsInt();
        reader.readShort();
        this.target = SVector.readWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeUnsignedShort(this.type);
        writer.writeUnsignedShort(0);
        this.target.saveWithPadding(writer);
    }
}
