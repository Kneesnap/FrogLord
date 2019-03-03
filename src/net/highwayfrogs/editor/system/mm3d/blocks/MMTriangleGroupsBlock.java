package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A group of triangles. Used to determine material.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MMTriangleGroupsBlock extends MMDataBlockBody {
    private int flags;
    private String name;
    private List<Long> triangleIndices = new ArrayList<>();
    private short smoothness;
    private long material; // Index into material list, 0xFFFFFFFF = None.

    public static final int FLAG_HIDDEN = Constants.BIT_FLAG_0; // Set if hidden, clear if visible
    public static final int FLAG_SELECTED = Constants.BIT_FLAG_1; // Set if selected, clear if unselected

    public MMTriangleGroupsBlock(MisfitModel3DObject parent) {
        super(parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        this.name = reader.readNullTerminatedString();

        int triangleCount = reader.readInt();
        for (int i = 0; i < triangleCount; i++)
            triangleIndices.add(reader.readUnsignedIntAsLong());

        this.smoothness = reader.readUnsignedByteAsShort();
        this.material = reader.readUnsignedIntAsLong();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.flags);
        writer.writeTerminatorString(this.name);
        writer.writeInt(this.triangleIndices.size());
        for (Long toWrite : this.triangleIndices)
            writer.writeUnsignedInt(toWrite);
        writer.writeUnsignedByte(this.smoothness);
        writer.writeUnsignedInt(this.material);
    }
}
