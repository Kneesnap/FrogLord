package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

/**
 * Describes triangular model vertices. Starts at zero.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
@Setter
public class MMTriangleFaceBlock extends MMDataBlockBody {
    private int flags;
    private long[] vertices = new long[3];

    public static final int FLAG_HIDDEN = Constants.BIT_FLAG_0; // Set if hidden, clear if visible
    public static final int FLAG_SELECTED = Constants.BIT_FLAG_1; // Set if selected, clear if unselected

    public MMTriangleFaceBlock(MisfitModel3DObject parent) {
        super(OffsetType.TRIANGLES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        readUnsignedIntArray(reader, this.vertices);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.flags);
        writeUnsignedIntArray(writer, this.vertices);
    }
}
