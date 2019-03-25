package net.highwayfrogs.editor.file.mof.hilite;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.List;

/**
 * Represents "MR_HILITE".
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
public class MOFHilite extends GameObject {
    private HiliteType type;
    private short flags; // Seems to always be 1, but the code supports more.
    private SVector vertex;
    private transient MOFPart parent;

    public static final int FLAG_VERTEX = Constants.BIT_FLAG_0;
    public static final int FLAG_PRIM = Constants.BIT_FLAG_1;

    public MOFHilite(MOFPart parent) {
        this.parent = parent;
    }

    @Override
    public void load(DataReader reader) {
        this.type = HiliteType.values()[reader.readUnsignedByteAsShort()];
        this.flags = reader.readUnsignedByteAsShort();
        this.vertex = getVertices().get(reader.readUnsignedShortAsInt());
        reader.skipInt(); // Runtime.
        reader.skipInt(); // Runtime.

        Utils.verify((this.flags & FLAG_VERTEX) == FLAG_VERTEX, "MOFHilite was not a vertex hilite!");
        Utils.verify((this.flags & FLAG_PRIM) != FLAG_PRIM, "MOFHilite was not a prim hilite!");
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte((short) type.ordinal());
        writer.writeUnsignedByte(flags);

        int saveId = getVertices().indexOf(getVertex());
        Utils.verify(saveId >= 0, "Invalid save ID, is the hilite vertex still registered?");
        writer.writeUnsignedShort(saveId);

        writer.writeNullPointer(); // Runtime.
        writer.writeNullPointer(); // Runtime.
    }

    private List<SVector> getVertices() {
        return getParent().getStaticPartcel().getVertices();
    }
}
