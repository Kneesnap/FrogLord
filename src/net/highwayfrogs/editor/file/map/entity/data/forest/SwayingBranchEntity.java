package net.highwayfrogs.editor.file.map.entity.data.forest;

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
public class SwayingBranchEntity extends GameObject implements MatrixEntity {
    private PSXMatrix matrix = new PSXMatrix();
    private int swayAngle;
    private int swayDuration;
    private int onceOffDelay;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.swayAngle = reader.readUnsignedShortAsInt();
        this.swayDuration = reader.readUnsignedShortAsInt();
        this.onceOffDelay = reader.readUnsignedShortAsInt();
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeUnsignedShort(this.swayAngle);
        writer.writeUnsignedShort(this.swayDuration);
        writer.writeUnsignedShort(this.onceOffDelay);
        writer.writeUnsignedShort(0);
    }
}
