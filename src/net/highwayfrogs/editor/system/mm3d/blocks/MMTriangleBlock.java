package net.highwayfrogs.editor.system.mm3d.blocks;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;

/**
 * Describes triangular model vertices. Starts at zero.
 * Created by Kneesnap on 2/28/2019.
 */
public class MMTriangleBlock extends MMDataBlockBody {
    private int flags;
    private long v1;
    private long v2;
    private long v3;

    public static final int FLAG_HIDDEN = Constants.BIT_FLAG_0; // Set if hidden, clear if visible
    public static final int FLAG_SELECTED = Constants.BIT_FLAG_1; // Set if selected, clear if unselected

    public MMTriangleBlock(MisfitModel3DObject parent) {
        super(parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        this.v1 = reader.readUnsignedIntAsLong();
        this.v2 = reader.readUnsignedIntAsLong();
        this.v3 = reader.readUnsignedIntAsLong();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.flags);
        writer.writeUnsignedInt(this.v1);
        writer.writeUnsignedInt(this.v2);
        writer.writeUnsignedInt(this.v3);
    }
}
