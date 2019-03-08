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
 * Represents a vertice.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
@Setter
public class MMVerticeBlock extends MMDataBlockBody {
    private int flags;
    private float x;
    private float y;
    private float z;

    public static final int FLAG_HIDDEN = Constants.BIT_FLAG_0; // Set if hidden, clear if visible
    public static final int FLAG_SELECTED = Constants.BIT_FLAG_1; // Set if selected, clear if unselected
    public static final int FLAG_FREE_VERTEX = Constants.BIT_FLAG_2; // Set if vertex does not have to be connected to a face (don't auto-delete when face is deleted)

    public MMVerticeBlock(MisfitModel3DObject parent) {
        super(OffsetType.VERTICES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        this.x = reader.readFloat();
        this.y = reader.readFloat();
        this.z = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.flags);
        writer.writeFloat(this.x);
        writer.writeFloat(this.y);
        writer.writeFloat(this.z);
    }
}
