package net.highwayfrogs.editor.file.map.entity.data.desert;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityFallingRock extends GameObject {
    private PSXMatrix matrix = new PSXMatrix();
    //TODO: DESERT FALLING ROCK TARGETS
    private int delay;
    private short bounceCount;
    private int flags;
    private int sound;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.delay = reader.readUnsignedShortAsInt();
        this.bounceCount = reader.readUnsignedByteAsShort();
        reader.readByte();
        this.flags = reader.readInt();
        this.sound = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeUnsignedShort(this.delay);
        writer.writeUnsignedByte(this.bounceCount);
        writer.writeByte(Constants.NULL_BYTE);
        writer.writeInt(this.flags);
        writer.writeInt(this.sound);
    }
}
