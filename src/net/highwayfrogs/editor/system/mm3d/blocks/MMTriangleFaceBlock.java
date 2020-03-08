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
    private short flags;
    private int[] vertices = new int[3];

    public static final short FLAG_HIDDEN = Constants.BIT_FLAG_0; // Set if hidden, clear if visible
    public static final short FLAG_SELECTED = Constants.BIT_FLAG_1; // Set if selected, clear if unselected

    public MMTriangleFaceBlock(MisfitModel3DObject parent) {
        super(OffsetType.TRIANGLES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        readIntArray(reader, this.vertices);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.flags);
        writeIntArray(writer, this.vertices);
    }
}
