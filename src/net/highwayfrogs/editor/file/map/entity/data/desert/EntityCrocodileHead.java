package net.highwayfrogs.editor.file.map.entity.data.desert;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityCrocodileHead extends GameObject {
    private PSXMatrix matrix = new PSXMatrix();
    private int riseHeight;
    private int riseSpeed;
    private int snapDelay;
    private int pauseDelay;
    private int snapOrNot;
    private int submergedDelay;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.riseHeight = reader.readUnsignedShortAsInt();
        this.riseSpeed = reader.readUnsignedShortAsInt();
        this.snapDelay = reader.readUnsignedShortAsInt();
        this.pauseDelay = reader.readUnsignedShortAsInt();
        this.snapOrNot = reader.readUnsignedShortAsInt();
        this.submergedDelay = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeUnsignedShort(this.riseHeight);
        writer.writeUnsignedShort(this.riseSpeed);
        writer.writeUnsignedShort(this.snapDelay);
        writer.writeUnsignedShort(this.pauseDelay);
        writer.writeUnsignedShort(this.snapOrNot);
        writer.writeUnsignedShort(this.submergedDelay);
    }
}
