package net.highwayfrogs.editor.file.mof.animation.transform;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents 'MR_QUATB_TRANS'.
 * Created by Kneesnap on 1/5/2019.
 */
@Getter
public class MR_QUATB_TRANS extends TransformObject {
    private byte c; // 'real'.
    private byte x; // Presumably delta?
    private byte y;
    private byte z;
    private short[] transform = new short[3];

    @Override
    public void load(DataReader reader) {
        this.c = reader.readByte();
        this.x = reader.readByte();
        this.y = reader.readByte();
        this.z = reader.readByte();

        for (int i = 0; i < this.transform.length; i++)
            this.transform[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte(this.c);
        writer.writeByte(this.x);
        writer.writeByte(this.y);
        writer.writeByte(this.z);

        for (short aTransfer : this.transform)
            writer.writeShort(aTransfer);
    }
}
