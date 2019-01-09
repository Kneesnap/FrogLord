package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents MR_HILITE
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
public class MOFHilite extends GameObject {
    private HiliteType type;
    private short flags;
    private int index; // Index of prim. For instance, 2 is the third prim.
    private int targetPointer; //TODO: May be a pointer to a vertex, may be a MR_MPOLY_<???>
    private int primOffset;

    public static final int FLAG_VERTEX = Constants.BIT_FLAG_0;
    public static final int FLAG_PRIM = Constants.BIT_FLAG_1;

    @Override
    public void load(DataReader reader) {
        this.type = HiliteType.values()[reader.readUnsignedByteAsShort()];
        this.flags = reader.readUnsignedByteAsShort();
        this.index = reader.readUnsignedShortAsInt();
        this.targetPointer = reader.readInt();
        this.primOffset = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte((short) type.ordinal());
        writer.writeUnsignedByte(flags);
        writer.writeUnsignedShort(index);
        writer.writeInt(targetPointer);
        writer.writeInt(primOffset);
    }
}
