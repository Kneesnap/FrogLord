package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

import java.util.ArrayList;
import java.util.List;

/**
 * A group of triangles. Used to determine material.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
@Setter
public class MMTriangleGroupsBlock extends MMDataBlockBody {
    private short flags;
    private String name = "";
    private final List<Integer> triangleIndices = new ArrayList<>();
    private short smoothness = 0xFF;
    private int material = -1; // Index into material list.

    public static final int EMPTY_MATERIAL = -1;
    public static final short FLAG_HIDDEN = Constants.BIT_FLAG_0; // Set if hidden, clear if visible
    public static final short FLAG_SELECTED = Constants.BIT_FLAG_1; // Set if selected, clear if unselected

    public MMTriangleGroupsBlock(MisfitModel3DObject parent) {
        super(OffsetType.GROUPS, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        this.name = reader.readNullTerminatedString();

        int triangleCount = reader.readInt();
        for (int i = 0; i < triangleCount; i++)
            triangleIndices.add(reader.readInt());

        this.smoothness = reader.readUnsignedByteAsShort();
        this.material = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.flags);
        writer.writeNullTerminatedString(this.name);
        writer.writeInt(this.triangleIndices.size());
        for (int toWrite : this.triangleIndices)
            writer.writeInt(toWrite);
        writer.writeUnsignedByte(this.smoothness);
        writer.writeInt(this.material);
    }
}
